package com.ghostteam.findetect.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

enum class IncomeRepeat { MONTHLY, BIWEEKLY, WEEKLY, ONCE }

data class IncomeSchedule(val anchor: LocalDate, val repeat: IncomeRepeat) {
    fun nextAfter(today: LocalDate): LocalDate? {
        if (anchor > today) return anchor
        return when (repeat) {
            IncomeRepeat.ONCE -> null
            IncomeRepeat.WEEKLY, IncomeRepeat.BIWEEKLY -> {
                val interval = if (repeat == IncomeRepeat.WEEKLY) 7 else 14
                val elapsed = today.toEpochDays() - anchor.toEpochDays()
                today.plus(interval - elapsed % interval, DateTimeUnit.DAY)
            }
            IncomeRepeat.MONTHLY -> {
                val elapsed = (today.year - anchor.year) * 12 + today.monthNumber - anchor.monthNumber
                val candidate = anchor.plus(elapsed, DateTimeUnit.MONTH)
                if (candidate > today) candidate else anchor.plus(elapsed + 1, DateTimeUnit.MONTH)
            }
        }
    }
}
