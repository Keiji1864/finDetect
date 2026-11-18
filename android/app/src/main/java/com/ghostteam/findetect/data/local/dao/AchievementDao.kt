package com.ghostteam.findetect.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ghostteam.findetect.data.local.entity.AchievementEntity
import com.ghostteam.findetect.data.local.entity.AchievementStateEntity
import com.ghostteam.findetect.data.local.entity.AchievementTransactionFactEntity
import com.ghostteam.findetect.domain.model.AchievementCode
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY earnedAt, code")
    fun observeEarned(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements ORDER BY earnedAt, code")
    suspend fun getEarned(): List<AchievementEntity>

    @Query("SELECT * FROM achievement_state WHERE id = 1")
    suspend fun getState(): AchievementStateEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertState(state: AchievementStateEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEarned(achievement: AchievementEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionFact(fact: AchievementTransactionFactEntity): Long

    @Query("SELECT * FROM achievement_transaction_facts ORDER BY enteredOn, enteredAt, transactionId")
    suspend fun getTransactionFacts(): List<AchievementTransactionFactEntity>

    @Query("SELECT * FROM achievements WHERE isLegacy = 0 AND xpEventId IS NULL ORDER BY earnedAt, code")
    suspend fun getPendingRewards(): List<AchievementEntity>

    @Query("UPDATE achievements SET xpEventId = :eventId WHERE code = :code AND isLegacy = 0 AND xpEventId IS NULL")
    suspend fun markRewarded(code: AchievementCode, eventId: String): Int
}
