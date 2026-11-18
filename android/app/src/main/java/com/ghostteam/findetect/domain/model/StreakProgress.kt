package com.ghostteam.findetect.domain.model

import kotlinx.datetime.LocalDate

data class StreakWeekDay(val date: LocalDate, val isActive: Boolean)

/** A calendar-day event, not a persistent notification or an acknowledgement by the user. */
enum class StreakEvent { NONE, GRACE_USED, GRACE_RESTORED, STREAK_RESET }

/** Derived from activity history; a forgiven day never counts as an active day or a week checkmark. */
data class StreakProgress(
    val today: LocalDate,
    val currentStreak: Int,
    val bestStreak: Int,
    val graceDayAvailable: Boolean,
    /** Consecutive active days since using the reserve; zero when it is available. */
    val graceRecoveryDays: Int,
    val lastActiveDate: LocalDate?,
    val event: StreakEvent,
    /** Exactly seven calendar dates, Monday through Sunday, including inactive and future days. */
    val week: List<StreakWeekDay>,
)
