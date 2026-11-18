package com.ghostteam.findetect.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.ghostteam.findetect.domain.model.DayActivity
import com.ghostteam.findetect.feature.activity.DayActivityUiState
import com.ghostteam.findetect.feature.activity.NoSpendingSection
import com.ghostteam.findetect.feature.achievements.AchievementsPreviewCard
import com.ghostteam.findetect.feature.achievements.AchievementsUiState
import com.ghostteam.findetect.feature.achievements.achievementsPreviewProgress
import com.ghostteam.findetect.feature.gamification.XpProgressContent
import com.ghostteam.findetect.feature.gamification.XpProgressUiState
import com.ghostteam.findetect.domain.usecase.CalculateXpProgressUseCase
import com.ghostteam.findetect.feature.streak.StreakCard
import com.ghostteam.findetect.feature.streak.streakPreviewProgress
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
    userName: String,
    onAddTransaction: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenStreak: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onRetry: () -> Unit = {},
    onConfirmNoSpending: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
) {
    val locale = LocalConfiguration.current.locales[0]
    val finance = state.finance

    PrototypeScreen(
        R.string.ui_home,
        floatingAction = true,
        showLogo = true,
        onSettings = onOpenSettings,
        onAnalytics = onOpenAnalytics,
        action = {
            FinButton(
                stringResource(R.string.ui_add_transaction),
                onAddTransaction,
            )
        },
    ) {
        item(key = "profile") {
            ProfileCard(
                userName = userName,
                onClick = onOpenProfile,
                gameElementsEnabled = state.gameElementsEnabled,
                xp = state.xp,
                onRetry = onRetry,
            )
        }

        if (state.gameElementsEnabled) {
            item(key = "streak") {
                StreakCard(
                    state.streak,
                    state.streakLoading,
                    state.streakError,
                    onRetry,
                    onClick = onOpenStreak,
                )
            }
        }

        item(key = "today") {
            TodayCard(state)
        }

        item(key = "no_spending") {
            NoSpendingSection(
                state.dayStatus,
                onConfirmNoSpending,
                onRetry,
            )
        }

        if (state.error) {
            item(key = "finance_error") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.home_finance_read_error),
                        style = FinTheme.typography.caption,
                        color = FinTheme.colors.red,
                    )
                    FinButton(
                        stringResource(R.string.finance_retry),
                        onRetry,
                        style = FinButtonStyle.Secondary,
                    )
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

        if (state.gameElementsEnabled) {
            item(key = "achievements") {
                AchievementsPreviewCard(
                    state.achievements,
                    onOpenAchievements,
                    onRetry,
                )
            }
        }

        item(key = "recent_title") {
            Text(
                stringResource(R.string.ui_recent),
                style = FinTheme.typography.sectionTitle,
                color = FinTheme.colors.navy,
            )
        }

        if (finance != null && finance.recentTransactions.isEmpty()) {
            item(key = "recent_empty") {
                FinNotice(
                    stringResource(R.string.home_no_transactions),
                    text = stringResource(R.string.home_no_transactions_hint),
                )
            }
        }

        items(
            finance?.recentTransactions.orEmpty(),
            key = { "recent-${it.transaction.id}" },
        ) { details ->
            val date = details.transaction.transactionDate
                .toLocalDateTime(state.day.zone)
                .date
                .formatDate(locale)

            val comment = details.transaction.comment?.takeUnless {
                it.isBlank()
            }

            val subtitle = if (comment == null) {
                date
            } else {
                stringResource(
                    R.string.home_transaction_subtitle,
                    comment,
                    date,
                )
            }

            TransactionRow(
                details,
                compact = true,
                subtitle = subtitle,
            )
        }
    }
}

@Composable
private fun ProfileCard(
    userName: String,
    onClick: () -> Unit,
    gameElementsEnabled: Boolean,
    xp: XpProgressUiState,
    onRetry: () -> Unit,
) {
    FinCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UiIconTile(R.drawable.ic_user)

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                val level = xp.progress?.level
                val title = if (gameElementsEnabled && level != null) {
                    stringResource(
                        R.string.ui_profile,
                        userName,
                        level,
                    )
                } else {
                    userName
                }

                Text(
                    title,
                    style = FinTheme.typography.label,
                    color = FinTheme.colors.navy,
                )

                if (gameElementsEnabled) {
                    XpProgressContent(xp, onRetry)
                }
            }

            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = FinTheme.colors.muted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TodayCard(state: HomeUiState) {
    val finance = state.finance
    val currency = state.currency
    val locale = LocalConfiguration.current.locales[0]

    FinCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UiIconTile(R.drawable.ic_wallet)

            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.ui_spent_today),
                    style = FinTheme.typography.caption,
                    color = FinTheme.colors.muted,
                )

                if (finance != null && currency != null) {
                    Text(
                        formatMoney(
                            finance.todaySpent,
                            currency.name,
                            locale,
                        ),
                        style = FinTheme.typography.title,
                        color = FinTheme.colors.navy,
                    )

                    val difference = finance.spendingDifference
                    val comparison = when {
                        finance.todaySpent.signum() == 0 &&
                                finance.yesterdaySpent.signum() == 0 -> {
                            stringResource(R.string.home_no_spending_two_days)
                        }

                        difference.signum() > 0 -> {
                            stringResource(
                                R.string.home_spending_more,
                                formatMoney(
                                    difference,
                                    currency.name,
                                    locale,
                                ),
                            )
                        }

                        difference.signum() < 0 -> {
                            stringResource(
                                R.string.home_spending_less,
                                formatMoney(
                                    difference.abs(),
                                    currency.name,
                                    locale,
                                ),
                            )
                        }

                        else -> {
                            stringResource(R.string.home_spending_same)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (difference.signum() != 0) {
                            Icon(
                                painterResource(R.drawable.ic_back),
                                contentDescription = null,
                                tint = if (difference.signum() > 0) {
                                    FinTheme.colors.yellow
                                } else {
                                    FinTheme.colors.green
                                },
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(
                                        if (difference.signum() > 0) {
                                            90f
                                        } else {
                                            270f
                                        },
                                    ),
                            )
                        }

                        Text(
                            comparison,
                            style = FinTheme.typography.caption,
                            color = FinTheme.colors.muted,
                        )
                    }
                } else {
                    Text(
                        stringResource(
                            if (state.loading) {
                                R.string.finance_loading
                            } else {
                                R.string.home_finance_unavailable
                            },
                        ),
                        style = FinTheme.typography.label,
                        color = FinTheme.colors.muted,
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    widthDp = 412,
    heightDp = 892,
)
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 740,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomePreview() {
    val progress = streakPreviewProgress()
    val transactions = transactionsPreviewSummary().days.first().transactions +
            transactionsPreviewSummary(isIncome = true)
                .days.first().transactions.map { details ->
                    details.copy(
                        transaction = details.transaction.copy(
                            id = "preview-income",
                        ),
                    )
                }

    MainScreenPreview(Destination.Home.route) {
        HomeScreen(
            state = HomeUiState(
                day = HomeFinanceDay(progress.today, TimeZone.UTC),
                currency = AppCurrency.RUB,
                finance = HomeFinanceSummary(
                    BigDecimal("125.00"),
                    BigDecimal("100.00"),
                    transactions,
                ),
                loading = false,
                gameElementsEnabled = true,
                dayStatus = DayActivityUiState(
                    HomeFinanceDay(progress.today, TimeZone.UTC),
                    DayActivity(),
                    loading = false,
                ),
                streak = progress,
                streakLoading = false,
                xp = XpProgressUiState(
                    CalculateXpProgressUseCase()(700L),
                    loading = false,
                ),
                achievements = AchievementsUiState(
                    achievementsPreviewProgress(),
                    loading = false,
                ),
            ),
            userName = "Анна",
        )
    }
}

@Preview(showBackground = true, widthDp = 364)
@Preview(
    showBackground = true,
    widthDp = 364,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun TodayCardPreview() {
    val day = HomeFinanceDay(
        LocalDate(2026, 11, 14),
        TimeZone.UTC,
    )

    FinDetectTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                "0.00" to "0.00",
                "125.00" to "125.00",
                "0.00" to "125.00",
            ).forEach { (today, yesterday) ->
                TodayCard(
                    HomeUiState(
                        day,
                        AppCurrency.RUB,
                        HomeFinanceSummary(
                            BigDecimal(today),
                            BigDecimal(yesterday),
                            emptyList(),
                        ),
                        loading = false,
                    ),
                )
            }
        }
    }
}