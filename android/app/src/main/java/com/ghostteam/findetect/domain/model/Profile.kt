package com.ghostteam.findetect.domain.model

/** The same trimmed name rule is used by registration, profile editing and the repository. */
object UserNameValidation {
    const val MAX_LENGTH = 50
    fun error(value: String): AuthFieldError? = when {
        value.trim().isEmpty() -> AuthFieldError.REQUIRED
        value.trim().length > MAX_LENGTH -> AuthFieldError.NAME_LENGTH
        else -> null
    }
}

enum class ProfileFailure { INVALID_NAME, NETWORK, TIMEOUT, UNAVAILABLE, RATE_LIMITED, BAD_RESPONSE, STORAGE }
class ProfileException(val reason: ProfileFailure) : Exception(reason.name)
