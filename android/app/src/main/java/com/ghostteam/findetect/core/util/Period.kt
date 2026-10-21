package com.ghostteam.findetect.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

data class Period(val start: LocalDate, val end: LocalDate) {

    companion object {
        fun today(): LocalDate =
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        /** По умолчанию: с 1-го числа текущего месяца по сегодня */
        fun currentMonth(today: LocalDate = today()) =
            Period(LocalDate(today.year, today.month, 1), today)

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
