package com.ghostteam.findetect.feature.settings

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinNotice
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.core.designsystem.component.FinRadio
import com.ghostteam.findetect.core.designsystem.component.FinRow
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.feature.prototype.UiIconTile

private data class CurrencyOption(@StringRes val code: Int, @StringRes val name: Int, @DrawableRes val icon: Int)

private val currencies = listOf(
    CurrencyOption(R.string.ui_currency_rub, R.string.ui_currency_rub_name, R.drawable.ic_currency_rub),
    CurrencyOption(R.string.ui_currency_usd, R.string.ui_currency_usd_name, R.drawable.ic_currency_usd),
    CurrencyOption(R.string.ui_currency_eur, R.string.ui_currency_eur_name, R.drawable.ic_currency_eur),
    CurrencyOption(R.string.ui_currency_cny, R.string.ui_currency_cny_name, R.drawable.ic_currency_cny),
)

@Composable
fun CurrencySettingsScreen(onBack: () -> Unit = {}, viewModel: CurrencySettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CurrencySettingsContent(state, viewModel::select, viewModel::retryRead, onBack)
}

@Composable
private fun CurrencySettingsContent(state: CurrencySettingsUiState, onSelect: (AppCurrency) -> Unit, onRetry: () -> Unit, onBack: () -> Unit) {
    FinSubScreen(title = stringResource(R.string.ui_currency_title), onBack = onBack) {
        if (state.error) {
            FinNotice(title = stringResource(R.string.currency_settings_error))
            FinButton(stringResource(R.string.finance_retry), onRetry)
        }
        currencies.forEachIndexed { index, currency ->
            FinRow(
                title = stringResource(currency.code),
                subtitle = stringResource(currency.name),
                leading = { UiIconTile(currency.icon) },
                onClick = if (state.ready && !state.saving) ({ onSelect(AppCurrency.entries[index]) }) else null,
                trailing = { FinRadio(selected = state.ready && AppCurrency.entries[index] == state.selected, onClick = null) },
            )
        }
        FinNotice(
            title = stringResource(R.string.ui_no_conversion),
            text = stringResource(R.string.ui_no_conversion_hint),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CurrencySettingsScreenPreview() {
    FinDetectTheme { CurrencySettingsContent(CurrencySettingsUiState(ready = true), {}, {}, {}) }
}
