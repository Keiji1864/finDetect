package com.ghostteam.findetect.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinIconTile
import com.ghostteam.findetect.core.designsystem.component.FinPeriodChip
import com.ghostteam.findetect.core.designsystem.component.FinRow
import com.ghostteam.findetect.core.designsystem.drawableResource
import com.ghostteam.findetect.core.designsystem.financeIcon
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.util.Period
import com.ghostteam.findetect.core.util.formatDate
import com.ghostteam.findetect.core.util.formatMoney
import com.ghostteam.findetect.core.util.formatMonth
import com.ghostteam.findetect.core.util.formatSignedMoney
import com.ghostteam.findetect.domain.model.CategoryTotal
import com.ghostteam.findetect.domain.model.TransactionsDay
import com.ghostteam.findetect.feature.transaction.localizedName
import java.math.BigDecimal

@Composable
internal fun PeriodSummary(total: BigDecimal?, currency: String?, period: Period, onChipClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val label = if (period == Period.currentMonth(period.start)) period.start.formatMonth(locale)
        else stringResource(R.string.transactions_period_range, period.start.formatDate(locale), period.end.formatDate(locale))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FinPeriodChip(label, onClick = onChipClick)
        if (total != null && currency != null) {
            Text(formatMoney(total, currency, locale), style = FinTheme.typography.balance, color = FinTheme.colors.navy)
        }
    }
}

@Composable
internal fun AnalyticsStatus(state: AnalyticsUiState, onRetry: () -> Unit) {
    if (state.loading) {
        Text(stringResource(R.string.finance_loading), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
    }
    if (state.error) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.analytics_read_error), style = FinTheme.typography.caption, color = FinTheme.colors.red)
            FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary)
        }
    }
}

@Composable
internal fun IncomePeriodNote(request: AnalyticsRequest) {
    val locale = LocalConfiguration.current.locales[0]
    if (request.period.start <= request.today && request.period.end >= request.today) {
        Text(stringResource(R.string.analytics_current_month_hint, request.today.formatDate(locale)),
            style = FinTheme.typography.caption, color = FinTheme.colors.muted)
    }
}

@Composable
internal fun AnalyticsCategoryRow(total: CategoryTotal, currency: String, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    FinRow(title = total.category.localizedName(), subtitle = stringResource(R.string.ui_percentage, total.percentage),
        leading = {
            FinIconTile { Icon(painterResource(financeIcon(total.category.iconId).drawableResource), null,
                tint = FinTheme.colors.navy, modifier = Modifier.size(24.dp)) }
        },
        trailing = { Text(formatMoney(total.amount, currency, locale), style = FinTheme.typography.label,
            color = if (total.category.isIncome) FinTheme.colors.green else FinTheme.colors.navy) },
        onClick = onClick)
}

internal fun LazyListScope.analyticsHistory(days: List<TransactionsDay>, onEditTransaction: (String) -> Unit) {
    item(key = "history-title") {
        Text(stringResource(R.string.analytics_history), style = FinTheme.typography.sectionTitle, color = FinTheme.colors.navy)
    }
    days.forEach { day ->
        item(key = "day-${day.date}") {
            Text(day.date.formatDate(LocalConfiguration.current.locales[0]),
                style = FinTheme.typography.caption, color = FinTheme.colors.muted)
        }
        items(day.transactions, key = { "transaction-${it.transaction.id}" }) { details ->
            val locale = LocalConfiguration.current.locales[0]
            val categoryName = details.category.localizedName()
            val comment = details.transaction.comment?.takeIf { it.isNotBlank() }
            FinRow(title = comment ?: categoryName,
                subtitle = categoryName.takeIf { comment != null },
                leading = {
                    FinIconTile { Icon(painterResource(financeIcon(details.category.iconId).drawableResource), null,
                        tint = FinTheme.colors.navy, modifier = Modifier.size(24.dp)) }
                },
                trailing = { Text(formatSignedMoney(details.transaction.amount, details.currency, details.category.isIncome, locale),
                    style = FinTheme.typography.label,
                    color = if (details.category.isIncome) FinTheme.colors.green else FinTheme.colors.navy) },
                onClick = { onEditTransaction(details.transaction.id) })
        }
    }
}

@Composable
internal fun AnalyticsOnResume(onResume: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val callback by rememberUpdatedState(onResume)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) callback() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
