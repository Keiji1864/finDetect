package com.ghostteam.findetect.feature.transaction

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.model.Account
import com.ghostteam.findetect.domain.model.AccountBalance
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.TransactionManagementError
import com.ghostteam.findetect.feature.accounts.AccountCreationRoute
import java.math.BigDecimal
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate

@Composable
fun EditTransactionRoute(viewModel: EditTransactionViewModel, snackbar: SnackbarHostState) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deletions by viewModel.deletions.collectAsStateWithLifecycle()
    val undoError by viewModel.undoError.collectAsStateWithLifecycle()
    val deleted = stringResource(R.string.transaction_deleted)
    val undo = stringResource(R.string.transaction_undo)
    val undoFailed = stringResource(R.string.transaction_undo_failed)
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
        if (undoError) { snackbar.showSnackbar(undoFailed); viewModel.clearUndoError() }
    }
    if (state.transactionId == null) return
    when {
        state.form.saved -> if (state.form.promptCurrency != null) {
            TransactionCurrencyPrompt(state.form, viewModel::switchCurrency, viewModel::dismissCurrencyPrompt)
        } else LaunchedEffect(Unit) { viewModel.dismiss() }
        state.form.createAccountRequest != null -> AccountCreationRoute(requireNotNull(state.form.createAccountRequest),
            viewModel::accountCreated, viewModel::cancelAccountCreation)
        else -> EditTransactionSheet(state, viewModel::changeAmount, viewModel::changeComment,
            viewModel::openPicker, viewModel::closePicker, viewModel::chooseCategory, viewModel::chooseAccount,
            viewModel::confirmAccount, viewModel::chooseDate, viewModel::createAccount, viewModel::retryData,
            viewModel::reload, viewModel::acceptCurrency, viewModel::save, viewModel::requestDelete,
            viewModel::delete, viewModel::cancelDelete, viewModel::dismiss)
    }
}

/** Existing-operation variant of the shared form, with conflict feedback and confirmed deletion. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    state: EditTransactionUiState,
    onAmount: (String) -> Unit,
    onComment: (String) -> Unit,
    onPicker: (TransactionPicker) -> Unit,
    onClosePicker: () -> Unit,
    onCategory: (String) -> Unit,
    onAccount: (String) -> Unit,
    onConfirmAccount: () -> Unit,
    onDate: (LocalDate) -> Unit,
    onCreateAccount: () -> Unit,
    onRetry: () -> Unit,
    onReload: () -> Unit,
    onAcceptCurrency: () -> Unit,
    onSave: () -> Unit,
    onRequestDelete: () -> Unit,
    onDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (state.original == null || state.unavailable) {
        FinBottomSheet(onDismiss = onDismiss) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(when {
                    state.unavailable -> R.string.transaction_not_found
                    state.loadError -> R.string.transaction_read_error
                    else -> R.string.finance_loading
                }), color = FinTheme.colors.navy)
                if (state.loadError) FinButton(stringResource(R.string.finance_retry), onRetry)
                FinButton(stringResource(R.string.ui_close), onDismiss, style = FinButtonStyle.Secondary)
            }
        }
        return
    }
    AddTransactionSheet(state.form, onAmount, onComment, {}, onPicker, onClosePicker, onCategory, onAccount,
        onConfirmAccount, onDate, onCreateAccount, onRetry, onSave, onDismiss,
        editTitleResource = if (state.form.isIncome) R.string.transaction_edit_income else R.string.transaction_edit_expense,
        saveEnabled = state.canSave, onDelete = onRequestDelete, deleteEnabled = state.canDelete,
        errorContent = {
            if (state.loadError) {
                FinNotice(stringResource(R.string.transaction_read_error))
                FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary)
            }
            if (state.changed) {
                FinNotice(stringResource(R.string.transaction_changed))
                FinButton(stringResource(R.string.transaction_reload), onReload, style = FinButtonStyle.Secondary, enabled = !state.form.saving)
            }
            if (state.currencyChanged) {
                val currency = state.form.selectedAccount?.account?.currency.orEmpty()
                FinNotice(stringResource(R.string.transaction_currency_changed, currency))
                FinButton(stringResource(R.string.transaction_accept_currency, currency), onAcceptCurrency,
                    style = FinButtonStyle.Secondary, enabled = !state.form.saving)
            }
            state.error?.takeUnless { it == TransactionManagementError.CHANGED && state.changed }?.let {
                Text(stringResource(it.messageResource()), color = FinTheme.colors.red, style = FinTheme.typography.caption)
            }
        },
    )
    if (state.confirmDelete) AlertDialog(
        onDismissRequest = onCancelDelete,
        title = { Text(stringResource(R.string.transaction_delete_title)) },
        text = { Text(stringResource(R.string.transaction_delete_hint)) },
        confirmButton = { TextButton(onClick = onDelete, enabled = state.canDelete) {
            Text(stringResource(R.string.transaction_delete_confirm), color = FinTheme.colors.red)
        } },
        dismissButton = { TextButton(onClick = onCancelDelete, enabled = !state.form.saving) {
            Text(stringResource(R.string.transaction_delete_cancel))
        } },
    )
}

@StringRes
private fun TransactionManagementError.messageResource(): Int = when (this) {
    TransactionManagementError.INVALID_AMOUNT -> R.string.transaction_amount_error
    TransactionManagementError.INVALID_DATE -> R.string.transaction_date_error
    TransactionManagementError.COMMENT_TOO_LONG -> R.string.transaction_comment_error
    TransactionManagementError.NOT_FOUND -> R.string.transaction_not_found
    TransactionManagementError.CHANGED -> R.string.transaction_changed
    TransactionManagementError.ACCOUNT_UNAVAILABLE -> R.string.transaction_account_error
    TransactionManagementError.ACCOUNT_CURRENCY_CHANGED -> R.string.transaction_account_error
    TransactionManagementError.CATEGORY_UNAVAILABLE -> R.string.transaction_category_error
    TransactionManagementError.STORAGE_FAILURE -> R.string.transaction_manage_error
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditTransactionPreview() {
    val details = transactionsPreviewSummary().days.first().transactions.first()
    val transaction = details.transaction
    val account = Account(transaction.accountId, stringResource(R.string.ui_account), "card", "RUB", BigDecimal("1000.00"), transaction.modifiedAt)
    FinDetectTheme {
        EditTransactionSheet(EditTransactionUiState(transactionId = transaction.id, original = details,
            loading = false, expectedCurrency = "RUB", form = AddTransactionUiState(
                mode = AddTransactionMode.Expense, amount = "125.00", amountValid = true,
                date = LocalDate(2026, 11, 14), accountId = account.id, categoryId = details.category.id,
                accounts = listOf(AccountBalance(account, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)), categories = listOf(details.category),
                accountsLoading = false, categoriesLoading = false, displayCurrency = AppCurrency.RUB)),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
