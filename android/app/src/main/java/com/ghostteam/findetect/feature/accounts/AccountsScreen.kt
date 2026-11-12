package com.ghostteam.findetect.feature.accounts

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.navigation.Destination
import com.ghostteam.findetect.feature.prototype.DisplayRow
import com.ghostteam.findetect.feature.prototype.MainScreenPreview
import com.ghostteam.findetect.feature.prototype.PrototypeScreen

/** Ключ шторки создания счёта; остальные ключи это [AccountItem.key]. */
private const val NewAccountKey = "new"

private data class AccountItem(
    val key: String,
    @StringRes val subtitle: Int,
    val sample: AccountSheetSample,
)

private val accountItems = listOf(
    AccountItem(
        key = "main",
        subtitle = R.string.ui_ruble_account,
        sample = AccountSheetSample(R.string.ui_main_account, R.string.ui_main_balance, R.drawable.ic_wallet, R.string.ui_wallet),
    ),
    AccountItem(
        key = "cash",
        subtitle = R.string.ui_wallet,
        sample = AccountSheetSample(R.string.ui_cash, R.string.ui_cash_balance, R.drawable.ic_cash, R.string.ui_acc_icon_cash),
    ),
    AccountItem(
        key = "savings",
        subtitle = R.string.ui_future,
        sample = AccountSheetSample(R.string.ui_savings, R.string.ui_savings_balance, R.drawable.ic_wallet, R.string.ui_wallet),
    ),
)

@Composable
fun AccountsScreen(onOpenAnalytics: () -> Unit = {}, onOpenSettings: () -> Unit = {}) {
    // null: шторка закрыта, NewAccountKey: новый счёт, иначе ключ редактируемого счёта
    var sheetKey by rememberSaveable { mutableStateOf<String?>(null) }

    PrototypeScreen(R.string.ui_accounts, onAnalytics = onOpenAnalytics, onSettings = onOpenSettings) {
        item(key = "balance") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.ui_total_balance), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                Text(stringResource(R.string.ui_balance_total), style = FinTheme.typography.balance, color = FinTheme.colors.navy)
            }
        }
        items(accountItems, key = { it.key }) { account ->
            DisplayRow(
                title = account.sample.name,
                subtitle = account.subtitle,
                amount = account.sample.balance,
                icon = account.sample.icon,
                onClick = { sheetKey = account.key },
            )
        }
        item(key = "add") {
            FinButton(
                text = stringResource(R.string.ui_add_account),
                onClick = { sheetKey = NewAccountKey },
                style = FinButtonStyle.Secondary,
            )
        }
    }

    sheetKey?.let { key ->
        AccountSheet(
            sample = accountItems.firstOrNull { it.key == key }?.sample,
            onDismiss = { sheetKey = null },
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountsPreview() { MainScreenPreview(Destination.Accounts.route) { AccountsScreen() } }