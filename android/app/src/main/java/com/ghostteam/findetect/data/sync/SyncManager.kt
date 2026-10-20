package com.ghostteam.findetect.data.sync

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class SyncManager @Inject constructor(
    private val syncables: Set<@JvmSuppressWildcards Syncable>,
) {
    private val mutex = Mutex()

    suspend fun syncAll() = mutex.withLock {
        syncables.sortedBy { it.order }.forEach { runCatching { it.sync() } }
    }
}