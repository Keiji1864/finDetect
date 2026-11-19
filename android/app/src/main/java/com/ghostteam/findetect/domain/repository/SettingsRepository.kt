package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.AppSettings
import com.ghostteam.findetect.domain.model.IncomeSchedule
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    val currency: Flow<AppCurrency>
    suspend fun setCurrency(currency: AppCurrency)
    suspend fun setGameElementsEnabled(enabled: Boolean)
    suspend fun setDailyReminderEnabled(enabled: Boolean)
    suspend fun setDailyReminderHour(hour: Int)
    suspend fun setIncomeSchedule(schedule: IncomeSchedule?, confirmedDate: LocalDate?)
}

