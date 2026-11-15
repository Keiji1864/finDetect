package com.ghostteam.findetect.feature.recovery

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinTopBar
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

private const val CodeLength = 6

/**
 * Восстановление пароля, шаг 1: "Введите код" (DESIGN.md 5.20). Стрелка "назад" и заголовок, пояснение
 * с адресом почты, шесть ячеек для цифр, ссылка "Отправить ещё раз", внизу "Продолжить".
 *
 * Сервера пока нет: [onContinue] вызывается, когда введены все шесть цифр (верность кода проверит сервер),
 * [onResend] вызывается по "Отправить ещё раз" и очищает введённый код. Почта пока заглушка из макета,
 * позже её передадут с экрана входа.
 */
@Composable
fun RecoveryCodeScreen(
    email: String = stringResource(R.string.ui_login_email_placeholder),
    onBack: () -> Unit = {},
    onResend: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    var code by rememberSaveable { mutableStateOf("") }
    // Ошибку показываем только после нажатия на "Продолжить" с неполным кодом
    var submitted by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val c = FinTheme.colors
    val hintStyle = FinTheme.typography.label.copy(fontWeight = FontWeight.Normal)
    val codeError = submitted && code.length < CodeLength

    // Сразу открываем клавиатуру: код вводят цифрами с неё
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        FinTopBar(
            title = stringResource(R.string.ui_recovery_code_title),
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.ui_recovery_code_hint),
                    style = hintStyle,
                    color = c.muted,
                    textAlign = TextAlign.Center,
                )
                Text(text = email, style = hintStyle, color = c.navy, textAlign = TextAlign.Center)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CodeInput(
                    code = code,
                    isError = codeError,
                    focusRequester = focusRequester,
                    onCodeChange = { code = it },
                )
                if (codeError) {
                    Text(
                        text = stringResource(R.string.ui_recovery_code_error),
                        style = FinTheme.typography.caption,
                        color = c.red,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.ui_recovery_code_no_code),
                    style = hintStyle,
                    color = c.muted,
                )
                Text(
                    text = stringResource(R.string.ui_recovery_code_resend),
                    style = FinTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                    color = c.navy,
                    modifier = Modifier
                        .clip(FinTheme.shapes.r8)
                        .clickable {
                            code = ""
                            submitted = false
                            onResend()
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinDimens.screenPadding)
                .padding(bottom = 16.dp),
        ) {
            FinButton(
                text = stringResource(R.string.ui_onboarding_continue),
                onClick = {
                    submitted = true
                    if (code.length == CodeLength) onContinue()
                },
            )
        }
    }
}

/**
 * Шесть ячеек под цифры. Ввод один, невидимый: он ловит клавиатуру и фокус, а ячейки рисуют его значение.
 * Пустая ячейка показывает точку, активная (куда пойдёт следующая цифра) обведена жёлтым, при ошибке все красные.
 */
@Composable
private fun CodeInput(
    code: String,
    isError: Boolean,
    focusRequester: FocusRequester,
    onCodeChange: (String) -> Unit,
) {
    val c = FinTheme.colors
    var focused by remember { mutableStateOf(false) }

    BasicTextField(
        // Курсор всегда в конце: цифры добавляются справа, стирается последняя
        value = TextFieldValue(code, selection = TextRange(code.length)),
        onValueChange = { onCodeChange(it.text.filter(Char::isDigit).take(CodeLength)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(CodeLength) { index ->
                    val active = focused && index == code.length.coerceAtMost(CodeLength - 1)
                    val borderColor = when {
                        isError -> c.red
                        active -> c.yellow
                        else -> c.line
                    }
                    val borderWidth = if (active || isError) FinDimens.borderFocused else FinDimens.borderWidth
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.8f)
                            .clip(FinTheme.shapes.r12)
                            .background(c.white)
                            .border(borderWidth, borderColor, FinTheme.shapes.r12),
                        contentAlignment = Alignment.Center,
                    ) {
                        val digit = code.getOrNull(index)
                        if (digit != null) {
                            Text(text = digit.toString(), style = FinTheme.typography.heading, color = c.navy)
                        } else {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(c.navy))
                        }
                    }
                }
            }
        },
    )
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RecoveryCodeScreenPreview() { FinDetectTheme { RecoveryCodeScreen() } }