package com.ghostteam.findetect.feature.settings

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinBottomSheet
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinLazySubScreen
import com.ghostteam.findetect.core.designsystem.component.FinNotice
import com.ghostteam.findetect.core.designsystem.component.FinRadio
import com.ghostteam.findetect.core.designsystem.component.FinRow
import com.ghostteam.findetect.core.designsystem.component.FinSegmentedSwitch
import com.ghostteam.findetect.core.designsystem.component.FinSwitchPanel
import com.ghostteam.findetect.core.designsystem.component.FinTextField
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.model.Category
import com.ghostteam.findetect.domain.model.CategoryNames
import com.ghostteam.findetect.domain.model.CategorySetting
import com.ghostteam.findetect.domain.model.FinanceIcon
import com.ghostteam.findetect.feature.transaction.localizedName

@Composable
internal fun CategorySettingsScreen(onBack: () -> Unit, viewModel: CategorySettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CategorySettingsContent(
        state = state,
        onBack = onBack,
        onType = viewModel::selectType,
        onName = viewModel::changeName,
        onAdd = viewModel::add,
        onEnabled = viewModel::setEnabled,
        onOpenRemoval = viewModel::openRemoval,
        onCloseRemoval = viewModel::closeRemoval,
        onRemove = viewModel::remove,
        onRetry = viewModel::retry,
    )
}

@Composable
private fun CategorySettingsContent(
    state: CategorySettingsUiState,
    onBack: () -> Unit,
    onType: (Boolean) -> Unit,
    onName: (String) -> Unit,
    onAdd: () -> Unit,
    onEnabled: (String, Boolean) -> Unit,
    onOpenRemoval: () -> Unit,
    onCloseRemoval: () -> Unit,
    onRemove: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val submit: () -> Unit = {
        if (state.canAdd) {
            focus.clearFocus()
            onAdd()
        }
    }
    BackHandler(enabled = state.saving) {}
    FinLazySubScreen(
        title = stringResource(R.string.category_settings_title),
        onBack = { if (!state.saving) onBack() },
        titleMaxLines = 2,
    ) {
        item(key = "editor") {
            Column(verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap)) {
                FinSegmentedSwitch(
                    options = listOf(false to stringResource(R.string.ui_expenses), true to stringResource(R.string.ui_income)),
                    selected = state.isIncome,
                    onSelect = onType,
                )
                FinTextField(
                    label = stringResource(R.string.category_settings_name),
                    value = state.name,
                    onValueChange = onName,
                    enabled = state.canEdit,
                    errorText = if (state.name.trim().length > Category.MAX_CUSTOM_NAME_LENGTH) {
                        stringResource(R.string.category_settings_name_too_long, Category.MAX_CUSTOM_NAME_LENGTH)
                    } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                FinButton(stringResource(R.string.category_settings_add), submit, enabled = state.canAdd)
                FinButton(
                    text = stringResource(R.string.category_settings_remove),
                    onClick = { focus.clearFocus(); onOpenRemoval() },
                    style = FinButtonStyle.Secondary,
                    enabled = state.canEdit && state.customCategories.isNotEmpty(),
                )
            }
        }
        if (state.writeError && !state.removalOpen) {
            item(key = "write_error") { CategoryWriteError() }
        }
        when {
            state.readError -> item(key = "read_error") {
                Column(verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap)) {
                    FinNotice(stringResource(R.string.category_settings_read_error))
                    FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary)
                }
            }
            state.loading -> item(key = "loading") {
                Text(stringResource(R.string.finance_loading), color = FinTheme.colors.muted)
            }
            state.categories.isEmpty() -> item(key = "empty") {
                FinNotice(stringResource(R.string.category_settings_empty))
            }
            else -> items(state.categories, key = { it.category.id }) { setting ->
                FinSwitchPanel(
                    title = setting.category.localizedName(),
                    checked = setting.enabled,
                    onCheckedChange = { onEnabled(setting.category.id, it) },
                    enabled = state.canEdit,
                    toggleOnRowClick = true,
                )
            }
        }
    }
    if (state.removalOpen) {
        CategoryRemovalSheet(state.customCategories, state.saving, state.writeError, onCloseRemoval, onRemove)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryRemovalSheet(
    categories: List<Category>,
    saving: Boolean,
    writeError: Boolean,
    onClose: () -> Unit,
    onRemove: (String) -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val busy by rememberUpdatedState(saving)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !busy },
    )
    FinBottomSheet(onDismiss = onClose, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap),
        ) {
            Text(stringResource(R.string.category_settings_remove), style = FinTheme.typography.sheetTitle, color = FinTheme.colors.navy)
            Text(stringResource(R.string.category_settings_remove_hint), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
            categories.forEach { category ->
                FinRow(
                    title = category.localizedName(),
                    inSheet = true,
                    onClick = if (saving) null else ({ selectedId = category.id }),
                    trailing = { FinRadio(selectedId == category.id, onClick = null) },
                )
            }
            if (writeError) CategoryWriteError()
            FinButton(
                text = stringResource(R.string.category_settings_remove_confirm),
                onClick = { selectedId?.let(onRemove) },
                style = FinButtonStyle.Danger,
                enabled = !saving && categories.any { it.id == selectedId },
            )
            FinButton(stringResource(R.string.ui_close), onClose, style = FinButtonStyle.Secondary, enabled = !saving)
        }
    }
}

@Composable
private fun CategoryWriteError() {
    Text(stringResource(R.string.category_settings_write_error), style = FinTheme.typography.caption, color = FinTheme.colors.red)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategorySettingsPreview() {
    val custom = Category("preview-bike", Category.customKey("preview-bike"), CategoryNames("Велосипед", "Bicycle"),
        FinanceIcon.WALLET.key, isIncome = false)
    val grocery = Category("preview-groceries", "groceries", CategoryNames("Продукты", "Groceries"),
        FinanceIcon.BASKET.key, isIncome = false)
    val state = CategorySettingsUiState(
        categories = listOf(CategorySetting(grocery, enabled = true), CategorySetting(custom, enabled = false)),
        loading = false,
    )
    FinDetectTheme {
        CategorySettingsContent(state, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {})
    }
}
