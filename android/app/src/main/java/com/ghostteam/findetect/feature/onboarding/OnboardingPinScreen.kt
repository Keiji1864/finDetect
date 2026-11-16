package com.ghostteam.findetect.feature.onboarding

import androidx.compose.runtime.Composable
import com.ghostteam.findetect.feature.security.PinSetupScreen

@Composable
fun OnboardingPinScreen(onBack: () -> Unit = {}, onDone: () -> Unit = {}) =
    PinSetupScreen(onBack = onBack, onDone = onDone, autoCreate = true)
