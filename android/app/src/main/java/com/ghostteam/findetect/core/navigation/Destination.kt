package com.ghostteam.findetect.core.navigation

sealed class Destination(val route: String) {
    data object Expenses : Destination("expenses")
    data object Incomes : Destination("incomes")
    data object Accounts : Destination("accounts")
    data object Profile : Destination("profile")
    data object Analytics : Destination("analytics")
    data object Settings : Destination("settings")
    data object Login : Destination("login")
    data object Onboarding : Destination("onboarding")
    data object Lock : Destination("lock")
}