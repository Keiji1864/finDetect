package com.ghostteam.findetect.feature.settings

import androidx.compose.runtime.Composable
import com.ghostteam.findetect.feature.security.PinSetupScreen

@Composable
fun SecuritySettingsScreen(onBack: () -> Unit = {}) = PinSetupScreen(onBack = onBack)
