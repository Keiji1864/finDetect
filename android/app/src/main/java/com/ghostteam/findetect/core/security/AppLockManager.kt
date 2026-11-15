package com.ghostteam.findetect.core.security

import android.os.SystemClock
import com.ghostteam.findetect.domain.repository.PinRepository
import com.ghostteam.findetect.domain.repository.PinStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

enum class AppLockState { LOADING, UNLOCKED, LOCKED, ERROR }

/** Access permission lives only in this process; background duration uses the monotonic clock. */
@Singleton
class AppLockManager @Inject constructor(private val pin: PinRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var accessGranted = false
    private var backgroundedAt: Long? = null
    private val mutableState = MutableStateFlow(AppLockState.LOADING)
    val state = mutableState.asStateFlow()

    private fun updateState() {
        mutableState.value = when (pin.status.value) {
            PinStatus.LOADING -> AppLockState.LOADING
            PinStatus.ERROR -> AppLockState.ERROR
            PinStatus.DISABLED -> AppLockState.UNLOCKED
            PinStatus.ENABLED -> if (accessGranted) AppLockState.UNLOCKED else AppLockState.LOCKED
        }
    }

    init {
        scope.launch { pin.status.collect { updateState() } }
        retry()
    }

    fun onActivityCreated(restored: Boolean) {
        // Starting a new task must lock even if the process survived removal of its previous activity.
        if (!restored) { accessGranted = false; updateState() }
    }

    fun onForeground() {
        backgroundedAt?.let { if (SystemClock.elapsedRealtime() - it > 30_000) accessGranted = false }
        backgroundedAt = null
        updateState()
    }

    fun onBackground() { backgroundedAt = SystemClock.elapsedRealtime() }
    fun allowAccess() { accessGranted = true; updateState() }
    fun retry() { scope.launch { pin.refresh() } }
}
