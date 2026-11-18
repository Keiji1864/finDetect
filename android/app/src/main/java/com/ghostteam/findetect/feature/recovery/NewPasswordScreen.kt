package com.ghostteam.findetect.feature.recovery

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinPasswordField
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

private const val MinPasswordLength = 6

/**
 * Восстановление пароля, шаг 2: "Создайте новый пароль" (DESIGN.md 5.21). Стрелка "назад", заголовок в две строки,
 * пояснение, поля "Новый пароль" и "Повторите пароль" с глазом показа/скрытия, внизу "Войти".
 *
 * Проверка только на форму: пароль от 6 символов (как при регистрации), пароли совпадают. Текущего пароля в
 * приложении пока нет, поэтому плашка "Новый пароль не должен совпадать с текущим" включится, когда в
 * [currentPassword] начнут передавать настоящий пароль. [onDone] вызывается, когда проверки пройдены:
 * сервер обновит пароль, а приложение войдёт автоматически. Стрелка "назад" и системная кнопка ведут сразу
 * на вход, минуя ввод кода: куда именно вернуться, решает [onBack].
 */
@Composable
fun NewPasswordScreen(
    currentPassword: String? = null,
    onBack: () -> Unit = {},
    onDone: () -> Unit = {},
) {
    var password by rememberSaveable { mutableStateOf("") }
    var repeat by rememberSaveable { mutableStateOf("") }
    // Ошибки показываем только после первого нажатия на "Войти"
    var submitted by rememberSaveable { mutableStateOf(false) }

    val passwordValid = password.length >= MinPasswordLength
    val repeatValid = repeat == password
    val sameAsCurrent = currentPassword != null && password == currentPassword
    val c = FinTheme.colors

    // Системная "назад" ведёт туда же, куда и стрелка
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Своя шапка: заголовок переносится на две строки, а у FinTopBar высота фиксированная
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = FinDimens.screenPadding),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = null,
                    tint = c.navy,
                    modifier = Modifier.size(FinDimens.iconSize),
                )
            }
            Text(
                text = stringResource(R.string.ui_recovery_password_title),
                style = FinTheme.typography.title,
                color = c.navy,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
        }
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
            Text(
                text = stringResource(R.string.ui_recovery_password_hint),
                style = FinTheme.typography.label.copy(fontWeight = FontWeight.Normal),
                color = c.muted,
            )
            FinPasswordField(
                label = stringResource(R.string.ui_recovery_password_new_label),
                value = password,
                onValueChange = { password = it.take(64) },
                placeholder = stringResource(R.string.ui_login_password_placeholder),
                errorText = if (submitted && !passwordValid) stringResource(R.string.ui_register_password_error) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            )
            FinPasswordField(
                label = stringResource(R.string.ui_register_repeat_label),
                value = repeat,
                onValueChange = { repeat = it.take(64) },
                placeholder = stringResource(R.string.ui_login_password_placeholder),
                errorText = if (submitted && !repeatValid) stringResource(R.string.ui_register_repeat_error) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            )
            // Розовая плашка есть только при совпадении с текущим паролем
            if (submitted && sameAsCurrent) {
                SamePasswordNotice(text = stringResource(R.string.ui_recovery_password_same))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinDimens.screenPadding)
                .padding(bottom = 16.dp),
        ) {
            FinButton(
                text = stringResource(R.string.ui_login_sign_in),
                onClick = {
                    submitted = true
                    if (passwordValid && repeatValid && !sameAsCurrent) onDone()
                },
            )
        }
    }
}

/** Розовая плашка с ошибкой: те же отступы, что у FinNotice, фон red с прозрачностью. */
@Composable
private fun SamePasswordNotice(text: String) {
    val c = FinTheme.colors
    Text(
        text = text,
        style = FinTheme.typography.label,
        color = c.red,
        modifier = Modifier
            .fillMaxWidth()
            .clip(FinTheme.shapes.r12)
            .background(c.red.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NewPasswordScreenPreview() { FinDetectTheme { NewPasswordScreen() } }
