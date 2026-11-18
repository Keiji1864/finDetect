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
import com.ghostteam.findetect.feature.recovery.RecoveryRoute
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
        composable(Destination.Login.route) { entry ->
            val model: AuthViewModel = hiltViewModel()
            val state by model.state.collectAsStateWithLifecycle()
            val cached = (session as? SessionState.ReauthenticationRequired)?.user
            val reauthenticationReason = (session as? SessionState.ReauthenticationRequired)?.reason
            val recovery = session as? SessionState.StorageError
            val suggestedEmail by entry.savedStateHandle.getStateFlow<String?>(LOGIN_EMAIL, null).collectAsStateWithLifecycle()
            val accountCreated by entry.savedStateHandle.getStateFlow(LOGIN_ACCOUNT_CREATED, false).collectAsStateWithLifecycle()
            val passwordReset by entry.savedStateHandle.getStateFlow<String?>(LOGIN_PASSWORD_RESET, null).collectAsStateWithLifecycle()
            val prefillRevision by entry.savedStateHandle.getStateFlow(LOGIN_PREFILL_REVISION, 0L).collectAsStateWithLifecycle()
            var handledPrefill by androidx.compose.runtime.saveable.rememberSaveable { mutableLongStateOf(0L) }
            LaunchedEffect(prefillRevision) {
                if (prefillRevision != handledPrefill) {
                    model.resetFeedback()
                    handledPrefill = prefillRevision
                }
            }
            val message = authFormMessage(state) ?: when {
                passwordReset == RESET_CONFIRMED -> stringResource(R.string.password_changed_sign_in)
                passwordReset == RESET_UNCONFIRMED -> stringResource(R.string.password_unconfirmed_sign_in)
                accountCreated || recovery?.accountCreated == true -> stringResource(R.string.auth_account_created)
                reauthenticationReason == ReauthenticationReason.PASSWORD_CHANGED -> stringResource(R.string.password_changed_sign_in)
                reauthenticationReason == ReauthenticationReason.PASSWORD_CHANGE_UNCONFIRMED -> stringResource(R.string.password_unconfirmed_sign_in)
                session is SessionState.ReauthenticationRequired -> stringResource(R.string.session_expired)
                recovery != null -> stringResource(R.string.session_storage_error)
                else -> null
            }
            LoginScreen(onLogin = model::login, onRegister = { nav.navigate(Destination.Register.route) },
                onForgotPassword = { email ->
                    entry.savedStateHandle[RECOVERY_EMAIL] = email
                    nav.navigate(Destination.Recovery.route) { launchSingleTop = true }
                },
                initialEmail = suggestedEmail ?: recovery?.email ?: cached?.email.orEmpty(), emailRevision = prefillRevision,
                state = state, message = message,
                messageIsError = state.error != null || !(passwordReset == RESET_CONFIRMED ||
                    (passwordReset == null && reauthenticationReason == ReauthenticationReason.PASSWORD_CHANGED)),
                onFieldChange = {
                    model.fieldChanged(it)
                    if (it == AuthField.EMAIL) {
                        entry.savedStateHandle[LOGIN_ACCOUNT_CREATED] = false
                        entry.savedStateHandle[LOGIN_PASSWORD_RESET] = null
                    }
                }, onRetry = if (recovery != null) onRetry else null)
        }
        composable(Destination.Register.route) {
            val model: AuthViewModel = hiltViewModel()
            val state by model.state.collectAsStateWithLifecycle()
            RegisterScreen(onBack = { nav.popBackStack() }, onRegister = model::register, state = state,
                onFieldChange = model::fieldChanged, onOpenLogin = { email ->
                    nav.getBackStackEntry(Destination.Login.route).savedStateHandle.apply {
                        this[LOGIN_EMAIL] = email
                        this[LOGIN_ACCOUNT_CREATED] = state.accountCreated
                        this[LOGIN_PASSWORD_RESET] = null
                        this[LOGIN_PREFILL_REVISION] = (get<Long>(LOGIN_PREFILL_REVISION) ?: 0L) + 1L
                    }
                    nav.popBackStack(Destination.Login.route, false)
                })
        }
        composable(Destination.Recovery.route) {
            val login = remember(it) { nav.getBackStackEntry(Destination.Login.route) }
            RecoveryRoute(initialEmail = login.savedStateHandle[RECOVERY_EMAIL] ?: "",
                onBack = { nav.popBackStack() }, onOpenLogin = { email, confirmed ->
                    login.savedStateHandle.apply {
                        this[LOGIN_EMAIL] = email
                        this[LOGIN_ACCOUNT_CREATED] = false
                        this[LOGIN_PASSWORD_RESET] = if (confirmed) RESET_CONFIRMED else RESET_UNCONFIRMED
                        this[LOGIN_PREFILL_REVISION] = (get<Long>(LOGIN_PREFILL_REVISION) ?: 0L) + 1L
                    }
                    nav.popBackStack(Destination.Login.route, false)
                })
        }
    }
}

private const val LOGIN_EMAIL = "auth_login_email"
private const val LOGIN_ACCOUNT_CREATED = "auth_login_account_created"
private const val LOGIN_PREFILL_REVISION = "auth_login_prefill_revision"
private const val LOGIN_PASSWORD_RESET = "auth_login_password_reset"
private const val RECOVERY_EMAIL = "auth_recovery_email"
private const val RESET_CONFIRMED = "confirmed"
private const val RESET_UNCONFIRMED = "unconfirmed"

@Composable
private fun SessionLoading() {
    Box(Modifier.fillMaxSize().background(FinTheme.colors.paper), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = FinTheme.colors.navy)
    }
}
