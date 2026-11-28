package com.ghostteam.findetect.feature.accounts

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ghostteam.findetect.domain.model.AppCurrency
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.navigation.Destination
import com.ghostteam.findetect.core.util.formatMoney
import androidx.compose.ui.platform.LocalConfiguration
import com.ghostteam.findetect.feature.prototype.MainScreenPreview
import com.ghostteam.findetect.feature.prototype.PrototypeScreen
import java.math.BigDecimal

@Composable
fun AccountsRoute(onOpenAnalytics: () -> Unit = {}, onOpenSettings: () -> Unit = {}, viewModel: AccountsViewModel = hiltViewModel(),
    editor: AccountEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editorState by editor.state.collectAsStateWithLifecycle()
    val currencySwitch by editor.currencySwitch.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        AccountsScreen(state, viewModel::openForm, viewModel::retryRead, viewModel::retryCurrencySwitch, onOpenAnalytics, onOpenSettings,
            onFilterCurrency = viewModel::filterByCurrency,
            onEditAccount = { editor.open(it) }, onAdjustBalance = { editor.open(it) },
            accountActionsEnabled = !state.formOpen && !state.switchingCurrency && !editorState.open && currencySwitch == null)
        AccountManagementContent(editor, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
    if (state.formOpen) {
        AccountSheet(state, viewModel::changeDraft, viewModel::openPicker, viewModel::closePicker,
            viewModel::selectCurrency, viewModel::selectIcon, viewModel::confirmIcon, viewModel::save, viewModel::closeForm)
    }
}

@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onCreate: () -> Unit,
    onRetry: () -> Unit = {},
    onRetryCurrency: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onEditAccount: (String) -> Unit = {},
    onAdjustBalance: (String) -> Unit = {},
    accountActionsEnabled: Boolean = true,
    onFilterCurrency: (AppCurrency) -> Unit = {},
) {
    PrototypeScreen(R.string.ui_accounts, onAnalytics = onOpenAnalytics, onSettings = onOpenSettings) {
        item(key = "balance") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.ui_total_balance), modifier = Modifier.weight(1f),
                        style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                    CurrencyFilterChip(state.currency, onFilterCurrency)
                }
                if (state.summary != null || (!state.loading && !state.readError)) {
                    Text(formatMoney(state.summary?.total ?: BigDecimal.ZERO, state.currency.name, LocalConfiguration.current.locales[0]),
                        style = FinTheme.typography.balance, color = FinTheme.colors.navy)
                }
            }
        }
        if (state.loading) {
            item(key = "loading") { Text(stringResource(R.string.finance_loading), color = FinTheme.colors.muted) }
        }
        if (state.readError) {
            item(key = "error") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.accounts_read_error), color = FinTheme.colors.red)
                    FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary)
                }
            }
        }
        if (state.pendingCurrency != null && !state.switchingCurrency) {
            item(key = "switch_error") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.accounts_currency_error), color = FinTheme.colors.red)
                    FinButton(stringResource(R.string.finance_retry), onRetryCurrency, style = FinButtonStyle.Secondary)
                }
            }
        }
        if (!state.loading && !state.readError && state.summary?.accounts.isNullOrEmpty()) {
            item(key = "empty") {
                FinNotice(stringResource(R.string.accounts_empty), text = stringResource(R.string.accounts_empty_hint))
            }
        }
        items(state.summary?.accounts.orEmpty(), key = { it.account.id }) { balance ->
            AccountRow(balance, onEdit = { onEditAccount(balance.account.id) },
                onBalance = { onAdjustBalance(balance.account.id) }, enabled = accountActionsEnabled)
        }
        item(key = "add") {
            FinButton(stringResource(R.string.ui_add_account), onCreate, style = FinButtonStyle.Secondary,
                enabled = state.currencyReady && !state.switchingCurrency)
        }
    }
}

@Composable
private fun CurrencyFilterChip(selected: AppCurrency, onSelect: (AppCurrency) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val ink = FinTheme.colors.navy
    Box {
        Row(
            modifier = Modifier.clip(FinTheme.shapes.r12).background(FinTheme.colors.cream)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.ui_acc_currency_label)) { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(selected.name, style = FinTheme.typography.label, color = ink)
            Canvas(Modifier.size(12.dp)) {
                val tip = Offset(size.width / 2, size.height * 0.68f)
                drawLine(ink, Offset(size.width * 0.12f, size.height * 0.32f), tip, 1.6.dp.toPx(), StrokeCap.Round)
                drawLine(ink, tip, Offset(size.width * 0.88f, size.height * 0.32f), 1.6.dp.toPx(), StrokeCap.Round)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            shape = FinTheme.shapes.r16, containerColor = FinTheme.colors.cream) {
            AppCurrency.entries.forEach { currency ->
                DropdownMenuItem(text = { Text("${currency.name} (${currency.symbol})") }, onClick = {
                    expanded = false
                    onSelect(currency)
                })
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountsPreview() {
    MainScreenPreview(Destination.Accounts.route) {
        AccountsScreen(AccountsUiState(loading = false, currencyReady = true), onCreate = {})
    }
}
