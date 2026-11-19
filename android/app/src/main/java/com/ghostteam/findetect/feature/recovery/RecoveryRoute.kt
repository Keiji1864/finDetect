package com.ghostteam.findetect.feature.recovery

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.component.FinTextField
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.model.PasswordFailure
import com.ghostteam.findetect.domain.model.PasswordField
import com.ghostteam.findetect.domain.model.PasswordFieldError

/** All three steps share one back-stack entry; no password, code or grant becomes a route argument. */
@Composable
fun RecoveryRoute(initialEmail: String, onBack: () -> Unit, onOpenLogin: (String, Boolean) -> Unit,
    viewModel: RecoveryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.initialize(initialEmail) }
    LaunchedEffect(state.step) {
        if (state.step == RecoveryStep.FINISHED) onOpenLogin(state.email, state.loginConfirmed)
    }
    val back: () -> Unit = {
        if (!state.working) {
            if (state.step == RecoveryStep.CODE) viewModel.restart() else onBack()
        }
    }
    BackHandler(onBack = back)
    when (state.step) {
        RecoveryStep.EMAIL -> RecoveryEmailScreen(state, back, viewModel::edit, viewModel::requestCode)
        RecoveryStep.CODE -> RecoveryCodeScreen(state, back, viewModel::edit, viewModel::requestCode, viewModel::verifyCode)
        RecoveryStep.PASSWORD -> NewPasswordScreen(state, back, viewModel::edit, viewModel::complete,
            viewModel::restart, viewModel::openLoginAfterUnknownResult)
        RecoveryStep.FINISHED -> Unit
    }
}

/** The email step uses the same fields, buttons and spacing as the rest of authentication. */
@Composable
fun RecoveryEmailScreen(state: RecoveryUiState = RecoveryUiState(), onBack: () -> Unit = {},
    onEdit: (PasswordField, String) -> Unit = { _, _ -> }, onSend: () -> Unit = {}) {
    val focus = LocalFocusManager.current
    val submit: () -> Unit = { if (!state.requestBlocked) { focus.clearFocus(); onSend() } }
    FinSubScreen(title = stringResource(R.string.recovery_email_title), titleMaxLines = 2,
        onBack = { if (!state.working) onBack() }) {
        Text(stringResource(R.string.recovery_email_hint), style = FinTheme.typography.body, color = FinTheme.colors.muted)
        FinTextField(label = stringResource(R.string.ui_login_email_label), value = state.email,
            onValueChange = { onEdit(PasswordField.EMAIL, it) }, enabled = !state.working,
            placeholder = stringResource(R.string.ui_login_email_placeholder),
            errorText = state.fieldErrors[PasswordField.EMAIL]?.let {
                stringResource(if (it == PasswordFieldError.REQUIRED) R.string.auth_email_required else R.string.ui_login_email_error)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }))
        RecoveryFeedback(state)
        FinButton(text = if (state.resendSeconds > 0) stringResource(R.string.recovery_send_wait, state.resendSeconds)
            else stringResource(R.string.recovery_send_code), onClick = submit,
            enabled = !state.requestBlocked, loading = state.working)
    }
}

@Composable
internal fun RecoveryFeedback(state: RecoveryUiState) {
    val wait = when (state.operation) {
        RecoveryOperation.REQUEST -> state.resendSeconds
        RecoveryOperation.VERIFY -> state.verifyRetrySeconds
        RecoveryOperation.COMPLETE -> state.completeRetrySeconds
        null -> 0
    }
    val message = when {
        state.outcomeUnknown -> stringResource(R.string.recovery_complete_unconfirmed)
        state.restartRequired -> stringResource(R.string.recovery_token_invalid)
        state.exchangeUnconfirmed -> stringResource(R.string.recovery_exchange_unconfirmed)
        state.error == PasswordFailure.RESET_CODE_EXPIRED -> stringResource(R.string.recovery_code_expired)
        state.newCodeRequired -> stringResource(R.string.recovery_new_code_required)
        state.error == PasswordFailure.RATE_LIMITED && wait > 0 -> stringResource(R.string.auth_retry_after, wait)
        else -> state.error?.let { failure ->
            when (failure) {
                PasswordFailure.RESET_CODE_INVALID -> R.string.recovery_code_invalid
                PasswordFailure.NEW_PASSWORD_SAME_AS_OLD -> R.string.ui_recovery_password_same
                PasswordFailure.VALIDATION -> R.string.auth_check_fields
                PasswordFailure.RATE_LIMITED -> R.string.auth_rate_limited
                PasswordFailure.NETWORK -> R.string.session_network_error
                PasswordFailure.TIMEOUT -> R.string.auth_timeout
                PasswordFailure.STORAGE -> R.string.session_storage_error
                else -> R.string.recovery_unavailable
            }.let { stringResource(it) }
        }
    }
    message?.let { Text(it, style = FinTheme.typography.caption, color = FinTheme.colors.red) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RecoveryEmailPreview() { FinDetectTheme { RecoveryEmailScreen() } }
