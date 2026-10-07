package com.ghostteam.findetect.feature.income

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IncomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Доходы",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Доходы, всего: 0 ₽",
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = "Пока нет доходов",
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}