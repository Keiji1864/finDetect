package com.ghostteam.findetect.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/** Frozen once per user DB; a partially observed activation day may not prove a timely-entry day. */
@Entity(tableName = "achievement_state")
data class AchievementStateEntity(
    @PrimaryKey val id: Int = 1,
    val startedAt: Instant,
    val startedOn: String,
    val zoneId: String,
    val freshStartDate: String,
)
