package com.ghostteam.findetect.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ghostteam.findetect.domain.model.SyncStatus
import kotlinx.datetime.Instant

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconId: String,
    val currency: String,
    val initialBalanceMinor: Long,
    val modifiedAt: Instant,
    val includeInForecast: Boolean = true,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val deletedAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    val legacyEmoji: String? = null,
)
