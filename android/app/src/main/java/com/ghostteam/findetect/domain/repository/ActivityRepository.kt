package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.ConfirmNoSpendingResult
import com.ghostteam.findetect.domain.model.DayActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

interface ActivityRepository {
    fun observeDay(date: LocalDate, zone: TimeZone): Flow<DayActivity>
    suspend fun confirmNoSpending(expectedDate: LocalDate): ConfirmNoSpendingResult
    suspend fun claimDailyReminder(date: LocalDate, zone: TimeZone): Boolean
}
