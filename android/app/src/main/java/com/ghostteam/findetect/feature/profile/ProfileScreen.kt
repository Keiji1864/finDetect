package com.ghostteam.findetect.feature.profile

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinRow
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.component.FinTextField
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.feature.prototype.UiIconTile
import com.ghostteam.findetect.domain.model.ProfileFailure
import com.ghostteam.findetect.domain.model.SessionUser
import com.ghostteam.findetect.domain.usecase.CalculateXpProgressUseCase
import com.ghostteam.findetect.feature.gamification.XpProgressCard
import com.ghostteam.findetect.feature.gamification.XpProgressUiState

/**
 * Профиль (Figma 09): аватар, имя и почта, поля, "Сменить пароль", "Уровень", выход и удаление аккаунта.
 * Выход из аккаунта только здесь (из Настроек убран).
 * Confirmed identity and the editable draft are separate. Email is read-only, deletion requires confirmation.
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    gameElementsEnabled: Boolean = true,
    state: ProfileUiState = ProfileUiState(),
    working: Boolean = false,
    onNameChange: (String) -> Unit = {},
    onSaveName: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onDismissDelete: () -> Unit = {},
    onRetryXp: () -> Unit = {},
) {
    val name = state.user?.name ?: stringResource(R.string.ui_profile_name_value)
    val email = state.user?.email ?: stringResource(R.string.ui_profile_email_value)
    val draft = if (state.user == null) name else state.draftName
    val busy = working || state.busy
    val focus = LocalFocusManager.current
    val save: () -> Unit = { if (state.canSave && !working) { focus.clearFocus(); onSaveName() } }
    BackHandler(enabled = busy) {}

    FinSubScreen(title = stringResource(R.string.ui_profile_title), onBack = { if (!busy) onBack() }) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(FinDimens.avatarProfile)
                    .clip(FinTheme.shapes.round)
                    .background(FinTheme.colors.cream),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_user), null, tint = FinTheme.colors.navy, modifier = Modifier.size(40.dp))
            }
            Text(
                text = name,
                modifier = Modifier.padding(top = 8.dp),
                style = FinTheme.typography.heading,
                color = FinTheme.colors.navy,
            )
            Text(
                text = email,
                style = FinTheme.typography.label,
                color = FinTheme.colors.muted,
            )
        }
        FinTextField(
            label = stringResource(R.string.ui_profile_name_label),
            value = draft,
            onValueChange = onNameChange,
            enabled = !busy,
            placeholder = stringResource(R.string.ui_register_name_placeholder),
            errorText = when {
                state.dirty && !state.validName -> stringResource(R.string.auth_name_length)
                state.nameRejected -> stringResource(R.string.profile_name_rejected)
                else -> null
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
        )
        if (state.dirty || state.saving) FinButton(stringResource(R.string.ui_save), save,
            enabled = state.canSave && !working, loading = state.saving)
        if (state.saved) Text(stringResource(R.string.profile_name_saved), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
        FinTextField(
            label = stringResource(R.string.ui_profile_email_label),
            value = email,
            onValueChange = {},
            enabled = false,
            placeholder = email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        if (state.refreshing) Text(stringResource(R.string.finance_loading), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
        if (state.deleting) Text(stringResource(R.string.profile_deleting), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
        if (state.error != null && !(state.errorAction == ProfileAction.SAVE && state.nameRejected)) {
            Text(profileErrorText(state), style = FinTheme.typography.caption, color = FinTheme.colors.red)
            if (state.errorAction == ProfileAction.LOAD) FinButton(stringResource(R.string.finance_retry), onRefresh,
                enabled = !busy && !state.refreshing, style = FinButtonStyle.Secondary)
        }
        FinRow(
            title = stringResource(R.string.ui_change_password),
            subtitle = stringResource(R.string.ui_change_password_hint),
            leading = { UiIconTile(R.drawable.ic_lock) },
            onClick = { if (!busy) onChangePassword() },
        )
        if (gameElementsEnabled) XpProgressCard(state.xp, onRetryXp)
        FinButton(
            text = stringResource(R.string.ui_sign_out),
            onClick = onLogout,
            loading = working,
            enabled = !busy,
            style = FinButtonStyle.Secondary,
        )
        Text(
            text = stringResource(R.string.ui_delete_account),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(FinTheme.shapes.r12)
                .clickable(enabled = !busy, onClick = { focus.clearFocus(); onDeleteAccount() })
                .padding(horizontal = 16.dp, vertical = 8.dp),
            style = FinTheme.typography.label,
            color = FinTheme.colors.red,
        )
    }
    if (state.confirmDelete) AlertDialog(
        onDismissRequest = onDismissDelete,
        containerColor = FinTheme.colors.paper,
        title = { Text(stringResource(R.string.ui_delete_account), style = FinTheme.typography.heading, color = FinTheme.colors.navy) },
        text = { Text(stringResource(R.string.profile_delete_warning), style = FinTheme.typography.body, color = FinTheme.colors.navy) },
        confirmButton = { TextButton(onClick = onConfirmDelete, enabled = !busy) {
            Text(stringResource(R.string.ui_delete_account), style = FinTheme.typography.label, color = FinTheme.colors.red)
        } },
        dismissButton = { TextButton(onClick = onDismissDelete, enabled = !busy) {
            Text(stringResource(R.string.account_delete_cancel), style = FinTheme.typography.label, color = FinTheme.colors.navy)
        } },
    )
}

@Composable
private fun profileErrorText(state: ProfileUiState): String {
    if (state.errorAction == ProfileAction.DELETE) return stringResource(R.string.profile_delete_unconfirmed)
    return stringResource(when (state.error) {
        ProfileFailure.INVALID_NAME -> R.string.profile_name_rejected
        ProfileFailure.NETWORK -> R.string.session_network_error
        ProfileFailure.TIMEOUT -> R.string.auth_timeout
        ProfileFailure.RATE_LIMITED -> R.string.auth_rate_limited
        ProfileFailure.STORAGE -> R.string.profile_storage_error
        else -> R.string.session_server_error
    })
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenPreview() {
    FinDetectTheme {
        ProfileScreen(state = ProfileUiState(xp = XpProgressUiState(CalculateXpProgressUseCase()(700L), loading = false)))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileEditingPreview() {
    FinDetectTheme { ProfileScreen(state = ProfileUiState(user = SessionUser("preview", "student@example.com", "Алексей"), draftName = "Новое имя")) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileDeletePreview() { FinDetectTheme { ProfileScreen(state = ProfileUiState(confirmDelete = true)) } }
