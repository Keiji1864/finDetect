package com.ghostteam.findetect.feature.expenses

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.core.navigation.Destination
import com.ghostteam.findetect.feature.prototype.MainScreenPreview
import com.ghostteam.findetect.core.util.Period
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.feature.transaction.TransactionsRoute
import com.ghostteam.findetect.feature.transaction.TransactionsScreen
import com.ghostteam.findetect.feature.transaction.TransactionsUiState
import com.ghostteam.findetect.feature.transaction.transactionsPreviewSummary
import kotlinx.datetime.LocalDate

@Composable
fun ExpensesRoute(
    period: Period,
    onOpenPeriod: () -> Unit,
    onAddTransaction: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
) {
    TransactionsRoute(false, period, onOpenPeriod, onAddTransaction, onOpenSettings, onOpenAnalytics)
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExpensesScreenPreview() {
    MainScreenPreview(Destination.Expenses.route) {
        TransactionsScreen(TransactionsUiState(currency = AppCurrency.RUB, summary = transactionsPreviewSummary(), loading = false),
            false, Period.currentMonth(LocalDate(2026, 11, 14)), {}, {})
    }
}
