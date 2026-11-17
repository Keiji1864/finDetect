package com.ghostteam.findetect.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ghostteam.findetect.domain.model.SyncStatus
import com.ghostteam.findetect.domain.model.XpReason
import kotlinx.datetime.Instant

/**
 * Durable journal, independent of transaction deletion. sourceId is a transaction ID, mark date,
 * milestone number or achievement code; its pair with reason uniquely identifies the local action.
 * reasonRef is the API field for milestones/achievements and is null for basic awards.
 */
@Entity(
    tableName = "xp_events",
    indices = [
        Index(value = ["reason", "sourceId"], unique = true),
        Index(value = ["localDate", "reason", "counted"]),
        Index("syncStatus"),
    ],
)
data class XpEventEntity(
    @PrimaryKey val id: String,
    val reason: XpReason,
    val sourceId: String,
    val reasonRef: String?,
    val amount: Int,
    val counted: Boolean,
    val localDate: String,
    val occurredAt: Instant,
    val createdAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
)
