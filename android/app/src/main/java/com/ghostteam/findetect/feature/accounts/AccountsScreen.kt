package com.ghostteam.findetect.feature.accounts

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.drawableResource
import com.ghostteam.findetect.core.designsystem.financeIcon
import com.ghostteam.findetect.core.designsystem.nameResource
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.navigation.Destination
import com.ghostteam.findetect.core.util.formatMoney
import com.ghostteam.findetect.feature.prototype.MainScreenPreview
import com.ghostteam.findetect.feature.prototype.PrototypeScreen
import java.math.BigDecimal

@Composable
fun AccountsRoute(onOpenAnalytics: () -> Unit = {}, onOpenSettings: () -> Unit = {}, viewModel: AccountsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AccountsScreen(state, viewModel::openForm, viewModel::retryRead, viewModel::retryCurrencySwitch, onOpenAnalytics, onOpenSettings)
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
) {
    PrototypeScreen(R.string.ui_accounts, onAnalytics = onOpenAnalytics, onSettings = onOpenSettings) {
        if (state.summary != null || (!state.loading && !state.readError)) {
            item(key = "balance") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.ui_total_balance), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                    Text(formatMoney(state.summary?.total ?: BigDecimal.ZERO, state.currency.name),
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
            FinRow(
                title = balance.account.name,
                subtitle = stringResource(state.currency.nameResource),
                leading = {
                    FinIconTile {
                        Icon(painterResource(financeIcon(balance.account.iconId).drawableResource), null,
                            tint = FinTheme.colors.navy, modifier = Modifier.size(24.dp))
                    }
                },
                trailing = { Text(formatMoney(balance.balance, balance.account.currency),
                    style = FinTheme.typography.label, color = FinTheme.colors.navy) },
            )
        }
        item(key = "add") {
            FinButton(stringResource(R.string.ui_add_account), onCreate, style = FinButtonStyle.Secondary,
                enabled = state.currencyReady && !state.switchingCurrency)
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
