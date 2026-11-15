package com.ghostteam.findetect.core.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.util.Period
import com.ghostteam.findetect.feature.accounts.AccountsRoute
import com.ghostteam.findetect.feature.analytics.AnalyticsRoute
import com.ghostteam.findetect.feature.analytics.CategoryDetailRoute
import com.ghostteam.findetect.feature.expenses.ExpensesRoute
import com.ghostteam.findetect.feature.home.HomeRoute
import com.ghostteam.findetect.feature.incomes.IncomeRoute
import com.ghostteam.findetect.feature.login.LoginScreen
import com.ghostteam.findetect.feature.onboarding.OnboardingIncomeScreen
import com.ghostteam.findetect.feature.onboarding.OnboardingPinScreen
import com.ghostteam.findetect.feature.profile.ProfileScreen
import com.ghostteam.findetect.feature.register.RegisterScreen
import com.ghostteam.findetect.feature.settings.AppearanceSettingsScreen
import com.ghostteam.findetect.feature.settings.CurrencySettingsScreen
import com.ghostteam.findetect.feature.settings.GamificationSettingsScreen
import com.ghostteam.findetect.feature.settings.RemindersSettingsScreen
import com.ghostteam.findetect.feature.settings.SecuritySettingsScreen
import com.ghostteam.findetect.feature.settings.SettingsScreen
import com.ghostteam.findetect.feature.transaction.AddTransactionMode
import com.ghostteam.findetect.feature.transaction.AddTransactionRoute
import com.ghostteam.findetect.feature.transaction.EditTransactionRoute
import com.ghostteam.findetect.feature.transaction.EditTransactionViewModel
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
    val editTransactionViewModel: EditTransactionViewModel = hiltViewModel()
    val transactionSnackbar = remember { SnackbarHostState() }
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
    val showBottomBar = currentRoute in mainRoutes

    val back: () -> Unit = { controller.popBackStack() }
    val open: (Destination) -> Unit = { controller.navigate(it.route) { launchSingleTop = true } }
    val openSettings = { open(Destination.Settings) }
    /** Иконка графика в шапке: тип по умолчанию зависит от вкладки-источника. */
    val openAnalytics: (Boolean) -> Unit = { income ->
        controller.navigate(Destination.Analytics.createRoute(income, financePeriod.selected)) { launchSingleTop = true }
    }
    val openAnalyticsCategory: (Boolean, String, Period) -> Unit = { income, categoryId, period ->
        controller.navigate(Destination.CategoryDetail.createRoute(income, categoryId, period)) { launchSingleTop = true }
    }
    val openIncomeMonth: (Period) -> Unit = { period ->
        controller.navigate(Destination.IncomeMonth.createRoute(period)) { launchSingleTop = true }
    }

    Scaffold(
        containerColor = FinTheme.colors.paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(transactionSnackbar) },
        bottomBar = {
            if (showBottomBar) {
                BottomBar(currentRoute ?: Destination.Home.route) { route ->
                    controller.navigate(route) {
                        popUpTo(Destination.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { padding ->
        NavHost(controller, Destination.Login.route, Modifier.padding(padding)) {
            composable(Destination.Login.route) {
                LoginScreen(
                    onLogin = {
                        controller.navigate(Destination.Home.route) {
                            popUpTo(Destination.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    // TODO: экран восстановления пароля (в макете его пока нет)
                    onForgotPassword = {},
                    onRegister = { open(Destination.Register) },
                )
            }
            composable(Destination.Register.route) {
                RegisterScreen(
                    onBack = back,
                    onRegister = { open(Destination.Onboarding) },
                )
            }
            composable(Destination.Onboarding.route) {
                OnboardingIncomeScreen(
                    onBack = back,
                    onContinue = { open(Destination.OnboardingPin) },
                    onSkip = { open(Destination.OnboardingPin) },
                )
            }
            composable(Destination.OnboardingPin.route) {
                OnboardingPinScreen(
                    onBack = back,
                    onDone = {
                        controller.navigate(Destination.Home.route) {
                            popUpTo(Destination.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Destination.Home.route) {
                HomeRoute(
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
                    onEditTransaction = editTransactionViewModel::open,
                    onOpenSettings = openSettings,
                    onOpenAnalytics = { openAnalytics(false) },
                )
            }
            composable(Destination.Incomes.route) {
                IncomeRoute(
                    period = financePeriod.selected,
                    onOpenPeriod = financePeriodViewModel::open,
                    onAddTransaction = { openTransaction(AddTransactionMode.Income) },
                    onEditTransaction = editTransactionViewModel::open,
                    onOpenSettings = openSettings,
                    onOpenAnalytics = { openAnalytics(true) },
                )
            }
            composable(Destination.Accounts.route) {
                AccountsRoute(onOpenSettings = openSettings, onOpenAnalytics = { openAnalytics(false) })
            }

            composable(
                Destination.Analytics.route,
                arguments = listOf(
                    navArgument(Destination.Analytics.ARG_TYPE) { type = NavType.StringType },
                    navArgument(Destination.Analytics.ARG_START) { type = NavType.StringType },
                    navArgument(Destination.Analytics.ARG_END) { type = NavType.StringType },
                ),
            ) {
                AnalyticsRoute(
                    onBack = back,
                    onOpenCategory = openAnalyticsCategory,
                    onOpenMonth = openIncomeMonth,
                    onEditTransaction = editTransactionViewModel::open,
                )
            }
            composable(
                Destination.CategoryDetail.route,
                arguments = listOf(
                    navArgument(Destination.CategoryDetail.ARG_TYPE) { type = NavType.StringType },
                    navArgument(Destination.CategoryDetail.ARG_CATEGORY_ID) { type = NavType.StringType },
                    navArgument(Destination.CategoryDetail.ARG_START) { type = NavType.StringType },
                    navArgument(Destination.CategoryDetail.ARG_END) { type = NavType.StringType },
                ),
            ) {
                CategoryDetailRoute(
                    onBack = back,
                    onEditTransaction = editTransactionViewModel::open,
                )
            }
            composable(
                Destination.IncomeMonth.route,
                arguments = listOf(
                    navArgument(Destination.IncomeMonth.ARG_START) { type = NavType.StringType },
                    navArgument(Destination.IncomeMonth.ARG_END) { type = NavType.StringType },
                    navArgument("type") { type = NavType.StringType; defaultValue = "income" },
                    navArgument("monthDetail") { type = NavType.BoolType; defaultValue = true },
                ),
            ) {
                AnalyticsRoute(
                    onBack = back,
                    onOpenCategory = openAnalyticsCategory,
                    onOpenMonth = openIncomeMonth,
                    onEditTransaction = editTransactionViewModel::open,
                )
            }

            composable(Destination.Profile.route) {
                ProfileScreen(
                    onBack = back,
                    onLogout = {
                        controller.navigate(Destination.Login.route) {
                            popUpTo(controller.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
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
    EditTransactionRoute(editTransactionViewModel, transactionSnackbar)
}
