package com.ghostteam.findetect.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ghostteam.findetect.domain.model.SyncStatus

@Entity(tableName = "balance_adjustments")
data class AdjustmentEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val amount: Double,
    val createdAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
)