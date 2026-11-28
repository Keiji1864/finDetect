package com.ghostteam.findetect.feature.accounts

import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import com.ghostteam.findetect.domain.model.FinanceIcon
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.drawableResource
import com.ghostteam.findetect.core.designsystem.financeIcon
import com.ghostteam.findetect.core.designsystem.labelResource
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditorSheet(
    state: AccountEditorState,
    onDraft: (AccountEditDraft) -> Unit,
    onActualBalance: (String) -> Unit,
    onRequestDelete: () -> Unit,
    onDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!state.open) return
    val dismiss by rememberUpdatedState(onDismiss)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { value ->
            if (value == SheetValue.Hidden) dismiss()
            value != SheetValue.Hidden
        })
    FinBottomSheet(sheetState = sheetState, onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().weight(1f, fill = false).imePadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap)) {
            val balance = state.balance
            when {
                state.loading && balance == null -> {
                    Text(stringResource(R.string.finance_loading), color = FinTheme.colors.muted)
                }
                balance == null || balance.account.isDeleted -> {
                    Text(stringResource(if (state.error == AccountEditorError.UNAVAILABLE || !state.readError)
                        R.string.account_edit_unavailable else R.string.account_edit_load_error),
                        style = FinTheme.typography.label, color = FinTheme.colors.red)
                    FinButton(stringResource(R.string.finance_retry), onRetry, enabled = !state.busy)
                }
                else -> {
                    val draft = state.draft
                    var icons by remember { mutableStateOf(false) }
                    Text(stringResource(R.string.ui_acc_edit_title), style = FinTheme.typography.sheetTitle, color = FinTheme.colors.navy)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FinTextField(stringResource(R.string.ui_acc_name_label), draft.name,
                            { onDraft(draft.copy(name = it)) }, Modifier.weight(1f),
                            errorText = if (draft.name.trim().length !in 1..100) stringResource(R.string.accounts_name_error) else null)
                        Box {
                            IconButton(onClick = { icons = true }) {
                                Icon(painterResource(financeIcon(draft.iconId).drawableResource), stringResource(R.string.ui_acc_icon_label))
                            }
                            if (icons) {
                                AccountIconDialog(draft.iconId, onDismiss = { icons = false }) { icon ->
                                    icons = false
                                    onDraft(draft.copy(iconId = icon.key))
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FinTextField(stringResource(R.string.account_editor_balance), state.actualBalanceText,
                            onActualBalance, Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            errorText = if (!state.actualBalanceValid) stringResource(R.string.accounts_balance_error) else null)
                        FinTextField(stringResource(R.string.ui_acc_currency_label), balance.account.currency,
                            onValueChange = {}, modifier = Modifier.width(104.dp), enabled = false)
                    }
                    FinSwitchPanel(
                        title = stringResource(R.string.ui_acc_forecast),
                        checked = draft.includeInForecast,
                        onCheckedChange = { onDraft(draft.copy(includeInForecast = it)) },
                        enabled = !state.busy,
                        toggleOnRowClick = true,
                    )
                    EditorError(state, onRetry)
                    FinButton(stringResource(R.string.ui_acc_delete), onRequestDelete, style = FinButtonStyle.Danger,
                        enabled = !state.busy && !state.loading && !state.readError)
                }
            }
        }
    }
    if (state.confirmDelete) {
        AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            containerColor = FinTheme.colors.white,
            titleContentColor = FinTheme.colors.navy,
            textContentColor = FinTheme.colors.muted,
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
    if (error != AccountEditorError.DELETE && error != AccountEditorError.UNAVAILABLE) {
        FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary, enabled = !state.busy)
    }
}

@Composable
private fun AccountIconDialog(selected: String, onDismiss: () -> Unit, onSelect: (FinanceIcon) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = FinTheme.shapes.r12, color = FinTheme.colors.white) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.accounts_icon_title), style = FinTheme.typography.sheetTitle,
                    color = FinTheme.colors.navy)
                FinanceIcon.entries.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { icon ->
                            val active = icon.key == selected
                            Box(Modifier.weight(1f).height(52.dp).clip(FinTheme.shapes.r12)
                                .background(if (active) FinTheme.colors.cream else FinTheme.colors.paper)
                                .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(icon) }),
                                contentAlignment = Alignment.Center) {
                                Icon(painterResource(icon.drawableResource), stringResource(icon.labelResource),
                                    modifier = Modifier.size(24.dp), tint = FinTheme.colors.navy)
                            }
                        }
                    }
                }
            }
        }
    }
}
