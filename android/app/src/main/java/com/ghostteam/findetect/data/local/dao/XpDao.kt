package com.ghostteam.findetect.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ghostteam.findetect.data.local.entity.XpEventEntity
import com.ghostteam.findetect.data.local.entity.XpStateEntity
import com.ghostteam.findetect.domain.model.XpReason
import kotlinx.coroutines.flow.Flow

@Dao
interface XpDao {
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events WHERE counted = 1")
    fun observeTotalXp(): Flow<Long>

    @Query("SELECT * FROM xp_state WHERE id = 1")
    suspend fun getState(): XpStateEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertState(state: XpStateEntity)

    @Query("SELECT * FROM xp_events WHERE reason = :reason AND sourceId = :sourceId")
    suspend fun getBySource(reason: XpReason, sourceId: String): XpEventEntity?

    @Query("SELECT COUNT(*) FROM xp_events WHERE localDate = :localDate AND reason = :reason AND counted = 1")
    suspend fun countDailyAwards(localDate: String, reason: XpReason): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: XpEventEntity)

    @Query("SELECT * FROM xp_events WHERE syncStatus != 'SYNCED' ORDER BY occurredAt, id")
    suspend fun getPendingEvents(): List<XpEventEntity>
}
