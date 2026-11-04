package com.ghostteam.findetect.core.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.feature.accounts.AccountsScreen
import com.ghostteam.findetect.feature.analytics.AnalyticsScreen
import com.ghostteam.findetect.feature.analytics.CategoryDetailScreen
import com.ghostteam.findetect.feature.expenses.ExpensesScreen
import com.ghostteam.findetect.feature.home.HomeScreen
import com.ghostteam.findetect.feature.incomes.IncomeScreen
import com.ghostteam.findetect.feature.profile.ProfileScreen
import com.ghostteam.findetect.feature.settings.AppearanceSettingsScreen
import com.ghostteam.findetect.feature.settings.CurrencySettingsScreen
import com.ghostteam.findetect.feature.settings.GamificationSettingsScreen
import com.ghostteam.findetect.feature.settings.RemindersSettingsScreen
import com.ghostteam.findetect.feature.settings.SecuritySettingsScreen
import com.ghostteam.findetect.feature.settings.SettingsScreen
import com.ghostteam.findetect.feature.transaction.AddTransactionMode
import com.ghostteam.findetect.feature.transaction.AddTransactionSheet

private val mainRoutes = setOf(
    Destination.Home.route,
    Destination.Expenses.route,
    Destination.Incomes.route,
    Destination.Accounts.route,
)

@Composable
fun AppNavHost(
    onLogout: () -> Unit = {}
) {
    val controller = rememberNavController()
    val entry by controller.currentBackStackEntryAsState()

    var sheetMode by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val currentRoute = entry?.destination?.route
    val showBottomBar = currentRoute == null ||
            currentRoute in mainRoutes

    val back: () -> Unit = {
        controller.popBackStack()
    }

    val open: (Destination) -> Unit = {
        controller.navigate(it.route) {
            launchSingleTop = true
        }
    }

    val openSettings = {
        open(Destination.Settings)
    }

    val openAnalytics: (Boolean) -> Unit = { income ->
        controller.navigate(
            Destination.Analytics.createRoute(income)
        ) {
            launchSingleTop = true
        }
    }

    Scaffold(
        containerColor = FinTheme.colors.paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    currentRoute ?: Destination.Home.route
                ) { route ->
                    controller.navigate(route) {
                        popUpTo(
                            controller.graph.findStartDestination().id
                        ) {
                            saveState = true
                        }

                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = controller,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onAddTransaction = {
                        sheetMode = AddTransactionMode.Switchable.name
                    },
                    onOpenProfile = {
                        open(Destination.Profile)
                    },
                    onOpenSettings = openSettings,
                    onOpenAnalytics = {
                        openAnalytics(false)
                    }
                )
            }

            composable(Destination.Expenses.route) {
                ExpensesScreen(
                    onAddTransaction = {
                        sheetMode = AddTransactionMode.Expense.name
                    },
                    onOpenSettings = openSettings,
                    onOpenAnalytics = {
                        openAnalytics(false)
                    }
                )
            }

            composable(Destination.Incomes.route) {
                IncomeScreen(
                    onAddTransaction = {
                        sheetMode = AddTransactionMode.Income.name
                    },
                    onOpenSettings = openSettings,
                    onOpenAnalytics = {
                        openAnalytics(true)
                    }
                )
            }

            composable(Destination.Accounts.route) {
                AccountsScreen(
                    onOpenSettings = openSettings,
                    onOpenAnalytics = {
                        openAnalytics(false)
                    }
                )
            }

            composable(
                route = Destination.Analytics.route,
                arguments = listOf(
                    navArgument(Destination.Analytics.ARG_TYPE) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val income = backStackEntry.arguments
                    ?.getString(Destination.Analytics.ARG_TYPE) == "income"

                AnalyticsScreen(
                    initialIncome = income,
                    onBack = back,
                    onOpenCategory = { isIncome, index, period ->
                        controller.navigate(
                            Destination.CategoryDetail.createRoute(
                                isIncome,
                                index,
                                period
                            )
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Destination.CategoryDetail.route,
                arguments = listOf(
                    navArgument(Destination.CategoryDetail.ARG_TYPE) {
                        type = NavType.StringType
                    },
                    navArgument(Destination.CategoryDetail.ARG_INDEX) {
                        type = NavType.IntType
                    },
                    navArgument(Destination.CategoryDetail.ARG_PERIOD) {
                        type = NavType.IntType
                    }
                )
            ) { backStackEntry ->
                val args = backStackEntry.arguments

                CategoryDetailScreen(
                    isIncome = args?.getString(
                        Destination.CategoryDetail.ARG_TYPE
                    ) == "income",
                    categoryIndex = args?.getInt(
                        Destination.CategoryDetail.ARG_INDEX
                    ) ?: 0,
                    initialPeriod = args?.getInt(
                        Destination.CategoryDetail.ARG_PERIOD
                    ) ?: 0,
                    onBack = back
                )
            }

            composable(Destination.Profile.route) {
                ProfileScreen(onBack = back)
            }

            composable(Destination.Settings.route) {
                SettingsScreen(
                    onLogout = onLogout,
                    onBack = back,
                    onOpenAppearance = {
                        open(Destination.SettingsAppearance)
                    },
                    onOpenCurrency = {
                        open(Destination.SettingsCurrency)
                    },
                    onOpenSecurity = {
                        open(Destination.SettingsSecurity)
                    },
                    onOpenReminders = {
                        open(Destination.SettingsReminders)
                    },
                    onOpenGamification = {
                        open(Destination.SettingsGamification)
                    }
                )
            }

            composable(Destination.SettingsAppearance.route) {
                AppearanceSettingsScreen(onBack = back)
            }

            composable(Destination.SettingsCurrency.route) {
                CurrencySettingsScreen(onBack = back)
            }

            composable(Destination.SettingsSecurity.route) {
                SecuritySettingsScreen(onBack = back)
            }

            composable(Destination.SettingsReminders.route) {
                RemindersSettingsScreen(onBack = back)
            }

            composable(Destination.SettingsGamification.route) {
                GamificationSettingsScreen(onBack = back)
            }
        }
    }

    sheetMode?.let { mode ->
        AddTransactionSheet(
            AddTransactionMode.valueOf(mode),
            onDismiss = {
                sheetMode = null
            }
        )
    }
}