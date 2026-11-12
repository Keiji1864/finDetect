package com.ghostteam.findetect.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.*
import com.ghostteam.findetect.core.navigation.Destination
import com.ghostteam.findetect.feature.prototype.*

@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
) {
    PrototypeScreen(R.string.ui_home, floatingAction = true, showLogo = true, onSettings = onOpenSettings, onAnalytics = onOpenAnalytics, action = {
        FinButton(stringResource(R.string.ui_add_transaction), onAddTransaction)
    }) {
        item(key = "profile") { ProfileCard(onOpenProfile) }
        item(key = "streak") { StreakCard() }
        item(key = "today") { TodayCard() }
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
        item(key = "groceries") {
            DisplayRow(R.string.ui_groceries, R.string.ui_store_today, R.string.ui_groceries_amount,
                R.drawable.ic_basket, compact = true)
        }
        item(key = "coffee") {
            DisplayRow(R.string.ui_coffee, R.string.ui_coffee_today, R.string.ui_coffee_amount,
                R.drawable.ic_coffee, compact = true)
        }
        item(key = "salary") {
            DisplayRow(R.string.ui_salary, R.string.ui_employer_today, R.string.ui_salary_amount,
                R.drawable.ic_cash, isIncome = true, compact = true)
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
private fun TodayCard() {
    FinCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UiIconTile(R.drawable.ic_wallet)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ui_spent_today), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                Text(stringResource(R.string.ui_spent_amount), style = FinTheme.typography.title, color = FinTheme.colors.navy)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ArrowDropUp, null, tint = FinTheme.colors.yellow, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.ui_today_hint), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
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
private fun HomePreview() { MainScreenPreview(Destination.Home.route) { HomeScreen() } }