package com.ghostteam.findetect.core.navigation

import android.net.Uri
import com.ghostteam.findetect.core.util.Period

sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object Expenses : Destination("expenses")
    data object Incomes : Destination("incomes")
    data object Accounts : Destination("accounts")
    data object Profile : Destination("profile")
    data object Streak : Destination("streak")
    data object Achievements : Destination("achievements")

    data object Analytics : Destination("analytics/{type}/{start}/{end}") {
        const val ARG_TYPE = "type"
        const val ARG_START = "start"
        const val ARG_END = "end"
        fun createRoute(income: Boolean, period: Period) =
            "analytics/${if (income) "income" else "expense"}/${period.start}/${period.end}"
    }

    data object CategoryDetail : Destination("category/{type}/{categoryId}/{start}/{end}") {
        const val ARG_TYPE = "type"
        const val ARG_CATEGORY_ID = "categoryId"
        const val ARG_START = "start"
        const val ARG_END = "end"
        fun createRoute(income: Boolean, categoryId: String, period: Period) =
            "category/${if (income) "income" else "expense"}/${Uri.encode(categoryId)}/${period.start}/${period.end}"
    }

    data object IncomeMonth : Destination("income-month/{start}/{end}") {
        const val ARG_START = "start"
        const val ARG_END = "end"
        fun createRoute(period: Period) = "income-month/${period.start}/${period.end}"
    }

    data object ChangePassword : Destination("profile/password")
    data object Settings : Destination("settings")
    data object SettingsAppearance : Destination("settings/appearance")
    data object SettingsCurrency : Destination("settings/currency")
    data object SettingsSecurity : Destination("settings/security")
    data object SettingsReminders : Destination("settings/reminders")
    data object SettingsGamification : Destination("settings/gamification")
    data object Login : Destination("login")
    data object Register : Destination("register")
    data object RecoveryCode : Destination("recovery/code")
    data object RecoveryPassword : Destination("recovery/password")
    data object Onboarding : Destination("onboarding")
    data object OnboardingPin : Destination("onboarding/pin")
    data object Lock : Destination("lock")
}
