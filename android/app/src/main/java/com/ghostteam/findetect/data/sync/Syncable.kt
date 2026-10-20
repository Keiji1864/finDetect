package com.ghostteam.findetect.data.sync

interface Syncable {
    val order: Int
    suspend fun sync(): Result<Unit>
}