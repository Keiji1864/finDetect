package com.ghostteam.findetect.core.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.feature.accounts.AccountsScreen
import com.ghostteam.findetect.feature.expenses.ExpensesScreen
import com.ghostteam.findetect.feature.home.HomeScreen
import com.ghostteam.findetect.feature.incomes.IncomeScreen
import com.ghostteam.findetect.feature.transaction.AddTransactionMode
import com.ghostteam.findetect.feature.transaction.AddTransactionSheet

@Composable
fun AppNavHost() {
    val controller = rememberNavController()
    val entry by controller.currentBackStackEntryAsState()
    var sheetMode by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        containerColor = FinTheme.colors.paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomBar(entry?.destination?.route ?: Destination.Home.route) { route ->
                controller.navigate(route) {
                    popUpTo(controller.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
    ) { padding ->
        NavHost(controller, Destination.Home.route, Modifier.padding(padding)) {
            composable(Destination.Home.route) {
                HomeScreen { sheetMode = AddTransactionMode.Switchable.name }
            }
            composable(Destination.Expenses.route) {
                ExpensesScreen { sheetMode = AddTransactionMode.Expense.name }
            }
            composable(Destination.Incomes.route) {
                IncomeScreen { sheetMode = AddTransactionMode.Income.name }
            }
            composable(Destination.Accounts.route) { AccountsScreen() }
        }
    }
    sheetMode?.let { mode ->
        AddTransactionSheet(AddTransactionMode.valueOf(mode), onDismiss = { sheetMode = null })
    }
}
