package com.ghostteam.findetect.feature.incomes

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.core.navigation.Destination
import com.ghostteam.findetect.feature.prototype.MainScreenPreview
import com.ghostteam.findetect.feature.prototype.TransactionsContent

@Composable
fun IncomeScreen(onAddTransaction: () -> Unit = {}, onOpenSettings: () -> Unit = {}) {
    TransactionsContent(isIncome = true, onAdd = onAddTransaction, onSettings = onOpenSettings)
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IncomeScreenPreview() { MainScreenPreview(Destination.Incomes.route) { IncomeScreen() } }
