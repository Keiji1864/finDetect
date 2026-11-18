package com.ghostteam.findetect.data.demo

import android.content.Context
import com.ghostteam.findetect.domain.model.SessionUser
import com.ghostteam.findetect.domain.model.PasswordValidation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
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

    @Serializable
    private data class Account(val user: SessionUser, val deleted: Boolean = false,
        val password: PasswordVerifier? = null, val authVersion: Long = 0)
    @Serializable
    private data class PasswordVerifier(val salt: String, val digest: String)
    @Serializable
    private data class Server(val accounts: Map<String, Account> = emptyMap(), val retiredIds: Set<String> = emptySet(),
        val deletedEmails: Set<String> = emptySet())

    data class AuthenticatedAccount(val user: SessionUser, val authVersion: Long)
    enum class PasswordChange { CHANGED, UNAUTHORIZED, CURRENT_PASSWORD_INVALID, SAME_PASSWORD, INVALID_NEW_PASSWORD }

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
        write(server.copy(accounts = server.accounts - entry.key, retiredIds = server.retiredIds + userId,
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
        write(server.copy(accounts = server.accounts + (entry.key to updated)))
        PasswordChange.CHANGED
    }

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
        fun initialId(email: String): String = UUID.nameUUIDFromBytes(email.toByteArray(Charsets.UTF_8)).toString()
    }
}
