package com.ghostteam.findetect.feature.accounts

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R
import kotlinx.coroutines.withTimeoutOrNull

/** Shared modal host preserves the edit draft while moving to balance or picker pages. */
@Composable
fun AccountManagementContent(viewModel: AccountEditorViewModel, snackbarModifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deletions by viewModel.deletions.collectAsStateWithLifecycle()
    val undoError by viewModel.undoError.collectAsStateWithLifecycle()
    val currencySwitch by viewModel.currencySwitch.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val deleted = stringResource(R.string.account_deleted)
    val undo = stringResource(R.string.account_undo)
    val undoFailed = stringResource(R.string.account_undo_failed)
    LaunchedEffect(deletions) {
        for (deletion in deletions) {
            val remaining = viewModel.remainingUndoMillis(deletion)
            if (remaining == 0L) continue
            val result = withTimeoutOrNull(remaining) {
                snackbar.showSnackbar(deleted, actionLabel = undo, duration = SnackbarDuration.Indefinite)
            }
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(deletion)
        }
    }
    LaunchedEffect(undoError) {
        if (undoError) {
            snackbar.showSnackbar(undoFailed)
            viewModel.clearUndoError()
        }
    }
    SnackbarHost(snackbar, modifier = snackbarModifier)
    if (state.page != null) {
        AccountEditorSheet(state, viewModel::changeDraft, viewModel::openPage, viewModel::selectIcon,
            viewModel::confirmIcon, viewModel::changeActual, viewModel::save, viewModel::confirmBalance,
            viewModel::requestDelete, viewModel::delete, viewModel::cancelDelete, viewModel::retry, viewModel::dismiss)
    }
    currencySwitch?.let { pending ->
        AlertDialog(
            onDismissRequest = { if (!pending.busy) viewModel.dismissCurrencyNotice() },
            text = { Text(stringResource(if (pending.busy) R.string.finance_loading else R.string.account_edit_currency_error)) },
            confirmButton = {
                if (!pending.busy) TextButton(onClick = viewModel::retryCurrencySwitch) { Text(stringResource(R.string.finance_retry)) }
            },
            dismissButton = {
                if (!pending.busy) TextButton(onClick = viewModel::dismissCurrencyNotice) { Text(stringResource(R.string.ui_close)) }
            },
        )
    }
}
