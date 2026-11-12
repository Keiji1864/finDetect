package com.ghostteam.findetect.domain.model

import kotlinx.datetime.Instant
import java.math.BigDecimal

data class BalanceAdjustment(
    val id: String,
    val accountId: String,
    val amount: BigDecimal,
    val occurredAt: Instant,
    val createdAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
)
