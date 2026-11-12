package com.ghostteam.findetect.domain.model

import kotlinx.datetime.Instant
import java.math.BigDecimal

data class Transaction(
    val id: String,           // UUID
    val accountId: String,
    val categoryId: String,
    val amount: BigDecimal,   // всегда > 0, доход/расход определяет статья
    val transactionDate: Instant,
    val enteredAt: Instant,   // когда внесена (для серии)
    val modifiedAt: Instant,
    val comment: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val deletedAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
) {
    val isDeleted: Boolean get() = deletedAt != null
}
