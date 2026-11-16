package com.ghostteam.findetect.core.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.R
import com.ghostteam.findetect.domain.model.SessionState
import com.ghostteam.findetect.domain.model.OnboardingStep
import com.ghostteam.findetect.feature.session.UserSessionViewModel
import com.ghostteam.findetect.core.util.Period
import com.ghostteam.findetect.feature.accounts.AccountsRoute
import com.ghostteam.findetect.feature.analytics.AnalyticsRoute
import com.ghostteam.findetect.feature.analytics.CategoryDetailRoute
import com.ghostteam.findetect.feature.expenses.ExpensesRoute
import com.ghostteam.findetect.feature.home.HomeRoute
import com.ghostteam.findetect.feature.incomes.IncomeRoute
import com.ghostteam.findetect.feature.onboarding.OnboardingIncomeScreen
import com.ghostteam.findetect.feature.onboarding.OnboardingPinScreen
import com.ghostteam.findetect.feature.profile.ProfileRoute
import com.ghostteam.findetect.feature.login.LoginScreen
import com.ghostteam.findetect.feature.onboarding.OnboardingIncomeScreen
import com.ghostteam.findetect.feature.onboarding.OnboardingPinScreen
import com.ghostteam.findetect.feature.profile.ProfileScreen
import com.ghostteam.findetect.feature.recovery.NewPasswordScreen
import com.ghostteam.findetect.feature.recovery.RecoveryCodeScreen
import com.ghostteam.findetect.feature.register.RegisterScreen
import com.ghostteam.findetect.feature.settings.AppearanceSettingsScreen
import com.ghostteam.findetect.feature.settings.CurrencySettingsScreen
import com.ghostteam.findetect.feature.settings.GamificationSettingsScreen
import com.ghostteam.findetect.feature.settings.RemindersSettingsScreen
import com.ghostteam.findetect.feature.settings.SecuritySettingsScreen
import com.ghostteam.findetect.feature.settings.SettingsScreen
import com.ghostteam.findetect.feature.settings.SettingsViewModel
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
fun AppNavHost(session: SessionState.Active, sessionModel: UserSessionViewModel, homeRequest: Int = 0, onExit: () -> Unit = {}) {
    val sessionUi by sessionModel.state.collectAsStateWithLifecycle()
    val start = remember(session.identity) {
        when (session.onboarding) {
            OnboardingStep.INCOME -> Destination.Onboarding.route
            OnboardingStep.PIN -> Destination.OnboardingPin.route
            OnboardingStep.DONE -> Destination.Home.route
        }
    }
    val editTransactionViewModel: EditTransactionViewModel = hiltViewModel()
    val transactionSnackbar = remember { SnackbarHostState() }
    val financePeriodViewModel: FinancePeriodViewModel = hiltViewModel()
    val financePeriod by financePeriodViewModel.state.collectAsStateWithLifecycle()
    val controller = rememberNavController()
    val entry by controller.currentBackStackEntryAsState()
    var sheetMode by rememberSaveable { mutableStateOf<String?>(null) }
    var transactionSessionId by rememberSaveable { mutableStateOf("") }
    var handledHomeRequest by rememberSaveable { mutableIntStateOf(0) }
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

    LaunchedEffect(homeRequest, session.onboarding) {
        if (session.onboarding == OnboardingStep.DONE && homeRequest > 0 && homeRequest != handledHomeRequest) {
            sheetMode = null
            financePeriodViewModel.dismiss()
            editTransactionViewModel.dismiss()
            controller.navigate(Destination.Home.route) {
                popUpTo(controller.graph.id) { inclusive = true }
                launchSingleTop = true
            }
            handledHomeRequest = homeRequest
        }
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
                    onForgotPassword = { open(Destination.RecoveryCode) },
                    onRegister = { open(Destination.Register) },
                )
            }
            composable(Destination.Register.route) {
                RegisterScreen(
                    onBack = back,
                    onRegister = { open(Destination.Onboarding) },
                )
            }
            composable(Destination.RecoveryCode.route) {
                RecoveryCodeScreen(
                    onBack = back,
                    onContinue = { open(Destination.RecoveryPassword) },
                    )
                }
            composable(Destination.RecoveryPassword.route) {
                NewPasswordScreen(
                    // Стрелка "назад" ведёт сразу на вход, минуя экран ввода кода
                    onBack = { controller.popBackStack(Destination.Login.route, inclusive = false) },
                    // Пароль обновлён: входим автоматически
                    onDone = {
                        controller.navigate(Destination.Home.route) {
                            popUpTo(Destination.Login.route) { inclusive = true }
                            launchSingleTop = true
                            }
                        },
                    )
                }
            composable(Destination.Onboarding.route) {
                OnboardingIncomeScreen(
                    onBack = onExit,
                    onContinue = { sessionModel.advance(OnboardingStep.PIN) { open(Destination.OnboardingPin) } },
                    onSkip = { sessionModel.advance(OnboardingStep.PIN) { open(Destination.OnboardingPin) } },
                )
            }
            composable(Destination.OnboardingPin.route) {
                OnboardingPinScreen(
                    onBack = { sessionModel.advance(OnboardingStep.INCOME) {
                        controller.navigate(Destination.Onboarding.route) { popUpTo(controller.graph.id) { inclusive = true } }
                    } },
                    onDone = {
                        sessionModel.advance(OnboardingStep.DONE) {
                            controller.navigate(Destination.Home.route) {
                                popUpTo(controller.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
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
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                val settings by settingsViewModel.state.collectAsStateWithLifecycle()
                ProfileRoute(
                    gameElementsEnabled = settings.settings?.gameElementsEnabled == true,
                    onBack = back,
                    onLogout = { sessionModel.logout() },
                    logoutWorking = sessionUi.working,
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
    val sessionError = stringResource(R.string.session_action_error)
    LaunchedEffect(sessionUi.error) { if (sessionUi.error) transactionSnackbar.showSnackbar(sessionError) }
    if (sessionUi.confirmLogout) AlertDialog(
        onDismissRequest = sessionModel::dismissLogout,
        containerColor = FinTheme.colors.paper,
        title = { Text(stringResource(R.string.session_logout_title), style = FinTheme.typography.heading, color = FinTheme.colors.navy) },
        text = { Text(stringResource(R.string.session_logout_pending), style = FinTheme.typography.body, color = FinTheme.colors.navy) },
        confirmButton = { TextButton(onClick = { sessionModel.logout(confirmed = true) }) {
            Text(stringResource(R.string.ui_sign_out), color = FinTheme.colors.red, style = FinTheme.typography.label)
        } },
        dismissButton = { TextButton(onClick = sessionModel::dismissLogout) {
            Text(stringResource(R.string.session_stay), color = FinTheme.colors.navy, style = FinTheme.typography.label)
        } },
    )
}
