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

private const val MinPasswordLength = 6

/**
 * Регистрация (Figma 06): стрелка "назад" и заголовок "Создать аккаунт", четыре поля
 * (имя, почта, пароль, повтор пароля), кнопка "Создать аккаунт" прижата к низу экрана.
 *
 * Проверка только на форму: имя не пустое, почта похожа на адрес, пароль от 6 символов, пароли совпадают.
 * Настоящей регистрации пока нет, [onRegister] вызывается, когда все проверки пройдены.
 */
@Composable
fun RegisterScreen(
    onBack: () -> Unit = {},
    onRegister: () -> Unit = {},
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var repeat by rememberSaveable { mutableStateOf("") }
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
            onBack = onBack,
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
                onValueChange = { name = it.take(40) },
                placeholder = stringResource(R.string.ui_register_name_placeholder),
                errorText = if (submitted && !nameValid) stringResource(R.string.ui_register_name_error) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
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
                errorText = if (submitted && !passwordValid) stringResource(R.string.ui_register_password_error) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                visualTransformation = PasswordVisualTransformation(),
            )
            FinTextField(
                label = stringResource(R.string.ui_register_repeat_label),
                value = repeat,
                onValueChange = { repeat = it.take(64) },
                placeholder = stringResource(R.string.ui_login_password_placeholder),
                errorText = if (submitted && !repeatValid) stringResource(R.string.ui_register_repeat_error) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = FinDimens.screenPadding).padding(bottom = 16.dp)) {
            FinButton(
                text = stringResource(R.string.ui_register_submit),
                onClick = {
                    submitted = true
                    if (nameValid && emailValid && passwordValid && repeatValid) onRegister()
                },
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegisterScreenPreview() { FinDetectTheme { RegisterScreen() } }