package com.ghostteam.findetect.feature.accounts

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
fun AccountsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Счета",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Баланс, всего: 0 ₽",
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = "Пока нет счетов",
            modifier = Modifier.padding(top = 24.dp)
        )

        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Добавить счёт")
        }
    }
}
