package com.ghostteam.findetect.feature.accounts

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.*
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.model.AccountCreationError
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.CreateAccountInput
import com.ghostteam.findetect.domain.model.FinanceIcon

/** Controlled creation sheet. Picker pages share the same modal and preserve the form draft. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AccountSheet(
    state: AccountsUiState,
    onChange: (CreateAccountInput) -> Unit,
    onOpenPicker: (AccountPicker) -> Unit,
    onClosePicker: () -> Unit,
    onCurrency: (AppCurrency) -> Unit,
    onIcon: (String) -> Unit,
    onConfirmIcon: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val saving by rememberUpdatedState(state.isSaving)
    key(state.picker) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !saving })
    FinBottomSheet(sheetState = sheetState, onDismiss = { if (state.picker != null) onClosePicker() else onDismiss() }) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).imePadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap),
        ) {
            when (state.picker) {
                AccountPicker.CURRENCY -> {
                    Text(stringResource(R.string.ui_acc_currency_label), style = FinTheme.typography.sheetTitle, color = FinTheme.colors.navy)
                    AppCurrency.entries.forEach { currency ->
                        FinRow(title = stringResource(currency.codeResource), subtitle = stringResource(currency.nameResource),
                            inSheet = true, onClick = { onCurrency(currency) },
                            trailing = { FinRadio(state.draft.currency == currency.name, onClick = null) })
                    }
                    FinButton(stringResource(R.string.ui_close), onClosePicker, style = FinButtonStyle.Secondary)
                }
                AccountPicker.ICON -> {
                    Text(stringResource(R.string.accounts_icon_title), style = FinTheme.typography.sheetTitle, color = FinTheme.colors.navy)
                    FinanceIcon.entries.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { icon ->
                                Box(Modifier.weight(1f).height(64.dp).clip(FinTheme.shapes.r12)
                                    .background(if (state.pendingIcon == icon.key) FinTheme.colors.cream else FinTheme.colors.paper)
                                    .selectable(state.pendingIcon == icon.key, role = Role.RadioButton, onClick = { onIcon(icon.key) }),
                                    contentAlignment = Alignment.Center) {
                                    Icon(painterResource(icon.drawableResource), stringResource(icon.labelResource),
                                        tint = FinTheme.colors.navy, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                    FinButton(stringResource(R.string.accounts_icon_choose), onConfirmIcon)
                    FinButton(stringResource(R.string.ui_close), onClosePicker, style = FinButtonStyle.Secondary)
                }
                null -> {
                    val draft = state.draft
                    Text(stringResource(R.string.ui_acc_new_title), style = FinTheme.typography.sheetTitle, color = FinTheme.colors.navy)
                    FinTextField(label = stringResource(R.string.ui_acc_name_label), value = draft.name,
                        onValueChange = { onChange(draft.copy(name = it)) }, enabled = !state.isSaving,
                        errorText = if (draft.name.isNotEmpty() && state.validationError == AccountCreationError.INVALID_NAME)
                            stringResource(R.string.accounts_name_error) else null)
                    FinSelectField(stringResource(R.string.ui_acc_currency_label), draft.currency,
                        onClick = { if (!state.isSaving) onOpenPicker(AccountPicker.CURRENCY) })
                    FinTextField(label = stringResource(R.string.ui_acc_balance_label), value = draft.initialBalanceText,
                        onValueChange = { onChange(draft.copy(initialBalanceText = it)) }, enabled = !state.isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        errorText = if (state.validationError == AccountCreationError.INVALID_BALANCE)
                            stringResource(R.string.accounts_balance_error) else null)
                    FinRow(title = stringResource(R.string.ui_acc_icon_label),
                        subtitle = stringResource(financeIcon(draft.iconId).labelResource),
                        leading = { FinIconTile { Icon(painterResource(financeIcon(draft.iconId).drawableResource), null,
                            tint = FinTheme.colors.navy, modifier = Modifier.size(24.dp)) } },
                        inSheet = true, onClick = if (state.isSaving) null else { { onOpenPicker(AccountPicker.ICON) } })
                    FinSwitchPanel(stringResource(R.string.ui_acc_forecast), draft.includeInForecast,
                        onCheckedChange = { if (!state.isSaving) onChange(draft.copy(includeInForecast = it)) })
                    if (state.saveError != null) {
                        Text(stringResource(R.string.accounts_save_error), color = FinTheme.colors.red, style = FinTheme.typography.caption)
                    }
                    FinButton(stringResource(R.string.ui_save), onSave,
                        enabled = state.validationError == null && !state.isSaving, loading = state.isSaving)
                    FinButton(stringResource(R.string.ui_close), onDismiss, style = FinButtonStyle.Secondary, enabled = !state.isSaving)
                }
            }
        }
    }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountSheetPreview() {
    FinDetectTheme { AccountSheet(AccountsUiState(formOpen = true), {}, {}, {}, {}, {}, {}, {}, {}) }
}
