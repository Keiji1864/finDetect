package com.ghostteam.findetect.core.security

import android.os.SystemClock
import com.ghostteam.findetect.data.session.UserDataScopes
import com.ghostteam.findetect.domain.repository.PinStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

enum class AppLockState { LOADING, UNLOCKED, LOCKED, ERROR }
data class UserLockState(val sessionId: String? = null, val lock: AppLockState = AppLockState.LOADING)

/** PIN access is process-local, belongs to one login, and expires after 30 seconds in background. */
@Singleton
class AppLockManager @Inject constructor(private val users: UserDataScopes) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var accessGranted = false
    private var owner: String? = null
    private var backgroundedAt: Long? = null
    private val mutableState = MutableStateFlow(UserLockState())
    val state = mutableState.asStateFlow()

    private fun updateState() {
        val current = users.current.value
        val sessionId = current?.identity?.sessionId
        if (owner != sessionId) { owner = sessionId; accessGranted = false }
        val next = when (current?.pin?.status?.value) {
            null, PinStatus.LOADING -> AppLockState.LOADING
            PinStatus.ERROR -> AppLockState.ERROR
            PinStatus.DISABLED -> AppLockState.UNLOCKED
            PinStatus.ENABLED -> if (accessGranted) AppLockState.UNLOCKED else AppLockState.LOCKED
        }
        mutableState.value = UserLockState(sessionId, next)
    }

    init {
        scope.launch {
            users.current.collectLatest { current ->
                updateState()
                current?.pin?.status?.collect { updateState() }
            }
        }
    }

    fun onActivityCreated(restored: Boolean) {
        if (!restored) { accessGranted = false; updateState() }
    }
    fun onForeground() {
        backgroundedAt?.let { if (SystemClock.elapsedRealtime() - it > 30_000) accessGranted = false }
        backgroundedAt = null
        updateState()
    }
    fun onBackground() { backgroundedAt = SystemClock.elapsedRealtime() }
    fun allowAccess(sessionId: String) {
        if (users.current.value?.identity?.sessionId != sessionId) return
        updateState()
        accessGranted = true
        updateState()
    }
    fun retry() { val pin = users.current.value?.pin ?: return; scope.launch { pin.refresh() } }
}
