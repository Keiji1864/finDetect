package com.ghostteam.findetect.core.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.datetime.LocalDate

private fun LocalDate.calendar(): Calendar {
    val date = this
    return Calendar.getInstance().apply { clear(); set(date.year, date.monthNumber - 1, date.dayOfMonth) }
}

fun LocalDate.formatDate(locale: Locale = Locale.getDefault()): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(calendar().time)

fun LocalDate.formatMonth(locale: Locale = Locale.getDefault()): String =
    SimpleDateFormat("LLLL yyyy", locale).format(calendar().time)
