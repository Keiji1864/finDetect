package com.ghostteam.findetect.feature.analytics

import android.content.res.Configuration
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinLazySubScreen
import com.ghostteam.findetect.core.designsystem.component.FinNotice
import com.ghostteam.findetect.core.designsystem.component.FinRow
import com.ghostteam.findetect.core.designsystem.component.FinSegmentedSwitch
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.util.Period
import com.ghostteam.findetect.core.util.formatMoney
import com.ghostteam.findetect.core.util.formatMonth
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.usecase.buildAnalyticsSummary
import com.ghostteam.findetect.feature.transaction.FinancePeriodSheet
import com.ghostteam.findetect.feature.transaction.transactionsPreviewSummary
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@Composable
fun AnalyticsRoute(
    onBack: () -> Unit,
    onOpenCategory: (Boolean, String, Period) -> Unit,
    onOpenMonth: (Period) -> Unit,
    onEditTransaction: (String) -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val income by viewModel.isIncome.collectAsStateWithLifecycle()
    val selection = if (income) viewModel.incomePeriod else viewModel.expensePeriod
    val calendar by selection.state.collectAsStateWithLifecycle()
    AnalyticsOnResume(viewModel::onResume)
    val request = viewModel.request()
    val visibleState = state.takeIf { it.request == request } ?: AnalyticsUiState(request)
    AnalyticsScreen(visibleState, viewModel.monthDetail, onBack, viewModel::selectType, selection::open,
        viewModel::retry, onOpenCategory, onOpenMonth, onEditTransaction)
    if (calendar.open) FinancePeriodSheet(selection, viewModel::applyPeriod)
}

@Composable
internal fun AnalyticsScreen(
    state: AnalyticsUiState,
    monthDetail: Boolean,
    onBack: () -> Unit,
    onSelectType: (Boolean) -> Unit,
    onOpenPeriod: () -> Unit,
    onRetry: () -> Unit,
    onOpenCategory: (Boolean, String, Period) -> Unit,
    onOpenMonth: (Period) -> Unit,
    onEditTransaction: (String) -> Unit,
) {
    val request = state.request
    val summary = state.summary
    val currency = state.currency?.name
    val transactions = summary?.transactions
    FinLazySubScreen(stringResource(if (monthDetail) R.string.ui_income else R.string.ui_analytics), onBack) {
        if (!monthDetail) {
            item(key = "type") {
                FinSegmentedSwitch(
                    options = listOf(false to stringResource(R.string.ui_expenses), true to stringResource(R.string.ui_income)),
                    selected = request.isIncome,
                    onSelect = onSelectType,
                )
            }
        }
        item(key = "summary") { PeriodSummary(transactions?.total, currency, request.period, onOpenPeriod) }
        item(key = "accounts") {
            Text(stringResource(R.string.analytics_all_accounts), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
        }
        if (state.loading || state.error) {
            item(key = "status") { AnalyticsStatus(state, onRetry) }
        }
        if (request.isIncome && request.today >= request.period.start && request.today <= request.period.end) {
            item(key = "month-note") { IncomePeriodNote(request) }
        }
        if (transactions != null && transactions.days.isEmpty()) {
            item(key = "empty") {
                FinNotice(stringResource(if (request.isIncome) R.string.transactions_no_income else R.string.transactions_no_expenses),
                    text = stringResource(R.string.analytics_empty_hint))
            }
        }
        if (summary != null && transactions != null && currency != null && transactions.days.isNotEmpty()) {
            if (!monthDetail && (!request.isIncome || summary.buckets.size > 1)) {
                item(key = "chart") {
                    AnalyticsChart(summary, request, currency, onOpenMonth = if (request.isIncome) onOpenMonth else null)
                }
            }
            if (request.isIncome && !monthDetail) {
                item(key = "months-title") {
                    Text(stringResource(R.string.analytics_months), style = FinTheme.typography.sectionTitle, color = FinTheme.colors.navy)
                }
                items(summary.buckets, key = { "month-${it.start}" }) { bucket ->
                    val locale = LocalConfiguration.current.locales[0]
                    FinRow(title = bucket.start.formatMonth(locale),
                        subtitle = if (bucket.start > request.today) stringResource(R.string.analytics_month_future)
                            else if (bucket.end >= request.today) stringResource(R.string.analytics_month_unfinished)
                            else if (bucket.start.dayOfMonth != 1 || bucket.end != Period.currentMonth(bucket.start).end)
                                stringResource(R.string.analytics_month_partial) else null,
                        trailing = { Text(formatMoney(bucket.total, currency, locale),
                            style = FinTheme.typography.label, color = FinTheme.colors.green) },
                        onClick = { onOpenMonth(Period(bucket.start, bucket.end)) })
                }
                if (summary.buckets.any { it.total.signum() == 0 }) {
                    item(key = "zero-months") {
                        Text(stringResource(R.string.analytics_zero_months_hint),
                            style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                    }
                }
            }
            item(key = "categories-title") {
                Text(stringResource(R.string.ui_categories), style = FinTheme.typography.sectionTitle, color = FinTheme.colors.navy)
            }
            items(transactions.categories, key = { "category-${it.category.id}" }) { category ->
                AnalyticsCategoryRow(category, currency) { onOpenCategory(request.isIncome, category.category.id, request.period) }
            }
            if (monthDetail) analyticsHistory(transactions.days, onEditTransaction)
        }
    }
}

internal fun analyticsPreviewState(isIncome: Boolean = false): AnalyticsUiState {
    val summary = transactionsPreviewSummary(isIncome)
    val period = if (isIncome) Period(LocalDate(2026, 6, 1), LocalDate(2026, 11, 30))
        else Period(LocalDate(2026, 11, 1), LocalDate(2026, 11, 30))
    val request = AnalyticsRequest(isIncome, period, LocalDate(2026, 11, 15), TimeZone.UTC)
    return AnalyticsUiState(request, AppCurrency.RUB,
        buildAnalyticsSummary(summary, isIncome, period.start, period.end), loading = false)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AnalyticsPreview() {
    FinDetectTheme { AnalyticsScreen(analyticsPreviewState(), false, {}, {}, {}, {}, { _, _, _ -> }, {}, {}) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IncomeAnalyticsPreview() {
    FinDetectTheme { AnalyticsScreen(analyticsPreviewState(true), false, {}, {}, {}, {}, { _, _, _ -> }, {}, {}) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IncomeMonthPreview() {
    val data = analyticsPreviewState(true)
    val month = Period.currentMonth(data.request.today)
    val state = data.copy(request = data.request.copy(period = month),
        summary = buildAnalyticsSummary(requireNotNull(data.summary).transactions, true, month.start, month.end))
    FinDetectTheme { AnalyticsScreen(state, true, {}, {}, {}, {}, { _, _, _ -> }, {}, {}) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyAnalyticsPreview() {
    val data = analyticsPreviewState()
    val empty = com.ghostteam.findetect.domain.model.TransactionsSummary(java.math.BigDecimal.ZERO.setScale(2), emptyList(), emptyList())
    val state = data.copy(summary = buildAnalyticsSummary(empty, false, data.request.period.start, data.request.period.end))
    FinDetectTheme { AnalyticsScreen(state, false, {}, {}, {}, {}, { _, _, _ -> }, {}, {}) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AnalyticsErrorPreview() {
    val state = analyticsPreviewState().copy(summary = null, loading = false, error = true)
    FinDetectTheme { AnalyticsScreen(state, false, {}, {}, {}, {}, { _, _, _ -> }, {}, {}) }
}
