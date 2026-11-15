package com.ghostteam.findetect.feature.security

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinNotice
import com.ghostteam.findetect.core.designsystem.component.FinPinInput
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.repository.PinStatus

@Composable
fun PinSetupScreen(onBack: () -> Unit, onDone: () -> Unit = onBack, autoCreate: Boolean = false,
    viewModel: PinEntryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ClearPinOnLeave(viewModel)
    LaunchedEffect(state.status, state.stage, autoCreate) {
        if (autoCreate && state.status == PinStatus.DISABLED && state.stage == PinStage.MENU) viewModel.begin(PinStage.CREATE)
    }
    LaunchedEffect(state.completed) { if (state.completed) onDone() }
    BackHandler { viewModel.cancel(); onBack() }
    FinSubScreen(title = stringResource(if (autoCreate) R.string.ui_onboarding_pin_title else R.string.ui_settings_security), onBack = { viewModel.cancel(); onBack() }) {
        when {
            state.status == PinStatus.LOADING -> Text(stringResource(R.string.finance_loading), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
            state.status == PinStatus.ERROR -> {
                FinNotice(stringResource(R.string.settings_pin_error), text = stringResource(R.string.pin_storage_error))
                FinButton(stringResource(R.string.finance_retry), viewModel::retryStorage)
            }
            state.stage != PinStage.MENU -> PinEntryForm(state, viewModel::digit, viewModel::delete)
            else -> {
                FinNotice(stringResource(if (state.status == PinStatus.ENABLED) R.string.settings_pin_enabled else R.string.settings_pin_disabled), text = stringResource(R.string.pin_security_about))
                if (autoCreate && state.status == PinStatus.ENABLED) {
                    FinNotice(stringResource(R.string.pin_already_enabled))
                    FinButton(stringResource(R.string.ui_onboarding_continue), onDone)
                } else if (state.status == PinStatus.DISABLED) {
                    FinButton(stringResource(R.string.pin_enable), { viewModel.begin(PinStage.CREATE) })
                } else {
                    FinButton(stringResource(R.string.pin_change), { viewModel.begin(PinStage.CURRENT_FOR_CHANGE) })
                    FinButton(stringResource(R.string.pin_disable), { viewModel.begin(PinStage.CURRENT_FOR_DISABLE) }, style = FinButtonStyle.Secondary)
                }
            }
        }
    }
}

@Composable
fun PinUnlockScreen(onExit: () -> Unit, onReauthenticate: () -> Unit = {},
    viewModel: PinEntryViewModel = hiltViewModel(key = "app-lock-pin")) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ClearPinOnLeave(viewModel)
    LaunchedEffect(state.stage, state.status) {
        if (state.stage == PinStage.MENU && state.status == PinStatus.ENABLED) viewModel.begin(PinStage.UNLOCK)
    }
    BackHandler(onBack = onExit)
    Column(Modifier.fillMaxSize().background(FinTheme.colors.paper).statusBarsPadding().navigationBarsPadding()
        .verticalScroll(rememberScrollState()).padding(FinDimens.screenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.pin_unlock_title), style = FinTheme.typography.title, color = FinTheme.colors.navy)
        PinEntryForm(state, viewModel::digit, viewModel::delete)
        FinButton(stringResource(R.string.session_sign_in_again), onReauthenticate, style = FinButtonStyle.Secondary)
    }
}

/** Common form for setup, confirmation, current PIN checks, and unlocking. */
@Composable
internal fun PinEntryForm(state: PinUiState, onDigit: (Char) -> Unit, onDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinPinInput(
            title = stringResource(when (state.stage) {
                PinStage.CREATE, PinStage.MENU -> R.string.ui_pin_create
                PinStage.REPEAT -> R.string.ui_pin_repeat
                PinStage.CURRENT_FOR_CHANGE, PinStage.CURRENT_FOR_DISABLE -> R.string.pin_enter_current
                PinStage.UNLOCK -> R.string.pin_enter_to_unlock
            }), filled = state.filled, onDigit = onDigit, onDelete = onDelete, enabled = !state.working && !state.completed && state.stage != PinStage.MENU,
        )
        if (state.working) Text(stringResource(R.string.finance_loading), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
        if (state.error != PinError.NONE) Text(
            text = when (state.error) {
                PinError.INCORRECT -> stringResource(R.string.pin_incorrect)
                PinError.MISMATCH -> stringResource(R.string.pin_mismatch)
                PinError.THROTTLED -> stringResource(R.string.pin_throttled, state.retrySeconds)
                PinError.STORAGE -> stringResource(R.string.pin_storage_error)
                else -> stringResource(R.string.pin_save_error)
            }, style = FinTheme.typography.caption, color = FinTheme.colors.red,
        )
    }
}

@Composable
private fun ClearPinOnLeave(viewModel: PinEntryViewModel) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, viewModel) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) viewModel.cancel() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); viewModel.cancel() }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 700)
@Preview(showBackground = true, widthDp = 360, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PinEntryFormPreview() {
    FinDetectTheme {
        FinSubScreen(stringResource(R.string.ui_settings_security), {}) {
            PinEntryForm(PinUiState(status = PinStatus.ENABLED, stage = PinStage.UNLOCK, filled = 2), {}, {})
        }
    }
}
