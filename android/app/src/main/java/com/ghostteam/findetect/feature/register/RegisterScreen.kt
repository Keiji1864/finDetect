package com.ghostteam.findetect.feature.register

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinTextField
import com.ghostteam.findetect.core.designsystem.component.FinPasswordField
import com.ghostteam.findetect.core.designsystem.component.FinTopBar
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.model.AuthField
import com.ghostteam.findetect.feature.session.*

@Composable
fun RegisterScreen(
    onBack: () -> Unit = {},
    onRegister: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    state: AuthUiState = AuthUiState(),
    onFieldChange: (AuthField) -> Unit = {},
    onOpenLogin: (String) -> Unit = {},
    message: String? = null,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val localErrors = if (submitted && !state.accountCreated) AuthFormValidation.registration(name, email, password, repeat) else emptyMap()
    val working = state.working
    val editable = !working && !state.accountCreated
    val feedback = message ?: authFormMessage(state)
    val focus = LocalFocusManager.current
    val submit: () -> Unit = {
        if (!state.submissionBlocked) {
            submitted = true
            focus.clearFocus()
            onRegister(name.trim(), email.trim(), password, repeat)
        }
    }
    BackHandler(enabled = working) {}

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
                onValueChange = { name = it; onFieldChange(AuthField.NAME) },
                enabled = editable,
                placeholder = stringResource(R.string.ui_register_name_placeholder),
                errorText = authFieldText(AuthField.NAME, localErrors[AuthField.NAME] ?: state.fieldErrors[AuthField.NAME]),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            FinTextField(
                label = stringResource(R.string.ui_login_email_label),
                value = email,
                onValueChange = { email = it; onFieldChange(AuthField.EMAIL) },
                enabled = editable,
                placeholder = stringResource(R.string.ui_login_email_placeholder),
                errorText = authFieldText(AuthField.EMAIL, localErrors[AuthField.EMAIL] ?: state.fieldErrors[AuthField.EMAIL]),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            FinPasswordField(
                label = stringResource(R.string.ui_login_password_label),
                value = password,
                onValueChange = { password = it; onFieldChange(AuthField.PASSWORD) },
                enabled = editable,
                placeholder = stringResource(R.string.ui_login_password_placeholder),
                errorText = authFieldText(AuthField.PASSWORD, localErrors[AuthField.PASSWORD] ?: state.fieldErrors[AuthField.PASSWORD]),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            )
            FinPasswordField(
                label = stringResource(R.string.ui_register_repeat_label),
                value = repeat,
                onValueChange = { repeat = it; onFieldChange(AuthField.CONFIRMATION) },
                enabled = editable,
                placeholder = stringResource(R.string.ui_login_password_placeholder),
                errorText = authFieldText(AuthField.CONFIRMATION, localErrors[AuthField.CONFIRMATION] ?: state.fieldErrors[AuthField.CONFIRMATION]),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            if (feedback != null) Text(feedback, style = FinTheme.typography.caption, color = FinTheme.colors.red)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = FinDimens.screenPadding).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap)) {
            if (!state.accountCreated) FinButton(
                text = stringResource(R.string.ui_register_submit),
                onClick = submit,
                loading = working,
                enabled = !state.submissionBlocked,
            )
            if (state.offerLogin) FinButton(stringResource(R.string.auth_go_to_login), { onOpenLogin(email.trim()) },
                enabled = !working, style = FinButtonStyle.Secondary)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegisterScreenPreview() { FinDetectTheme { RegisterScreen() } }

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegisterRecoveryPreview() {
    FinDetectTheme { RegisterScreen(state = AuthUiState(accountCreated = true)) }
}
