package com.ghostteam.findetect.feature.accounts

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.*
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.FinanceIcon

/** Shared currency choices for account creation and editing; selection stays in the caller's draft. */
@Composable
internal fun AccountCurrencyPicker(selected: String, onSelect: (AppCurrency) -> Unit, onClose: () -> Unit, enabled: Boolean = true) {
    Text(stringResource(R.string.ui_acc_currency_label), style = FinTheme.typography.sheetTitle, color = FinTheme.colors.navy)
    AppCurrency.entries.forEach { currency ->
        FinRow(title = stringResource(currency.codeResource), subtitle = stringResource(currency.nameResource),
            inSheet = true, onClick = if (enabled) ({ onSelect(currency) }) else null,
            trailing = { FinRadio(selected == currency.name, onClick = null) })
    }
    FinButton(stringResource(R.string.ui_close), onClose, style = FinButtonStyle.Secondary, enabled = enabled)
}

/** Icon selection is confirmed explicitly so closing the picker preserves the original draft icon. */
@Composable
internal fun AccountIconPicker(selected: String, onSelect: (String) -> Unit, onConfirm: () -> Unit, onClose: () -> Unit, enabled: Boolean = true) {
    Text(stringResource(R.string.accounts_icon_title), style = FinTheme.typography.sheetTitle, color = FinTheme.colors.navy)
    FinanceIcon.entries.chunked(4).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { icon ->
                Box(Modifier.weight(1f).height(64.dp).clip(FinTheme.shapes.r12)
                    .background(if (selected == icon.key) FinTheme.colors.cream else FinTheme.colors.paper)
                    .selectable(selected == icon.key, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(icon.key) }),
                    contentAlignment = Alignment.Center) {
                    Icon(painterResource(icon.drawableResource), stringResource(icon.labelResource),
                        tint = FinTheme.colors.navy, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
    FinButton(stringResource(R.string.accounts_icon_choose), onConfirm, enabled = enabled)
    FinButton(stringResource(R.string.ui_close), onClose, style = FinButtonStyle.Secondary, enabled = enabled)
}

@Preview(showBackground = true, widthDp = 364)
@Preview(showBackground = true, widthDp = 364, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountCurrencyPickerPreview() {
    FinDetectTheme { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { AccountCurrencyPicker("RUB", {}, {}) } }
}

@Preview(showBackground = true, widthDp = 364)
@Preview(showBackground = true, widthDp = 364, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountIconPickerPreview() {
    FinDetectTheme { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { AccountIconPicker("wallet", {}, {}, {}) } }
}
