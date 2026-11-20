package com.ghostteam.findetect.feature.forecast

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.util.formatDate
import com.ghostteam.findetect.core.util.formatMoney
import com.ghostteam.findetect.domain.model.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import java.math.BigDecimal

@Composable
private fun SpendingForecast.money(value: BigDecimal) = formatMoney(value, currency.name, LocalConfiguration.current.locales[0])

@Composable
private fun SpendingForecast.statusText() = when (status) {
    ForecastStatus.NO_DATE -> stringResource(R.string.forecast_no_date)
    ForecastStatus.DATE_PASSED -> stringResource(R.string.forecast_date_passed)
    ForecastStatus.NO_ACCOUNTS -> stringResource(R.string.forecast_no_accounts)
    ForecastStatus.COLLECTING -> stringResource(R.string.forecast_collecting, 3 - knownDays)
    ForecastStatus.GREEN -> stringResource(R.string.forecast_green)
    ForecastStatus.YELLOW -> stringResource(R.string.forecast_yellow)
    ForecastStatus.RED -> stringResource(R.string.forecast_red)
}

@Composable
private fun SpendingForecast.advice(): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    if (dailyLimit != null && status in listOf(ForecastStatus.RED, ForecastStatus.YELLOW)) {
        val action = if (balance.signum() <= 0) stringResource(R.string.forecast_check_balance)
            else stringResource(R.string.forecast_risk_action, money(dailyLimit))
        result += statusText() to action
    }
    if (growth != null) {
        val names = growth.names
        val name = if (LocalConfiguration.current.locales[0].language == "en") names.en else names.ru
        result += stringResource(R.string.forecast_category, name, money(growth.increase)) to
            stringResource(R.string.forecast_category_action)
    }
    if (expectedDaily != null) {
        result += stringResource(R.string.forecast_pace, money(expectedDaily)) to
            stringResource(R.string.forecast_pace_action, knownDays)
    }
    return result
}

@Composable
private fun ForecastReadStatus(state: ForecastState, retry: () -> Unit) {
    if (state.failed) {
        FinNotice(stringResource(R.string.forecast_error))
        FinButton(stringResource(R.string.finance_retry), retry)
    } else Text(stringResource(R.string.finance_loading), style = FinTheme.typography.caption)
}

/** Карточка главной с листанием доступных советов и переходом к расчёту. */
@Composable
internal fun HomeForecast(state: ForecastState, retry: () -> Unit, open: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val forecast = state.result
        if (forecast == null) ForecastReadStatus(state, retry) else {
            val advice = if (state.tips) forecast.advice() else emptyList()
            if (advice.isEmpty()) FinNotice(forecast.statusText())
            else {
                val pager = rememberPagerState { advice.size }
                HorizontalPager(state = pager) { index ->
                    val (title, body) = advice[index]
                    FinAdviceCard(title, body, caption = stringResource(R.string.ui_advice),
                        pager = if (advice.size > 1) stringResource(R.string.forecast_page, index + 1, advice.size) else null,
                        onClick = open)
                }
            }
        }
        FinButton(stringResource(R.string.forecast_open), open, style = FinButtonStyle.Secondary)
    }
}

@Composable
internal fun ForecastScreen(state: ForecastState, back: () -> Unit, retry: () -> Unit, editIncome: () -> Unit,
    accounts: () -> Unit, explain: () -> Unit) {
    FinSubScreen(stringResource(R.string.forecast_title), back) {
        val forecast = state.result
        if (forecast == null) ForecastReadStatus(state, retry) else {
            FinNotice(forecast.statusText(), text = forecast.daysUntilIncome?.let { stringResource(R.string.forecast_days, it) })
            when (forecast.status) {
                ForecastStatus.NO_DATE, ForecastStatus.DATE_PASSED -> FinButton(stringResource(R.string.income_schedule_title), editIncome)
                ForecastStatus.NO_ACCOUNTS -> FinButton(stringResource(R.string.forecast_accounts_action), accounts)
                else -> Unit
            }
            forecast.dailyLimit?.let { limit ->
                Text(stringResource(R.string.forecast_limit), style = FinTheme.typography.label)
                Text(forecast.money(limit), style = FinTheme.typography.balance, color = FinTheme.colors.navy)
                Text(stringResource(when (forecast.confidence) {
                    ForecastConfidence.PRELIMINARY -> R.string.forecast_preliminary
                    ForecastConfidence.STANDARD -> R.string.forecast_standard
                    ForecastConfidence.HIGH -> R.string.forecast_high
                }), style = FinTheme.typography.caption)
                if (state.tips) forecast.advice().forEach { (title, body) -> FinAdviceCard(title, body) }
            }
            FinRow(stringResource(R.string.forecast_explain_action), onClick = explain)
        }
    }
}

@Composable
internal fun ForecastExplanation(state: ForecastState, back: () -> Unit, retry: () -> Unit, accounts: () -> Unit) {
    FinSubScreen(stringResource(R.string.forecast_explanation), back) {
        val forecast = state.result
        if (forecast == null) ForecastReadStatus(state, retry) else {
            FinNotice(stringResource(R.string.forecast_limit), text = stringResource(R.string.forecast_method))
            FinRow(stringResource(R.string.forecast_history), subtitle = stringResource(R.string.forecast_window,
                forecast.today.minus(28, DateTimeUnit.DAY).formatDate(), forecast.today.minus(1, DateTimeUnit.DAY).formatDate(),
                forecast.knownDays))
            FinRow(stringResource(R.string.forecast_accounts), subtitle = stringResource(R.string.forecast_balances,
                forecast.accounts, forecast.money(forecast.balance)))
            FinRow(stringResource(R.string.income_schedule_title),
                subtitle = forecast.income?.formatDate() ?: forecast.statusText())
            FinButton(stringResource(R.string.forecast_accounts_action), accounts, style = FinButtonStyle.Secondary)
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ForecastPreview() {
    val forecast = SpendingForecast(AppCurrency.RUB, LocalDate(2026, 11, 20), LocalDate(2026, 11, 30),
        BigDecimal("10000"), 2, 10, ForecastStatus.GREEN, ForecastConfidence.STANDARD, BigDecimal("900"), BigDecimal("300"))
    FinDetectTheme { HomeForecast(ForecastState(forecast), {}, {}) }
}
