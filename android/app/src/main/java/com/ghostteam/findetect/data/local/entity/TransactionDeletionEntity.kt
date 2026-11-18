package com.ghostteam.findetect.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.ghostteam.findetect.domain.model.SyncStatus
import kotlinx.datetime.Instant

/** Durable sync hold and restoration state for a transaction's undo window. */
@Entity(
    tableName = "transaction_deletions",
    foreignKeys = [ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionId"])],
)
data class TransactionDeletionEntity(
    @PrimaryKey val transactionId: String,
    val deletedAt: Instant,
    val undoUntil: Instant,
    val previousSyncStatus: SyncStatus,
)
