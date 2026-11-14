package com.ghostteam.findetect.feature.transaction

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.drawableResource
import com.ghostteam.findetect.core.designsystem.financeIcon
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.navigation.Destination
import com.ghostteam.findetect.core.util.Period
import com.ghostteam.findetect.core.util.formatDate
import com.ghostteam.findetect.core.util.formatMoney
import com.ghostteam.findetect.core.util.formatMonth
import com.ghostteam.findetect.core.util.formatSignedMoney
import com.ghostteam.findetect.domain.model.*
import com.ghostteam.findetect.feature.prototype.MainScreenPreview
import com.ghostteam.findetect.feature.prototype.PrototypeScreen
import java.math.BigDecimal
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Composable
fun TransactionsRoute(
    isIncome: Boolean,
    period: Period,
    onOpenPeriod: () -> Unit,
    onAddTransaction: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAnalytics: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(isIncome, period) { viewModel.bind(isIncome, period) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, isIncome, period) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.bind(isIncome, period)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val visibleState = if (state.request?.let { it.period == period && it.isIncome == isIncome } == true) state
        else state.copy(summary = null, loading = true)
    TransactionsScreen(visibleState, isIncome, period, onOpenPeriod, onAddTransaction, onOpenSettings, onOpenAnalytics, viewModel::retry)
}

/** One data-driven layout serves both tabs; the period, list, chart and total share the same filter. */
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    isIncome: Boolean,
    period: Period,
    onOpenPeriod: () -> Unit,
    onAddTransaction: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val locale = LocalConfiguration.current.locales[0]
    val periodLabel = if (period == Period.currentMonth(period.start)) period.start.formatMonth(locale)
        else stringResource(R.string.transactions_period_range, period.start.formatDate(locale), period.end.formatDate(locale))
    val summary = state.summary
    val currency = state.currency?.name
    PrototypeScreen(if (isIncome) R.string.ui_income else R.string.ui_expenses,
        onSettings = onOpenSettings, onAnalytics = onOpenAnalytics,
        action = { FinButton(stringResource(if (isIncome) R.string.ui_add_income else R.string.ui_add_expense), onAddTransaction) }) {
        item(key = "summary") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FinPeriodChip(periodLabel, onClick = onOpenPeriod)
                if (summary != null && currency != null) {
                    Text(formatMoney(summary.total, currency, locale), style = FinTheme.typography.balance, color = FinTheme.colors.navy)
                }
            }
        }
        if (state.loading) {
            item(key = "loading") { Text(stringResource(R.string.finance_loading), color = FinTheme.colors.muted) }
        }
        if (state.error) {
            item(key = "error") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.transactions_read_error), color = FinTheme.colors.red, style = FinTheme.typography.caption)
                    FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary)
                }
            }
        }
        if (summary != null && summary.days.isEmpty()) {
            item(key = "empty") {
                FinNotice(stringResource(if (isIncome) R.string.transactions_no_income else R.string.transactions_no_expenses),
                    text = stringResource(R.string.transactions_empty_hint))
            }
        }
        if (summary != null && summary.categories.isNotEmpty() && currency != null) {
            item(key = "chart") { CategoryChart(summary.categories, summary.total, currency) }
            item(key = "insight") {
                val largest = summary.categories.first()
                FinAdviceCard(
                    title = stringResource(if (isIncome) R.string.transactions_largest_income else R.string.transactions_largest_expense,
                        largest.category.localizedName()),
                    text = stringResource(R.string.transactions_category_share, largest.percentage),
                )
            }
        }
        summary?.days?.forEach { day ->
            item(key = "day-${day.date}") {
                Text(day.date.formatDate(locale), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
            }
            items(day.transactions, key = { "transaction-${it.transaction.id}" }) { details -> TransactionRow(details) }
        }
    }
}

/** Historical row with an exact signed value; home uses a compact row and a dated subtitle. */
@Composable
fun TransactionRow(
    details: TransactionDetails,
    compact: Boolean = false,
    subtitle: String? = details.transaction.comment?.takeUnless { it.isBlank() },
) {
    val locale = LocalConfiguration.current.locales[0]
    FinRow(title = details.category.localizedName(), subtitle = subtitle, compact = compact,
        leading = {
            FinIconTile { Icon(painterResource(financeIcon(details.category.iconId).drawableResource), null,
                tint = FinTheme.colors.navy, modifier = Modifier.size(24.dp)) }
        },
        trailing = {
            Text(formatSignedMoney(details.transaction.amount, details.currency, details.category.isIncome, locale),
                style = FinTheme.typography.label, color = if (details.category.isIncome) FinTheme.colors.green else FinTheme.colors.navy)
        })
}

@Composable
internal fun Category.localizedName(): String = if (LocalConfiguration.current.locales[0].language == "en") names.en else names.ru

internal fun transactionsPreviewSummary(isIncome: Boolean = false): TransactionsSummary {
    val date = LocalDate(2026, 11, 14)
    val instant = Instant.parse("2026-11-14T10:00:00Z")
    val category = Category("preview-category", if (isIncome) "salary" else "coffee",
        if (isIncome) CategoryNames("Зарплата", "Salary") else CategoryNames("Кофе", "Coffee"),
        if (isIncome) "cash" else "coffee", isIncome)
    val amount = BigDecimal("125.00")
    val transaction = Transaction("preview-transaction", "preview-account", category.id, amount, instant, instant, instant)
    return TransactionsSummary(amount, listOf(TransactionsDay(date, listOf(TransactionDetails(transaction, category, "RUB")))),
        listOf(CategoryTotal(category, amount, 100)))
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransactionsScreenPreview() {
    MainScreenPreview(Destination.Expenses.route) {
        TransactionsScreen(TransactionsUiState(currency = AppCurrency.RUB, summary = transactionsPreviewSummary(), loading = false),
            false, Period.currentMonth(LocalDate(2026, 11, 14)), {}, {})
    }
}

@Preview(showBackground = true, widthDp = 364)
@Preview(showBackground = true, widthDp = 364, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransactionRowPreview() {
    FinDetectTheme { TransactionRow(transactionsPreviewSummary().days.first().transactions.first()) }
}

@Preview(showBackground = true, widthDp = 364)
@Preview(showBackground = true, widthDp = 364, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CompactTransactionRowPreview() {
    FinDetectTheme {
        TransactionRow(transactionsPreviewSummary(true).days.first().transactions.first(), compact = true,
            subtitle = LocalDate(2026, 11, 14).formatDate())
    }
}
