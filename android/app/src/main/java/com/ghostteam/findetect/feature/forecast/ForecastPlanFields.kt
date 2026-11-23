package com.ghostteam.findetect.feature.forecast

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinTextField
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.util.currencySymbol
import com.ghostteam.findetect.domain.model.ForecastPlan
import com.ghostteam.findetect.domain.model.SpendingForecast
import com.ghostteam.findetect.domain.validation.InitialBalanceParser
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private fun BigDecimal?.inputText() = this?.stripTrailingZeros()?.toPlainString().orEmpty()

@Composable
internal fun ForecastPlanFields(f: SpendingForecast, save: suspend (SpendingForecast, ForecastPlan) -> Unit) {
    val stored = f.plan
    var savings by rememberSaveable(stored.savings) { mutableStateOf(stored.savings.inputText()) }
    var payments by rememberSaveable(stored.payments) { mutableStateOf(stored.payments.inputText()) }
    var manual by rememberSaveable(stored.manualDaily) { mutableStateOf(stored.manualDaily.inputText()) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val parser = remember { InitialBalanceParser() }
    val plan = ForecastPlan(parser.parse(savings), parser.parse(payments),
        if (f.usualDaily == null) parser.parse(manual) else stored.manualDaily)
    val symbol = currencySymbol(f.currency.name)
    val invalidSavings = savings.isNotBlank() && plan.savings == null
    val invalidPayments = payments.isNotBlank() && plan.payments == null
    val invalidManual = f.usualDaily == null && manual.isNotBlank() && plan.manualDaily == null
    ForecastAmountField(R.string.forecast_goal, R.string.forecast_goal_hint, savings,
        { savings = it }, symbol, invalidSavings, busy)
    ForecastAmountField(R.string.forecast_payments, R.string.forecast_payments_hint, payments,
        { payments = it }, symbol, invalidPayments, busy)
    if (f.usualDaily == null) ForecastAmountField(R.string.forecast_manual, R.string.forecast_manual_placeholder,
        manual, { manual = it }, symbol, invalidManual, busy)
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val valid = !invalidSavings && !invalidPayments && !invalidManual
    if (plan != stored || !valid || busy) {
        if (failed) Text(stringResource(R.string.forecast_save_error), color = FinTheme.colors.red)
        FinButton(stringResource(R.string.ui_save), onClick = {
            if (!busy) {
                busy = true
                failed = false
                focus.clearFocus()
                scope.launch {
                    try { save(f, plan) }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { failed = true }
                    finally { busy = false }
                }
            }
        }, enabled = valid && !busy, loading = busy)
    }
}

@Composable
private fun ForecastAmountField(label: Int, hint: Int, value: String, change: (String) -> Unit,
    currency: String, invalid: Boolean, busy: Boolean) {
    FinTextField(label = stringResource(label), value = value, onValueChange = change,
        placeholder = stringResource(hint), enabled = !busy,
        errorText = if (invalid) stringResource(R.string.forecast_amount_error) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        trailing = { Text(currency, style = FinTheme.typography.label) })
}
