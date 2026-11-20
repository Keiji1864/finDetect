package com.ghostteam.findetect.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeRoute(
    userName: String,
    forecast: @Composable () -> Unit,
    onAddTransaction: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenStreak: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        userName = userName,
        forecast = forecast,
        onAddTransaction = onAddTransaction,
        onOpenProfile = onOpenProfile,
        onOpenStreak = onOpenStreak,
        onOpenSettings = onOpenSettings,
        onOpenAnalytics = onOpenAnalytics,
        onRetry = viewModel::retry,
        onConfirmNoSpending = viewModel::confirmNoSpending,
        onOpenAchievements = onOpenAchievements,
    )
}
