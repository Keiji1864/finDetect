package com.ghostteam.findetect.data.mapper

import java.math.BigDecimal
import java.math.RoundingMode

internal enum class MoneyRule { NON_NEGATIVE, POSITIVE, SIGNED_NON_ZERO }

private val apiMoneyPattern = Regex("-?(0|[1-9][0-9]{0,8})(\\.[0-9]{1,2})?")
private val maxMoneyAmount = BigDecimal("999999999.99")

internal fun parseMoney(value: String, rule: MoneyRule): BigDecimal {
    require(apiMoneyPattern.matches(value)) { "Invalid API money amount: $value" }
    val amount = BigDecimal(value)
    require(rule == MoneyRule.SIGNED_NON_ZERO || !value.startsWith('-')) { "Negative unsigned amount" }
    return checkedMoney(amount, rule)
}

internal fun formatMoney(amount: BigDecimal, rule: MoneyRule): String = checkedMoney(amount, rule).toPlainString()

internal fun checkedMoney(amount: BigDecimal, rule: MoneyRule): BigDecimal {
    val scaled = amount.setScale(2, RoundingMode.UNNECESSARY)
    require(scaled.abs() <= maxMoneyAmount) { "Money amount exceeds 9 integer digits" }
    require(
        when (rule) {
            MoneyRule.NON_NEGATIVE -> scaled.signum() >= 0
            MoneyRule.POSITIVE -> scaled.signum() > 0
            MoneyRule.SIGNED_NON_ZERO -> scaled.signum() != 0
        },
    ) { "Invalid money amount for $rule" }
    return scaled
}
