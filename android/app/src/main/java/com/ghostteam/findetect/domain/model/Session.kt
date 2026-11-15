package com.ghostteam.findetect.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SessionUser(val id: String, val email: String, val name: String)

/** The login instance changes on sign-in, but remains stable across rotation and restarts. */
data class SessionIdentity(val userId: String, val sessionId: String)

enum class OnboardingStep { INCOME, PIN, DONE }

sealed interface SessionState {
    data object Restoring : SessionState
    data object SignedOut : SessionState
    data class Active(val identity: SessionIdentity, val user: SessionUser, val onboarding: OnboardingStep) : SessionState
    data class ReauthenticationRequired(val user: SessionUser?) : SessionState
    data object StorageError : SessionState
}

enum class AuthFailure { INVALID_CREDENTIALS, EMAIL_TAKEN, NETWORK, UNAVAILABLE, STORAGE }

class AuthException(val reason: AuthFailure) : Exception(reason.name)

enum class LogoutResult { DONE, NEEDS_CONFIRMATION }
