package com.ghostteam.findetect.feature.session

import android.util.Patterns
import com.ghostteam.findetect.domain.model.AuthField
import com.ghostteam.findetect.domain.model.AuthFieldError

/** Shared by form feedback and submission validation. Password whitespace is significant. */
object AuthFormValidation {
    const val MAX_NAME = 50
    const val MAX_EMAIL = 254
    const val MIN_PASSWORD = 8
    const val MAX_PASSWORD = 128

    fun login(email: String, password: String): Map<AuthField, AuthFieldError> = buildMap {
        validateEmail(email)?.let { put(AuthField.EMAIL, it) }
        if (password.isEmpty()) put(AuthField.PASSWORD, AuthFieldError.REQUIRED)
    }

    fun registration(name: String, email: String, password: String, confirmation: String): Map<AuthField, AuthFieldError> = buildMap {
        val trimmed = name.trim()
        when {
            trimmed.isEmpty() -> put(AuthField.NAME, AuthFieldError.REQUIRED)
            trimmed.length > MAX_NAME -> put(AuthField.NAME, AuthFieldError.NAME_LENGTH)
        }
        validateEmail(email)?.let { put(AuthField.EMAIL, it) }
        if (password.length !in MIN_PASSWORD..MAX_PASSWORD) put(AuthField.PASSWORD, AuthFieldError.PASSWORD_LENGTH)
        if (confirmation != password) put(AuthField.CONFIRMATION, AuthFieldError.PASSWORD_MISMATCH)
    }

    private fun validateEmail(value: String): AuthFieldError? = when {
        value.trim().isEmpty() -> AuthFieldError.REQUIRED
        value.trim().length > MAX_EMAIL || !Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches() -> AuthFieldError.INVALID_EMAIL
        else -> null
    }
}
