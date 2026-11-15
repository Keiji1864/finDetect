package com.ghostteam.findetect.feature.analytics

import android.content.res.Configuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinLazySubScreen
import com.ghostteam.findetect.core.designsystem.component.FinNotice
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.model.TransactionsDay
import com.ghostteam.findetect.feature.transaction.FinancePeriodSheet
import com.ghostteam.findetect.feature.transaction.localizedName
import java.math.BigDecimal

@Composable
fun CategoryDetailRoute(
    onBack: () -> Unit,
    onEditTransaction: (String) -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val calendar by viewModel.period.state.collectAsStateWithLifecycle()
    AnalyticsOnResume(viewModel::onResume)
    val request = viewModel.request()
    val visibleState = state.takeIf { it.data.request == request } ?: CategoryDetailUiState(AnalyticsUiState(request))
    CategoryDetailScreen(visibleState, onBack, viewModel.period::open, viewModel::retry, onEditTransaction)
    if (calendar.open) FinancePeriodSheet(viewModel.period, viewModel::applyPeriod)
}

@Composable
internal fun CategoryDetailScreen(
    state: CategoryDetailUiState,
    onBack: () -> Unit,
    onOpenPeriod: () -> Unit,
    onRetry: () -> Unit,
    onEditTransaction: (String) -> Unit,
) {
    val data = state.data
    val category = state.category
    val summary = data.summary
    val currency = data.currency?.name
    val categoryTotal = summary?.transactions?.categories?.firstOrNull { it.category.id == category?.id }
    val history = remember(summary, category?.id) {
        summary?.transactions?.days.orEmpty().mapNotNull { day ->
            day.transactions.filter { it.category.id == category?.id }.takeIf { it.isNotEmpty() }
                ?.let { TransactionsDay(day.date, it) }
        }
    }
    FinLazySubScreen(category?.localizedName() ?: stringResource(R.string.ui_category), onBack) {
        item(key = "summary") {
            PeriodSummary(if (summary != null && category != null) categoryTotal?.amount ?: BigDecimal.ZERO.setScale(2) else null,
                currency, data.request.period, onOpenPeriod)
        }
        if (data.loading || data.error) {
            item(key = "status") { AnalyticsStatus(data, onRetry) }
        }
        if (category != null && summary != null && currency != null) {
            item(key = "share") {
                Text(stringResource(if (data.request.isIncome) R.string.ui_share_of_income else R.string.ui_share_of_expense,
                    categoryTotal?.percentage ?: 0), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
            }
            if (data.request.isIncome && data.request.today >= data.request.period.start && data.request.today <= data.request.period.end) {
                item(key = "month-note") { IncomePeriodNote(data.request) }
            }
            if (history.isEmpty()) {
                item(key = "empty") {
                    FinNotice(stringResource(R.string.analytics_category_empty), text = stringResource(R.string.analytics_empty_hint))
                }
            } else {
                if (!data.request.isIncome || summary.buckets.size > 1) {
                    item(key = "chart") { AnalyticsChart(summary, data.request, currency, category = category) }
                }
                analyticsHistory(history, onEditTransaction)
            }
        } else if (summary != null && !data.loading && !data.error) {
            item(key = "missing-category") { FinNotice(stringResource(R.string.analytics_category_missing)) }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryDetailPreview() {
    val data = analyticsPreviewState()
    FinDetectTheme {
        CategoryDetailScreen(CategoryDetailUiState(data, data.summary?.transactions?.categories?.first()?.category), {}, {}, {}, {})
    }
}
