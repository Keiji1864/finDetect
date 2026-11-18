package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.SessionIdentity

interface PasswordRepository {
    /** Only a confirmed change clears authorization; local financial data, settings and PIN remain. */
    suspend fun changePassword(owner: SessionIdentity, currentPassword: String, newPassword: String)

    /** Explicitly open sign-in to check an uncertain result, without claiming success or deleting data. */
    suspend fun signInAfterUnconfirmedChange(owner: SessionIdentity)
}
