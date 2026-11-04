package com.ghostteam.findetect.feature.expenses

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.core.navigation.Destination
import com.ghostteam.findetect.feature.prototype.MainScreenPreview
import com.ghostteam.findetect.feature.prototype.TransactionsContent

@Composable
fun ExpensesScreen(onAddTransaction: () -> Unit = {}, onOpenSettings: () -> Unit = {}) {
    TransactionsContent(isIncome = false, onAdd = onAddTransaction, onSettings = onOpenSettings)
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExpensesScreenPreview() { MainScreenPreview(Destination.Expenses.route) { ExpensesScreen() } }
