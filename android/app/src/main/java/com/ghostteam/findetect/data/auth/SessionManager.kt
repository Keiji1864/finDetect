package com.ghostteam.findetect.data.auth

import com.ghostteam.findetect.data.remote.dto.AuthResponseDto
import com.ghostteam.findetect.data.remote.dto.TokenPairDto
import com.ghostteam.findetect.data.session.DevicePreferences
import com.ghostteam.findetect.data.session.UserDataScopes
import com.ghostteam.findetect.data.session.canonicalUserId
import com.ghostteam.findetect.data.remote.ProfileApi
import com.ghostteam.findetect.domain.model.*
import com.ghostteam.findetect.domain.repository.AuthRepository
import com.ghostteam.findetect.domain.repository.ProfileRepository
import dagger.Lazy
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
    private val finance: Lazy<com.ghostteam.findetect.data.sync.FinanceSyncApi>,
    private val profileApi: Lazy<ProfileApi>,
) : AuthRepository, ProfileRepository {
    private val transitions = Mutex()
    private val profileWrites = Mutex()
    private val jobs = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Restoring)
    override val state = mutableState.asStateFlow()
    @Volatile private var record: StoredSession? = null
    private var financeJob: Job? = null
    private var generation = 0L
    private var restoreFinished = false
    // Read and changed only under onSession, together with the session record.
    private var refreshRevision = 0L
    private var refreshAttempt: RefreshAttempt? = null
    private var profileRevision = 0L

    override suspend fun restore() = withContext(NonCancellable) {
        onSession {
            if (restoreFinished && state.value !is SessionState.StorageError) return@onSession
            val recovery = state.value as? SessionState.StorageError
            mutableState.value = SessionState.Restoring
            try {
                device.discardLegacyData()
                val cleanupReceipts = device.pendingCleanups()
                cleanupReceipts.forEach { users.deleteUserData(it) }
                var saved = storage.read()
                record = saved
                if (saved?.user?.id?.let { it in cleanupReceipts } == true) {
                    saved = StoredSession(mode = "signed_out")
                    storage.write(checkNotNull(saved))
                    record = saved
                }
                val restored = saved
                if (restored?.mode == "active" && device.isSessionBlocked(checkNotNull(restored.sessionId))) {
                    saved = StoredSession(mode = "reauth", user = restored.user, cleanupUserId = restored.cleanupUserId)
                    record = saved
                }
                saved?.cleanupUserId?.let { id ->
                    users.deleteUserData(id)
                    val previous = checkNotNull(saved)
                    saved = if (previous.user?.id == id) StoredSession(mode = "signed_out") else previous.copy(cleanupUserId = null)
                    storage.write(checkNotNull(saved))
                    record = saved
                }
                saved?.takeIf { it.mode == "active" }?.resetPinUserId?.let { id ->
                    users.resetPin(id)
                    saved = checkNotNull(saved).copy(resetPinUserId = null)
                    storage.write(checkNotNull(saved))
                }
                record = saved
                device.finishCleanups(cleanupReceipts)
                when (saved?.mode) {
                    "active" -> activate(checkNotNull(saved), false)
                    "reauth" -> mutableState.value = SessionState.ReauthenticationRequired(saved?.user)
                    else -> mutableState.value = SessionState.SignedOut
                }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableState.value = recovery ?: SessionState.StorageError(); closeUserScope() }
            finally { restoreFinished = true }
        }
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
                val cleanupReceipts = device.pendingCleanups()
                cleanupReceipts.forEach { users.deleteUserData(it) }
                record?.cleanupUserId?.let { users.deleteUserData(it) }
                // Persist registration progress before credentials, including a crash between these writes.
                users.saveOnboarding(id, users.onboarding(id, registration))
                storage.write(next, recoverAfterAuthentication = true)
                record = next
                device.finishCleanups(cleanupReceipts)
                mutableState.value = SessionState.Restoring
                closeUserScope()
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
            closeUserScope()
            throw AuthException(AuthFailure.STORAGE)
        }
    }

    private suspend fun closeUserScope() {
        financeJob?.cancel()
        financeJob = null
        users.close()
    }

    private suspend fun activate(saved: StoredSession, registration: Boolean) {
        val user = checkNotNull(saved.user)
        val identity = SessionIdentity(canonicalUserId(user.id), checkNotNull(saved.sessionId))
        UUID.fromString(identity.sessionId)
        check(!saved.accessToken.isNullOrBlank() && !saved.refreshToken.isNullOrBlank() && saved.expiresAt != null)
        val step = users.onboarding(user.id, registration)
        users.saveOnboarding(user.id, step)
        val scope = users.open(identity)
        val remote = !com.ghostteam.findetect.BuildConfig.USE_MOCK_API
        if (remote) {
            try {
                scope.exchange.exchange { finance.get().initial(it,checkNotNull(saved.accessToken)) }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { scope.syncFailed(true) }
        }
        users.publish(scope)
        refreshRevision += 1
        profileRevision += 1
        refreshAttempt = null
        mutableState.value = SessionState.Active(identity, user, step)
        financeJob?.cancel()
        if (remote) financeJob = jobs.launch {
            while (isCurrent(identity)) {
                try {
                    scope.exchange.awaitChange()
                    delay(350)
                    if (!isCurrent(identity)) break
                    scope.exchange.exchange { finance.get().exchange(it,identity) }
                    scope.syncFailed(false)
                }
                catch (error: CancellationException) { throw error }
                catch (_: Exception) { scope.syncFailed(true); delay(15_000) }
            }
        }
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
                    closeUserScope()
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
            resetPinUserId = if (resetPin) previous.user?.id else null, cleanupUserId = previous.cleanupUserId)
        try {
            previous.sessionId?.let { preventCachedRestore(it) }
            storage.write(next)
            record = next
            refreshAttempt = null
            mutableState.value = SessionState.ReauthenticationRequired(next.user)
        } catch (_: Exception) { mutableState.value = SessionState.StorageError() }
        closeUserScope()
    }

    override suspend fun logout(discardPendingChanges: Boolean): LogoutResult {
        val scope = users.current.value ?: return LogoutResult.DONE
        val remote = !com.ghostteam.findetect.BuildConfig.USE_MOCK_API
        if (remote && !discardPendingChanges) {
            try { scope.exchange.exchange { finance.get().exchange(it,scope.identity) } }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { /* Ask before discarding any unconfirmed local changes. */ }
        }
        return onSession {
            if (!isCurrent(scope.identity)) return@onSession LogoutResult.DONE
            val current = state.value as? SessionState.Active ?: return@onSession LogoutResult.DONE
            if (!discardPendingChanges && (if (remote) scope.exchange.pending() else scope.hasPendingChanges())) return@onSession LogoutResult.NEEDS_CONFIRMATION
            val refresh = record?.refreshToken
            clearLocalSessionLocked(current.user.id)
            if (refresh != null) jobs.launch {
                try { withTimeout(5_000) { api.logout(refresh) } }
                catch (error: CancellationException) { throw error }
                catch (_: Exception) { /* Local logout has already completed. */ }
            }
            LogoutResult.DONE
        }
    }

    /** Used by logout and by a confirmed DELETE, never by an unconfirmed network failure. */
    private suspend fun clearLocalSessionLocked(userId: String, keepAuthScreen: Boolean = false) {
        withContext(NonCancellable) {
            val journal = StoredSession(mode = "signed_out", cleanupUserId = userId)
            try {
                drainOtherCleanupLocked(userId)
                record?.sessionId?.let { preventCachedRestore(it) }
                rememberCleanupSafely(userId)
                storage.write(journal)
                record = journal
                refreshAttempt = null
                profileRevision += 1
                if (!keepAuthScreen) {
                    ++generation
                    mutableState.value = SessionState.Restoring
                }
                if (users.current.value?.identity?.userId == userId) closeUserScope()
                users.deleteUserData(userId)
                val done = StoredSession(mode = "signed_out")
                storage.write(done)
                record = done
                device.finishCleanups(setOf(userId))
                mutableState.value = SessionState.SignedOut
            } catch (_: Exception) {
                mutableState.value = SessionState.StorageError()
                if (users.current.value?.identity?.userId == userId) closeUserScope()
                throw AuthException(AuthFailure.STORAGE)
            }
        }
    }

    override suspend fun refreshUser() {
        val owner = credentials()?.identity ?: return
        try { refresh(owner) }
        catch (error: CancellationException) { throw error }
        catch (_: Exception) { /* Cached user and offline access remain available. */ }
    }

    override suspend fun refresh(owner: SessionIdentity) {
        val revision = onSession { requireOwnerLocked(owner); profileRevision }
        val user = try { profileApi.get().read(owner) }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            val relevant = onSession { requireOwnerLocked(owner); profileRevision == revision }
            if (relevant) throw error else return
        }
        withContext(NonCancellable) {
            onSession {
                requireOwnerLocked(owner)
                if (profileRevision == revision) persistUserLocked(owner, user)
            }
        }
    }

    override suspend fun saveName(owner: SessionIdentity, name: String) {
        val normalized = name.trim()
        if (UserNameValidation.error(normalized) != null) throw ProfileException(ProfileFailure.INVALID_NAME)
        jobs.async {
            profileWrites.withLock {
                onSession { requireOwnerLocked(owner); profileRevision += 1 }
                val user = profileApi.get().updateName(owner, normalized)
                withContext(NonCancellable) {
                    onSession {
                        requireOwnerLocked(owner)
                        if (user.name.trim() != normalized) throw ProfileException(ProfileFailure.BAD_RESPONSE)
                        persistUserLocked(owner, user)
                        // Even an idempotent save invalidates GETs which began during the PATCH.
                        profileRevision += 1
                    }
                }
            }
        }.await()
    }

    override suspend fun deleteAccount(owner: SessionIdentity) {
        jobs.async {
            profileWrites.withLock {
                onSession { requireOwnerLocked(owner); profileRevision += 1 }
                profileApi.get().deleteAccount(owner) // Only 204 reaches the cleanup below.
                withContext(NonCancellable) {
                    onSession {
                        val current = state.value as? SessionState.Active
                        if (record?.user?.id == owner.userId || current?.user?.id == owner.userId) {
                            // A newer login to the same server account is also invalid after its deletion.
                            clearLocalSessionLocked(owner.userId, keepAuthScreen = current == null)
                        } else {
                            // A late receipt for A may clean A's files but must preserve B's session.
                            drainOtherCleanupLocked(owner.userId)
                            val base = record ?: StoredSession(mode = "signed_out")
                            val journal = base.copy(cleanupUserId = owner.userId)
                            rememberCleanupSafely(owner.userId)
                            storage.write(journal)
                            record = journal
                            users.deleteUserData(owner.userId)
                            val done = journal.copy(cleanupUserId = null)
                            storage.write(done)
                            record = done
                            device.finishCleanups(setOf(owner.userId))
                        }
                    }
                }
            }
        }.await()
    }

    private suspend fun drainOtherCleanupLocked(target: String) {
        val receipts = device.pendingCleanups() - target
        receipts.forEach { users.deleteUserData(it) }
        val pending = record?.cleanupUserId?.takeIf { it != target }
        if (pending != null) {
            users.deleteUserData(pending)
            val done = checkNotNull(record).copy(cleanupUserId = null)
            storage.write(done)
            record = done
        }
        device.finishCleanups(receipts + listOfNotNull(pending))
    }

    private suspend fun rememberCleanupSafely(userId: String) {
        // Independent receipt lets startup finish a confirmed cleanup if writing the encrypted journal fails.
        try { device.rememberCleanup(userId) } catch (_: Exception) { /* The encrypted journal is still attempted. */ }
    }

    private fun requireOwnerLocked(owner: SessionIdentity) {
        if (!isCurrent(owner) || record?.sessionId != owner.sessionId) throw CancellationException("Profile session changed")
    }

    private suspend fun persistUserLocked(owner: SessionIdentity, user: SessionUser) {
        val current = state.value as SessionState.Active
        val id = runCatching { canonicalUserId(user.id) }.getOrNull()
        if (id != owner.userId || !user.email.equals(current.user.email, ignoreCase = true) ||
            UserNameValidation.error(user.name) != null) throw ProfileException(ProfileFailure.BAD_RESPONSE)
        val normalized = user.copy(id = owner.userId, name = user.name.trim(), email = current.user.email)
        if (normalized == current.user) return
        val next = checkNotNull(record).copy(user = normalized)
        try { storage.write(next) }
        catch (_: Exception) { throw ProfileException(ProfileFailure.STORAGE) }
        record = next
        profileRevision += 1
        mutableState.value = current.copy(user = normalized)
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
