package com.ghostteam.findetect.feature.password

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.domain.model.*
import com.ghostteam.findetect.domain.repository.AuthRepository
import com.ghostteam.findetect.domain.repository.PasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmation: String = "",
    val working: Boolean = false,
    val fieldErrors: Map<PasswordField, PasswordFieldError> = emptyMap(),
    val error: PasswordFailure? = null,
    val retrySeconds: Int = 0,
    val outcomeUnknown: Boolean = false,
) {
    val submissionBlocked: Boolean get() = working || retrySeconds > 0 || outcomeUnknown
}

/** Password drafts survive configuration changes in memory only, never in saved instance state. */
@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val passwords: PasswordRepository,
    private val auth: AuthRepository,
    private val owner: SessionIdentity,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ChangePasswordUiState())
    val state = mutableState.asStateFlow()
    private var cooldown: Job? = null

    init {
        viewModelScope.launch {
            auth.state.collect {
                if (!current()) {
                    cooldown?.cancel()
                    mutableState.value = ChangePasswordUiState()
                }
            }
        }
    }

    private fun current() = (auth.state.value as? SessionState.Active)?.identity == owner

    fun edit(field: PasswordField, value: String) {
        if (!current() || state.value.working || state.value.outcomeUnknown) return
        val cleared = when (field) {
            PasswordField.NEW_PASSWORD -> setOf(field, PasswordField.CONFIRMATION)
            PasswordField.CURRENT_PASSWORD -> setOf(field, PasswordField.NEW_PASSWORD)
            PasswordField.CONFIRMATION -> setOf(field)
            else -> return
        }
        mutableState.update {
            val edited = when (field) {
                PasswordField.CURRENT_PASSWORD -> it.copy(currentPassword = value)
                PasswordField.NEW_PASSWORD -> it.copy(newPassword = value)
                else -> it.copy(confirmation = value)
            }
            edited.copy(fieldErrors = it.fieldErrors - cleared,
                error = it.error.takeIf { failure -> failure == PasswordFailure.RATE_LIMITED && it.retrySeconds > 0 })
        }
    }

    fun submit() {
        val form = state.value
        if (!current() || form.submissionBlocked) return
        val errors = PasswordValidation.change(form.currentPassword, form.newPassword, form.confirmation)
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(fieldErrors = errors, error = PasswordFailure.VALIDATION) }
            return
        }
        mutableState.update { it.copy(working = true, fieldErrors = emptyMap(), error = null) }
        viewModelScope.launch {
            try { passwords.changePassword(owner, form.currentPassword, form.newPassword) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: PasswordException) {
                if (current()) {
                    mutableState.update {
                        it.copy(error = error.reason, fieldErrors = error.fieldErrors.filterKeys { field ->
                            field in setOf(PasswordField.CURRENT_PASSWORD, PasswordField.NEW_PASSWORD, PasswordField.CONFIRMATION)
                        }, outcomeUnknown = error.outcomeUnknown,
                            currentPassword = if (error.outcomeUnknown) "" else it.currentPassword,
                            newPassword = if (error.outcomeUnknown) "" else it.newPassword,
                            confirmation = if (error.outcomeUnknown) "" else it.confirmation)
                    }
                    if (error.reason == PasswordFailure.RATE_LIMITED) startCooldown(error.retryAfterSeconds)
                }
            } catch (_: Exception) {
                if (current()) mutableState.value = ChangePasswordUiState(error = PasswordFailure.UNAVAILABLE, outcomeUnknown = true)
            } finally { if (current()) mutableState.update { it.copy(working = false) } }
        }
    }

    fun openLogin() {
        if (!current() || state.value.working || !state.value.outcomeUnknown) return
        mutableState.update { it.copy(working = true) }
        viewModelScope.launch {
            try { passwords.signInAfterUnconfirmedChange(owner) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (current()) mutableState.update { it.copy(error = PasswordFailure.STORAGE) }
            } finally { if (current()) mutableState.update { it.copy(working = false) } }
        }
    }

    private fun startCooldown(seconds: Long?) {
        cooldown?.cancel()
        if (seconds == null || seconds <= 0) return
        val deadline = SystemClock.elapsedRealtime() + seconds.coerceAtMost(Int.MAX_VALUE.toLong()) * 1_000
        fun remaining() = ((deadline - SystemClock.elapsedRealtime() + 999) / 1_000).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        mutableState.update { it.copy(retrySeconds = remaining()) }
        cooldown = viewModelScope.launch {
            while (state.value.retrySeconds > 0) {
                delay(1_000)
                val left = remaining()
                mutableState.update { it.copy(retrySeconds = left,
                    error = if (left == 0 && it.error == PasswordFailure.RATE_LIMITED) null else it.error) }
            }
        }
    }

    override fun onCleared() {
        mutableState.value = ChangePasswordUiState()
        super.onCleared()
    }
}
