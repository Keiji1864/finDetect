package com.ghostteam.findetect.data.auth

import com.ghostteam.findetect.data.remote.dto.AuthResponseDto
import com.ghostteam.findetect.data.remote.dto.TokenPairDto
import com.ghostteam.findetect.data.session.DevicePreferences
import com.ghostteam.findetect.data.session.UserDataScopes
import com.ghostteam.findetect.data.session.canonicalUserId
import com.ghostteam.findetect.core.network.SessionOwner
import com.ghostteam.findetect.domain.model.*
import com.ghostteam.findetect.domain.repository.AuthRepository
import dagger.Lazy
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

data class AccessCredentials(val identity: SessionIdentity, val token: String, val refreshRevision: Long)

/** Retains the latest attempt, including its failure, for requests sent before it completed. */
private class RefreshAttempt(
    val identity: SessionIdentity,
    val accessToken: String,
    val revision: Long,
    val result: Deferred<Unit>,
)

/** Serializes durable transitions. Network replies are accepted only for their original login instance. */
@Singleton
class SessionManager @Inject constructor(
    private val storage: TokenStorage,
    private val api: AuthApi,
    private val users: UserDataScopes,
    private val device: DevicePreferences,
    private val clock: Clock,
    private val protectedClient: Lazy<HttpClient>,
) : AuthRepository {
    private val transitions = Mutex()
    private val jobs = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Restoring)
    override val state = mutableState.asStateFlow()
    @Volatile private var record: StoredSession? = null
    private var generation = 0L
    private var restoreFinished = false
    // Read and changed only under onSession, together with the session record.
    private var refreshRevision = 0L
    private var refreshAttempt: RefreshAttempt? = null

    override suspend fun restore() = onSession {
        if (restoreFinished && state.value !is SessionState.StorageError) return@onSession
        val recovery = state.value as? SessionState.StorageError
        mutableState.value = SessionState.Restoring
        try {
            device.discardLegacyData()
            var saved = storage.read()
            record = saved
            val restored = saved
            if (restored?.mode == "active" && device.isSessionBlocked(checkNotNull(restored.sessionId))) {
                saved = StoredSession(mode = "reauth", user = restored.user)
                record = saved
            }
            saved?.cleanupUserId?.let { id ->
                users.deleteUserData(id)
                saved = StoredSession(mode = "signed_out")
                storage.write(checkNotNull(saved))
                record = saved
            }
            saved?.takeIf { it.mode == "active" }?.resetPinUserId?.let { id ->
                users.resetPin(id)
                saved = checkNotNull(saved).copy(resetPinUserId = null)
                storage.write(checkNotNull(saved))
            }
            record = saved
            when (saved?.mode) {
                "active" -> activate(checkNotNull(saved), false)
                "reauth" -> mutableState.value = SessionState.ReauthenticationRequired(saved?.user)
                else -> mutableState.value = SessionState.SignedOut
            }
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { mutableState.value = recovery ?: SessionState.StorageError(); users.close() }
        finally { restoreFinished = true }
    }

    override suspend fun login(email: String, password: String) {
        val attempt = onSession { check(restoreFinished); ++generation }
        val response = api.login(email.trim(), password)
        establish(response, false, attempt)
    }

    override suspend fun register(name: String, email: String, password: String) {
        val attempt = onSession { check(restoreFinished); ++generation }
        val response = api.register(name.trim(), email.trim(), password)
        try { establish(response, true, attempt) }
        catch (error: CancellationException) { throw error }
        catch (error: AuthException) {
            throw AuthException(error.reason, error.fieldErrors, error.retryAfterSeconds, accountCreated = true)
        } catch (_: Exception) { throw AuthException(AuthFailure.BAD_RESPONSE, accountCreated = true) }
    }

    private suspend fun establish(response: AuthResponseDto, registration: Boolean, attempt: Long) = onSession {
        if (attempt != generation) throw CancellationException("Superseded login")
        val id = canonicalUserId(response.user.id)
        require(response.user.email.isNotBlank() && response.user.name.isNotBlank())
        validateTokens(response.tokens)
        val resetPin = record?.resetPinUserId?.takeIf { it == id }
        var next = StoredSession(mode = "active", user = response.user.copy(id = id), sessionId = UUID.randomUUID().toString(),
            accessToken = response.tokens.accessToken, refreshToken = response.tokens.refreshToken,
            expiresAt = expiry(response.tokens), resetPinUserId = resetPin)
        try {
            withContext(NonCancellable) {
                record?.cleanupUserId?.let { users.deleteUserData(it) }
                // Persist registration progress before credentials, including a crash between these writes.
                users.saveOnboarding(id, users.onboarding(id, registration))
                storage.write(next, recoverAfterAuthentication = true)
                record = next
                mutableState.value = SessionState.Restoring
                users.close()
                if (resetPin != null) {
                    users.resetPin(id)
                    next = next.copy(resetPinUserId = null)
                    storage.write(next)
                }
                record = next
                activate(next, registration)
            }
        } catch (_: Exception) {
            mutableState.value = SessionState.StorageError(email = response.user.email, accountCreated = registration)
            users.close()
            throw AuthException(AuthFailure.STORAGE)
        }
    }

    private suspend fun activate(saved: StoredSession, registration: Boolean) {
        val user = checkNotNull(saved.user)
        val identity = SessionIdentity(canonicalUserId(user.id), checkNotNull(saved.sessionId))
        UUID.fromString(identity.sessionId)
        check(!saved.accessToken.isNullOrBlank() && !saved.refreshToken.isNullOrBlank() && saved.expiresAt != null)
        val step = users.onboarding(user.id, registration)
        users.saveOnboarding(user.id, step)
        val scope = users.open(identity)
        users.publish(scope)
        refreshRevision += 1
        refreshAttempt = null
        mutableState.value = SessionState.Active(identity, user, step)
    }

    suspend fun credentials(): AccessCredentials? = onSession { credentialsLocked() }

    private fun credentialsLocked(): AccessCredentials? {
        val current = state.value as? SessionState.Active ?: return null
        val saved = record ?: return null
        if (saved.sessionId != current.identity.sessionId) return null
        val token = saved.accessToken ?: return null
        val pending = refreshAttempt?.takeIf {
            it.identity == current.identity && it.accessToken == token && !it.result.isCompleted
        }
        return AccessCredentials(current.identity, token, pending?.revision ?: refreshRevision)
    }

    fun isCurrent(identity: SessionIdentity): Boolean = (state.value as? SessionState.Active)?.identity == identity

    suspend fun invalidate(identity: SessionIdentity) = onSession {
        if (isCurrent(identity)) expireLocked(resetPin = false)
    }

    suspend fun refreshAfterUnauthorized(used: AccessCredentials): AccessCredentials? {
        val attempt = onSession {
            val current = credentialsLocked()?.takeIf { it.identity == used.identity } ?: return@onSession null
            if (current.token != used.token) return@onSession null
            val previous = refreshAttempt
            if (previous != null && previous.identity == used.identity && previous.accessToken == used.token &&
                (previous.revision >= used.refreshRevision || !previous.result.isCompleted)) {
                return@onSession previous.result
            }

            // Owner, access token and refresh token are captured in one serialized transition.
            val refreshToken = checkNotNull(record?.refreshToken)
            val revision = refreshRevision
            val result = jobs.async(start = CoroutineStart.LAZY) {
                try {
                    rotateTokens(used.identity, refreshToken, revision)
                } finally {
                    withContext(NonCancellable) {
                        onSession {
                            if (isCurrent(used.identity) && refreshRevision == revision) refreshRevision += 1
                        }
                    }
                }
            }
            refreshAttempt = RefreshAttempt(used.identity, current.token, revision, result)
            result.start()
            result
        }
        // Awaiting a job owned by SessionManager never makes it a child of this HTTP request.
        attempt?.await()
        return onSession { credentialsLocked()?.takeIf { it.identity == used.identity } }
    }

    private suspend fun rotateTokens(owner: SessionIdentity, refreshToken: String, revision: Long) {
        val rotated = try { api.refresh(refreshToken) }
        catch (error: AuthException) {
            if (error.reason == AuthFailure.INVALID_CREDENTIALS) {
                withContext(NonCancellable) {
                    onSession { if (isCurrent(owner)) expireLocked(resetPin = false) }
                }
                return
            }
            throw error // Offline/429/5xx preserve credentials and local access.
        }
        // Protect the handoff to Main and the lock acquisition as well as the actual file write.
        withContext(NonCancellable) {
            onSession commit@{
                if (!isCurrent(owner)) return@commit
                validateTokens(rotated)
                val next = checkNotNull(record).copy(accessToken = rotated.accessToken, refreshToken = rotated.refreshToken,
                    expiresAt = expiry(rotated))
                try {
                    storage.write(next)
                    record = next
                    if (refreshRevision == revision) refreshRevision += 1
                } catch (_: Exception) {
                    // A rotated server token must never leave an old pair silently usable in memory.
                    preventCachedRestore(owner.sessionId)
                    refreshAttempt = null
                    mutableState.value = SessionState.StorageError()
                    users.close()
                    throw AuthException(AuthFailure.STORAGE)
                }
            }
        }
    }

    override suspend fun advanceOnboarding(step: OnboardingStep) = onSession {
        val current = state.value as? SessionState.Active ?: return@onSession
        withContext(NonCancellable) {
            users.saveOnboarding(current.user.id, step)
            mutableState.value = current.copy(onboarding = step)
        }
    }

    override suspend fun requireReauthentication() = onSession {
        if (state.value is SessionState.Active) expireLocked(resetPin = true)
    }

    private suspend fun expireLocked(resetPin: Boolean) = withContext(NonCancellable) {
        val previous = record ?: return@withContext
        ++generation
        val next = StoredSession(mode = "reauth", user = previous.user,
            resetPinUserId = if (resetPin) previous.user?.id else null)
        try {
            previous.sessionId?.let { preventCachedRestore(it) }
            storage.write(next)
            record = next
            refreshAttempt = null
            mutableState.value = SessionState.ReauthenticationRequired(next.user)
        } catch (_: Exception) { mutableState.value = SessionState.StorageError() }
        users.close()
    }

    override suspend fun logout(discardPendingChanges: Boolean): LogoutResult = onSession {
        val current = state.value as? SessionState.Active ?: return@onSession LogoutResult.DONE
        if (!discardPendingChanges && users.requireCurrent().hasPendingChanges()) return@onSession LogoutResult.NEEDS_CONFIRMATION
        val refresh = record?.refreshToken
        withContext(NonCancellable) {
            val journal = StoredSession(mode = "signed_out", cleanupUserId = current.user.id)
            try {
                preventCachedRestore(current.identity.sessionId)
                storage.write(journal)
                record = journal
                refreshAttempt = null
                ++generation
                mutableState.value = SessionState.Restoring
                users.close()
                users.deleteUserData(current.user.id)
                val done = StoredSession(mode = "signed_out")
                storage.write(done)
                record = done
                mutableState.value = SessionState.SignedOut
            } catch (_: Exception) {
                mutableState.value = SessionState.StorageError()
                users.close()
                throw AuthException(AuthFailure.STORAGE)
            }
        }
        if (refresh != null) jobs.launch {
            try { withTimeout(5_000) { api.logout(refresh) } }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { /* Local logout has already completed; never persist a revocation token in plaintext. */ }
        }
        LogoutResult.DONE
    }

    override suspend fun refreshUser() {
        val owner = credentials()?.identity ?: return
        try {
            val user = protectedClient.get().get("me") { attributes.put(SessionOwner, owner) }.body<SessionUser>()
            onSession {
                if (!isCurrent(owner)) return@onSession
                check(canonicalUserId(user.id) == owner.userId)
                val next = checkNotNull(record).copy(user = user)
                storage.write(next)
                record = next
                val active = state.value as SessionState.Active
                mutableState.value = active.copy(user = user)
            }
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { /* Cached user and offline access remain available. */ }
    }

    /** UI ownership changes are serialized on Main, including when a worker receives a 401. */
    private suspend fun <T> onSession(action: suspend () -> T): T = withContext(Dispatchers.Main.immediate) {
        transitions.withLock { action() }
    }

    private fun validateTokens(tokens: TokenPairDto) {
        require(tokens.accessToken.isNotBlank() && tokens.refreshToken.isNotBlank() && tokens.expiresIn in 1..31_536_000)
    }
    private suspend fun preventCachedRestore(sessionId: String) {
        // Independent fail-closed latch if the encrypted file cannot be replaced after a refusal/rotation.
        try { device.blockSession(sessionId) } catch (_: Exception) { /* The encrypted transition is still attempted. */ }
    }
    private fun expiry(tokens: TokenPairDto) = clock.now().toEpochMilliseconds() + tokens.expiresIn * 1_000
}
