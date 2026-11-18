package com.ghostteam.findetect.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/** One row per user DB. Milestones up to legacyBestStreak were reached before XP and are not paid. */
@Entity(tableName = "xp_state")
data class XpStateEntity(
    @PrimaryKey val id: Int = 1,
    val startedAt: Instant,
    val legacyBestStreak: Int,
)
