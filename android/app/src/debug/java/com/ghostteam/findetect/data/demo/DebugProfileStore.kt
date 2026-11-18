package com.ghostteam.findetect.data.demo

import android.content.Context
import android.util.Base64
import com.ghostteam.findetect.domain.model.SessionUser
import com.ghostteam.findetect.domain.model.PasswordValidation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Debug server state survives client logout; stores salted password verifiers and token epochs, never plaintext passwords/tokens. */
@Singleton
class DebugProfileStore @Inject constructor(@ApplicationContext context: Context, private val json: Json) {
    private val preferences = context.getSharedPreferences("debug_profile_server_v1", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    // A mock mailbox lives only in memory, outside persisted client or server state.
    private val mailbox = mutableMapOf<String, String>()

    @Serializable
    private data class Account(val user: SessionUser, val deleted: Boolean = false,
        val password: PasswordVerifier? = null, val authVersion: Long = 0)
    @Serializable
    private data class PasswordVerifier(val salt: String, val digest: String)
    @Serializable
    private data class Server(val accounts: Map<String, Account> = emptyMap(), val retiredIds: Set<String> = emptySet(),
        val deletedEmails: Set<String> = emptySet(),
        val resetChallenges: Map<String, ResetChallenge> = emptyMap(),
        val resetGrants: Map<String, ResetGrant> = emptyMap(),
        val resetLimits: Map<String, ResetLimit> = emptyMap(),
        val resetCodeKey: String? = null,
        val requestTimes: List<Long> = emptyList(), val verifyTimes: List<Long> = emptyList())
    @Serializable
    private data class ResetChallenge(val emailKey: String, val userId: String?, val authVersion: Long?,
        val codeDigest: String, val expiresAt: Long, val scenario: String = "", val consumed: Boolean = false)
    @Serializable
    private data class ResetGrant(val userId: String, val authVersion: Long, val expiresAt: Long, val scenario: String = "")
    @Serializable
    private data class ResetLimit(val sends: List<Long> = emptyList(), val failures: List<Long> = emptyList())

    data class AuthenticatedAccount(val user: SessionUser, val authVersion: Long)
    enum class PasswordChange { CHANGED, UNAUTHORIZED, CURRENT_PASSWORD_INVALID, SAME_PASSWORD, INVALID_NEW_PASSWORD }
    data class RecoveryReply(val status: Int, val error: String? = null, val requestId: String? = null,
        val token: String? = null, val retrySeconds: Int? = null)

    suspend fun login(email: String, password: String, defaultName: String): AuthenticatedAccount? = access { server ->
        val key = email.trim().lowercase()
        if (emailHash(key) in server.deletedEmails) return@access null
        val existing = server.accounts[key]
        if (existing != null) {
            if (existing.deleted || !matches(existing.password, password)) return@access null
            // Profiles created by earlier fixtures acquire a verifier without losing their UUID or data.
            if (existing.password == null) write(server.copy(accounts = server.accounts +
                (key to existing.copy(password = verifier(password)))))
            return@access AuthenticatedAccount(existing.user, existing.authVersion)
        }
        if (password != DEFAULT_PASSWORD) return@access null
        val user = SessionUser(initialId(key), key, defaultName)
        write(server.copy(accounts = server.accounts + (key to Account(user, password = verifier(password)))))
        AuthenticatedAccount(user, 0)
    }

    suspend fun register(email: String, name: String, password: String): AuthenticatedAccount? = access { server ->
        val key = email.trim().lowercase()
        val existing = server.accounts[key]
        if (existing != null && !existing.deleted) return@access null
        val user = SessionUser(UUID.randomUUID().toString(), key, name.trim())
        write(server.copy(accounts = server.accounts + (key to Account(user, password = verifier(password))),
            deletedEmails = server.deletedEmails - emailHash(key)))
        AuthenticatedAccount(user, 0)
    }

    suspend fun authorize(tokenUser: SessionUser, authVersion: Long): SessionUser? = access { server ->
        val key = tokenUser.email.trim().lowercase()
        if (tokenUser.id in server.retiredIds) return@access null
        if (emailHash(key) in server.deletedEmails) return@access null
        val existing = server.accounts[key]
        if (existing != null) return@access existing.user.takeIf {
            !existing.deleted && it.id == tokenUser.id && existing.authVersion == authVersion
        }
        if (authVersion != 0L) return@access null
        // Adopt tokens issued by the previous stateless fixtures without resetting their user ID/name.
        val user = tokenUser.copy(email = key)
        write(server.copy(accounts = server.accounts + (key to Account(user))))
        user
    }

    suspend fun updateName(userId: String, authVersion: Long, name: String): SessionUser? = access { server ->
        val entry = server.accounts.entries.firstOrNull {
            !it.value.deleted && it.value.user.id == userId && it.value.authVersion == authVersion
        } ?: return@access null
        val user = entry.value.user.copy(name = name)
        write(server.copy(accounts = server.accounts + (entry.key to entry.value.copy(user = user))))
        user
    }

    suspend fun delete(userId: String, authVersion: Long): Boolean = access { server ->
        val entry = server.accounts.entries.firstOrNull {
            !it.value.deleted && it.value.user.id == userId && it.value.authVersion == authVersion
        } ?: return@access false
        write(revokeRecovery(server, userId).copy(accounts = server.accounts - entry.key, retiredIds = server.retiredIds + userId,
            deletedEmails = server.deletedEmails + emailHash(entry.key)))
        true
    }

    /** Password verification, replacement and revocation share one durable, serialized server write. */
    suspend fun changePassword(userId: String, authVersion: Long, current: String, new: String): PasswordChange = access { server ->
        val entry = server.accounts.entries.firstOrNull {
            !it.value.deleted && it.value.user.id == userId && it.value.authVersion == authVersion
        } ?: return@access PasswordChange.UNAUTHORIZED
        if (!matches(entry.value.password, current)) return@access PasswordChange.CURRENT_PASSWORD_INVALID
        if (new.length !in PasswordValidation.MIN_LENGTH..PasswordValidation.MAX_LENGTH)
            return@access PasswordChange.INVALID_NEW_PASSWORD
        if (matches(entry.value.password, new)) return@access PasswordChange.SAME_PASSWORD
        val updated = entry.value.copy(password = verifier(new), authVersion = Math.addExact(entry.value.authVersion, 1))
        write(revokeRecovery(server, userId).copy(accounts = server.accounts + (entry.key to updated)))
        PasswordChange.CHANGED
    }

    /** Same limits and public response for real and synthetic accounts. No account is created for unknown mail. */
    suspend fun requestPasswordReset(email: String): RecoveryReply = access { original ->
        val now = System.currentTimeMillis()
        val key = email.trim().lowercase()
        val emailKey = emailHash(key)
        var server = original
        // Named demo fixtures can be recovered before their first login. Ordinary unknown addresses stay synthetic.
        if (key in RECOVERY_DEMOS && key !in server.accounts && emailHash(key) !in server.deletedEmails) {
            val user = SessionUser(initialId(key), key, if (key == "demo-b@example.com") "Ирина" else "Алексей")
            server = server.copy(accounts = server.accounts + (key to Account(user, password = verifier(DEFAULT_PASSWORD))))
        }
        val oldLimit = server.resetLimits[emailKey] ?: ResetLimit()
        val sends = oldLimit.sends.filter { it > now - HOUR }
        val failures = oldLimit.failures.filter { it > now - TEN_MINUTES }
        val ipRequests = server.requestTimes.filter { it > now - MINUTE }
        val waitUntil = maxOf(sends.lastOrNull()?.plus(MINUTE) ?: now,
            if (sends.size >= 5) sends.first() + HOUR else now,
            if (ipRequests.size >= 30) ipRequests.first() + MINUTE else now)
        if (waitUntil > now) return@access RecoveryReply(429, "RATE_LIMITED", retrySeconds = seconds(waitUntil, now))
        val requestId = UUID.randomUUID().toString()
        val codeKey = server.resetCodeKey ?: ByteArray(32).also { SecureRandom().nextBytes(it) }.toHex()
        val previousCodes = server.resetChallenges.filterValues { it.emailKey == emailKey && !it.consumed }
        val random = SecureRandom()
        var code: String
        do { code = random.nextInt(1_000_000).toString().padStart(6, '0') }
        while (previousCodes.any { (id, value) -> codeDigest(codeKey, id, code) == value.codeDigest })
        val account = server.accounts[key]?.takeUnless { it.deleted }
        val scenario = key.substringBefore('@').takeIf { key in RECOVERY_DEMOS }.orEmpty()
        val challenge = ResetChallenge(emailKey, account?.user?.id, account?.authVersion,
            codeDigest(codeKey, requestId, code), if (scenario == "reset-code-expired") now - 1 else now + TEN_MINUTES, scenario)
        val challenges = server.resetChallenges.filterValues { it.emailKey != emailKey && it.expiresAt > now - TEN_MINUTES } +
            (requestId to challenge)
        mailbox.keys.retainAll(challenges.keys)
        mailbox[requestId] = code
        write(server.copy(resetCodeKey = codeKey,
            resetChallenges = challenges,
            resetGrants = server.resetGrants.filterValues { it.expiresAt > now },
            resetLimits = server.resetLimits + (emailKey to ResetLimit(sends + now, failures)),
            requestTimes = ipRequests + now))
        RecoveryReply(202, requestId = requestId)
    }

    suspend fun verifyPasswordReset(requestId: String, code: String): RecoveryReply = access { server ->
        val now = System.currentTimeMillis()
        val ipChecks = server.verifyTimes.filter { it > now - MINUTE }
        if (ipChecks.size >= 60) return@access RecoveryReply(429, "RATE_LIMITED", retrySeconds = seconds(ipChecks.first() + MINUTE, now))
        val checked = server.copy(verifyTimes = ipChecks + now)
        val challenge = server.resetChallenges[requestId]
        if (challenge == null) {
            write(checked)
            return@access RecoveryReply(400, "RESET_CODE_INVALID")
        }
        val oldLimit = server.resetLimits[challenge.emailKey] ?: ResetLimit()
        val failures = oldLimit.failures.filter { it > now - TEN_MINUTES }
        if (failures.size >= 5) {
            write(checked)
            return@access RecoveryReply(429, "RATE_LIMITED", retrySeconds = seconds(failures.first() + TEN_MINUTES, now))
        }
        if (!challenge.consumed && challenge.expiresAt <= now) {
            write(checked)
            return@access RecoveryReply(400, "RESET_CODE_EXPIRED")
        }
        val account = server.accounts.values.firstOrNull {
            !it.deleted && it.user.id == challenge.userId && it.authVersion == challenge.authVersion
        }
        val expected = challenge.codeDigest.toByteArray(Charsets.US_ASCII)
        val actual = codeDigest(checkNotNull(server.resetCodeKey), requestId, code).toByteArray(Charsets.US_ASCII)
        if (challenge.consumed || account == null || !MessageDigest.isEqual(expected, actual)) {
            val updatedFailures = failures + now
            write(checked.copy(resetLimits = server.resetLimits +
                (challenge.emailKey to oldLimit.copy(failures = updatedFailures))))
            return@access if (updatedFailures.size >= 5) RecoveryReply(429, "RATE_LIMITED",
                retrySeconds = seconds(updatedFailures.first() + TEN_MINUTES, now)) else RecoveryReply(400, "RESET_CODE_INVALID")
        }
        val token = Base64.encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) },
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val grant = ResetGrant(account.user.id, account.authVersion,
            if (challenge.scenario == "reset-token-expired") now - 1 else now + TEN_MINUTES, challenge.scenario)
        write(checked.copy(resetChallenges = server.resetChallenges + (requestId to challenge.copy(consumed = true)),
            resetGrants = server.resetGrants + (emailHash(token) to grant)))
        mailbox.remove(requestId)
        RecoveryReply(200, token = token)
    }

    /** Password, token consumption and revocation are committed atomically with the account epoch. */
    suspend fun completePasswordReset(token: String, new: String): RecoveryReply = access { server ->
        val grant = server.resetGrants[emailHash(token)] ?: return@access RecoveryReply(400, "RESET_TOKEN_INVALID")
        if (grant.expiresAt <= System.currentTimeMillis()) return@access RecoveryReply(400, "RESET_TOKEN_INVALID")
        val entry = server.accounts.entries.firstOrNull {
            !it.value.deleted && it.value.user.id == grant.userId && it.value.authVersion == grant.authVersion
        } ?: return@access RecoveryReply(400, "RESET_TOKEN_INVALID")
        if (new.length !in PasswordValidation.MIN_LENGTH..PasswordValidation.MAX_LENGTH)
            return@access RecoveryReply(400, "VALIDATION_ERROR")
        if (matches(entry.value.password, new)) return@access RecoveryReply(400, "NEW_PASSWORD_SAME_AS_OLD")
        val updated = entry.value.copy(password = verifier(new), authVersion = Math.addExact(entry.value.authVersion, 1))
        write(revokeRecovery(server, grant.userId).copy(accounts = server.accounts + (entry.key to updated)))
        RecoveryReply(204)
    }

    suspend fun recoveryScenario(requestId: String? = null, token: String? = null): String = access { server ->
        if (requestId != null) server.resetChallenges[requestId]?.scenario.orEmpty()
        else token?.let { server.resetGrants[emailHash(it)]?.scenario }.orEmpty()
    }

    suspend fun previewResetCode(requestId: String): String? = access { mailbox[requestId] }

    private fun revokeRecovery(server: Server, userId: String): Server {
        server.resetChallenges.filterValues { it.userId == userId }.keys.forEach(mailbox::remove)
        return server.copy(resetChallenges = server.resetChallenges.filterValues { it.userId != userId },
            resetGrants = server.resetGrants.filterValues { it.userId != userId })
    }

    private fun codeDigest(key: String, requestId: String, code: String): String = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key.toByteArray(Charsets.US_ASCII), "HmacSHA256"))
        doFinal("$requestId\u0000$code".toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun seconds(deadline: Long, now: Long) = ((deadline - now + 999) / 1_000).coerceAtLeast(1).toInt()

    private fun matches(verifier: PasswordVerifier?, candidate: String): Boolean {
        if (verifier == null) return candidate == DEFAULT_PASSWORD // Upgrade of existing debug profiles.
        val expected = verifier.digest.toByteArray(Charsets.US_ASCII)
        return MessageDigest.isEqual(expected, derive(candidate, verifier.salt).toByteArray(Charsets.US_ASCII))
    }

    private fun verifier(password: String): PasswordVerifier {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
        return PasswordVerifier(salt, derive(password, salt))
    }

    private fun derive(password: String, salt: String): String {
        // Available on minSdk 24. This local verifier belongs to the debug mock, not the production backend.
        val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(Charsets.US_ASCII), 120_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded.toHex() }
        finally { spec.clearPassword() }
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private suspend fun <T> access(block: (Server) -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            val stored = preferences.getString("server", null)
            block(if (stored == null) Server() else json.decodeFromString<Server>(stored))
        }
    }
    private fun write(server: Server) { check(preferences.edit().putString("server", json.encodeToString(server)).commit()) }
    private fun emailHash(email: String): String = MessageDigest.getInstance("SHA-256").digest(email.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    companion object {
        const val DEFAULT_PASSWORD = "password123"
        private const val MINUTE = 60_000L
        private const val TEN_MINUTES = 10 * MINUTE
        private const val HOUR = 60 * MINUTE
        private val RECOVERY_DEMOS = setOf("demo-a", "demo-b", "reset-code-expired", "reset-token-expired",
            "reset-request-validation", "reset-request-validation-unknown", "reset-request-rate-limit", "reset-request-rate-limit-no-header",
            "reset-request-server", "reset-request-offline", "reset-request-slow", "reset-request-lost", "reset-request-malformed",
            "reset-verify-server", "reset-verify-offline", "reset-verify-slow", "reset-verify-lost", "reset-verify-malformed",
            "reset-complete-validation", "reset-complete-validation-unknown", "reset-complete-rate-limit", "reset-complete-rate-limit-no-header",
            "reset-complete-server", "reset-complete-offline", "reset-complete-slow", "reset-complete-lost", "reset-complete-timeout",
            "reset-complete-unexpected-success", "reset-token-invalid").map { "$it@example.com" }.toSet()
        fun initialId(email: String): String = UUID.nameUUIDFromBytes(email.toByteArray(Charsets.UTF_8)).toString()
    }
}
