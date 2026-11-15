package com.ghostteam.findetect.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.ghostteam.findetect.domain.model.SyncStatus
import kotlinx.datetime.Instant

/** Holds a deletion out of the sync queue and preserves its original status through process death. */
@Entity(
    tableName = "account_deletions",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"])],
)
data class AccountDeletionEntity(
    @PrimaryKey val accountId: String,
    val name: String,
    val deletedAt: Instant,
    val undoUntil: Instant,
    val previousSyncStatus: SyncStatus,
)
