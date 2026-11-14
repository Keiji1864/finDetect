package com.ghostteam.findetect.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.*
import com.ghostteam.findetect.core.navigation.Destination
import com.ghostteam.findetect.core.util.formatDate
import com.ghostteam.findetect.core.util.formatMoney
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.HomeFinanceSummary
import com.ghostteam.findetect.feature.prototype.*
import com.ghostteam.findetect.feature.transaction.TransactionRow
import com.ghostteam.findetect.feature.transaction.transactionsPreviewSummary
import java.math.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun HomeScreen(
    state: HomeUiState,
    onAddTransaction: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val locale = LocalConfiguration.current.locales[0]
    val finance = state.finance
    PrototypeScreen(R.string.ui_home, floatingAction = true, showLogo = true, onSettings = onOpenSettings, onAnalytics = onOpenAnalytics, action = {
        FinButton(stringResource(R.string.ui_add_transaction), onAddTransaction)
    }) {
        item(key = "profile") { ProfileCard(onOpenProfile) }
        item(key = "streak") { StreakCard() }
        item(key = "today") { TodayCard(state) }
        if (state.error) {
            item(key = "finance_error") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_finance_read_error),
                        style = FinTheme.typography.caption, color = FinTheme.colors.red)
                    FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary)
                }
            }
        }
        item(key = "advice") {
            FinAdviceCard(
                title = stringResource(R.string.ui_advice_title),
                text = stringResource(R.string.ui_advice_hint),
                caption = stringResource(R.string.ui_advice),
                pager = stringResource(R.string.ui_advice_page),
            )
        }
        item(key = "achievements") { AchievementsCard() }
        item(key = "recent_title") {
            Text(stringResource(R.string.ui_recent), style = FinTheme.typography.sectionTitle, color = FinTheme.colors.navy)
        }
        if (finance != null && finance.recentTransactions.isEmpty()) {
            item(key = "recent_empty") {
                FinNotice(stringResource(R.string.home_no_transactions), text = stringResource(R.string.home_no_transactions_hint))
            }
        }
        items(finance?.recentTransactions.orEmpty(), key = { "recent-${it.transaction.id}" }) { details ->
            val date = details.transaction.transactionDate.toLocalDateTime(state.day.zone).date.formatDate(locale)
            val comment = details.transaction.comment?.takeUnless { it.isBlank() }
            val subtitle = if (comment == null) date else stringResource(R.string.home_transaction_subtitle, comment, date)
            TransactionRow(details, compact = true, subtitle = subtitle)
        }
    }
}

@Composable
private fun ProfileCard(onClick: () -> Unit) {
    FinCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UiIconTile(R.drawable.ic_user)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.ui_profile), style = FinTheme.typography.label, color = FinTheme.colors.navy)
                Text(stringResource(R.string.ui_profile_hint), style = FinTheme.typography.caption, color = FinTheme.colors.muted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.fillMaxWidth().height(6.dp).clip(FinTheme.shapes.tiny).background(FinTheme.colors.line)) {
                    Box(Modifier.fillMaxWidth(0.56f).fillMaxHeight().background(FinTheme.colors.yellow))
                }
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = FinTheme.colors.muted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun StreakCard() {
    val c = FinTheme.colors
    FinCard(background = c.cream) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(painterResource(R.drawable.ic_flame), null, tint = c.yellow, modifier = Modifier.size(24.dp))
                Text(stringResource(R.string.ui_streak), Modifier.weight(1f), style = FinTheme.typography.subheading,
                    color = c.navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.ui_record), style = FinTheme.typography.caption, color = c.muted)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(R.string.ui_mon, R.string.ui_tue, R.string.ui_wed, R.string.ui_thu,
                    R.string.ui_fri, R.string.ui_sat, R.string.ui_sun).forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(32.dp).background(c.yellow, FinTheme.shapes.chip), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_check), null, tint = c.navy, modifier = Modifier.size(20.dp))
                        }
                        Text(stringResource(day), style = FinTheme.typography.caption, color = c.muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayCard(state: HomeUiState) {
    val finance = state.finance
    val currency = state.currency
    val locale = LocalConfiguration.current.locales[0]
    FinCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UiIconTile(R.drawable.ic_wallet)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ui_spent_today), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                if (finance != null && currency != null) {
                    Text(formatMoney(finance.todaySpent, currency.name, locale),
                        style = FinTheme.typography.title, color = FinTheme.colors.navy)
                    val difference = finance.spendingDifference
                    val comparison = when {
                        finance.todaySpent.signum() == 0 && finance.yesterdaySpent.signum() == 0 ->
                            stringResource(R.string.home_no_spending_two_days)
                        difference.signum() > 0 -> stringResource(R.string.home_spending_more, formatMoney(difference, currency.name, locale))
                        difference.signum() < 0 -> stringResource(R.string.home_spending_less, formatMoney(difference.abs(), currency.name, locale))
                        else -> stringResource(R.string.home_spending_same)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (difference.signum() != 0) {
                            Icon(painterResource(R.drawable.ic_back), null,
                                tint = if (difference.signum() > 0) FinTheme.colors.yellow else FinTheme.colors.green,
                                modifier = Modifier.size(16.dp).rotate(if (difference.signum() > 0) 90f else 270f))
                        }
                        Text(comparison, style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                    }
                } else {
                    Text(stringResource(if (state.loading) R.string.finance_loading else R.string.home_finance_unavailable),
                        style = FinTheme.typography.label, color = FinTheme.colors.muted)
                }
            }
        }
    }
}

@Composable
private fun AchievementsCard() {
    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_achievements), Modifier.weight(1f),
                    style = FinTheme.typography.label, color = FinTheme.colors.navy)
                Icon(Icons.Outlined.ChevronRight, null, tint = FinTheme.colors.muted, modifier = Modifier.size(20.dp))
            }
            FinRow(stringResource(R.string.ui_achievements_title), subtitle = stringResource(R.string.ui_achievements_hint),
                compact = true, inSheet = true, leading = { UiIconTile(R.drawable.ic_trophy) })
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomePreview() {
    val transactions = transactionsPreviewSummary().days.first().transactions +
        transactionsPreviewSummary(isIncome = true).days.first().transactions.map { details ->
            details.copy(transaction = details.transaction.copy(id = "preview-income"))
        }
    MainScreenPreview(Destination.Home.route) {
        HomeScreen(HomeUiState(
            day = HomeFinanceDay(LocalDate(2026, 11, 14), TimeZone.UTC),
            currency = AppCurrency.RUB,
            finance = HomeFinanceSummary(BigDecimal("125.00"), BigDecimal("100.00"), transactions),
            loading = false,
        ))
    }
}

@Preview(showBackground = true, widthDp = 364)
@Preview(showBackground = true, widthDp = 364, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TodayCardPreview() {
    val day = HomeFinanceDay(LocalDate(2026, 11, 14), TimeZone.UTC)
    FinDetectTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("0.00" to "0.00", "125.00" to "125.00", "0.00" to "125.00").forEach { (today, yesterday) ->
                TodayCard(HomeUiState(day, AppCurrency.RUB,
                    HomeFinanceSummary(BigDecimal(today), BigDecimal(yesterday), emptyList()), loading = false))
            }
        }
    }
}
