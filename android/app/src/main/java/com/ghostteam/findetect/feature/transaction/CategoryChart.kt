package com.ghostteam.findetect.feature.transaction

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinCard
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.util.currencySymbol
import com.ghostteam.findetect.domain.model.CategoryTotal
import java.math.BigDecimal
import java.math.RoundingMode

/** Ring and legend use the same category totals; Float is confined to drawing angles. */
@Composable
fun CategoryChart(categories: List<CategoryTotal>, total: BigDecimal, currency: String) {
    if (categories.isEmpty() || total.signum() <= 0) return
    val colors = categories.map { categoryColor(it.category.key) }
    val sweeps = remember(categories, total) {
        categories.map { it.amount.divide(total, 12, RoundingMode.HALF_UP).multiply(BigDecimal(360)).toFloat() }
    }
    val cream = FinTheme.colors.cream
    val line = FinTheme.colors.line
    val description = stringResource(R.string.ui_chart_description)
    FinCard {
        BoxWithConstraints {
            val ringSize = if (maxWidth < 300.dp) 112.dp else 132.dp
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.size(ringSize).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize().padding(12.dp)) {
                        var start = -90f
                        sweeps.forEachIndexed { index, sweep ->
                            if (colors[index] == cream) {
                                drawArc(line, start, sweep, false, style = Stroke(width = 23.dp.toPx()))
                            }
                            drawArc(colors[index], start, sweep, false, style = Stroke(width = 21.dp.toPx()))
                            start += sweep
                        }
                    }
                    Text(currencySymbol(currency), style = FinTheme.typography.heading, color = FinTheme.colors.muted)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    categories.forEachIndexed { index, category ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(colors[index])
                                .then(if (colors[index] == cream) Modifier.border(1.dp, line, CircleShape) else Modifier))
                            Text(category.category.localizedName(), Modifier.weight(1f), style = FinTheme.typography.caption,
                                color = FinTheme.colors.navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(R.string.ui_percentage, category.percentage),
                                style = FinTheme.typography.caption, color = FinTheme.colors.navy)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun categoryColor(key: String): Color {
    val palette = listOf(FinTheme.colors.yellow, FinTheme.colors.navy, FinTheme.colors.slate, FinTheme.colors.cream)
    return when (key) {
        "groceries", "products" -> palette[0]
        "coffee", "cafe" -> palette[1]
        "transport" -> palette[2]
        "other" -> palette[3]
        else -> palette[(key.hashCode() and Int.MAX_VALUE) % palette.size]
    }
}

@Preview(showBackground = true, widthDp = 364)
@Preview(showBackground = true, widthDp = 364, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryChartPreview() {
    val summary = transactionsPreviewSummary()
    FinDetectTheme { CategoryChart(summary.categories, summary.total, "RUB") }
}
