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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.feature.accounts.AccountsRoute
import com.ghostteam.findetect.feature.analytics.AnalyticsScreen
import com.ghostteam.findetect.feature.analytics.CategoryDetailScreen
import com.ghostteam.findetect.feature.expenses.ExpensesRoute
import com.ghostteam.findetect.feature.home.HomeScreen
import com.ghostteam.findetect.feature.incomes.IncomeRoute
import com.ghostteam.findetect.feature.profile.ProfileScreen
import com.ghostteam.findetect.feature.settings.AppearanceSettingsScreen
import com.ghostteam.findetect.feature.settings.CurrencySettingsScreen
import com.ghostteam.findetect.feature.settings.GamificationSettingsScreen
import com.ghostteam.findetect.feature.settings.RemindersSettingsScreen
import com.ghostteam.findetect.feature.settings.SecuritySettingsScreen
import com.ghostteam.findetect.feature.settings.SettingsScreen
import com.ghostteam.findetect.feature.transaction.AddTransactionMode
import com.ghostteam.findetect.feature.transaction.AddTransactionRoute
import com.ghostteam.findetect.feature.transaction.FinancePeriodViewModel
import com.ghostteam.findetect.feature.transaction.FinancePeriodSheet
import java.util.UUID

/** Нижняя навигация показывается только на четырёх главных вкладках */
private val mainRoutes = setOf(
    Destination.Home.route,
    Destination.Expenses.route,
    Destination.Incomes.route,
    Destination.Accounts.route,
)

@Composable
fun AppNavHost() {
    val financePeriodViewModel: FinancePeriodViewModel = hiltViewModel()
    val financePeriod by financePeriodViewModel.state.collectAsStateWithLifecycle()
    val controller = rememberNavController()
    val entry by controller.currentBackStackEntryAsState()
    var sheetMode by rememberSaveable { mutableStateOf<String?>(null) }
    var transactionSessionId by rememberSaveable { mutableStateOf("") }
    val openTransaction: (AddTransactionMode) -> Unit = { mode ->
        transactionSessionId = UUID.randomUUID().toString()
        sheetMode = mode.name
    }

    val currentRoute = entry?.destination?.route
    val showBottomBar = currentRoute == null || currentRoute in mainRoutes

    val back: () -> Unit = { controller.popBackStack() }
    val open: (Destination) -> Unit = { controller.navigate(it.route) { launchSingleTop = true } }
    val openSettings = { open(Destination.Settings) }
    /** Иконка графика в шапке: тип по умолчанию зависит от вкладки-источника. */
    val openAnalytics: (Boolean) -> Unit = { income ->
        controller.navigate(Destination.Analytics.createRoute(income)) { launchSingleTop = true }
    }

    Scaffold(
        containerColor = FinTheme.colors.paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                BottomBar(currentRoute ?: Destination.Home.route) { route ->
                    controller.navigate(route) {
                        popUpTo(controller.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { padding ->
        NavHost(controller, Destination.Home.route, Modifier.padding(padding)) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onAddTransaction = { openTransaction(AddTransactionMode.Switchable) },
                    onOpenProfile = { open(Destination.Profile) },
                    onOpenSettings = openSettings,
                    onOpenAnalytics = { openAnalytics(false) },
                )
            }
            composable(Destination.Expenses.route) {
                ExpensesRoute(
                    period = financePeriod.selected,
                    onOpenPeriod = financePeriodViewModel::open,
                    onAddTransaction = { openTransaction(AddTransactionMode.Expense) },
                    onOpenSettings = openSettings,
                    onOpenAnalytics = { openAnalytics(false) },
                )
            }
            composable(Destination.Incomes.route) {
                IncomeRoute(
                    period = financePeriod.selected,
                    onOpenPeriod = financePeriodViewModel::open,
                    onAddTransaction = { openTransaction(AddTransactionMode.Income) },
                    onOpenSettings = openSettings,
                    onOpenAnalytics = { openAnalytics(true) },
                )
            }
            composable(Destination.Accounts.route) {
                AccountsRoute(onOpenSettings = openSettings, onOpenAnalytics = { openAnalytics(false) })
            }

            composable(
                Destination.Analytics.route,
                arguments = listOf(navArgument(Destination.Analytics.ARG_TYPE) { type = NavType.StringType }),
            ) { backStackEntry ->
                val income = backStackEntry.arguments?.getString(Destination.Analytics.ARG_TYPE) == "income"
                AnalyticsScreen(
                    initialIncome = income,
                    onBack = back,
                    onOpenCategory = { isIncome, index, period ->
                        controller.navigate(Destination.CategoryDetail.createRoute(isIncome, index, period)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                Destination.CategoryDetail.route,
                arguments = listOf(
                    navArgument(Destination.CategoryDetail.ARG_TYPE) { type = NavType.StringType },
                    navArgument(Destination.CategoryDetail.ARG_INDEX) { type = NavType.IntType },
                    navArgument(Destination.CategoryDetail.ARG_PERIOD) { type = NavType.IntType },
                ),
            ) { backStackEntry ->
                val args = backStackEntry.arguments
                CategoryDetailScreen(
                    isIncome = args?.getString(Destination.CategoryDetail.ARG_TYPE) == "income",
                    categoryIndex = args?.getInt(Destination.CategoryDetail.ARG_INDEX) ?: 0,
                    initialPeriod = args?.getInt(Destination.CategoryDetail.ARG_PERIOD) ?: 0,
                    onBack = back,
                )
            }

            composable(Destination.Profile.route) {
                // TODO: onLogout -> экран входа (Destination.Login), когда он появится
                ProfileScreen(onBack = back)
            }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    onBack = back,
                    onOpenAppearance = { open(Destination.SettingsAppearance) },
                    onOpenCurrency = { open(Destination.SettingsCurrency) },
                    onOpenSecurity = { open(Destination.SettingsSecurity) },
                    onOpenReminders = { open(Destination.SettingsReminders) },
                    onOpenGamification = { open(Destination.SettingsGamification) },
                )
            }
            composable(Destination.SettingsAppearance.route) { AppearanceSettingsScreen(onBack = back) }
            composable(Destination.SettingsCurrency.route) { CurrencySettingsScreen(onBack = back) }
            composable(Destination.SettingsSecurity.route) { SecuritySettingsScreen(onBack = back) }
            composable(Destination.SettingsReminders.route) { RemindersSettingsScreen(onBack = back) }
            composable(Destination.SettingsGamification.route) { GamificationSettingsScreen(onBack = back) }
        }
    }
    sheetMode?.let { mode ->
        AddTransactionRoute(transactionSessionId.ifEmpty { mode }, AddTransactionMode.valueOf(mode), onDismiss = { sheetMode = null })
    }
    if (financePeriod.open) FinancePeriodSheet(financePeriodViewModel)
}
