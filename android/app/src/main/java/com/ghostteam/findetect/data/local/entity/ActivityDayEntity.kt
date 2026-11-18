package com.ghostteam.findetect.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/** Local calendar date is captured when the user acts, so later time zone changes do not move it. */
@Entity(tableName = "activity_days")
data class ActivityDayEntity(
    @PrimaryKey val date: String,
    val hadTransaction: Boolean,
    val noSpendingConfirmed: Boolean,
    val enteredAt: Instant,
    val modifiedAt: Instant,
)

/** A persistent daily notification claim, shared by future notification sources too. */
@Entity(tableName = "notification_days")
data class NotificationDayEntity(@PrimaryKey val date: String, val claimedAt: Instant)
