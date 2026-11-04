package com.ghostteam.findetect.core.navigation

sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object Expenses : Destination("expenses")
    data object Incomes : Destination("incomes")
    data object Accounts : Destination("accounts")
    data object Profile : Destination("profile")

    data object Analytics : Destination("analytics/{type}") {
        const val ARG_TYPE = "type"
        fun createRoute(income: Boolean) = "analytics/${if (income) "income" else "expense"}"
    }

    data object CategoryDetail : Destination("category/{type}/{index}/{period}") {
        const val ARG_TYPE = "type"
        const val ARG_INDEX = "index"
        const val ARG_PERIOD = "period"
        fun createRoute(income: Boolean, index: Int, period: Int) =
            "category/${if (income) "income" else "expense"}/$index/$period"
    }

    data object Settings : Destination("settings")
    data object SettingsAppearance : Destination("settings/appearance")
    data object SettingsCurrency : Destination("settings/currency")
    data object SettingsSecurity : Destination("settings/security")
    data object SettingsReminders : Destination("settings/reminders")
    data object SettingsGamification : Destination("settings/gamification")
    data object Login : Destination("login")
    data object Onboarding : Destination("onboarding")
    data object Lock : Destination("lock")
}