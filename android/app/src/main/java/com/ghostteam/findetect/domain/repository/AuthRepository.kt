package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.LogoutResult
import com.ghostteam.findetect.domain.model.OnboardingStep
import com.ghostteam.findetect.domain.model.SessionState
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val state: StateFlow<SessionState>
    suspend fun restore()
    suspend fun login(email: String, password: String)
    suspend fun register(name: String, email: String, password: String)
    suspend fun advanceOnboarding(step: OnboardingStep)
    suspend fun logout(discardPendingChanges: Boolean = false): LogoutResult
    suspend fun requireReauthentication()
    suspend fun refreshUser()
}

