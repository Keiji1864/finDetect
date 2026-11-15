package com.ghostteam.findetect.feature.accounts

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.drawableResource
import com.ghostteam.findetect.core.designsystem.financeIcon
import com.ghostteam.findetect.core.designsystem.labelResource
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.util.formatMoney
import com.ghostteam.findetect.domain.model.Account
import com.ghostteam.findetect.domain.model.AccountBalance
import java.math.BigDecimal
import kotlinx.datetime.Instant

/** Controlled edit and balance pages retain their drafts when opening pickers or returning to edit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditorSheet(
    state: AccountEditorState,
    onDraft: (AccountEditDraft) -> Unit,
    onPage: (AccountEditorPage) -> Unit,
    onIcon: (String) -> Unit,
    onConfirmIcon: () -> Unit,
    onActualBalance: (String) -> Unit,
    onSave: () -> Unit,
    onConfirmBalance: () -> Unit,
    onRequestDelete: () -> Unit,
    onDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val page = state.page ?: return
    val busy by rememberUpdatedState(state.busy)
    val locale = LocalConfiguration.current.locales[0]
    key(page) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden || !busy })
        FinBottomSheet(sheetState = sheetState, onDismiss = { if (!busy) onDismiss() }) {
            Column(Modifier.fillMaxWidth().weight(1f, fill = false).imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap)) {
                val balance = state.balance
                when {
                    state.loading && balance == null -> {
                        Text(stringResource(R.string.finance_loading), color = FinTheme.colors.muted)
                        FinButton(stringResource(R.string.ui_close), onDismiss, style = FinButtonStyle.Secondary)
                    }
                    balance == null || balance.account.isDeleted -> {
                        Text(stringResource(if (state.error == AccountEditorError.UNAVAILABLE || !state.readError)
                            R.string.account_edit_unavailable else R.string.account_edit_load_error),
                            style = FinTheme.typography.label, color = FinTheme.colors.red)
                        FinButton(stringResource(R.string.finance_retry), onRetry, enabled = !state.busy)
                        FinButton(stringResource(R.string.ui_close), onDismiss, style = FinButtonStyle.Secondary, enabled = !state.busy)
                    }
                    page == AccountEditorPage.CURRENCY -> {
                        if (state.currencyLocked) {
                            Text(stringResource(R.string.account_currency_locked_hint), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                            FinButton(stringResource(R.string.ui_close), onDismiss, style = FinButtonStyle.Secondary, enabled = !state.busy)
                        } else {
                            AccountCurrencyPicker(state.draft.currency,
                                onSelect = { onDraft(state.draft.copy(currency = it.name)); onPage(AccountEditorPage.EDIT) },
                                onClose = onDismiss, enabled = !state.busy)
                        }
                    }
                    page == AccountEditorPage.ICON -> AccountIconPicker(state.pendingIcon, onIcon, onConfirmIcon, onDismiss, enabled = !state.busy)
                    page == AccountEditorPage.BALANCE -> {
                        Text(stringResource(R.string.account_balance_title), style = FinTheme.typography.sheetTitle, color = FinTheme.colors.navy)
                        Text(balance.account.name, style = FinTheme.typography.label, color = FinTheme.colors.navy)
                        FinTextField(stringResource(R.string.account_current_balance), formatMoney(balance.balance, balance.account.currency, locale),
                            onValueChange = {}, enabled = false)
                        FinTextField(stringResource(R.string.account_actual_balance), state.actualBalanceText,
                            onValueChange = onActualBalance, enabled = !state.busy,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            errorText = if (state.actualBalanceText.isNotEmpty() && !state.actualBalanceValid)
                                stringResource(R.string.accounts_balance_error) else null)
                        Text(stringResource(R.string.account_balance_hint), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                        EditorError(state, onRetry)
                        FinButton(stringResource(R.string.account_balance_confirm), onConfirmBalance,
                            enabled = state.canConfirmBalance, loading = state.busy)
                        FinButton(stringResource(R.string.ui_close), onDismiss, style = FinButtonStyle.Secondary, enabled = !state.busy)
                    }
                    else -> {
                        val draft = state.draft
                        Text(stringResource(R.string.ui_acc_edit_title), style = FinTheme.typography.sheetTitle, color = FinTheme.colors.navy)
                        FinTextField(stringResource(R.string.ui_acc_name_label), draft.name,
                            onValueChange = { onDraft(draft.copy(name = it)) }, enabled = !state.busy,
                            errorText = if (draft.name.isNotEmpty() && draft.name.trim().length !in 1..100)
                                stringResource(R.string.accounts_name_error) else null)
                        if (state.currencyLocked) {
                            FinTextField(stringResource(R.string.ui_acc_currency_label), draft.currency, onValueChange = {}, enabled = false)
                            Text(stringResource(R.string.account_currency_locked_hint), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                        } else {
                            FinSelectField(stringResource(R.string.ui_acc_currency_label), draft.currency,
                                onClick = { if (!state.busy) onPage(AccountEditorPage.CURRENCY) })
                        }
                        FinTextField(stringResource(R.string.ui_acc_balance_label), formatMoney(balance.account.initialBalance, balance.account.currency, locale),
                            onValueChange = {}, enabled = false)
                        Text(stringResource(R.string.account_initial_balance_hint), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                        FinRow(title = stringResource(R.string.account_current_balance), inSheet = true,
                            trailing = { Text(formatMoney(balance.balance, balance.account.currency, locale),
                                style = FinTheme.typography.label, color = FinTheme.colors.navy) },
                            onClick = if (state.busy) null else ({ onPage(AccountEditorPage.BALANCE) }))
                        FinRow(title = stringResource(R.string.ui_acc_icon_label), subtitle = stringResource(financeIcon(draft.iconId).labelResource),
                            leading = { FinIconTile { Icon(painterResource(financeIcon(draft.iconId).drawableResource), null,
                                tint = FinTheme.colors.navy, modifier = Modifier.size(24.dp)) } },
                            inSheet = true, onClick = if (state.busy) null else ({ onPage(AccountEditorPage.ICON) }))
                        FinSwitchPanel(stringResource(R.string.ui_acc_forecast), draft.includeInForecast,
                            onCheckedChange = { if (!state.busy) onDraft(draft.copy(includeInForecast = it)) })
                        EditorError(state, onRetry)
                        FinButton(stringResource(R.string.ui_save), onSave, enabled = state.canSave && !state.busy, loading = state.busy)
                        FinButton(stringResource(R.string.ui_acc_delete), onRequestDelete, style = FinButtonStyle.Danger,
                            enabled = !state.busy && !state.loading && !state.readError)
                        FinButton(stringResource(R.string.ui_close), onDismiss, style = FinButtonStyle.Secondary, enabled = !state.busy)
                    }
                }
            }
        }
    }
    if (state.confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!state.busy) onCancelDelete() },
            title = { Text(stringResource(R.string.account_delete_title, state.balance?.account?.name.orEmpty())) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.account_delete_history_hint))
                    if (state.error == AccountEditorError.DELETE) {
                        Text(stringResource(R.string.account_edit_delete_error), color = FinTheme.colors.red)
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDelete, enabled = !state.busy) {
                Text(stringResource(R.string.account_delete_confirm), color = FinTheme.colors.red)
            } },
            dismissButton = { TextButton(onClick = onCancelDelete, enabled = !state.busy) { Text(stringResource(R.string.account_delete_cancel)) } },
        )
    }
}

@Composable
private fun EditorError(state: AccountEditorState, onRetry: () -> Unit) {
    val error = state.error ?: if (state.readError) AccountEditorError.LOAD else return
    val message = when (error) {
        AccountEditorError.LOAD -> R.string.account_edit_load_error
        AccountEditorError.SAVE -> R.string.account_edit_save_error
        AccountEditorError.CONFLICT -> R.string.account_edit_conflict
        AccountEditorError.CURRENCY_LOCKED -> R.string.account_edit_currency_locked
        AccountEditorError.UNAVAILABLE -> R.string.account_edit_unavailable
        AccountEditorError.DELETE -> R.string.account_edit_delete_error
        AccountEditorError.BALANCE -> R.string.account_edit_balance_error
    }
    Text(stringResource(message), style = FinTheme.typography.caption, color = FinTheme.colors.red)
    if (error == AccountEditorError.LOAD || error == AccountEditorError.CONFLICT || error == AccountEditorError.CURRENCY_LOCKED) {
        FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary, enabled = !state.busy)
    }
}

private fun editorPreviewState(page: AccountEditorPage): AccountEditorState {
    val account = Account("preview-account", "Наличные", "wallet", "RUB", BigDecimal("1000.00"), Instant.parse("2026-11-14T10:00:00Z"))
    return AccountEditorState(page = page, balance = AccountBalance(account, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
        draft = AccountEditDraft(account.name, account.currency, account.iconId), currencyLocked = false,
        actualBalanceText = "950", actualBalanceValid = true, canConfirmBalance = true, canSave = true)
}

@Composable
private fun EditorPreview(page: AccountEditorPage) {
    FinDetectTheme {
        AccountEditorSheet(editorPreviewState(page), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountEditPreview() { EditorPreview(AccountEditorPage.EDIT) }

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountBalancePreview() { EditorPreview(AccountEditorPage.BALANCE) }
