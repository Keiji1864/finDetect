package com.ghostteam.findetect.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ghostteam.findetect.domain.model.AchievementCode
import com.ghostteam.findetect.domain.model.SyncStatus
import kotlinx.datetime.Instant

/** Non-legacy rows with no xpEventId are durable reward intents for the later XP integration. */
@Entity(tableName = "achievements", indices = [Index("syncStatus"), Index(value = ["isLegacy", "xpEventId"])])
data class AchievementEntity(
    @PrimaryKey val code: AchievementCode,
    val earnedAt: Instant,
    val earnedOn: String,
    val isLegacy: Boolean,
    val xpEventId: String? = null,
    val createdAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
)
