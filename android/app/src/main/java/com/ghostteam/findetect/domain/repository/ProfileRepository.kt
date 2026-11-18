package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.SessionIdentity

interface ProfileRepository {
    suspend fun refresh(owner: SessionIdentity)
    suspend fun saveName(owner: SessionIdentity, name: String)
    suspend fun deleteAccount(owner: SessionIdentity)
}
