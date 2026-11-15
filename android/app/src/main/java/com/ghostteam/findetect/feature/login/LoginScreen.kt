package com.ghostteam.findetect.feature.login

import android.content.res.Configuration
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

/**
 * Вход (Figma 06): логотип и слоган, поля "Электронная почта" и "Пароль",
 * ниже "Зарегистрироваться", "Войти" и "Забыли пароль?". Нижней навигации и стрелки "назад" нет.
 * Всё содержимое сдвинуто вниз: сверху свободное место, кнопки идут сразу под полями.
 *
 * Поля необязательные: можно сразу нажать «Войти». Если почта введена, проверяется её формат. Настоящей
 * авторизации пока нет, [onLogin] вызывается, когда проверка пройдена. Экранов регистрации и восстановления пароля
 * в макете нет, поэтому [onForgotPassword] и [onRegister] по умолчанию ничего не делают.
 */
@Composable
fun LoginScreen(
    onLogin: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onRegister: () -> Unit = {},
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    // Ошибки показываем только после первого нажатия на "Войти"
    var submitted by rememberSaveable { mutableStateOf(false) }

    // Пустая почта допустима; если что-то введено, оно должно быть похоже на адрес
    val emailValid = email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val c = FinTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = FinDimens.screenPadding),
    ) {
        // Свободное место сверху (доли подобраны по макету; с клавиатурой сжимается)
        Spacer(Modifier.weight(0.3f))

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
                onValueChange = { email = it.take(60) },
                placeholder = stringResource(R.string.ui_login_email_placeholder),
                errorText = if (submitted && !emailValid) stringResource(R.string.ui_login_email_error) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            FinTextField(
                label = stringResource(R.string.ui_login_password_label),
                value = password,
                onValueChange = { password = it.take(64) },
                placeholder = stringResource(R.string.ui_login_password_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                visualTransformation = PasswordVisualTransformation(),
            )
        }

        // Кнопки сразу под полями; оставшееся место уходит вниз
        Spacer(Modifier.height(32.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap),
        ) {
            FinButton(
                text = stringResource(R.string.ui_login_sign_in),
                onClick = {
                    submitted = true
                    if (emailValid) onLogin()
                },
            )
            FinButton(
                text = stringResource(R.string.ui_login_forgot),
                onClick = onForgotPassword,
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
                style = FinButtonStyle.Secondary,
            )
        }

        Spacer(Modifier.weight(0.7f))
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenPreview() { FinDetectTheme { LoginScreen() } }