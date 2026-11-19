package com.ghostteam.findetect.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "personal_settings")
data class PersonalSettings(
    @PrimaryKey val id: Int = 1,
    val gameEnabled: Boolean,
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val modifiedAt: String,
    val nextIncomeDate: String? = null,
    val incomeRepeat: String? = null,
)

@Dao
interface PersonalSettingsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seed(row: PersonalSettings)
    @Query("SELECT * FROM personal_settings WHERE id=1")
    fun observe(): Flow<PersonalSettings>
    @Query("UPDATE personal_settings SET nextIncomeDate=:date, incomeRepeat=:repeat, modifiedAt=:now WHERE id=1")
    suspend fun income(date: String?, repeat: String?, now: String): Int
    @Query("UPDATE personal_settings SET gameEnabled=:enabled, modifiedAt=:now WHERE id=1")
    suspend fun game(enabled: Boolean, now: String)
    @Query("UPDATE personal_settings SET reminderEnabled=:enabled, modifiedAt=:now WHERE id=1")
    suspend fun reminder(enabled: Boolean, now: String)
    @Query("UPDATE personal_settings SET reminderHour=:hour, modifiedAt=:now WHERE id=1")
    suspend fun hour(hour: Int, now: String)
}
