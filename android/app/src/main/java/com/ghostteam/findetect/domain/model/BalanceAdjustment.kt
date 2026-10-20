package com.ghostteam.findetect.domain.model

import kotlinx.datetime.Instant

data class BalanceAdjustment(
    val id: String,
    val accountId: String,
    val amount: Double,
    val createdAt: Instant,
)
