package com.ghostteam.findetect.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinBottomBar
import com.ghostteam.findetect.core.designsystem.component.FinTab

@Composable
fun BottomBar(selectedRoute: String?, onSelect: (String) -> Unit) {
    FinBottomBar(
        tabs = listOf(
            FinTab(Destination.Home.route, stringResource(R.string.ui_home), icon = Icons.Outlined.Home),
            FinTab(Destination.Expenses.route, stringResource(R.string.ui_expenses), icon = Icons.Outlined.ReceiptLong),
            FinTab(Destination.Incomes.route, stringResource(R.string.ui_income), icon = Icons.Outlined.Timer),
            FinTab(Destination.Accounts.route, stringResource(R.string.ui_accounts), icon = Icons.Outlined.AccountBalanceWallet),
        ),
        selectedRoute = selectedRoute,
        onSelect = { onSelect(it.route) },
    )
}
