package com.ghostteam.findetect.feature.prototype

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.*
import com.ghostteam.findetect.core.navigation.BottomBar

@Composable
fun PrototypeScreen(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    floatingAction: Boolean = false,
    action: (@Composable () -> Unit)? = null,
    onSettings: () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().background(FinTheme.colors.paper).statusBarsPadding()) {
        MainHeader(title, onSettings)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = FinDimens.screenPadding,
                    end = FinDimens.screenPadding,
                    top = 8.dp,
                    bottom = if (floatingAction && action != null) FinDimens.floatingPanelContentInset else 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap),
                content = content,
            )
            if (floatingAction && action != null) {
                FinFloatingActionPanel(Modifier.align(Alignment.BottomCenter)) { action() }
            }
        }
        if (!floatingAction && action != null) {
            Box(Modifier.fillMaxWidth().padding(horizontal = FinDimens.screenPadding, vertical = 16.dp)) {
                action()
            }
        }
    }
}

@Composable
private fun MainHeader(@StringRes title: Int, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .padding(start = FinDimens.screenPadding, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(title), Modifier.weight(1f), style = FinTheme.typography.title,
            color = FinTheme.colors.navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
        IconButton(onClick = {}, enabled = false, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.BarChart, stringResource(R.string.ui_analytics), tint = FinTheme.colors.navy,
                modifier = Modifier.size(FinDimens.iconSize))
        }
        IconButton(onClick = onSettings, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.Settings, stringResource(R.string.ui_settings), tint = FinTheme.colors.navy,
                modifier = Modifier.size(FinDimens.iconSize))
        }
    }
}

/** Иконка в кремовой плитке из готовой дизайн-системы. */
@Composable
fun UiIconTile(icon: ImageVector, modifier: Modifier = Modifier) {
    FinIconTile(modifier = modifier) {
        Icon(icon, contentDescription = null, tint = FinTheme.colors.navy, modifier = Modifier.size(24.dp))
    }
}

/** Строка интерфейса: подписи и суммы передаются непосредственно из экрана. */
@Composable
fun DisplayRow(
    @StringRes title: Int,
    @StringRes subtitle: Int,
    @StringRes amount: Int,
    icon: ImageVector,
    isIncome: Boolean = false,
    compact: Boolean = false,
) {
    FinRow(
        title = stringResource(title),
        subtitle = stringResource(subtitle),
        compact = compact,
        leading = { UiIconTile(icon) },
        trailing = {
            Text(stringResource(amount), style = FinTheme.typography.label,
                color = if (isIncome) FinTheme.colors.green else FinTheme.colors.navy)
        },
    )
}

/** Статичная диаграмма из референса; значения заданы только для отрисовки UI. */
@Composable
fun CategoryChart(isIncome: Boolean) {
    val c = FinTheme.colors
    val categories = if (isIncome) listOf(R.string.ui_salary, R.string.ui_transfers, R.string.ui_interest, R.string.ui_other)
        else listOf(R.string.ui_groceries, R.string.ui_cafe, R.string.ui_transport, R.string.ui_other)
    val colors = listOf(c.yellow, c.navy, c.slate, c.cream)
    val description = stringResource(R.string.ui_chart_description)
    val percentages = listOf(50, 20, 15, 15)
    FinCard {
        BoxWithConstraints {
            val ringSize = if (maxWidth < 300.dp) 112.dp else 132.dp
            Row(
                Modifier.fillMaxWidth().heightIn(min = 132.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(Modifier.size(ringSize).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize().padding(11.dp)) {
                        var start = -90f
                        percentages.forEachIndexed { index, percent ->
                            val sweep = percent * 3.6f
                            drawArc(color = colors[index], startAngle = start, sweepAngle = sweep,
                                useCenter = false, style = Stroke(width = 21.dp.toPx()))
                            start += sweep
                        }
                    }
                    Text(stringResource(R.string.ui_currency), style = FinTheme.typography.heading, color = c.muted)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    categories.forEachIndexed { index, category ->
                        LegendRow(stringResource(category), colors[index], percentages[index])
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, color: Color, percent: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(8.dp).clip(FinTheme.shapes.round).background(color))
        Text(label, Modifier.weight(1f), style = FinTheme.typography.caption, color = FinTheme.colors.navy,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(stringResource(R.string.ui_percentage, percent), style = FinTheme.typography.caption, color = FinTheme.colors.navy)
    }
}

@Composable
fun TransactionsContent(isIncome: Boolean, onAdd: () -> Unit, onSettings: () -> Unit = {}) {
    var period by rememberSaveable { mutableIntStateOf(0) }
    var showPeriod by rememberSaveable { mutableStateOf(false) }
    val periodLabels = listOf(R.string.ui_october, R.string.ui_september, R.string.ui_all_time)
    PrototypeScreen(if (isIncome) R.string.ui_income else R.string.ui_expenses, onSettings = onSettings, action = {
        FinButton(stringResource(if (isIncome) R.string.ui_add_income else R.string.ui_add_expense), onAdd)
    }) {
        item(key = "summary") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FinPeriodChip(stringResource(periodLabels[period]), onClick = { showPeriod = true })
                Text(stringResource(if (isIncome) R.string.ui_income_total else R.string.ui_expense_total),
                    style = FinTheme.typography.balance, color = FinTheme.colors.navy)
            }
        }
        item(key = "chart") { CategoryChart(isIncome) }
        item(key = "insight") {
            FinAdviceCard(
                title = stringResource(if (isIncome) R.string.ui_income_insight else R.string.ui_expense_insight),
                text = stringResource(if (isIncome) R.string.ui_income_insight_hint else R.string.ui_expense_insight_hint),
            )
        }
        item(key = "list_label") {
            Text(stringResource(R.string.ui_today_date), style = FinTheme.typography.caption,
                color = FinTheme.colors.muted, modifier = Modifier.padding(top = 2.dp))
        }
        if (isIncome) {
            item(key = "salary") {
                DisplayRow(R.string.ui_salary, R.string.ui_employer, R.string.ui_salary_amount,
                    Icons.Outlined.Payments, isIncome = true)
            }
            item(key = "freelance") {
                DisplayRow(R.string.ui_side_job, R.string.ui_freelance, R.string.ui_freelance_amount,
                    Icons.Outlined.AccountBalanceWallet, isIncome = true)
            }
        } else {
            item(key = "groceries") {
                DisplayRow(R.string.ui_groceries, R.string.ui_store, R.string.ui_groceries_amount,
                    Icons.Outlined.ShoppingBasket)
            }
            item(key = "coffee") {
                DisplayRow(R.string.ui_coffee, R.string.ui_coffee_shop, R.string.ui_coffee_amount,
                    Icons.Outlined.LocalCafe)
            }
        }
    }
    if (showPeriod) {
        PeriodSheet(period, onSelect = { period = it; showPeriod = false }, onDismiss = { showPeriod = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSheet(selected: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    FinBottomSheet(onDismiss = onDismiss) {
        Text(stringResource(R.string.ui_choose_period), style = FinTheme.typography.sheetTitle, color = FinTheme.colors.navy)
        listOf(R.string.ui_october, R.string.ui_september, R.string.ui_all_time).forEachIndexed { index, title ->
            FinRow(stringResource(title), onClick = { onSelect(index) }, inSheet = true,
                trailing = { FinRadio(selected = selected == index, onClick = null) })
        }
    }
}

/** Полный фрейм для Android Studio Preview, включая нижние вкладки. */
@Composable
fun MainScreenPreview(route: String, content: @Composable () -> Unit) {
    FinDetectTheme {
        Surface(color = FinTheme.colors.paper) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { content() }
                BottomBar(route, onSelect = {})
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 364)
@Preview(showBackground = true, widthDp = 364, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ComponentsPreview() {
    FinDetectTheme {
        Column(Modifier.background(FinTheme.colors.paper).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryChart(isIncome = false)
            DisplayRow(R.string.ui_main_account, R.string.ui_ruble_account, R.string.ui_main_balance, Icons.Outlined.AccountBalanceWallet)
        }
    }
}
