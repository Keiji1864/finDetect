package com.ghostteam.findetect.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ghostteam.findetect.data.local.entity.ActivityDayEntity
import com.ghostteam.findetect.data.local.entity.NotificationDayEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface ActivityDayDao {
    @Query("SELECT date FROM activity_days WHERE hadTransaction = 1 OR noSpendingConfirmed = 1 ORDER BY date ASC")
    fun observeActiveDates(): Flow<List<String>>

    @Query("""SELECT EXISTS(SELECT 1 FROM transactions t
        INNER JOIN categories c ON c.id = t.categoryId
        WHERE t.deletedAt IS NULL AND c.isIncome = 0
        AND t.transactionDate >= :from AND t.transactionDate < :until)""")
    fun observeHasExpense(from: Instant, until: Instant): Flow<Boolean>

    @Query("""SELECT EXISTS(SELECT 1 FROM transactions t
        INNER JOIN categories c ON c.id = t.categoryId
        WHERE t.deletedAt IS NULL AND c.isIncome = 0
        AND t.transactionDate >= :from AND t.transactionDate < :until)""")
    suspend fun hasExpense(from: Instant, until: Instant): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE enteredAt >= :from AND enteredAt < :until)")
    suspend fun hasEnteredTransaction(from: Instant, until: Instant): Boolean

    @Query("SELECT * FROM activity_days WHERE date = :date")
    fun observe(date: String): Flow<ActivityDayEntity?>

    @Query("SELECT * FROM activity_days WHERE date = :date")
    suspend fun get(date: String): ActivityDayEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(day: ActivityDayEntity)

    @Query("""UPDATE activity_days SET hadTransaction = 1, enteredAt = MIN(enteredAt, :at), modifiedAt = MAX(modifiedAt, :at)
        WHERE date = :date""")
    suspend fun setHadTransaction(date: String, at: Instant)

    @Query("UPDATE activity_days SET noSpendingConfirmed = 1, enteredAt = MIN(enteredAt, :at), modifiedAt = MAX(modifiedAt, :at) WHERE date = :date")
    suspend fun setNoSpendingConfirmed(date: String, at: Instant)

    @Transaction
    suspend fun recordTransaction(date: String, at: Instant) {
        insert(ActivityDayEntity(date, hadTransaction = true, noSpendingConfirmed = false, enteredAt = at, modifiedAt = at))
        setHadTransaction(date, at)
    }

    @Transaction
    suspend fun confirmNoSpending(date: String, at: Instant) {
        insert(ActivityDayEntity(date, hadTransaction = false, noSpendingConfirmed = true, enteredAt = at, modifiedAt = at))
        setNoSpendingConfirmed(date, at)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun claimNotification(day: NotificationDayEntity): Long
}
