package com.ghostteam.findetect.feature.recovery

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.core.network.RecoveryDebugInfo
import com.ghostteam.findetect.domain.model.*
import com.ghostteam.findetect.domain.repository.PasswordRepository
import com.ghostteam.findetect.feature.session.AuthFormValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RecoveryStep { EMAIL, CODE, PASSWORD, FINISHED }
enum class RecoveryOperation { REQUEST, VERIFY, COMPLETE }

data class RecoveryUiState(
    val step: RecoveryStep = RecoveryStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmation: String = "",
    val working: Boolean = false,
    val operation: RecoveryOperation? = null,
    val error: PasswordFailure? = null,
    val fieldErrors: Map<PasswordField, PasswordFieldError> = emptyMap(),
    val resendSeconds: Int = 0,
    val verifyRetrySeconds: Int = 0,
    val completeRetrySeconds: Int = 0,
    val codeSeconds: Int = 0,
    val grantSeconds: Int = 0,
    val newCodeRequired: Boolean = false,
    val restartRequired: Boolean = false,
    val exchangeUnconfirmed: Boolean = false,
    val outcomeUnknown: Boolean = false,
    val loginConfirmed: Boolean = false,
    val debugHint: String? = null,
) {
    val requestBlocked get() = working || resendSeconds > 0
    val verifyBlocked get() = working || verifyRetrySeconds > 0 || newCodeRequired
    val completeBlocked get() = working || completeRetrySeconds > 0 || restartRequired || outcomeUnknown
}

/** One public flow owns its secrets in memory. Only the email may survive process death. */
@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val passwords: PasswordRepository,
    private val savedState: SavedStateHandle,
    private val debug: RecoveryDebugInfo,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RecoveryUiState(email = savedState[EMAIL] ?: ""))
    val state = mutableState.asStateFlow()
    private var initialized = savedState.contains(EMAIL)
    private var requestId: String? = null
    private var grant: PasswordResetGrant? = null
    private var codeDeadline = 0L
    private var grantDeadline = 0L
    private val sendDeadlines = mutableMapOf<String, Long>()
    private val verifyDeadlines = mutableMapOf<String, Long>()
    private var completeDeadline = 0L

    init {
        viewModelScope.launch {
            while (isActive) { updateClocks(); delay(1_000) }
        }
    }

    fun initialize(email: String) {
        if (initialized) return
        initialized = true
        edit(PasswordField.EMAIL, email)
    }

    fun edit(field: PasswordField, value: String) {
        val form = state.value
        if (form.working || form.step == RecoveryStep.FINISHED) return
        if (field == PasswordField.EMAIL && form.step == RecoveryStep.EMAIL) {
            savedState[EMAIL] = value
            mutableState.update { it.copy(email = value, error = null, exchangeUnconfirmed = false,
                fieldErrors = it.fieldErrors - field) }
            updateClocks()
        } else if (field == PasswordField.CODE && form.step == RecoveryStep.CODE && !form.newCodeRequired) {
            mutableState.update { it.copy(code = value.filter { c -> c in '0'..'9' }.take(6),
                fieldErrors = it.fieldErrors - field, error = it.error.takeIf { failure -> failure == PasswordFailure.RATE_LIMITED }) }
        } else if (form.step == RecoveryStep.PASSWORD && !form.restartRequired && !form.outcomeUnknown) {
            mutableState.update {
                when (field) {
                    PasswordField.NEW_PASSWORD -> it.copy(newPassword = value,
                        fieldErrors = it.fieldErrors - setOf(field, PasswordField.CONFIRMATION), error = null)
                    PasswordField.CONFIRMATION -> it.copy(confirmation = value, fieldErrors = it.fieldErrors - field, error = null)
                    else -> it
                }
            }
        }
    }

    fun requestCode() {
        updateClocks()
        val form = state.value
        if (form.requestBlocked || form.step !in setOf(RecoveryStep.EMAIL, RecoveryStep.CODE)) return
        val email = form.email.trim()
        val invalid = AuthFormValidation.validateEmail(email)
        if (invalid != null) {
            mutableState.update { it.copy(error = PasswordFailure.VALIDATION, fieldErrors = mapOf(PasswordField.EMAIL to
                if (invalid == AuthFieldError.REQUIRED) PasswordFieldError.REQUIRED else PasswordFieldError.INVALID_VALUE)) }
            return
        }
        begin(RecoveryOperation.REQUEST)
        val started = now()
        viewModelScope.launch {
            try {
                val challenge = passwords.requestReset(email)
                val received = now()
                requestId = challenge.requestId
                codeDeadline = started + challenge.expiresInSeconds * 1_000L
                sendDeadlines[key(email)] = received + challenge.resendAfterSeconds * 1_000L
                val hint = try { debug.hint(challenge.requestId) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
                mutableState.value = RecoveryUiState(step = RecoveryStep.CODE, email = email, working = true,
                    operation = RecoveryOperation.REQUEST, debugHint = hint)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: PasswordException) { fail(error, RecoveryOperation.REQUEST) }
            catch (_: Exception) { fail(PasswordException(PasswordFailure.UNAVAILABLE, outcomeUnknown = true), RecoveryOperation.REQUEST) }
            finally { finishWork() }
        }
    }

    fun verifyCode() {
        updateClocks()
        val form = state.value
        if (form.step != RecoveryStep.CODE || form.verifyBlocked) return
        val id = requestId ?: return
        if (!Regex("^[0-9]{6}$").matches(form.code)) {
            mutableState.update { it.copy(error = PasswordFailure.VALIDATION,
                fieldErrors = mapOf(PasswordField.CODE to PasswordFieldError.LENGTH)) }
            return
        }
        begin(RecoveryOperation.VERIFY)
        val started = now()
        viewModelScope.launch {
            try {
                grant = passwords.verifyReset(id, form.code)
                grantDeadline = started + checkNotNull(grant).expiresInSeconds * 1_000L
                requestId = null
                codeDeadline = 0L
                mutableState.value = RecoveryUiState(step = RecoveryStep.PASSWORD, email = form.email, working = true,
                    operation = RecoveryOperation.VERIFY)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: PasswordException) { fail(error, RecoveryOperation.VERIFY) }
            catch (_: Exception) { fail(PasswordException(PasswordFailure.UNAVAILABLE, outcomeUnknown = true), RecoveryOperation.VERIFY) }
            finally { finishWork() }
        }
    }

    fun complete() {
        updateClocks()
        val form = state.value
        if (form.step != RecoveryStep.PASSWORD || form.completeBlocked) return
        val token = grant?.token ?: return
        val errors = PasswordValidation.reset(form.newPassword, form.confirmation)
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(error = PasswordFailure.VALIDATION, fieldErrors = errors) }
            return
        }
        begin(RecoveryOperation.COMPLETE)
        viewModelScope.launch {
            try {
                passwords.completeReset(form.email, token, form.newPassword)
                clearSecrets()
                mutableState.value = RecoveryUiState(step = RecoveryStep.FINISHED, email = form.email, loginConfirmed = true)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: PasswordException) { fail(error, RecoveryOperation.COMPLETE) }
            catch (_: Exception) { fail(PasswordException(PasswordFailure.UNAVAILABLE, outcomeUnknown = true), RecoveryOperation.COMPLETE) }
            finally { finishWork() }
        }
    }

    fun restart() {
        if (state.value.working) return
        clearSecrets()
        mutableState.value = RecoveryUiState(email = state.value.email)
        updateClocks()
    }

    fun openLoginAfterUnknownResult() {
        if (state.value.working || !state.value.outcomeUnknown) return
        clearSecrets()
        mutableState.value = RecoveryUiState(step = RecoveryStep.FINISHED, email = state.value.email)
    }

    private fun begin(operation: RecoveryOperation) {
        mutableState.update { it.copy(working = true, operation = operation, error = null, fieldErrors = emptyMap(), exchangeUnconfirmed = false) }
    }

    private fun finishWork() {
        mutableState.update { it.copy(working = false) }
        updateClocks()
    }

    private fun fail(error: PasswordException, operation: RecoveryOperation) {
        val email = key(state.value.email)
        if (error.reason == PasswordFailure.RATE_LIMITED && error.retryAfterSeconds != null) {
            val deadline = now() + error.retryAfterSeconds.coerceIn(0, Int.MAX_VALUE.toLong()) * 1_000
            when (operation) {
                RecoveryOperation.REQUEST -> sendDeadlines[email] = maxOf(sendDeadlines[email] ?: 0L, deadline)
                RecoveryOperation.VERIFY -> verifyDeadlines[email] = maxOf(verifyDeadlines[email] ?: 0L, deadline)
                RecoveryOperation.COMPLETE -> completeDeadline = maxOf(completeDeadline, deadline)
            }
        }
        val fields = error.fieldErrors.filterKeys { field -> when (operation) {
            RecoveryOperation.REQUEST -> field == PasswordField.EMAIL
            RecoveryOperation.VERIFY -> field == PasswordField.CODE
            RecoveryOperation.COMPLETE -> field == PasswordField.NEW_PASSWORD
        } }
        mutableState.update { it.copy(error = error.reason, fieldErrors = fields, operation = operation) }
        if (operation == RecoveryOperation.COMPLETE) {
            if (error.outcomeUnknown || error.reason == PasswordFailure.RESET_TOKEN_INVALID || PasswordField.RESET_TOKEN in error.fieldErrors) {
                clearSecrets()
                mutableState.update { it.copy(newPassword = "", confirmation = "", outcomeUnknown = error.outcomeUnknown,
                    restartRequired = !error.outcomeUnknown, fieldErrors = emptyMap()) }
            }
        } else if (error.outcomeUnknown || error.reason == PasswordFailure.RESET_CODE_EXPIRED || PasswordField.REQUEST_ID in error.fieldErrors) {
            requestId = null
            codeDeadline = 0L
            if (operation == RecoveryOperation.REQUEST && error.outcomeUnknown)
                sendDeadlines[email] = maxOf(sendDeadlines[email] ?: 0L, now() + 60_000)
            mutableState.update { it.copy(code = "", debugHint = null, newCodeRequired = true,
                exchangeUnconfirmed = error.outcomeUnknown, fieldErrors = emptyMap()) }
        }
    }

    private fun updateClocks() {
        val time = now()
        val form = state.value
        if (form.step == RecoveryStep.FINISHED) return
        val codeLeft = remaining(codeDeadline, time)
        val grantLeft = remaining(grantDeadline, time)
        if (!form.working && form.step == RecoveryStep.CODE && requestId != null && codeLeft == 0) {
            requestId = null
            mutableState.update { it.copy(code = "", debugHint = null, newCodeRequired = true, error = PasswordFailure.RESET_CODE_EXPIRED) }
        }
        if (!form.working && form.step == RecoveryStep.PASSWORD && grant != null && grantLeft == 0) {
            grant = null
            mutableState.update { it.copy(newPassword = "", confirmation = "", restartRequired = true, error = PasswordFailure.RESET_TOKEN_INVALID) }
        }
        mutableState.update { it.copy(resendSeconds = remaining(sendDeadlines[key(it.email)] ?: 0, time),
            verifyRetrySeconds = remaining(verifyDeadlines[key(it.email)] ?: 0, time),
            completeRetrySeconds = remaining(completeDeadline, time), codeSeconds = codeLeft, grantSeconds = grantLeft) }
    }

    private fun clearSecrets() { requestId = null; grant = null; codeDeadline = 0L; grantDeadline = 0L; completeDeadline = 0L }
    private fun now() = SystemClock.elapsedRealtime()
    private fun key(email: String) = email.trim().lowercase()
    private fun remaining(deadline: Long, time: Long) = ((deadline - time + 999) / 1_000).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()

    override fun onCleared() {
        clearSecrets()
        mutableState.value = RecoveryUiState()
        super.onCleared()
    }

    private companion object { const val EMAIL = "recovery_email" }
}
