package com.ghostteam.findetect.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ghostteam.findetect.domain.model.SyncStatus
import kotlinx.datetime.Instant

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"]),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"]),
    ],
    indices = [Index("accountId"), Index("categoryId"), Index("transactionDate")],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val categoryId: String,
    val amountMinor: Long,
    val transactionDate: Instant,
    val enteredAt: Instant,
    val modifiedAt: Instant,
    val comment: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val deletedAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
)
