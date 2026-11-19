package com.ghostteam.findetect.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.domain.model.*
import com.ghostteam.findetect.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserSessionUiState(val working: Boolean = false, val confirmLogout: Boolean = false, val error: Boolean = false)

/** Actions from an obsolete navigation owner cannot act on a later login. */
@HiltViewModel
class UserSessionViewModel @Inject constructor(private val auth: AuthRepository, private val owner: SessionIdentity,
    private val scope: com.ghostteam.findetect.data.session.UserDataScope) : ViewModel() {
    private val mutableState = MutableStateFlow(UserSessionUiState())
    val syncFailed = scope.syncFailed
    val state = mutableState.asStateFlow()
    private fun current() = (auth.state.value as? SessionState.Active)?.identity == owner

    fun saveIncome(dateMillis: Long, repeatIndex: Int, onSaved: () -> Unit) = run {
        val date = kotlinx.datetime.Instant.fromEpochMilliseconds(dateMillis).toString().substringBefore('T')
        val repeat = listOf("MONTHLY", "BIWEEKLY", "WEEKLY", "ONCE")[repeatIndex]
        scope.gate.use {
            scope.database.personalSettingsDao().income(date, repeat, kotlinx.datetime.Clock.System.now().toString())
        }
        auth.advanceOnboarding(OnboardingStep.PIN)
        if (current()) onSaved()
    }

    fun advance(step: OnboardingStep, onSaved: () -> Unit) = run {
        auth.advanceOnboarding(step)
        if (current()) onSaved()
    }
    fun reauthenticate() = run { auth.requireReauthentication() }
    fun logout(confirmed: Boolean = false) = run {
        val result = auth.logout(confirmed)
        if (current()) mutableState.value = UserSessionUiState(confirmLogout = result == LogoutResult.NEEDS_CONFIRMATION)
    }
    fun dismissLogout() { mutableState.value = UserSessionUiState() }

    private fun run(action: suspend () -> Unit) {
        if (!current() || state.value.working) return
        mutableState.value = UserSessionUiState(working = true)
        viewModelScope.launch {
            try { if (current()) action(); if (current()) mutableState.value = mutableState.value.copy(working = false) }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { if (current()) mutableState.value = UserSessionUiState(error = true) }
        }
    }
}
