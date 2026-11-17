package com.ghostteam.findetect.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import android.content.res.Configuration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import androidx.compose.ui.Alignment

@Composable
private fun FieldContainer(
    label: String,
    borderColor: androidx.compose.ui.graphics.Color,
    borderWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    val shape = FinTheme.shapes.r12
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = FinDimens.fieldMinHeight)
            .clip(shape)
            .background(FinTheme.colors.white)
            .border(borderWidth, borderColor, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = label, style = FinTheme.typography.caption, color = FinTheme.colors.muted)
        value()
    }
}

/**
 * Поле-выбор: Категория, Счёт, Дата, Тема, Язык. Только показывает значение, по нажатию вызывает onClick
 */
@Composable
fun FinSelectField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    val c = FinTheme.colors
    FieldContainer(
        label = label,
        borderColor = c.line,
        borderWidth = FinDimens.borderWidth,
        modifier = modifier
            .clip(FinTheme.shapes.r12)
            .clickable(onClick = onClick),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = FinTheme.typography.body, color = c.muted)
        } else {
            Text(value, style = FinTheme.typography.body, color = c.navy)
        }
    }
}

/**
 * Поле ввода текста: Название, Комментарий, Почта, Пароль, Сумма.
 * Открывает системную клавиатуру. Состояния: пустое (placeholder), в фокусе (1.5 dp navy), ошибка (red + текст).
 * Для пароля передай visualTransformation = PasswordVisualTransformation().
 * [keyboardActions] lets forms handle IME confirmation with the same guard as their submit button.
 * [trailing] adds optional content on the right, such as the password visibility toggle.
 */
@Composable
fun FinTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = FinTheme.colors
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        errorText != null -> c.red
        focused -> c.navy
        else -> c.line
    }
    val borderWidth = if (focused || errorText != null) FinDimens.borderFocused else FinDimens.borderWidth

    Column(modifier = modifier.fillMaxWidth()) {
        Box {
            FieldContainer(label = label, borderColor = borderColor, borderWidth = borderWidth) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = FinTheme.typography.body.copy(color = c.navy),
                    cursorBrush = SolidColor(c.navy),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    interactionSource = remember { MutableInteractionSource() },
                    modifier = Modifier
                        .fillMaxWidth()
                        // место под иконку справа (например, глаз у пароля), чтобы текст не заходил под неё
                        .padding(end = if (trailing != null) 32.dp else 0.dp)
                        .onFocusChanged { focused = it.isFocused },
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(placeholder, style = FinTheme.typography.body, color = c.muted)
                            }
                            inner()
                        }
                    },
                )
            }
            if (trailing != null) {
                Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) { trailing() }
            }
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = FinTheme.typography.caption,
                color = c.red,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FinTextFieldImePreview() {
    FinDetectTheme {
        Column(Modifier.background(FinTheme.colors.paper).padding(FinDimens.screenPadding)) {
            FinTextField(label = stringResource(R.string.ui_login_email_label), value = "", onValueChange = {},
                placeholder = stringResource(R.string.ui_login_email_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = {}))
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FinTextFieldTrailingPreview() {
    FinDetectTheme {
        Column(Modifier.background(FinTheme.colors.paper).padding(FinDimens.screenPadding)) {
            FinTextField(
                label = stringResource(R.string.ui_login_password_label),
                value = "",
                onValueChange = {},
                placeholder = stringResource(R.string.ui_login_password_placeholder),
                visualTransformation = PasswordVisualTransformation(),
                trailing = {
                    Icon(painterResource(R.drawable.ic_lock), null, tint = FinTheme.colors.navy,
                        modifier = Modifier.size(FinDimens.iconSize))
                },
            )
        }
    }
}
