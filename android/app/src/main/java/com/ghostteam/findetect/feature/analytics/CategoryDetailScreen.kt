package com.ghostteam.findetect.feature.analytics

import android.content.res.Configuration
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.feature.prototype.PeriodSheet

@Composable
fun CategoryDetailScreen(
    isIncome: Boolean,
    categoryIndex: Int,
    initialPeriod: Int,
    onBack: () -> Unit,
) {
    var period by rememberSaveable { mutableIntStateOf(initialPeriod.coerceIn(0, PeriodLabels.lastIndex)) }
    var showPeriod by rememberSaveable { mutableStateOf(false) }
    val stats = buildStats(isIncome, period, stringArrayResource(R.array.ui_analytics_months).toList())
    val index = categoryIndex.coerceIn(0, stats.categories.lastIndex)
    val category = stats.categories[index]
    val color = categoryColors()[index]

    FinSubScreen(title = stringResource(category.name), onBack = onBack) {
        PeriodSummary(category.amount, period, onChipClick = { showPeriod = true })
        Text(
            stringResource(if (isIncome) R.string.ui_share_of_income else R.string.ui_share_of_expense, stats.share(category.amount)),
            style = FinTheme.typography.caption, color = FinTheme.colors.muted,
        )
        StackedBarChart(
            title = stringResource(chartTitleRes(isIncome, stats.byMonths)),
            bars = stats.bars.map { listOf(it[index]) },
            colors = listOf(color),
            xLabels = stats.xLabels,
        )
    }
    if (showPeriod) {
        PeriodSheet(period, onSelect = { period = it; showPeriod = false }, onDismiss = { showPeriod = false })
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryDetailPreview() {
    FinDetectTheme { CategoryDetailScreen(isIncome = false, categoryIndex = 1, initialPeriod = 0, onBack = {}) }
}