package com.ghostteam.findetect.feature.expenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExpensesScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Расходы",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Расходы, всего: 0 ₽",
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = "Пока нет расходов",
            modifier = Modifier.padding(top = 24.dp)
        )

        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Добавить расход")
        }
    }
}