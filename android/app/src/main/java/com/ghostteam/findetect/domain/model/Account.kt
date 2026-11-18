package com.ghostteam.findetect.domain.model

import java.math.BigDecimal
import kotlinx.datetime.Instant

data class Account(
    val id: String,
    val name: String,
    val iconId: String,
    val currency: String,
    val initialBalance: BigDecimal,
    val modifiedAt: Instant,
    val includeInForecast: Boolean = true,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val deletedAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
) {
    val isDeleted: Boolean get() = deletedAt != null
}
