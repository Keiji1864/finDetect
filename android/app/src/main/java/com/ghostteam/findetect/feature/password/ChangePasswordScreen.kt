package com.ghostteam.findetect.feature.password

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.component.FinPasswordField
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.model.*

@Composable
fun ChangePasswordRoute(onBack: () -> Unit, viewModel: ChangePasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChangePasswordScreen(state, onBack, viewModel::edit, viewModel::submit, viewModel::openLogin)
}

/** Profile password form using the standard screen, fields and theme. Success is shown on sign-in. */
@Composable
fun ChangePasswordScreen(
    state: ChangePasswordUiState = ChangePasswordUiState(),
    onBack: () -> Unit = {},
    onEdit: (PasswordField, String) -> Unit = { _, _ -> },
    onSubmit: () -> Unit = {},
    onOpenLogin: () -> Unit = {},
) {
    val focus = LocalFocusManager.current
    val submit: () -> Unit = {
        if (!state.submissionBlocked) { focus.clearFocus(); onSubmit() }
    }
    val editable = !state.working && !state.outcomeUnknown
    BackHandler(enabled = state.working) {}
    FinSubScreen(title = stringResource(R.string.ui_change_password), titleMaxLines = 2,
        onBack = { if (!state.working) onBack() }) {
        Text(stringResource(R.string.password_change_hint), style = FinTheme.typography.body, color = FinTheme.colors.muted)
        FinPasswordField(
            label = stringResource(R.string.password_current_label),
            value = state.currentPassword,
            onValueChange = { onEdit(PasswordField.CURRENT_PASSWORD, it) },
            enabled = editable,
            placeholder = stringResource(R.string.ui_login_password_placeholder),
            errorText = passwordFieldText(PasswordField.CURRENT_PASSWORD, state.fieldErrors[PasswordField.CURRENT_PASSWORD]),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        )
        FinPasswordField(
            label = stringResource(R.string.ui_recovery_password_new_label),
            value = state.newPassword,
            onValueChange = { onEdit(PasswordField.NEW_PASSWORD, it) },
            enabled = editable,
            placeholder = stringResource(R.string.ui_login_password_placeholder),
            errorText = passwordFieldText(PasswordField.NEW_PASSWORD, state.fieldErrors[PasswordField.NEW_PASSWORD]),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        )
        FinPasswordField(
            label = stringResource(R.string.ui_register_repeat_label),
            value = state.confirmation,
            onValueChange = { onEdit(PasswordField.CONFIRMATION, it) },
            enabled = editable,
            placeholder = stringResource(R.string.ui_login_password_placeholder),
            errorText = passwordFieldText(PasswordField.CONFIRMATION, state.fieldErrors[PasswordField.CONFIRMATION]),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
        changePasswordMessage(state)?.let { Text(it, style = FinTheme.typography.caption, color = FinTheme.colors.red) }
        if (state.outcomeUnknown) {
            FinButton(stringResource(R.string.auth_go_to_login), onOpenLogin, enabled = !state.working,
                loading = state.working, style = FinButtonStyle.Secondary)
        } else {
            FinButton(stringResource(R.string.ui_change_password), submit,
                enabled = !state.submissionBlocked, loading = state.working)
        }
    }
}

@Composable
internal fun passwordFieldText(field: PasswordField, error: PasswordFieldError?): String? = error?.let {
    stringResource(when (it) {
        PasswordFieldError.REQUIRED -> R.string.session_password_required
        PasswordFieldError.LENGTH -> if (field == PasswordField.CURRENT_PASSWORD) R.string.password_current_length else R.string.auth_password_length
        PasswordFieldError.MISMATCH -> R.string.ui_register_repeat_error
        PasswordFieldError.CURRENT_PASSWORD_INVALID -> R.string.password_current_invalid
        PasswordFieldError.SAME_AS_CURRENT -> R.string.ui_recovery_password_same
        PasswordFieldError.INVALID_VALUE -> R.string.auth_invalid_field
    })
}

@Composable
private fun changePasswordMessage(state: ChangePasswordUiState): String? = when {
    state.outcomeUnknown -> stringResource(R.string.password_change_unconfirmed)
    state.retrySeconds > 0 -> stringResource(R.string.auth_retry_after, state.retrySeconds)
    state.error == PasswordFailure.CURRENT_PASSWORD_INVALID || state.error == PasswordFailure.NEW_PASSWORD_SAME_AS_OLD -> null
    else -> state.error?.let {
        when (it) {
            PasswordFailure.VALIDATION -> R.string.auth_check_fields
            PasswordFailure.SESSION_REFRESHED -> R.string.password_session_refreshed
            PasswordFailure.UNAUTHORIZED -> R.string.session_expired
            PasswordFailure.NETWORK -> R.string.session_network_error
            PasswordFailure.TIMEOUT -> R.string.auth_timeout
            PasswordFailure.RATE_LIMITED -> R.string.auth_rate_limited
            PasswordFailure.STORAGE -> R.string.session_storage_error
            else -> R.string.password_change_unavailable
        }.let { resource -> stringResource(resource) }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChangePasswordPreview() { FinDetectTheme { ChangePasswordScreen() } }

@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChangePasswordUnconfirmedPreview() {
    FinDetectTheme { ChangePasswordScreen(ChangePasswordUiState(outcomeUnknown = true)) }
}
