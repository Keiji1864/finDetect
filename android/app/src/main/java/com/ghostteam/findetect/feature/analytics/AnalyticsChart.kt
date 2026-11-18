package com.ghostteam.findetect.feature.analytics

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinCard
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.util.Period
import com.ghostteam.findetect.core.util.currencySymbol
import com.ghostteam.findetect.core.util.formatAmount
import com.ghostteam.findetect.core.util.formatDate
import com.ghostteam.findetect.core.util.formatMoney
import com.ghostteam.findetect.domain.model.AnalyticsBucket
import com.ghostteam.findetect.domain.model.AnalyticsGranularity
import com.ghostteam.findetect.domain.model.AnalyticsSummary
import com.ghostteam.findetect.domain.model.Category
import com.ghostteam.findetect.feature.transaction.categoryColor
import com.ghostteam.findetect.feature.transaction.localizedName
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormatSymbols
import java.util.Locale

private val ChartHeight = 120.dp

/** Decimal scale supports cents and totals above Int.MAX_VALUE without changing stored money. */
private fun axisStep(maximum: BigDecimal): BigDecimal {
    if (maximum.signum() <= 0) return BigDecimal.ONE
    val raw = maximum.divide(BigDecimal(3), 12, RoundingMode.CEILING).stripTrailingZeros()
    val magnitude = BigDecimal.ONE.scaleByPowerOfTen(raw.precision() - raw.scale() - 1)
    return listOf("1", "1.5", "2", "3", "5", "10").map { BigDecimal(it).multiply(magnitude) }
        .first { it >= raw }.max(BigDecimal("0.01"))
}

@Composable
private fun axisLabel(amount: BigDecimal, locale: Locale): String = when {
    amount >= BigDecimal("1000000000") -> stringResource(R.string.analytics_axis_billions,
        amount.divide(BigDecimal("1000000000"), 2, RoundingMode.HALF_UP).formatAmount(locale))
    amount >= BigDecimal("1000000") -> stringResource(R.string.analytics_axis_millions,
        amount.divide(BigDecimal("1000000"), 2, RoundingMode.HALF_UP).formatAmount(locale))
    amount >= BigDecimal("1000") -> stringResource(R.string.analytics_axis_thousands,
        amount.divide(BigDecimal("1000"), 2, RoundingMode.HALF_UP).formatAmount(locale))
    else -> amount.formatAmount(locale)
}

/** Monthly income totals and stacked expense buckets share an exact decimal scale and lazy scrolling. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AnalyticsChart(
    summary: AnalyticsSummary,
    request: AnalyticsRequest,
    currency: String,
    category: Category? = null,
    onOpenMonth: ((Period) -> Unit)? = null,
) {
    val c = FinTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val categories = if (category != null) listOf(category) else summary.transactions.categories.map { it.category }
    val colors = if (request.isIncome) listOf(c.green) else categories.map { categoryColor(it.key) }
    val values = remember(summary, request.isIncome, category) {
        summary.buckets.map { bucket ->
            when {
                category != null -> listOf(bucket.amounts[category.id] ?: BigDecimal.ZERO)
                request.isIncome -> listOf(bucket.total)
                else -> categories.map { bucket.amounts[it.id] ?: BigDecimal.ZERO }
            }
        }
    }
    val totals = remember(values) { values.map { parts -> parts.fold(BigDecimal.ZERO, BigDecimal::add) } }
    val step = remember(totals) { axisStep(totals.maxOrNull() ?: BigDecimal.ZERO) }
    val maximum = step.multiply(BigDecimal(3))
    val fractions = remember(values, maximum) {
        values.map { parts -> parts.map { it.divide(maximum, 12, RoundingMode.HALF_UP).toFloat() } }
    }
    val ticks = (0..3).map { axisLabel(step.multiply(BigDecimal(it)), locale) }
    val measurer = rememberTextMeasurer()
    val axisStyle = FinTheme.typography.axis
    val axisWidth = with(LocalDensity.current) { ticks.maxOf { measurer.measure(it, axisStyle).size.width }.toDp() } + 8.dp
    val monthly = summary.granularity == AnalyticsGranularity.MONTH
    val title = stringResource(when {
        request.isIncome -> R.string.ui_chart_income_months
        summary.granularity == AnalyticsGranularity.DAY -> R.string.ui_chart_expense_days
        summary.granularity == AnalyticsGranularity.WEEK -> R.string.analytics_chart_expense_weeks
        else -> R.string.ui_chart_expense_months
    })

    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = FinTheme.typography.label, color = c.navy)
            Text(stringResource(R.string.analytics_chart_currency, currencySymbol(currency)),
                style = FinTheme.typography.axis, color = c.muted)
            Row(Modifier.fillMaxWidth().padding(top = 7.dp)) {
                Box(Modifier.width(axisWidth).height(ChartHeight)) {
                    ticks.forEachIndexed { index, label ->
                        Text(label, Modifier.align(Alignment.TopEnd)
                            .offset(y = ChartHeight * (3 - index) / 3 - 7.dp).padding(end = 8.dp),
                            style = axisStyle, color = c.muted, maxLines = 1, softWrap = false)
                    }
                }
                BoxWithConstraints(Modifier.weight(1f)) {
                    val minSlot = when (summary.granularity) {
                        AnalyticsGranularity.DAY -> 6.dp
                        AnalyticsGranularity.WEEK -> 40.dp
                        AnalyticsGranularity.MONTH -> 32.dp
                    }
                    val slot = maxOf(minSlot, (maxWidth - 24.dp).coerceAtLeast(minSlot) / summary.buckets.size.coerceAtLeast(1))
                    Canvas(Modifier.fillMaxWidth().height(ChartHeight)) {
                        for (i in 0..3) {
                            val y = size.height * i / 3f
                            drawLine(c.line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                        }
                    }
                    key(request.isIncome, request.period, category?.id) {
                        LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp)) {
                            itemsIndexed(summary.buckets, key = { _, bucket -> bucket.start.toString() }) { index, bucket ->
                                val label = bucketLabel(bucket, index, summary.buckets.lastIndex, summary.granularity, locale)
                                val partial = monthly && bucket.partialMonth(request)
                                val displayedLabel = if (partial) stringResource(R.string.analytics_partial_label, label) else label
                                val dateRange = stringResource(R.string.transactions_period_range,
                                    bucket.start.formatDate(locale), bucket.end.formatDate(locale))
                                val description = stringResource(R.string.analytics_chart_bucket, dateRange,
                                    formatMoney(totals[index], currency, locale))
                                Column(Modifier.width(slot)
                                    .then(if (onOpenMonth != null) Modifier.clickable(role = Role.Button) {
                                        onOpenMonth(Period(bucket.start, bucket.end))
                                    } else Modifier)
                                    .semantics(mergeDescendants = true) { contentDescription = description },
                                    horizontalAlignment = Alignment.CenterHorizontally) {
                                    Canvas(Modifier.fillMaxWidth().height(ChartHeight)) {
                                        val width = minOf(if (monthly) 24.dp.toPx() else 11.dp.toPx(), size.width * 0.7f)
                                        val x = (size.width - width) / 2
                                        val topSegment = fractions[index].indexOfLast { it > 0f }
                                        var bottom = size.height
                                        fractions[index].forEachIndexed { part, fraction ->
                                            if (fraction <= 0f) return@forEachIndexed
                                            val top = bottom - size.height * fraction
                                            val corner = if (part == topSegment) CornerRadius(3.dp.toPx(), 3.dp.toPx()) else CornerRadius.Zero
                                            val path = Path().apply {
                                                addRoundRect(RoundRect(Rect(x, top, x + width, bottom),
                                                    topLeft = corner, topRight = corner,
                                                    bottomLeft = CornerRadius.Zero, bottomRight = CornerRadius.Zero))
                                            }
                                            drawPath(path, colors[part])
                                            if (colors[part] == c.cream) drawPath(path, c.line, style = Stroke(1.dp.toPx()))
                                            bottom = top
                                        }
                                    }
                                    Text(displayedLabel, modifier = Modifier.wrapContentWidth(unbounded = true).height(36.dp).padding(top = 6.dp),
                                        style = axisStyle, textAlign = TextAlign.Center, color = c.muted, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }
            if (!request.isIncome && category == null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEachIndexed { index, item ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).clip(FinTheme.shapes.round).background(colors[index])
                                .then(if (colors[index] == c.cream) Modifier.border(1.dp, c.line, FinTheme.shapes.round) else Modifier))
                            Text(item.localizedName(), style = FinTheme.typography.caption, color = c.navy)
                        }
                    }
                }
            }
            if (monthly && summary.buckets.any { it.partialMonth(request) }) {
                Text(stringResource(R.string.analytics_partial_month_hint), style = FinTheme.typography.caption, color = c.muted)
            }
            if (onOpenMonth != null) {
                Text(stringResource(R.string.analytics_open_month_hint), style = FinTheme.typography.caption, color = c.muted)
            }
        }
    }
}

private fun AnalyticsBucket.partialMonth(request: AnalyticsRequest): Boolean =
    start.dayOfMonth != 1 || end != Period.currentMonth(start).end || end >= request.today

@Composable
private fun bucketLabel(bucket: AnalyticsBucket, index: Int, last: Int, granularity: AnalyticsGranularity, locale: Locale): String {
    val month = DateFormatSymbols(locale).shortMonths[bucket.start.monthNumber - 1]
    return when {
        granularity == AnalyticsGranularity.MONTH -> stringResource(R.string.analytics_month_axis, month, bucket.start.year)
        granularity == AnalyticsGranularity.WEEK ->
            stringResource(R.string.analytics_day_axis, bucket.start.dayOfMonth, month)
        index % 5 == 0 || index == last -> bucket.start.dayOfMonth.toString()
        else -> ""
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AnalyticsChartPreview() {
    val state = analyticsPreviewState(true)
    FinDetectTheme { AnalyticsChart(requireNotNull(state.summary), state.request, "RUB", onOpenMonth = {}) }
}
