package com.ghostteam.findetect.core.designsystem

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ghostteam.findetect.R
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.FinanceIcon

@get:StringRes
val AppCurrency.nameResource: Int get() = when (this) {
    AppCurrency.RUB -> R.string.ui_currency_rub_name
    AppCurrency.USD -> R.string.ui_currency_usd_name
    AppCurrency.EUR -> R.string.ui_currency_eur_name
    AppCurrency.CNY -> R.string.ui_currency_cny_name
}

@get:StringRes
val AppCurrency.codeResource: Int get() = when (this) {
    AppCurrency.RUB -> R.string.ui_currency_rub
    AppCurrency.USD -> R.string.ui_currency_usd
    AppCurrency.EUR -> R.string.ui_currency_eur
    AppCurrency.CNY -> R.string.ui_currency_cny
}

@get:DrawableRes
val FinanceIcon.drawableResource: Int get() = when (this) {
    FinanceIcon.WALLET -> R.drawable.ic_wallet
    FinanceIcon.CHART -> R.drawable.ic_chart
    FinanceIcon.USER -> R.drawable.ic_user
    FinanceIcon.GEAR -> R.drawable.ic_settings
    FinanceIcon.BASKET -> R.drawable.ic_basket
    FinanceIcon.COFFEE -> R.drawable.ic_coffee
    FinanceIcon.BUS -> R.drawable.ic_bus
    FinanceIcon.CASH -> R.drawable.ic_cash
    FinanceIcon.CALENDAR -> R.drawable.ic_calendar
    FinanceIcon.TROPHY -> R.drawable.ic_trophy
    FinanceIcon.FLAME -> R.drawable.ic_flame
    FinanceIcon.SPARK -> R.drawable.ic_spark
    FinanceIcon.LOCK -> R.drawable.ic_lock
    FinanceIcon.RECEIPT -> R.drawable.ic_expenses
    FinanceIcon.PLUS -> R.drawable.ic_plus
    FinanceIcon.CHECK -> R.drawable.ic_check
}

@get:StringRes
val FinanceIcon.labelResource: Int get() = when (this) {
    FinanceIcon.WALLET -> R.string.ui_wallet
    FinanceIcon.CHART -> R.string.finance_icon_chart
    FinanceIcon.USER -> R.string.finance_icon_user
    FinanceIcon.GEAR -> R.string.finance_icon_gear
    FinanceIcon.BASKET -> R.string.finance_icon_basket
    FinanceIcon.COFFEE -> R.string.finance_icon_coffee
    FinanceIcon.BUS -> R.string.finance_icon_bus
    FinanceIcon.CASH -> R.string.ui_acc_icon_cash
    FinanceIcon.CALENDAR -> R.string.finance_icon_calendar
    FinanceIcon.TROPHY -> R.string.finance_icon_trophy
    FinanceIcon.FLAME -> R.string.finance_icon_flame
    FinanceIcon.SPARK -> R.string.finance_icon_spark
    FinanceIcon.LOCK -> R.string.finance_icon_lock
    FinanceIcon.RECEIPT -> R.string.finance_icon_receipt
    FinanceIcon.PLUS -> R.string.finance_icon_plus
    FinanceIcon.CHECK -> R.string.finance_icon_check
}

fun financeIcon(key: String): FinanceIcon = FinanceIcon.entries.firstOrNull { it.key == key } ?: FinanceIcon.WALLET
