package com.ghostteam.findetect.domain.model

data class DayActivity(val noSpendingConfirmed: Boolean = false, val hasExpense: Boolean = false)

enum class ConfirmNoSpendingResult { CONFIRMED, ALREADY_CONFIRMED, HAS_EXPENSE, DAY_CHANGED }
