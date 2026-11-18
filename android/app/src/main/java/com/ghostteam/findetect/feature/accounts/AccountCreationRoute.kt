package com.ghostteam.findetect.feature.accounts

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R

/** Reuses account creation from another form and reports the locally saved account's ID. */
@Composable
fun AccountCreationRoute(
    requestId: String,
    onCreated: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(key = "transaction-account-creation"),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activeRequest = state.creationRequestId == requestId
    LaunchedEffect(requestId, state.currencyReady, state.switchingCurrency) { viewModel.beginCreation(requestId) }
    LaunchedEffect(activeRequest, state.createdAccountId, state.switchingCurrency, state.pendingCurrency) {
        if (activeRequest && state.createdAccountId != null && !state.switchingCurrency && state.pendingCurrency == null) {
            onCreated(requireNotNull(state.createdAccountId))
        }
    }
    if (activeRequest && state.formOpen) {
        AccountSheet(state, viewModel::changeDraft, viewModel::openPicker, viewModel::closePicker,
            viewModel::selectCurrency, viewModel::selectIcon, viewModel::confirmIcon, viewModel::save,
            onDismiss = { if (!state.isSaving) { viewModel.closeForm(); onDismiss() } })
    } else if (activeRequest && state.createdAccountId != null && state.switchingCurrency) {
        AlertDialog(onDismissRequest = {}, text = { Text(stringResource(R.string.finance_loading)) }, confirmButton = {})
    } else if (activeRequest && state.createdAccountId != null && state.pendingCurrency != null) {
        AlertDialog(
            onDismissRequest = { onCreated(requireNotNull(state.createdAccountId)) },
            text = { Text(stringResource(R.string.accounts_currency_error)) },
            confirmButton = { TextButton(onClick = viewModel::retryCurrencySwitch) { Text(stringResource(R.string.finance_retry)) } },
            dismissButton = { TextButton(onClick = { onCreated(requireNotNull(state.createdAccountId)) }) {
                Text(stringResource(R.string.transaction_understood))
            } },
        )
    } else if (!state.currencyReady && !activeRequest) {
        AlertDialog(
            onDismissRequest = onDismiss,
            text = { Text(stringResource(if (state.readError) R.string.accounts_read_error else R.string.finance_loading)) },
            confirmButton = { TextButton(onClick = viewModel::retryRead) { Text(stringResource(R.string.finance_retry)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_close)) } },
        )
    }
}
