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
    data class StorageError(val email: String? = null, val accountCreated: Boolean = false) : SessionState
}

enum class AuthField { NAME, EMAIL, PASSWORD, CONFIRMATION }
enum class AuthFieldError { REQUIRED, INVALID_EMAIL, NAME_LENGTH, PASSWORD_LENGTH, PASSWORD_MISMATCH, EMAIL_TAKEN, INVALID_VALUE }
enum class AuthFailure { INVALID_CREDENTIALS, EMAIL_TAKEN, VALIDATION, RATE_LIMITED, NETWORK, TIMEOUT, BAD_RESPONSE, UNAVAILABLE, STORAGE }

/** Only safe machine-readable details cross into the UI; server messages never become user text. */
class AuthException(
    val reason: AuthFailure,
    val fieldErrors: Map<AuthField, AuthFieldError> = emptyMap(),
    val retryAfterSeconds: Long? = null,
    val accountCreated: Boolean = false,
) : Exception(reason.name)

enum class LogoutResult { DONE, NEEDS_CONFIRMATION }
