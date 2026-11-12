package com.ghostteam.findetect.feature.analytics

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.*
import com.ghostteam.findetect.core.util.formatAmount
import com.ghostteam.findetect.core.util.formatMoney
import com.ghostteam.findetect.feature.prototype.PeriodSheet
import com.ghostteam.findetect.feature.prototype.UiIconTile
import java.math.BigDecimal
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    initialIncome: Boolean,
    onBack: () -> Unit,
    onOpenCategory: (isIncome: Boolean, categoryIndex: Int, period: Int) -> Unit,
) {
    var isIncome by rememberSaveable { mutableStateOf(initialIncome) }
    var period by rememberSaveable { mutableIntStateOf(0) }
    var showPeriod by rememberSaveable { mutableStateOf(false) }
    val stats = buildStats(isIncome, period, stringArrayResource(R.array.ui_analytics_months).toList())
    val colors = categoryColors()
    val c = FinTheme.colors

    FinSubScreen(title = stringResource(R.string.ui_analytics), onBack = onBack) {
        FinSegmentedSwitch(
            options = listOf(false to stringResource(R.string.ui_expenses), true to stringResource(R.string.ui_income)),
            selected = isIncome,
            onSelect = { isIncome = it },
        )
        PeriodSummary(stats.total, period, onChipClick = { showPeriod = true })
        StackedBarChart(
            title = stringResource(chartTitleRes(isIncome, stats.byMonths)),
            bars = stats.bars,
            colors = colors,
            xLabels = stats.xLabels,
            legend = stats.categories.mapIndexed { i, cat -> stringResource(cat.name) to colors[i] },
        )
        Text(stringResource(R.string.ui_categories), style = FinTheme.typography.sectionTitle, color = c.navy)
        stats.categories.forEachIndexed { index, cat ->
            FinRow(
                title = stringResource(cat.name),
                subtitle = stringResource(R.string.ui_percentage, stats.share(cat.amount)),
                leading = { UiIconTile(cat.icon) },
                trailing = {
                    Text(money(cat.amount), style = FinTheme.typography.label, color = if (isIncome) c.green else c.navy)
                },
                onClick = { onOpenCategory(isIncome, index, period) },
            )
        }
    }
    if (showPeriod) {
        PeriodSheet(period, onSelect = { period = it; showPeriod = false }, onDismiss = { showPeriod = false })
    }
}

/** Чип периода + сумма. Общий блок для «Аналитики» и «Категория подробно». */
@Composable
internal fun PeriodSummary(total: Int, period: Int, onChipClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FinPeriodChip(stringResource(PeriodLabels[period]), onClick = onChipClick)
        Text(money(total), style = FinTheme.typography.balance, color = FinTheme.colors.navy)
    }
}

internal val PeriodLabels = listOf(R.string.ui_october, R.string.ui_september, R.string.ui_all_time)

internal class CategoryStat(@StringRes val name: Int, @DrawableRes val icon: Int, val amount: Int)

internal class PeriodStats(
    val categories: List<CategoryStat>,
    /** [корзина (день или месяц)][категория] */
    val bars: List<List<Int>>,
    /** Подпись под каждым столбцом, пустая строка = без подписи */
    val xLabels: List<String>,
    val byMonths: Boolean,
) {
    val total: Int get() = categories.sumOf { it.amount }
    fun share(amount: Int): Int = if (total == 0) 0 else (amount * 100f / total).roundToInt()
}

private class CategoryDef(@StringRes val name: Int, @DrawableRes val icon: Int)

private val expenseDefs = listOf(
    CategoryDef(R.string.ui_groceries, R.drawable.ic_basket),
    CategoryDef(R.string.ui_cafe, R.drawable.ic_coffee),
    CategoryDef(R.string.ui_transport, R.drawable.ic_bus),
    CategoryDef(R.string.ui_other, R.drawable.ic_wallet),
)
private val incomeDefs = listOf(
    CategoryDef(R.string.ui_salary, R.drawable.ic_cash),
    CategoryDef(R.string.ui_transfers, R.drawable.ic_accounts),
    CategoryDef(R.string.ui_interest, R.drawable.ic_spark),
    CategoryDef(R.string.ui_other, R.drawable.ic_wallet),
)

// строки: октябрь, сентябрь, за всё время; столбцы: категории
private val expenseTotals = listOf(
    listOf(12_425, 4_970, 3_728, 3_727), // = 24 850 ₽, как на экране «Расходы»
    listOf(11_200, 5_100, 3_400, 2_600),
    listOf(64_000, 26_500, 19_200, 16_300),
)
private val incomeTotals = listOf(
    listOf(50_000, 7_500, 1_500, 1_000), // = 60 000 ₽, как на экране «Доходы»
    listOf(50_000, 4_000, 1_200, 800),
    listOf(300_000, 40_000, 9_000, 6_000),
)

private val bucketCounts = listOf(19, 30, 6)

private fun weight(isIncome: Boolean, byMonths: Boolean, category: Int, day: Int): Int = when {
    byMonths -> 1 + (day + category) % 3
    isIncome && category == 0 -> if (day == 5) 1 else 0 // зарплата раз в месяц
    else -> (day * (2 * category + 3) + category) % 5
}

private fun spread(total: Int, weights: List<Int>): List<Int> {
    val sum = weights.sum()
    val last = weights.indexOfLast { it > 0 }.let { if (it >= 0) it else weights.lastIndex }
    if (sum == 0) return List(weights.size) { if (it == last) total else 0 }
    var left = total
    return weights.mapIndexed { i, w ->
        if (i == last) left else (total.toLong() * w / sum).toInt().also { left -= it }
    }
}

internal fun buildStats(isIncome: Boolean, period: Int, monthLabels: List<String>): PeriodStats {
    val defs = if (isIncome) incomeDefs else expenseDefs
    val totals = (if (isIncome) incomeTotals else expenseTotals)[period]
    val n = bucketCounts[period]
    val byMonths = period == 2
    val perCategory = defs.indices.map { k ->
        spread(totals[k], List(n) { i -> weight(isIncome, byMonths, k, i + 1) })
    }
    val labels = List(n) { i ->
        val day = i + 1
        when {
            byMonths -> monthLabels.getOrElse(i) { "" }
            day == 1 || day % 5 == 0 || day == n -> day.toString()
            else -> ""
        }
    }
    return PeriodStats(
        categories = defs.mapIndexed { k, d -> CategoryStat(d.name, d.icon, totals[k]) },
        bars = List(n) { i -> perCategory.map { it[i] } },
        xLabels = labels,
        byMonths = byMonths,
    )
}

internal fun money(amount: Int): String = formatMoney(BigDecimal.valueOf(amount.toLong()), "RUB")

@Composable
internal fun categoryColors(): List<Color> {
    val c = FinTheme.colors
    return listOf(c.yellow, c.navy, c.slate, c.cream)
}

@StringRes
internal fun chartTitleRes(isIncome: Boolean, byMonths: Boolean): Int = when {
    isIncome && byMonths -> R.string.ui_chart_income_months
    isIncome -> R.string.ui_chart_income_days
    byMonths -> R.string.ui_chart_expense_months
    else -> R.string.ui_chart_expense_days
}


private val ChartHeight = 120.dp

private fun niceStep(max: Int): Int {
    if (max <= 0) return 500
    val raw = max / 3.0
    val magnitude = 10.0.pow(floor(log10(raw)))
    val factor = listOf(1.0, 1.5, 2.0, 3.0, 5.0, 10.0).first { it * magnitude >= raw }
    return (factor * magnitude).roundToInt().coerceAtLeast(1)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StackedBarChart(
    title: String,
    bars: List<List<Int>>,
    colors: List<Color>,
    xLabels: List<String>,
    legend: List<Pair<String, Color>> = emptyList(),
) {
    val c = FinTheme.colors
    val step = niceStep(bars.maxOfOrNull { it.sum() } ?: 0)
    val axisMax = step * 3
    val description = stringResource(R.string.ui_analytics_chart_description)

    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = FinTheme.typography.label, color = c.navy)
            Row(Modifier.fillMaxWidth().padding(top = 7.dp).semantics { contentDescription = description }) {
                Box(Modifier.width(44.dp).height(ChartHeight)) {
                    for (i in 0..3) {
                        Text(
                            BigDecimal.valueOf((step * i).toLong()).formatAmount(),
                            modifier = Modifier.align(Alignment.TopEnd)
                                .offset(y = ChartHeight * (3 - i) / 3 - 7.dp)
                                .padding(end = 8.dp),
                            style = FinTheme.typography.axis, color = c.muted, maxLines = 1, softWrap = false,
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Canvas(Modifier.fillMaxWidth().height(ChartHeight)) {
                        val h = size.height
                        for (i in 0..3) {
                            val y = h - h * i / 3f
                            drawLine(c.line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                        }
                        if (bars.isEmpty() || axisMax <= 0) return@Canvas
                        val slot = size.width / bars.size
                        val barWidth = minOf(11.dp.toPx(), slot * 0.7f)
                        val radius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        bars.forEachIndexed { i, parts ->
                            val x = i * slot + (slot - barWidth) / 2f
                            val topSegment = parts.indexOfLast { it > 0 }
                            var bottom = h
                            parts.forEachIndexed { k, value ->
                                if (value <= 0) return@forEachIndexed
                                val top = bottom - h * value / axisMax
                                val corner = if (k == topSegment) radius else CornerRadius.Zero
                                val path = Path().apply {
                                    addRoundRect(
                                        RoundRect(
                                            Rect(x, top, x + barWidth, bottom),
                                            topLeft = corner, topRight = corner,
                                            bottomRight = CornerRadius.Zero, bottomLeft = CornerRadius.Zero,
                                        ),
                                    )
                                }
                                drawPath(path, colors[k])
                                // «Другое» (cream) без обводки сливается с белым
                                if (colors[k] == c.cream) drawPath(path, c.line, style = Stroke(1.dp.toPx()))
                                bottom = top
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        xLabels.forEach { label ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (label.isNotEmpty()) {
                                    Text(label, Modifier.wrapContentWidth(unbounded = true),
                                        style = FinTheme.typography.axis, color = c.muted, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
            if (legend.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    legend.forEach { (name, color) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                Modifier.size(8.dp).clip(FinTheme.shapes.round).background(color)
                                    .border(FinDimens.borderWidth, if (color == c.cream) c.line else Color.Transparent, FinTheme.shapes.round),
                            )
                            Text(name, style = FinTheme.typography.caption, color = c.navy)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AnalyticsPreview() {
    FinDetectTheme { AnalyticsScreen(initialIncome = false, onBack = {}, onOpenCategory = { _, _, _ -> }) }
}