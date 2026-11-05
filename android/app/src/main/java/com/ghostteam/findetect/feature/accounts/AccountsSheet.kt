package com.ghostteam.findetect.feature.accounts

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinBottomSheet
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinRow
import com.ghostteam.findetect.core.designsystem.component.FinSwitchPanel
import com.ghostteam.findetect.core.designsystem.component.FinTextField
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.feature.prototype.UiIconTile

/** Данные счёта для шторки в макете: только показ, ничего не сохраняется. */
data class AccountSheetSample(
    @StringRes val name: Int,
    @StringRes val balance: Int,
    val icon: ImageVector,
    @StringRes val iconName: Int,
)

/**
 * Шторка "Новый счёт" / "Изменить счёт" (Figma 03).
 * [sample] == null: создание нового счёта, иначе редактирование.
 * Макет: поля только показывают значения, нажимается лишь тумблер "Учитывать в прогнозе".
 * "Сохранить" и "Удалить счёт" просто закрывают шторку, данные не меняются.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(sample: AccountSheetSample?, onDismiss: () -> Unit) {
    val isNew = sample == null
    var includeInForecast by rememberSaveable { mutableStateOf(true) }

    FinBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap),
        ) {
            Text(
                text = stringResource(if (isNew) R.string.ui_acc_new_title else R.string.ui_acc_edit_title),
                style = FinTheme.typography.sheetTitle,
                color = FinTheme.colors.navy,
            )
            FinTextField(
                label = stringResource(R.string.ui_acc_name_label),
                value = stringResource(sample?.name ?: R.string.ui_acc_new_title),
                onValueChange = {},
                enabled = false,
            )
            FinTextField(
                label = stringResource(R.string.ui_acc_currency_label),
                value = stringResource(R.string.ui_acc_currency_rub),
                onValueChange = {},
                enabled = false,
            )
            FinTextField(
                label = stringResource(R.string.ui_acc_balance_label),
                value = stringResource(sample?.balance ?: R.string.ui_acc_zero_balance),
                onValueChange = {},
                enabled = false,
            )
            FinRow(
                title = stringResource(R.string.ui_acc_icon_label),
                subtitle = stringResource(sample?.iconName ?: R.string.ui_wallet),
                leading = { UiIconTile(sample?.icon ?: Icons.Outlined.AccountBalanceWallet) },
                inSheet = true,
            )
            FinSwitchPanel(
                title = stringResource(R.string.ui_acc_forecast),
                checked = includeInForecast,
                onCheckedChange = { includeInForecast = it },
            )
            FinButton(text = stringResource(R.string.ui_save), onClick = onDismiss)
            if (!isNew) {
                FinButton(
                    text = stringResource(R.string.ui_acc_delete),
                    onClick = onDismiss,
                    style = FinButtonStyle.Danger,
                )
            }
        }
    }
}