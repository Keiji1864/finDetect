package com.ghostteam.findetect.feature.session

import android.util.Patterns
import com.ghostteam.findetect.domain.model.AuthField
import com.ghostteam.findetect.domain.model.AuthFieldError
import com.ghostteam.findetect.domain.model.UserNameValidation
import com.ghostteam.findetect.domain.model.PasswordValidation

/** Shared by form feedback and submission validation. Password whitespace is significant. */
object AuthFormValidation {
    const val MAX_NAME = UserNameValidation.MAX_LENGTH
    const val MAX_EMAIL = 254
    const val MIN_PASSWORD = PasswordValidation.MIN_LENGTH
    const val MAX_PASSWORD = PasswordValidation.MAX_LENGTH

    fun login(email: String, password: String): Map<AuthField, AuthFieldError> = buildMap {
        validateEmail(email)?.let { put(AuthField.EMAIL, it) }
        if (password.isEmpty()) put(AuthField.PASSWORD, AuthFieldError.REQUIRED)
    }

    fun registration(name: String, email: String, password: String, confirmation: String): Map<AuthField, AuthFieldError> = buildMap {
        UserNameValidation.error(name)?.let { put(AuthField.NAME, it) }
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
