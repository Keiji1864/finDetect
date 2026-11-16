package com.ghostteam.findetect.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** The current device calendar, separate from dates already captured in activity history. */
data class LocalDay(val date: LocalDate, val zone: TimeZone)
