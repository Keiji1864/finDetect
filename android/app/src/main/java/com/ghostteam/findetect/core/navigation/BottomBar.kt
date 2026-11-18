package com.ghostteam.findetect.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinBottomBar
import com.ghostteam.findetect.core.designsystem.component.FinTab

@Composable
fun BottomBar(selectedRoute: String?, onSelect: (String) -> Unit) {
    FinBottomBar(
        tabs = listOf(
            FinTab(Destination.Home.route, stringResource(R.string.ui_home), iconRes = R.drawable.ic_home),
            FinTab(Destination.Expenses.route, stringResource(R.string.ui_expenses), iconRes = R.drawable.ic_expenses),
            FinTab(Destination.Incomes.route, stringResource(R.string.ui_income), iconRes = R.drawable.ic_income),
            FinTab(Destination.Accounts.route, stringResource(R.string.ui_accounts), iconRes = R.drawable.ic_accounts),
        ),
        selectedRoute = selectedRoute,
        onSelect = { onSelect(it.route) },
    )
}