package com.ghostteam.findetect.feature.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

enum class AddTransactionMode { Switchable, Expense, Income }

/** Ввод хранится только пока открыта форма. Сохранение транзакций отсутствует. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(mode: AddTransactionMode, onDismiss: () -> Unit) {
    var isIncome by rememberSaveable(mode) { mutableStateOf(mode == AddTransactionMode.Income) }
    var amount by rememberSaveable { mutableStateOf("") }
    var comment by rememberSaveable { mutableStateOf("") }
    val amountPattern = remember { Regex("[0-9]*([.,][0-9]{0,2})?") }
    val c = FinTheme.colors
    FinBottomSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().weight(1f, fill = false).imePadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (mode == AddTransactionMode.Switchable) {
                FinSegmentedSwitch(
                    options = listOf(false to stringResource(R.string.ui_expenses), true to stringResource(R.string.ui_income)),
                    selected = isIncome,
                    onSelect = { isIncome = it },
                )
            } else {
                Text(stringResource(if (isIncome) R.string.ui_add_income_title else R.string.ui_add_expense_title),
                    style = FinTheme.typography.sheetTitle, color = c.navy)
            }
            Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.ui_amount), style = FinTheme.typography.caption, color = c.muted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicTextField(
                        value = amount,
                        onValueChange = { if (it.length <= 12 && amountPattern.matches(it)) amount = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = FinTheme.typography.amountInput.copy(color = if (isIncome) c.green else c.navy),
                        cursorBrush = SolidColor(c.navy),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box {
                                if (amount.isEmpty()) Text(stringResource(R.string.ui_zero),
                                    style = FinTheme.typography.amountInput, color = c.muted)
                                inner()
                            }
                        },
                    )
                    Text(stringResource(R.string.ui_currency), style = FinTheme.typography.amountInput, color = c.muted)
                }
            }
            FinTextField(stringResource(R.string.ui_category), "", {}, enabled = false,
                placeholder = stringResource(R.string.ui_choose_category))
            FinTextField(stringResource(R.string.ui_account), "", {}, enabled = false,
                placeholder = stringResource(R.string.ui_choose_account))
            FinTextField(stringResource(R.string.ui_date), "", {}, enabled = false,
                placeholder = stringResource(R.string.ui_choose_date))
            FinTextField(stringResource(R.string.ui_comment), comment, { comment = it.take(200) },
                placeholder = stringResource(R.string.ui_optional))
            FinButton(stringResource(R.string.ui_save), onClick = {}, enabled = false)
            Text(stringResource(R.string.ui_save_disabled), style = FinTheme.typography.caption, color = c.muted)
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.ui_close), style = FinTheme.typography.label, color = c.navy)
            }
        }
    }
}
