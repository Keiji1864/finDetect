package com.ghostteam.findetect.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileRoute(onBack: () -> Unit, onLogout: () -> Unit, logoutWorking: Boolean, gameElementsEnabled: Boolean,
    viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileScreen(state = state, onBack = onBack, onLogout = onLogout, working = logoutWorking,
        gameElementsEnabled = gameElementsEnabled, onNameChange = viewModel::editName, onSaveName = viewModel::save,
        onRefresh = viewModel::refresh, onDeleteAccount = viewModel::askDelete,
        onConfirmDelete = viewModel::confirmDelete, onDismissDelete = viewModel::dismissDelete, onRetryXp = viewModel::retryXp)
}
