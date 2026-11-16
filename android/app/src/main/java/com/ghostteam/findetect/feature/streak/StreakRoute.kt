package com.ghostteam.findetect.feature.streak

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun StreakRoute(onBack: () -> Unit, viewModel: StreakViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StreakScreen(state, onBack, viewModel::confirmNoSpending, viewModel::retry)
}
