package com.ghostteam.findetect.feature.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinNotice
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.security.AppLockManager
import com.ghostteam.findetect.core.security.AppLockState

/** Removes app windows while locked and restores navigation/form state after successful verification. */
@Composable
fun AppLockHost(lock: AppLockManager, onExit: () -> Unit, content: @Composable () -> Unit) {
    val state by lock.state.collectAsStateWithLifecycle()
    val holder = rememberSaveableStateHolder()
    when (state) {
        AppLockState.UNLOCKED -> holder.SaveableStateProvider("app") { content() }
        AppLockState.LOCKED -> PinUnlockScreen(onExit)
        AppLockState.LOADING -> {
            BackHandler(onBack = onExit)
            Box(Modifier.fillMaxSize().background(FinTheme.colors.paper), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FinTheme.colors.navy)
            }
        }
        AppLockState.ERROR -> {
            BackHandler(onBack = onExit)
            FinSubScreen(stringResource(R.string.pin_unlock_title), onExit) {
                FinNotice(stringResource(R.string.settings_pin_error), text = stringResource(R.string.pin_storage_error))
                FinButton(stringResource(R.string.finance_retry), lock::retry)
            }
        }
    }
}
