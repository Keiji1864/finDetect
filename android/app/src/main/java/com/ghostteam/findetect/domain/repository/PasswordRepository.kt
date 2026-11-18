package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.SessionIdentity
import com.ghostteam.findetect.domain.model.PasswordResetChallenge
import com.ghostteam.findetect.domain.model.PasswordResetGrant

interface PasswordRepository {
    /** Only a confirmed change clears authorization; local financial data, settings and PIN remain. */
    suspend fun changePassword(owner: SessionIdentity, currentPassword: String, newPassword: String)

    /** Explicitly open sign-in to check an uncertain result, without claiming success or deleting data. */
    suspend fun signInAfterUnconfirmedChange(owner: SessionIdentity)

    suspend fun requestReset(email: String): PasswordResetChallenge
    suspend fun verifyReset(requestId: String, code: String): PasswordResetGrant

    /** Public reset. Email is used only for local feedback, never to select the server account. */
    suspend fun completeReset(email: String, resetToken: String, newPassword: String)
}
