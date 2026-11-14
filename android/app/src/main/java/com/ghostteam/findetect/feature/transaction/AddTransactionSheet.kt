package com.ghostteam.findetect.feature.transaction

import android.app.DatePickerDialog
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.drawableResource
import com.ghostteam.findetect.core.designsystem.financeIcon
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.util.currencySymbol
import com.ghostteam.findetect.core.util.formatMoney
import com.ghostteam.findetect.domain.model.Category
import com.ghostteam.findetect.domain.model.TransactionCreationError
import com.ghostteam.findetect.feature.accounts.AccountCreationRoute
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.datetime.LocalDate

@Composable
fun AddTransactionRoute(
    sessionId: String,
    mode: AddTransactionMode,
    onDismiss: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(key = "add-transaction"),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(sessionId) { viewModel.startSession(sessionId, mode) }
    DisposableEffect(viewModel) { onDispose { viewModel.stopObserving() } }
    LaunchedEffect(state.sessionId, state.saved, state.promptCurrency) {
        if (state.sessionId == sessionId && state.saved && state.promptCurrency == null) onDismiss()
    }
    if (state.sessionId != sessionId) return
    val createRequest = state.createAccountRequest
    when {
        createRequest != null -> AccountCreationRoute(createRequest, viewModel::accountCreated, viewModel::cancelAccountCreation)
        state.saved && state.promptCurrency != null -> {
            val currency = requireNotNull(state.promptCurrency)
            AlertDialog(
                onDismissRequest = viewModel::dismissCurrencyPrompt,
                title = { Text(stringResource(R.string.transaction_saved)) },
                text = { Text(stringResource(if (state.switchError) R.string.transaction_switch_error else R.string.transaction_other_currency,
                    currency.name)) },
                confirmButton = { TextButton(onClick = viewModel::switchCurrency, enabled = !state.switchingCurrency) {
                    Text(stringResource(R.string.transaction_switch_currency, currency.name))
                } },
                dismissButton = { TextButton(onClick = viewModel::dismissCurrencyPrompt, enabled = !state.switchingCurrency) {
                    Text(stringResource(R.string.transaction_understood))
                } },
            )
        }
        !state.saved -> AddTransactionSheet(state, viewModel::changeAmount, viewModel::changeComment, viewModel::changeType,
            viewModel::openPicker, viewModel::closePicker, viewModel::chooseCategory, viewModel::chooseAccount,
            viewModel::confirmAccount, viewModel::chooseDate, viewModel::createAccount, viewModel::retryData,
            viewModel::save, onDismiss)
    }
}

/** Stateless transaction form and selectors; all editable data belongs to the ViewModel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    state: AddTransactionUiState,
    onAmount: (String) -> Unit,
    onComment: (String) -> Unit,
    onType: (Boolean) -> Unit,
    onPicker: (TransactionPicker) -> Unit,
    onClosePicker: () -> Unit,
    onCategory: (String) -> Unit,
    onAccount: (String) -> Unit,
    onConfirmAccount: () -> Unit,
    onDate: (LocalDate) -> Unit,
    onCreateAccount: () -> Unit,
    onRetry: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val saving by rememberUpdatedState(state.saving)
    // A fresh modal when returning from a dismissed selector makes the form visible again.
    val page = state.picker?.takeUnless { it == TransactionPicker.DATE }
    key(page) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden || !saving })
        FinBottomSheet(sheetState = sheetState, onDismiss = {
            if (!saving) { if (state.picker != null) onClosePicker() else onDismiss() }
        }) {
            Column(Modifier.fillMaxWidth().weight(1f, fill = false).imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (page) {
                    TransactionPicker.CATEGORY -> {
                        SheetTitle(R.string.ui_category)
                        if (state.categoriesLoading) LoadingText()
                        if (state.categoriesError) RetryNotice(onRetry)
                        if (!state.categoriesLoading && !state.categoriesError && state.categories.isEmpty()) {
                            FinNotice(stringResource(R.string.transaction_no_categories))
                        }
                        state.categories.forEach { category ->
                            FinRow(title = category.displayName(), inSheet = true, onClick = { onCategory(category.id) },
                                leading = { ResourceIcon(category.iconId) },
                                trailing = { FinRadio(category.id == state.categoryId, onClick = null) })
                        }
                        FinButton(stringResource(R.string.ui_close), onClosePicker, style = FinButtonStyle.Secondary)
                    }
                    TransactionPicker.ACCOUNT -> {
                        SheetTitle(R.string.ui_account)
                        if (state.accountsLoading) LoadingText()
                        if (state.accountsError) RetryNotice(onRetry)
                        if (!state.accountsLoading && !state.accountsError && state.accounts.isEmpty()) {
                            FinNotice(stringResource(R.string.transaction_no_accounts))
                        }
                        state.accounts.forEach { balance ->
                            FinRow(title = balance.account.name,
                                subtitle = stringResource(R.string.transaction_account_balance,
                                    balance.account.currency, formatMoney(balance.balance, balance.account.currency)),
                                leading = { ResourceIcon(balance.account.iconId) }, inSheet = true,
                                onClick = { onAccount(balance.account.id) },
                                trailing = { FinRadio(state.pendingAccountId == balance.account.id, onClick = null) })
                        }
                        FinButton(stringResource(R.string.transaction_done), onConfirmAccount,
                            enabled = state.accounts.any { it.account.id == state.pendingAccountId })
                        FinButton(stringResource(R.string.ui_add_account), onCreateAccount, style = FinButtonStyle.Secondary)
                        FinButton(stringResource(R.string.ui_close), onClosePicker, style = FinButtonStyle.Secondary)
                    }
                    else -> {
                        if (state.mode == AddTransactionMode.Switchable) {
                            FinSegmentedSwitch(
                                options = listOf(false to stringResource(R.string.ui_expenses), true to stringResource(R.string.ui_income)),
                                selected = state.isIncome, onSelect = { if (!saving) onType(it) })
                        } else SheetTitle(if (state.isIncome) R.string.ui_add_income_title else R.string.ui_add_expense_title)
                        Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.ui_amount), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BasicTextField(value = state.amount, onValueChange = onAmount, enabled = !state.saving,
                                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = FinTheme.typography.amountInput.copy(color = if (state.isIncome) FinTheme.colors.green else FinTheme.colors.navy),
                                    cursorBrush = SolidColor(FinTheme.colors.navy), modifier = Modifier.weight(1f),
                                    decorationBox = { inner -> Box {
                                        if (state.amount.isEmpty()) Text(stringResource(R.string.ui_zero),
                                            style = FinTheme.typography.amountInput, color = FinTheme.colors.muted)
                                        inner()
                                    } })
                                val currency = state.selectedAccount?.account?.currency ?: state.displayCurrency?.name
                                if (currency != null) Text(currencySymbol(currency),
                                    style = FinTheme.typography.amountInput, color = FinTheme.colors.muted)
                            }
                            if (state.amount.isNotEmpty() && !state.amountValid) ErrorText(R.string.transaction_amount_error)
                        }
                        FinSelectField(stringResource(R.string.ui_category), state.selectedCategory?.displayName().orEmpty(),
                            onClick = { if (!saving) onPicker(TransactionPicker.CATEGORY) }, placeholder = stringResource(R.string.ui_choose_category))
                        FinSelectField(stringResource(R.string.ui_account), state.selectedAccount?.account?.name.orEmpty(),
                            onClick = { if (!saving) onPicker(TransactionPicker.ACCOUNT) }, placeholder = stringResource(R.string.ui_choose_account))
                        if (!state.accountsLoading && !state.accountsError && state.accounts.isEmpty()) {
                            FinNotice(stringResource(R.string.transaction_no_accounts))
                            FinButton(stringResource(R.string.ui_add_account), onCreateAccount,
                                style = FinButtonStyle.Secondary, enabled = !state.saving)
                        }
                        if (!state.categoriesLoading && !state.categoriesError && state.categories.isEmpty()) {
                            FinNotice(stringResource(R.string.transaction_no_categories))
                        }
                        FinSelectField(stringResource(R.string.ui_date), state.date.displayDate(),
                            onClick = { if (!saving) onPicker(TransactionPicker.DATE) })
                        FinTextField(stringResource(R.string.ui_comment), state.comment, onComment, enabled = !state.saving,
                            placeholder = stringResource(R.string.ui_optional),
                            errorText = if (state.comment.length > 200) stringResource(R.string.transaction_comment_error) else null)
                        if (state.accountsLoading || state.categoriesLoading) LoadingText()
                        if (state.accountsError || state.categoriesError || state.settingsError) RetryNotice(onRetry)
                        state.saveError?.let { ErrorText(it.messageResource()) }
                        FinButton(stringResource(R.string.ui_save), onSave, enabled = state.canSave, loading = state.saving)
                        FinButton(stringResource(R.string.ui_close), onDismiss, style = FinButtonStyle.Secondary, enabled = !state.saving)
                    }
                }
            }
        }
    }
    if (state.picker == TransactionPicker.DATE) {
        val context = LocalContext.current
        DisposableEffect(state.date) {
            val dialog = DatePickerDialog(context, { _, year, month, day -> onDate(LocalDate(year, month + 1, day)) },
                state.date.year, state.date.monthNumber - 1, state.date.dayOfMonth)
            dialog.setOnDismissListener { onClosePicker() }
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }
}

@Composable
private fun SheetTitle(@StringRes resource: Int) {
    Text(stringResource(resource), style = FinTheme.typography.sheetTitle, color = FinTheme.colors.navy)
}

@Composable
private fun ResourceIcon(iconId: String) {
    FinIconTile { Icon(painterResource(financeIcon(iconId).drawableResource), null,
        tint = FinTheme.colors.navy, modifier = Modifier.size(24.dp)) }
}

@Composable
private fun LoadingText() { Text(stringResource(R.string.finance_loading), color = FinTheme.colors.muted) }

@Composable
private fun ErrorText(@StringRes resource: Int) {
    Text(stringResource(resource), style = FinTheme.typography.caption, color = FinTheme.colors.red)
}

@Composable
private fun RetryNotice(onRetry: () -> Unit) {
    ErrorText(R.string.transaction_read_error)
    FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary)
}

@Composable
private fun Category.displayName(): String = if (LocalConfiguration.current.locales[0].language == "en") names.en else names.ru

private fun LocalDate.displayDate(): String {
    val date = this
    val calendar = Calendar.getInstance().apply { clear(); set(date.year, date.monthNumber - 1, date.dayOfMonth) }
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(calendar.time)
}

@StringRes
private fun TransactionCreationError.messageResource(): Int = when (this) {
    TransactionCreationError.INVALID_AMOUNT -> R.string.transaction_amount_error
    TransactionCreationError.ACCOUNT_REQUIRED, TransactionCreationError.ACCOUNT_NOT_FOUND,
    TransactionCreationError.ACCOUNT_DELETED -> R.string.transaction_account_error
    TransactionCreationError.CATEGORY_REQUIRED, TransactionCreationError.CATEGORY_NOT_FOUND,
    TransactionCreationError.CATEGORY_ARCHIVED, TransactionCreationError.CATEGORY_TYPE_MISMATCH -> R.string.transaction_category_error
    TransactionCreationError.COMMENT_TOO_LONG -> R.string.transaction_comment_error
    TransactionCreationError.INVALID_DATE -> R.string.transaction_date_error
    else -> R.string.transaction_save_error
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddTransactionPreview() {
    FinDetectTheme {
        AddTransactionSheet(AddTransactionUiState(date = LocalDate(2026, 11, 14)), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
