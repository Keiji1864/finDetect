package com.ghostteam.findetect.feature.accounts

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.theme.*
import com.ghostteam.findetect.core.navigation.Destination
import com.ghostteam.findetect.feature.prototype.*

@Composable
fun AccountsScreen(onOpenSettings: () -> Unit = {}) {
    PrototypeScreen(R.string.ui_accounts, onSettings = onOpenSettings) {
        item(key = "balance") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.ui_total_balance), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                Text(stringResource(R.string.ui_balance_total), style = FinTheme.typography.balance, color = FinTheme.colors.navy)
            }
        }
        item(key = "main_account") {
            DisplayRow(R.string.ui_main_account, R.string.ui_ruble_account, R.string.ui_main_balance,
                Icons.Outlined.AccountBalanceWallet)
        }
        item(key = "cash") {
            DisplayRow(R.string.ui_cash, R.string.ui_wallet, R.string.ui_cash_balance,
                Icons.Outlined.Payments)
        }
        item(key = "savings") {
            DisplayRow(R.string.ui_savings, R.string.ui_future, R.string.ui_savings_balance,
                Icons.Outlined.AccountBalanceWallet)
        }
        item(key = "add") {
            OutlinedButton(
                onClick = {}, enabled = false,
                modifier = Modifier.fillMaxWidth().height(FinDimens.buttonHeight),
                shape = FinTheme.shapes.r12,
                border = BorderStroke(1.dp, FinTheme.colors.slate),
                colors = ButtonDefaults.outlinedButtonColors(
                    disabledContainerColor = FinTheme.colors.white,
                    disabledContentColor = FinTheme.colors.muted,
                ),
            ) {
                Text(stringResource(R.string.ui_add_account), style = FinTheme.typography.label)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountsPreview() { MainScreenPreview(Destination.Accounts.route) { AccountsScreen() } }
