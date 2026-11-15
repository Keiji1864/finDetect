package com.ghostteam.findetect.feature.register

import android.content.res.Configuration
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinTextField
import com.ghostteam.findetect.core.designsystem.component.FinTopBar
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

private const val MinPasswordLength = 8

/**
 * Регистрация (Figma 06): стрелка "назад" и заголовок "Создать аккаунт", четыре поля
 * (имя, почта, пароль, повтор пароля), кнопка "Создать аккаунт" прижата к низу экрана.
 *
 * Form limits follow the API contract. Passwords remain in memory only.
 */
@Composable
fun RegisterScreen(
    onBack: () -> Unit = {},
    onRegister: (String, String, String) -> Unit = { _, _, _ -> },
    working: Boolean = false,
    message: String? = null,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    // Ошибки показываем только после первого нажатия на "Создать аккаунт"
    var submitted by rememberSaveable { mutableStateOf(false) }

    val nameValid = name.isNotBlank()
    val emailValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val passwordValid = password.length >= MinPasswordLength
    val repeatValid = repeat == password

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinTheme.colors.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        FinTopBar(
            title = stringResource(R.string.ui_register_title),
            onBack = { if (!working) onBack() },
            titleStyle = FinTheme.typography.title,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = FinDimens.screenPadding,
                    end = FinDimens.screenPadding,
                    top = FinDimens.blockGap,
                    bottom = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap),
        ) {
            FinTextField(
                label = stringResource(R.string.ui_register_name_label),
                value = name,
                onValueChange = { name = it.take(50) },
                enabled = !working,
                placeholder = stringResource(R.string.ui_register_name_placeholder),
                errorText = if (submitted && !nameValid) stringResource(R.string.ui_register_name_error) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            FinTextField(
                label = stringResource(R.string.ui_login_email_label),
                value = email,
                onValueChange = { email = it.take(254) },
                enabled = !working,
                placeholder = stringResource(R.string.ui_login_email_placeholder),
                errorText = if (submitted && !emailValid) stringResource(R.string.ui_login_email_error) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            FinTextField(
                label = stringResource(R.string.ui_login_password_label),
                value = password,
                onValueChange = { password = it.take(128) },
                enabled = !working,
                placeholder = stringResource(R.string.ui_login_password_placeholder),
                errorText = if (submitted && !passwordValid) stringResource(R.string.ui_register_password_error) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                visualTransformation = PasswordVisualTransformation(),
            )
            FinTextField(
                label = stringResource(R.string.ui_register_repeat_label),
                value = repeat,
                onValueChange = { repeat = it.take(128) },
                enabled = !working,
                placeholder = stringResource(R.string.ui_login_password_placeholder),
                errorText = if (submitted && !repeatValid) stringResource(R.string.ui_register_repeat_error) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                visualTransformation = PasswordVisualTransformation(),
            )
            if (message != null) Text(message, style = FinTheme.typography.caption, color = FinTheme.colors.red)
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = FinDimens.screenPadding).padding(bottom = 16.dp)) {
            FinButton(
                text = stringResource(R.string.ui_register_submit),
                onClick = {
                    submitted = true
                    if (nameValid && emailValid && passwordValid && repeatValid && !working) onRegister(name.trim(), email.trim(), password)
                },
                loading = working,
                enabled = !working,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegisterScreenPreview() { FinDetectTheme { RegisterScreen() } }
