package com.ghostteam.findetect.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

/** Password field with its own show/hide button; visibility is kept only in memory and starts hidden. */
@Composable
fun FinPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorText: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(value, enabled) { if (value.isEmpty() || !enabled) visible = false }
    FinTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        errorText = errorText,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            IconToggleButton(checked = visible, onCheckedChange = { visible = it }, enabled = enabled,
                modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(if (visible) R.drawable.ic_eye_slash else R.drawable.ic_eye),
                    contentDescription = stringResource(if (visible) R.string.ui_recovery_password_hide else R.string.ui_recovery_password_show),
                    tint = if (enabled) FinTheme.colors.navy else FinTheme.colors.muted,
                    modifier = Modifier.size(FinDimens.iconSize),
                )
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FinPasswordFieldPreview() {
    FinDetectTheme {
        Column(Modifier.background(FinTheme.colors.paper).padding(FinDimens.screenPadding)) {
            FinPasswordField(label = stringResource(R.string.ui_login_password_label), value = "password123", onValueChange = {},
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done))
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FinPasswordFieldDisabledPreview() {
    FinDetectTheme {
        Column(Modifier.background(FinTheme.colors.paper).padding(FinDimens.screenPadding)) {
            FinPasswordField(label = stringResource(R.string.ui_login_password_label), value = "password123", onValueChange = {}, enabled = false)
        }
    }
}
