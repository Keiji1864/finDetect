package com.ghostteam.findetect.feature.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ghostteam.findetect.R
import com.ghostteam.findetect.domain.model.*

/** Localized form messages are derived from safe error types, never a server message. */
@Composable
fun authFieldText(field: AuthField, error: AuthFieldError?): String? = error?.let {
    stringResource(when (it) {
        AuthFieldError.REQUIRED -> when (field) {
            AuthField.NAME -> R.string.ui_register_name_error
            AuthField.EMAIL -> R.string.auth_email_required
            else -> R.string.session_password_required
        }
        AuthFieldError.INVALID_EMAIL -> R.string.ui_login_email_error
        AuthFieldError.NAME_LENGTH -> R.string.auth_name_length
        AuthFieldError.PASSWORD_LENGTH -> R.string.auth_password_length
        AuthFieldError.PASSWORD_MISMATCH -> R.string.ui_register_repeat_error
        AuthFieldError.EMAIL_TAKEN -> R.string.session_email_taken
        AuthFieldError.INVALID_VALUE -> R.string.auth_invalid_field
    })
}

@Composable
fun authFormMessage(state: AuthUiState): String? = when {
    state.accountCreated -> stringResource(R.string.auth_account_created)
    state.registrationMayExist && state.error != null -> stringResource(R.string.auth_registration_unconfirmed)
    state.error == AuthFailure.RATE_LIMITED && state.retrySeconds > 0 -> stringResource(R.string.auth_retry_after, state.retrySeconds)
    else -> state.error?.let { failure ->
        when (failure) {
            AuthFailure.EMAIL_TAKEN -> if (state.fieldErrors.isEmpty()) stringResource(R.string.session_email_taken) else null
            AuthFailure.VALIDATION -> stringResource(R.string.auth_check_fields)
            AuthFailure.INVALID_CREDENTIALS -> stringResource(R.string.session_invalid_credentials)
            AuthFailure.NETWORK -> stringResource(R.string.session_network_error)
            AuthFailure.TIMEOUT -> stringResource(R.string.auth_timeout)
            AuthFailure.RATE_LIMITED -> stringResource(R.string.auth_rate_limited)
            AuthFailure.BAD_RESPONSE, AuthFailure.UNAVAILABLE -> stringResource(R.string.session_server_error)
            AuthFailure.STORAGE -> stringResource(R.string.session_storage_error)
        }
    }
}
