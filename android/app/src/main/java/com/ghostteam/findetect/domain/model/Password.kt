package com.ghostteam.findetect.domain.model

enum class PasswordField { CURRENT_PASSWORD, NEW_PASSWORD, CONFIRMATION, EMAIL, REQUEST_ID, CODE, RESET_TOKEN }

enum class PasswordFieldError { REQUIRED, LENGTH, MISMATCH, CURRENT_PASSWORD_INVALID, SAME_AS_CURRENT, INVALID_VALUE }

enum class PasswordFailure {
    VALIDATION, CURRENT_PASSWORD_INVALID, NEW_PASSWORD_SAME_AS_OLD,
    RESET_CODE_INVALID, RESET_CODE_EXPIRED, RESET_TOKEN_INVALID,
    UNAUTHORIZED, SESSION_REFRESHED, RATE_LIMITED, NETWORK, TIMEOUT, BAD_RESPONSE, UNAVAILABLE, STORAGE,
}

/** Safe error data only: no passwords, reset tokens, response bodies or server messages. */
class PasswordException(
    val reason: PasswordFailure,
    val fieldErrors: Map<PasswordField, PasswordFieldError> = emptyMap(),
    val retryAfterSeconds: Long? = null,
    val outcomeUnknown: Boolean = false,
) : Exception(reason.name)

/** The password policy shared by registration and password changes; whitespace is significant. */
object PasswordValidation {
    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 128

    fun reset(new: String, confirmation: String): Map<PasswordField, PasswordFieldError> = buildMap {
        if (new.isEmpty()) put(PasswordField.NEW_PASSWORD, PasswordFieldError.REQUIRED)
        else if (new.length !in MIN_LENGTH..MAX_LENGTH) put(PasswordField.NEW_PASSWORD, PasswordFieldError.LENGTH)
        if (confirmation.isEmpty()) put(PasswordField.CONFIRMATION, PasswordFieldError.REQUIRED)
        else if (confirmation != new) put(PasswordField.CONFIRMATION, PasswordFieldError.MISMATCH)
    }

    fun change(current: String, new: String, confirmation: String): Map<PasswordField, PasswordFieldError> =
        buildMap {
            if (current.isEmpty()) put(PasswordField.CURRENT_PASSWORD, PasswordFieldError.REQUIRED)
            else if (current.length > MAX_LENGTH) put(PasswordField.CURRENT_PASSWORD, PasswordFieldError.LENGTH)
            putAll(reset(new, confirmation))
            if (new == current && PasswordField.NEW_PASSWORD !in this)
                put(PasswordField.NEW_PASSWORD, PasswordFieldError.SAME_AS_CURRENT)
        }
}

data class PasswordResetChallenge(val requestId: String, val expiresInSeconds: Int, val resendAfterSeconds: Int)

/** A short-lived permission, kept only in memory; it is never an authenticated session. */
class PasswordResetGrant(val token: String, val expiresInSeconds: Int)
