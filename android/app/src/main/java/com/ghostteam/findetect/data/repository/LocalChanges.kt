package com.ghostteam.findetect.data.repository

import com.ghostteam.findetect.domain.model.SyncStatus
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal fun nextModifiedAt(previous: Instant? = null): Instant {
    val now = Clock.System.now()
    return if (previous == null || now > previous) now else previous + 1.nanoseconds
}

internal fun SyncStatus.afterLocalEdit(): SyncStatus = when (this) {
    SyncStatus.PENDING_CREATE -> SyncStatus.PENDING_CREATE
    SyncStatus.SYNCED, SyncStatus.PENDING_UPDATE -> SyncStatus.PENDING_UPDATE
    SyncStatus.PENDING_DELETE -> error("A pending deletion cannot be edited")
}

internal fun SyncStatus.isPendingUpsert(): Boolean =
    this == SyncStatus.PENDING_CREATE || this == SyncStatus.PENDING_UPDATE
