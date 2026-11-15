package com.ghostteam.findetect.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class Period(val start: LocalDate, val end: LocalDate) {
    init { require(start <= end) { "Period start must not follow its end" } }

    companion object {
        fun today(): LocalDate =
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        /** Полный календарный месяц, включая последний день. */
        fun currentMonth(today: LocalDate = today()): Period {
            val first = LocalDate(today.year, today.month, 1)
            return Period(first, first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY))
        }

        fun lastWeek(today: LocalDate = today()) =
            Period(today.minus(7, DateTimeUnit.DAY), today)

        fun lastMonth(today: LocalDate = today()) =
            Period(today.minus(1, DateTimeUnit.MONTH), today)

        fun lastQuarter(today: LocalDate = today()) =
            Period(today.minus(3, DateTimeUnit.MONTH), today)

        fun lastYear(today: LocalDate = today()) =
            Period(today.minus(1, DateTimeUnit.YEAR), today)
    }
}
