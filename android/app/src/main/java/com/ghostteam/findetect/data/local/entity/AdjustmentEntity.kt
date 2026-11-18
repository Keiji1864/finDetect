package com.ghostteam.findetect.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ghostteam.findetect.domain.model.SyncStatus
import kotlinx.datetime.Instant

@Entity(
    tableName = "balance_adjustments",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"]),
    ],
    indices = [Index("accountId"), Index("createdAt")],
)
data class AdjustmentEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val amountMinor: Long,
    val occurredAt: Instant,
    val createdAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
)
