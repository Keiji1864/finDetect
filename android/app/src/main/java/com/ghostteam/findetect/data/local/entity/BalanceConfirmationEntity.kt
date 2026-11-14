package com.ghostteam.findetect.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Local receipt makes even a zero-difference confirmation idempotent. */
@Entity(
    tableName = "balance_confirmations",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"])],
    indices = [Index("accountId")],
)
data class BalanceConfirmationEntity(
    @PrimaryKey val requestId: String,
    val accountId: String,
    val currency: String,
    val actualMinor: Long,
    val adjustmentId: String?,
)
