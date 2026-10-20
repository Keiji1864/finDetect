package com.ghostteam.findetect.data.local

import androidx.room.TypeConverter
import com.ghostteam.findetect.domain.model.SyncStatus

class Converters {
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}