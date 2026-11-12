package com.ghostteam.findetect.data.local

import androidx.room.TypeConverter
import com.ghostteam.findetect.domain.model.SyncStatus
import kotlinx.datetime.Instant

class Converters {
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()?.let { timestamp ->
        val utc = timestamp.removeSuffix("Z")
        if ('.' in utc) utc.substringBefore('.') + "." + utc.substringAfter('.').padEnd(9, '0') + "Z"
        else "$utc.000000000Z"
    }

    @TypeConverter
    fun toInstant(value: String?): Instant? = value?.let(Instant::parse)
}
