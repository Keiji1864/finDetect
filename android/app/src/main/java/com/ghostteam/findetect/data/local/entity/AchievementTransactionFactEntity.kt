package com.ghostteam.findetect.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/** Immutable original dates for every new operation, including deleted and XP-capped operations. */
@Entity(tableName = "achievement_transaction_facts", indices = [Index("enteredOn")])
data class AchievementTransactionFactEntity(
    @PrimaryKey val transactionId: String,
    val enteredOn: String,
    val transactionOn: String,
    val zoneId: String,
    val enteredAt: Instant,
)
