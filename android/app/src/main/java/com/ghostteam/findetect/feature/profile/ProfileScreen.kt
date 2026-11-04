package com.ghostteam.findetect.feature.profile

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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

/**
 * Профиль (Figma 09): аватар, имя и почта, поля, "Сменить пароль", "Уровень", выход и удаление аккаунта.
 * Выход из аккаунта только здесь (из Настроек убран).
 * onChangePassword, onLogout, onDeleteAccount пока без реализации: экранов смены пароля и входа,
 * а также диалога удаления в макете нет.
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
) {
    val defaultName = stringResource(R.string.ui_profile_name_value)
    val defaultEmail = stringResource(R.string.ui_profile_email_value)
    var name by rememberSaveable { mutableStateOf(defaultName) }
    var email by rememberSaveable { mutableStateOf(defaultEmail) }

    FinSubScreen(title = stringResource(R.string.ui_profile_title), onBack = onBack) {
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
                Icon(Icons.Outlined.PersonOutline, null, tint = FinTheme.colors.navy, modifier = Modifier.size(40.dp))
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
            value = name,
            onValueChange = { name = it.take(40) },
            placeholder = defaultName,
        )
        FinTextField(
            label = stringResource(R.string.ui_profile_email_label),
            value = email,
            onValueChange = { email = it.take(60) },
            placeholder = defaultEmail,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        FinRow(
            title = stringResource(R.string.ui_change_password),
            subtitle = stringResource(R.string.ui_change_password_hint),
            leading = { UiIconTile(Icons.Outlined.Lock) },
            onClick = onChangePassword,
        )
        FinRow(
            title = stringResource(R.string.ui_level),
            subtitle = stringResource(R.string.ui_level_hint),
            leading = { UiIconTile(Icons.Outlined.EmojiEvents) },
        )
        FinButton(
            text = stringResource(R.string.ui_sign_out),
            onClick = onLogout,
            style = FinButtonStyle.Secondary,
        )
        Text(
            text = stringResource(R.string.ui_delete_account),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(FinTheme.shapes.r12)
                .clickable(onClick = onDeleteAccount)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            style = FinTheme.typography.label,
            color = FinTheme.colors.red,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenPreview() { FinDetectTheme { ProfileScreen() } }