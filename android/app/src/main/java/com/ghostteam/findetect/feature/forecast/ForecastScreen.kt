package com.ghostteam.findetect.feature.forecast

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.util.formatDate
import com.ghostteam.findetect.core.util.formatMoney
import com.ghostteam.findetect.domain.model.*
import java.math.BigDecimal
import kotlinx.datetime.LocalDate

@Composable
internal fun SpendingForecast.money(value: BigDecimal) =
    formatMoney(value, currency.name, LocalConfiguration.current.locales[0])

@Composable
private fun ForecastReadStatus(state: ForecastState, retry: () -> Unit) {
    if (state.failed) {
        FinNotice(stringResource(R.string.forecast_error))
        FinButton(stringResource(R.string.finance_retry), retry)
    } else Text(stringResource(R.string.finance_loading), style = FinTheme.typography.caption)
}

@Composable
private fun SpendingForecast.unavailableText() = stringResource(when (status) {
    ForecastStatus.NO_DATE -> R.string.forecast_no_date
    ForecastStatus.DATE_PASSED -> R.string.forecast_date_passed
    ForecastStatus.NO_ACCOUNTS -> R.string.forecast_no_accounts
    ForecastStatus.READY -> R.string.forecast_manual_hint
})

@Composable
private fun Recommendation(f: SpendingForecast, open: (() -> Unit)? = null) {
    FinCard(background = FinTheme.colors.cream, onClick = open) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (f.recommended == null) {
                Text(f.unavailableText(), style = FinTheme.typography.label)
            } else {
                val spent = f.spentToday.signum() > 0
                Text(stringResource(if (spent) R.string.forecast_left_today else R.string.forecast_recommended),
                    style = FinTheme.typography.label)
                Text(f.money(requireNotNull(f.leftToday)), style = FinTheme.typography.balance, color = FinTheme.colors.navy)
                if (spent) Text(stringResource(R.string.forecast_spent, f.money(f.spentToday), f.money(f.recommended)),
                    style = FinTheme.typography.caption)
                Text(if (f.usualDaily != null) stringResource(R.string.forecast_known_days, f.knownDays)
                    else stringResource(R.string.forecast_manual_source), style = FinTheme.typography.caption)
            }
        }
    }
}

@Composable
internal fun HomeForecast(state: ForecastState, retry: () -> Unit, open: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.result?.let { Recommendation(it, open) } ?: ForecastReadStatus(state, retry)
        FinButton(stringResource(R.string.forecast_open), open, style = FinButtonStyle.Secondary)
    }
}

@Composable
internal fun ForecastScreen(
    state: ForecastState, back: () -> Unit, retry: () -> Unit, editIncome: () -> Unit,
    accounts: () -> Unit, save: suspend (SpendingForecast, ForecastPlan) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val focus = LocalFocusManager.current
    LaunchedEffect(expanded) { if (expanded) scroll.scrollTo(0) }
    val goBack = { if (expanded) expanded = false else back() }
    BackHandler(enabled = expanded) { expanded = false }
    FinSubScreenFrame(stringResource(R.string.forecast_title), goBack, Modifier, 2) {
        val f = state.result
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val panelHeight = maxHeight * 0.42f
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).verticalScroll(scroll)
                    .padding(horizontal = FinDimens.screenPadding, vertical = FinDimens.blockGap),
                    verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap)) {
                    when {
                        f == null -> ForecastReadStatus(state, retry)
                        f.status != ForecastStatus.READY -> {
                            FinNotice(f.unavailableText())
                            if (f.status == ForecastStatus.NO_ACCOUNTS)
                                FinButton(stringResource(R.string.forecast_accounts_action), accounts)
                            else FinButton(stringResource(R.string.income_schedule_title), editIncome)
                        }
                        else -> {
                            Text(stringResource(R.string.forecast_until, requireNotNull(f.daysUntilIncome),
                                requireNotNull(f.income).formatDate(LocalConfiguration.current.locales[0])),
                                style = FinTheme.typography.caption)
                            Recommendation(f)
                            ForecastTotals(f)
                            key(f.currency, f.income) { ForecastPlanFields(f, save) }
                        }
                    }
                }
                if (f?.status == ForecastStatus.READY) {
                    Column(Modifier.fillMaxWidth().heightIn(max = panelHeight).background(FinTheme.colors.white, FinTheme.shapes.r12)) {
                        FinRow(title = stringResource(R.string.forecast_explain_action),
                            trailing = { Text(stringResource(if (expanded) R.string.forecast_hide else R.string.forecast_show),
                                style = FinTheme.typography.captionMedium) },
                            onClick = {
                                focus.clearFocus()
                                expanded = !expanded
                            })
                        if (expanded) Column(Modifier.verticalScroll(rememberScrollState())
                            .padding(horizontal = FinDimens.screenPadding, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ForecastBreakdown(f, state.tips, editIncome, accounts)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastTotals(f: SpendingForecast) {
    FinCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.forecast_limit), style = FinTheme.typography.caption)
            Text(f.dailyLimit?.let { f.money(it) } ?: stringResource(R.string.forecast_set_goal),
                style = FinTheme.typography.label)
            if (f.spentToday.signum() > 0 && f.dailyLimit != null) {
                val left = (f.dailyLimit - f.spentToday).max(BigDecimal.ZERO)
                Text(stringResource(R.string.forecast_ceiling_left, f.money(left)), style = FinTheme.typography.caption)
            }
            if (f.plan.payments == null) Text(stringResource(R.string.forecast_no_payments), style = FinTheme.typography.caption)
            if (f.available.signum() < 0) Text(stringResource(R.string.forecast_shortage, f.money(-f.available)),
                style = FinTheme.typography.label, color = FinTheme.colors.red)
            f.projectedBalance?.let {
                Text(stringResource(R.string.forecast_projected), style = FinTheme.typography.caption)
                Text(stringResource(R.string.forecast_estimate, f.money(it)), style = FinTheme.typography.label)
            }
        }
    }
}

@Composable
private fun ForecastBreakdown(f: SpendingForecast, tips: Boolean, editIncome: () -> Unit, accounts: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    f.usualDaily?.let {
        Text(stringResource(R.string.forecast_usual, f.money(it), f.knownDays), style = FinTheme.typography.body)
    }
    Text(stringResource(R.string.forecast_balance_line, f.money(f.balance)), style = FinTheme.typography.body)
    f.plan.savings?.let { Text(stringResource(R.string.forecast_goal_line, f.money(it))) }
    f.plan.payments?.let { Text(stringResource(R.string.forecast_payments_line, f.money(it))) }
    f.dailyLimit?.let {
        Text(stringResource(R.string.forecast_split, f.money(f.available.max(BigDecimal.ZERO)),
            requireNotNull(f.daysUntilIncome), f.money(it)), style = FinTheme.typography.body)
    }
    if (f.spentToday.signum() > 0) Text(stringResource(R.string.forecast_today_explanation), style = FinTheme.typography.caption)
    if (f.usualDaily != null && f.recommended != null && f.recommended < f.usualDaily)
        Text(stringResource(R.string.forecast_capped), style = FinTheme.typography.caption)
    if (tips) f.growth?.let {
        val name = if (locale.language == "en") it.names.en else it.names.ru
        FinNotice(stringResource(R.string.forecast_category, name, f.money(it.increase)),
            text = stringResource(R.string.forecast_category_action))
    }
    FinRow(stringResource(R.string.income_schedule_title), subtitle = requireNotNull(f.income).formatDate(locale), onClick = editIncome)
    FinRow(stringResource(R.string.forecast_accounts_action), subtitle = stringResource(R.string.forecast_balances,
        f.accounts, f.money(f.balance)), onClick = accounts)
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ForecastPreview() {
    val f = SpendingForecast(AppCurrency.RUB, LocalDate(2026, 11, 22), LocalDate(2026, 11, 30),
        BigDecimal("70237"), 2, 6, ForecastStatus.READY, ForecastPlan(BigDecimal("50000"), BigDecimal("8000")),
        BigDecimal.ZERO, BigDecimal("775"), BigDecimal("1529"), BigDecimal("775"), BigDecimal("56037"))
    FinDetectTheme { ForecastScreen(ForecastState(f), {}, {}, {}, {}, { _, _ -> }) }
}
