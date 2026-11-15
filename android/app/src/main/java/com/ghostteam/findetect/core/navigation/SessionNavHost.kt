package com.ghostteam.findetect.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.security.AppLockManager
import com.ghostteam.findetect.domain.model.*
import com.ghostteam.findetect.feature.login.LoginScreen
import com.ghostteam.findetect.feature.register.RegisterScreen
import com.ghostteam.findetect.feature.security.AppLockHost
import com.ghostteam.findetect.feature.session.*

/** The outer back stack owns every user ViewModel and is removed completely on session changes. */
@Composable
fun SessionNavHost(lock: AppLockManager, homeRequest: Int, notificationOwner: SessionIdentity?, onExit: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()) {
    val session by viewModel.state.collectAsStateWithLifecycle()
    val root = rememberNavController()
    val destination = when (val current = session) {
        is SessionState.Active -> "user/${current.identity.sessionId}"
        SessionState.Restoring -> "loading"
        else -> "auth"
    }
    LaunchedEffect(destination) {
        root.navigate(destination) { popUpTo(root.graph.id) { inclusive = true }; launchSingleTop = true }
    }
    LaunchedEffect((session as? SessionState.Active)?.identity) {
        if (session is SessionState.Active) viewModel.refreshUser()
    }
    NavHost(root, "loading") {
        composable("loading") { SessionLoading() }
        composable("auth") {
            if (destination == "auth") {
                AuthNavHost(session, viewModel::retry)
            } else SessionLoading()
        }
        composable("user/{sessionId}") { entry ->
            val active = (session as? SessionState.Active)?.takeIf {
                it.identity == (viewModel.state.value as? SessionState.Active)?.identity
            }
            if (active != null && entry.arguments?.getString("sessionId") == active.identity.sessionId) {
                val userModel: UserSessionViewModel = hiltViewModel(entry)
                AppLockHost(lock, active.identity.sessionId, onExit, userModel::reauthenticate) {
                    AppNavHost(active, userModel, homeRequest = if (notificationOwner == active.identity) homeRequest else 0,
                        onExit = onExit)
                }
            } else SessionLoading()
        }
    }
}

@Composable
private fun AuthNavHost(session: SessionState, onRetry: () -> Unit) {
    val nav = rememberNavController()
    NavHost(nav, Destination.Login.route) {
        composable(Destination.Login.route) {
            val model: AuthViewModel = hiltViewModel()
            val state by model.state.collectAsStateWithLifecycle()
            val cached = (session as? SessionState.ReauthenticationRequired)?.user
            val message = state.error?.let { stringResource(authError(it)) } ?: when (session) {
                is SessionState.ReauthenticationRequired -> stringResource(R.string.session_expired)
                SessionState.StorageError -> stringResource(R.string.session_storage_error)
                else -> null
            }
            LoginScreen(onLogin = model::login, onRegister = { nav.navigate(Destination.Register.route) },
                initialEmail = cached?.email.orEmpty(), working = state.working, message = message,
                onRetry = if (session == SessionState.StorageError) onRetry else null)
        }
        composable(Destination.Register.route) {
            val model: AuthViewModel = hiltViewModel()
            val state by model.state.collectAsStateWithLifecycle()
            RegisterScreen(onBack = { nav.popBackStack() }, onRegister = model::register, working = state.working,
                message = state.error?.let { stringResource(authError(it)) })
        }
    }
}

private fun authError(reason: AuthFailure): Int = when (reason) {
    AuthFailure.INVALID_CREDENTIALS -> R.string.session_invalid_credentials
    AuthFailure.EMAIL_TAKEN -> R.string.session_email_taken
    AuthFailure.NETWORK -> R.string.session_network_error
    AuthFailure.UNAVAILABLE -> R.string.session_server_error
    AuthFailure.STORAGE -> R.string.session_storage_error
}

@Composable
private fun SessionLoading() {
    Box(Modifier.fillMaxSize().background(FinTheme.colors.paper), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = FinTheme.colors.navy)
    }
}
