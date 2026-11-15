package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.AppCurrency
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val currency: Flow<AppCurrency>
    suspend fun setCurrency(currency: AppCurrency)
}

