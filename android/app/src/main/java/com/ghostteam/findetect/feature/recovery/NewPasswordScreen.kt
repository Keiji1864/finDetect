package com.ghostteam.findetect.feature.recovery

import android.content.res.Configuration
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinPasswordField
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.model.PasswordField
import com.ghostteam.findetect.feature.password.passwordFieldText

/** The server grant authorizes only saving a new password. A confirmed save opens ordinary sign-in. */
@Composable
fun NewPasswordScreen(
    state: RecoveryUiState = RecoveryUiState(step = RecoveryStep.PASSWORD, grantSeconds = 600),
    onBack: () -> Unit = {},
    onEdit: (PasswordField, String) -> Unit = { _, _ -> },
    onDone: () -> Unit = {},
    onRestart: () -> Unit = {},
    onOpenLogin: () -> Unit = {},
) {
    val focus = LocalFocusManager.current
    val submit: () -> Unit = { if (!state.completeBlocked) { focus.clearFocus(); onDone() } }
    val editable = !state.working && !state.restartRequired && !state.outcomeUnknown
    FinSubScreen(title = stringResource(R.string.ui_recovery_password_title), titleMaxLines = 2,
        onBack = { if (!state.working) onBack() }) {
        Text(stringResource(R.string.ui_recovery_password_hint), style = FinTheme.typography.body, color = FinTheme.colors.muted)
        if (!state.restartRequired && !state.outcomeUnknown) {
            Text(stringResource(R.string.recovery_grant_remaining, state.grantSeconds / 60, state.grantSeconds % 60),
                style = FinTheme.typography.caption, color = FinTheme.colors.muted)
        }
        FinPasswordField(label = stringResource(R.string.ui_recovery_password_new_label), value = state.newPassword,
            onValueChange = { onEdit(PasswordField.NEW_PASSWORD, it) }, enabled = editable,
            placeholder = stringResource(R.string.ui_login_password_placeholder),
            errorText = passwordFieldText(PasswordField.NEW_PASSWORD, state.fieldErrors[PasswordField.NEW_PASSWORD]),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next))
        FinPasswordField(label = stringResource(R.string.ui_register_repeat_label), value = state.confirmation,
            onValueChange = { onEdit(PasswordField.CONFIRMATION, it) }, enabled = editable,
            placeholder = stringResource(R.string.ui_login_password_placeholder),
            errorText = passwordFieldText(PasswordField.CONFIRMATION, state.fieldErrors[PasswordField.CONFIRMATION]),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }))
        RecoveryFeedback(state)
        if (state.outcomeUnknown) {
            FinButton(stringResource(R.string.auth_go_to_login), onOpenLogin, enabled = !state.working)
        } else if (!state.restartRequired) {
            FinButton(if (state.completeRetrySeconds > 0) stringResource(R.string.recovery_save_wait, state.completeRetrySeconds)
                else stringResource(R.string.recovery_save_password), submit, enabled = !state.completeBlocked, loading = state.working)
        }
        if (state.restartRequired || state.outcomeUnknown) {
            FinButton(stringResource(R.string.recovery_restart), onRestart, enabled = !state.working, style = FinButtonStyle.Secondary)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NewPasswordScreenPreview() { FinDetectTheme { NewPasswordScreen() } }

@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NewPasswordUnconfirmedPreview() {
    FinDetectTheme { NewPasswordScreen(RecoveryUiState(step = RecoveryStep.PASSWORD, outcomeUnknown = true)) }
}
