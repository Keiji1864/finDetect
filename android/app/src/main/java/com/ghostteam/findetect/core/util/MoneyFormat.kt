package com.ghostteam.findetect.core.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private const val MINUS = "\u2212"

/** Символ валюты по коду. Валюты проекта: RUB, USD, EUR, CNY. */
fun currencySymbol(code: String): String = when (code.uppercase()) {
    "RUB" -> "₽"
    "USD" -> "$"
    "EUR" -> "€"
    "CNY" -> "¥"
    else -> code
}

/** "24 850" / "24 850,50": пробел между разрядами, дробная часть только если она есть. */
fun BigDecimal.formatAmount(locale: Locale = Locale.getDefault()): String {
    val symbols = DecimalFormatSymbols.getInstance(locale)
    if (locale.language == "ru") symbols.groupingSeparator = '\u00A0'
    val hasFraction = this.stripTrailingZeros().scale() > 0
    return DecimalFormat(if (hasFraction) "#,##0.00" else "#,##0", symbols).format(this)
}

/** "24 850 ₽" */
fun formatMoney(amount: BigDecimal, currencyCode: String, locale: Locale = Locale.getDefault()): String =
    "${amount.formatAmount(locale)}\u00A0${currencySymbol(currencyCode)}"

/** Расход: "− 980 ₽", доход: "+50 000 ₽" (сумма в базе всегда положительная, знак определяет статья). */
fun formatSignedMoney(
    amount: BigDecimal,
    currencyCode: String,
    isIncome: Boolean,
    locale: Locale = Locale.getDefault(),
): String {
    val money = formatMoney(amount, currencyCode, locale)
    return if (isIncome) "+$money" else "$MINUS $money"
}