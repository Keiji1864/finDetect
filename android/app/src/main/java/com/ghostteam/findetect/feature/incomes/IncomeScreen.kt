package com.ghostteam.findetect.feature.incomes

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
fun IncomeRoute(
    period: Period,
    onAddTransaction: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onEditTransaction: (String) -> Unit = {},
) {
    TransactionsRoute(
        isIncome = true,
        period = period,
        onOpenPeriod = null,
        onAddTransaction = onAddTransaction,
        onOpenSettings = onOpenSettings,
        onOpenAnalytics = onOpenAnalytics,
        onEditTransaction = onEditTransaction,
    )
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IncomeScreenPreview() {
    MainScreenPreview(Destination.Incomes.route) {
        TransactionsScreen(TransactionsUiState(currency = AppCurrency.RUB, summary = transactionsPreviewSummary(true), loading = false),
            true, Period.lastSixCalendarMonths(LocalDate(2026, 11, 14)), null, {})
    }
}
