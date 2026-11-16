package com.ghostteam.findetect.feature.login

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinTextField
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.model.AuthField
import com.ghostteam.findetect.domain.model.AuthFailure
import com.ghostteam.findetect.feature.session.*

/**
 * Вход (Figma 06): логотип и слоган, поля "Электронная почта" и "Пароль",
 * ниже "Зарегистрироваться", "Войти" и "Забыли пароль?". Нижней навигации и стрелки "назад" нет.
 * Содержимое прокручивается при открытой клавиатуре, кнопки идут сразу под полями.
 *
 * Credentials are submitted to the session layer; passwords are never put in saved instance state.
 */
@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit = { _, _ -> },
    onForgotPassword: () -> Unit = {},
    onRegister: () -> Unit = {},
    initialEmail: String = "",
    emailRevision: Long = 0L,
    state: AuthUiState = AuthUiState(),
    onFieldChange: (AuthField) -> Unit = {},
    message: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    var email by rememberSaveable(initialEmail, emailRevision) { mutableStateOf(initialEmail) }
    var password by remember(initialEmail, emailRevision) { mutableStateOf("") }
    // Ошибки показываем только после первого нажатия на "Войти"
    var submitted by rememberSaveable(initialEmail, emailRevision) { mutableStateOf(false) }

    val localErrors = if (submitted) AuthFormValidation.login(email, password) else emptyMap()
    val working = state.working
    val feedback = message ?: authFormMessage(state)
    val focus = LocalFocusManager.current
    val submit: () -> Unit = {
        if (!state.submissionBlocked) {
            submitted = true
            focus.clearFocus()
            onLogin(email.trim(), password)
        }
    }
    BackHandler(enabled = working) {}
    val c = FinTheme.colors

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(c.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight)
                .padding(horizontal = FinDimens.screenPadding, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FinDimens.headerGap),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_findetective),
                    contentDescription = stringResource(R.string.app_name),
                    tint = c.navy,
                    modifier = Modifier.size(width = 180.dp, height = 38.dp),
                )
                Text(
                    text = stringResource(R.string.ui_login_tagline),
                    style = FinTheme.typography.label,
                    color = c.muted,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap)) {
                FinTextField(
                    label = stringResource(R.string.ui_login_email_label),
                    value = email,
                    onValueChange = { email = it; onFieldChange(AuthField.EMAIL) },
                    enabled = !working,
                    placeholder = stringResource(R.string.ui_login_email_placeholder),
                    errorText = authFieldText(AuthField.EMAIL, localErrors[AuthField.EMAIL] ?: state.fieldErrors[AuthField.EMAIL]),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
                FinTextField(
                    label = stringResource(R.string.ui_login_password_label),
                    value = password,
                    onValueChange = { password = it; onFieldChange(AuthField.PASSWORD) },
                    enabled = !working,
                    errorText = authFieldText(AuthField.PASSWORD, localErrors[AuthField.PASSWORD] ?: state.fieldErrors[AuthField.PASSWORD]),
                    placeholder = stringResource(R.string.ui_login_password_placeholder),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
            }

            // Кнопки сразу под полями; оставшееся место уходит вниз
            Spacer(Modifier.height(32.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap),
            ) {
                if (feedback != null) Text(feedback, style = FinTheme.typography.caption, color = c.red)
                if (onRetry != null) FinButton(stringResource(R.string.finance_retry), onRetry, enabled = !working, style = FinButtonStyle.Secondary)
                FinButton(
                    text = stringResource(R.string.ui_login_sign_in),
                    onClick = submit,
                    loading = working,
                    enabled = !state.submissionBlocked,
                )
                FinButton(
                    text = stringResource(R.string.ui_login_forgot),
                    onClick = onForgotPassword,
                    enabled = !working,
                    style = FinButtonStyle.Secondary,
                )
                Text(
                    text = stringResource(R.string.ui_login_no_account),
                    modifier = Modifier.padding(top = 4.dp),
                    style = FinTheme.typography.caption,
                    color = c.muted,
                )
                FinButton(
                    text = stringResource(R.string.ui_login_register),
                    onClick = onRegister,
                    enabled = !working,
                    style = FinButtonStyle.Secondary,
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenPreview() { FinDetectTheme { LoginScreen() } }

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginRateLimitPreview() {
    FinDetectTheme { LoginScreen(initialEmail = "auth-rate-limit@example.com",
        state = AuthUiState(error = AuthFailure.RATE_LIMITED, retrySeconds = 5)) }
}
