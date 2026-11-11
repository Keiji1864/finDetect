package com.ghostteam.findetect.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.toMinorUnits(): Long =
    setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()

fun Long.toMoneyAmount(): BigDecimal = BigDecimal.valueOf(this, 2)
