package com.ghostteam.findetect.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import android.os.SystemClock
import com.ghostteam.findetect.domain.model.*
import com.ghostteam.findetect.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val working: Boolean = false,
    val error: AuthFailure? = null,
    val fieldErrors: Map<AuthField, AuthFieldError> = emptyMap(),
    val retrySeconds: Int = 0,
    val accountCreated: Boolean = false,
    val registrationMayExist: Boolean = false,
) {
    val submissionBlocked: Boolean get() = working || retrySeconds > 0 || accountCreated
    val offerLogin: Boolean get() = accountCreated || registrationMayExist
}

@HiltViewModel
class AuthViewModel @Inject constructor(private val auth: AuthRepository, private val savedState: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState(accountCreated = savedState.get<Boolean>(ACCOUNT_CREATED) == true))
    val state = mutableState.asStateFlow()
    private var cooldown: Job? = null

    fun login(email: String, password: String) = submit(AuthFormValidation.login(email, password), registration = false) {
        auth.login(email.trim(), password)
    }

    fun register(name: String, email: String, password: String, confirmation: String) =
        submit(AuthFormValidation.registration(name, email, password, confirmation), registration = true) {
            auth.register(name.trim(), email.trim(), password)
        }

    fun fieldChanged(field: AuthField) {
        if (state.value.working || state.value.accountCreated) return
        val cleared = if (field == AuthField.PASSWORD) setOf(field, AuthField.CONFIRMATION) else setOf(field)
        mutableState.update {
            it.copy(fieldErrors = it.fieldErrors - cleared,
                error = it.error.takeIf { failure -> failure == AuthFailure.RATE_LIMITED && it.retrySeconds > 0 },
                registrationMayExist = it.registrationMayExist && field != AuthField.EMAIL)
        }
    }

    fun resetFeedback() {
        if (state.value.working || state.value.accountCreated) return
        mutableState.update {
            it.copy(fieldErrors = emptyMap(), registrationMayExist = false,
                error = it.error.takeIf { failure -> failure == AuthFailure.RATE_LIMITED && it.retrySeconds > 0 })
        }
    }

    private fun submit(errors: Map<AuthField, AuthFieldError>, registration: Boolean, action: suspend () -> Unit) {
        if (state.value.submissionBlocked) return
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(error = AuthFailure.VALIDATION, fieldErrors = errors) }
            return
        }
        mutableState.value = AuthUiState(working = true)
        viewModelScope.launch {
            try { action(); mutableState.value = AuthUiState() }
            catch (error: CancellationException) { throw error }
            catch (error: AuthException) {
                if (registration && error.accountCreated) savedState[ACCOUNT_CREATED] = true
                val fields = if (registration) error.fieldErrors else error.fieldErrors.filterKeys { it == AuthField.EMAIL || it == AuthField.PASSWORD }
                mutableState.value = AuthUiState(error = error.reason, fieldErrors = fields,
                    accountCreated = registration && error.accountCreated,
                    registrationMayExist = registration && error.reason in setOf(AuthFailure.NETWORK, AuthFailure.TIMEOUT,
                        AuthFailure.BAD_RESPONSE, AuthFailure.UNAVAILABLE))
                if (error.reason == AuthFailure.RATE_LIMITED) startCooldown(error.retryAfterSeconds)
            } catch (_: Exception) {
                mutableState.value = AuthUiState(error = AuthFailure.UNAVAILABLE, registrationMayExist = registration)
            } finally { mutableState.update { it.copy(working = false) } }
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
                    error = if (left == 0 && it.error == AuthFailure.RATE_LIMITED) null else it.error) }
            }
        }
    }

    private companion object { const val ACCOUNT_CREATED = "auth_confirmed_registration" }
}
