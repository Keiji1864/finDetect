package com.ghostteam.findetect.feature.recovery

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.model.PasswordFailure
import com.ghostteam.findetect.domain.model.PasswordField

private const val CodeLength = 6

/** Code entry is explicit: neither typing the sixth digit nor reaching a deadline sends a request. */
@Composable
fun RecoveryCodeScreen(
    state: RecoveryUiState = RecoveryUiState(step = RecoveryStep.CODE, codeSeconds = 600, resendSeconds = 60),
    onBack: () -> Unit = {},
    onEdit: (PasswordField, String) -> Unit = { _, _ -> },
    onResend: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    val editable = !state.working && !state.newCodeRequired
    val submit: () -> Unit = { if (!state.verifyBlocked) { focus.clearFocus(); onContinue() } }
    val codeError = state.fieldErrors.containsKey(PasswordField.CODE) || state.error == PasswordFailure.RESET_CODE_INVALID
    LaunchedEffect(Unit) { if (editable) focusRequester.requestFocus() }

    FinSubScreen(title = stringResource(R.string.ui_recovery_code_title),
        onBack = { if (!state.working) onBack() }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap)) {
            Text(stringResource(R.string.ui_recovery_code_hint), style = FinTheme.typography.body,
                color = FinTheme.colors.muted, textAlign = TextAlign.Center)
            Text(state.email, style = FinTheme.typography.label, color = FinTheme.colors.navy, textAlign = TextAlign.Center)
            CodeInput(code = state.code, isError = codeError, enabled = editable, focusRequester = focusRequester,
                onCodeChange = { onEdit(PasswordField.CODE, it) }, onDone = submit)
            if (state.fieldErrors.containsKey(PasswordField.CODE)) {
                Text(stringResource(R.string.ui_recovery_code_error), style = FinTheme.typography.caption, color = FinTheme.colors.red)
            }
            if (!state.newCodeRequired) {
                Text(stringResource(R.string.recovery_code_remaining, state.codeSeconds / 60, state.codeSeconds % 60),
                    style = FinTheme.typography.caption, color = FinTheme.colors.muted)
            }
            state.debugHint?.let { Text(it, style = FinTheme.typography.caption, color = FinTheme.colors.muted) }
        }
        RecoveryFeedback(state)
        FinButton(text = if (state.verifyRetrySeconds > 0) stringResource(R.string.recovery_verify_wait, state.verifyRetrySeconds)
            else stringResource(R.string.ui_onboarding_continue), onClick = submit, enabled = !state.verifyBlocked,
            loading = state.working && state.operation == RecoveryOperation.VERIFY)
        Text(stringResource(R.string.ui_recovery_code_no_code), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
        FinButton(text = if (state.resendSeconds > 0) stringResource(R.string.recovery_resend_wait, state.resendSeconds)
            else stringResource(R.string.ui_recovery_code_resend), onClick = { focus.clearFocus(); onResend() },
            enabled = !state.requestBlocked, loading = state.working && state.operation == RecoveryOperation.REQUEST,
            style = FinButtonStyle.Secondary)
    }
}

/** One editable value behind six decorative cells preserves leading zeros and supports paste and IME. */
@Composable
private fun CodeInput(code: String, isError: Boolean, enabled: Boolean, focusRequester: FocusRequester,
    onCodeChange: (String) -> Unit, onDone: () -> Unit) {
    val c = FinTheme.colors
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = TextFieldValue(code, selection = TextRange(code.length)),
        onValueChange = { onCodeChange(it.text.filter { digit -> digit in '0'..'9' }.take(CodeLength)) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Box {
                Box(Modifier.size(1.dp).alpha(0f)) { innerTextField() }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.clearAndSetSemantics {}) {
                    repeat(CodeLength) { index ->
                        val active = enabled && focused && index == code.length.coerceAtMost(CodeLength - 1)
                        val borderColor = when { isError -> c.red; active -> c.yellow; else -> c.line }
                        val borderWidth = if (active || isError) FinDimens.borderFocused else FinDimens.borderWidth
                        Box(modifier = Modifier.weight(1f).aspectRatio(0.8f).clip(FinTheme.shapes.r12)
                            .background(c.white).border(borderWidth, borderColor, FinTheme.shapes.r12),
                            contentAlignment = Alignment.Center) {
                            val digit = code.getOrNull(index)
                            if (digit != null) Text(digit.toString(), style = FinTheme.typography.heading, color = c.navy)
                            else Box(Modifier.size(6.dp).clip(CircleShape).background(c.navy))
                        }
                    }
                }
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RecoveryCodeScreenPreview() { FinDetectTheme { RecoveryCodeScreen() } }

@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RecoveryCodeExpiredPreview() {
    FinDetectTheme { RecoveryCodeScreen(RecoveryUiState(step = RecoveryStep.CODE, newCodeRequired = true,
        error = PasswordFailure.RESET_CODE_EXPIRED)) }
}
