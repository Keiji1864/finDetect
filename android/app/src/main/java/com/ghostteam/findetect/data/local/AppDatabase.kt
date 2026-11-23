package com.ghostteam.findetect.data.local

import com.ghostteam.findetect.data.local.dao.PersonalSettings
import com.ghostteam.findetect.data.local.dao.ForecastBalance
import com.ghostteam.findetect.data.local.dao.ForecastPlanDao
import com.ghostteam.findetect.data.local.dao.ForecastPlanRow
import com.ghostteam.findetect.data.local.dao.ForecastDay
import com.ghostteam.findetect.data.local.dao.ForecastHistoryDao
import com.ghostteam.findetect.data.local.dao.PersonalSettingsDao
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ghostteam.findetect.data.local.dao.AccountDao
import com.ghostteam.findetect.data.local.dao.ActivityDayDao
import com.ghostteam.findetect.data.local.entity.ActivityDayEntity
import com.ghostteam.findetect.data.local.entity.NotificationDayEntity
import com.ghostteam.findetect.data.local.dao.AccountDeletionDao
import com.ghostteam.findetect.data.local.dao.AdjustmentDao
import com.ghostteam.findetect.data.local.dao.BalanceDao
import com.ghostteam.findetect.data.local.dao.BalanceConfirmationDao
import com.ghostteam.findetect.data.local.dao.CategoryDao
import com.ghostteam.findetect.data.local.dao.TransactionDao
import com.ghostteam.findetect.data.local.dao.TransactionDeletionDao
import com.ghostteam.findetect.data.local.entity.AccountEntity
import com.ghostteam.findetect.data.local.entity.AccountDeletionEntity
import com.ghostteam.findetect.data.local.entity.BalanceConfirmationEntity
import com.ghostteam.findetect.data.local.entity.AdjustmentEntity
import com.ghostteam.findetect.data.local.entity.CategoryEntity
import com.ghostteam.findetect.data.local.entity.TransactionEntity
import com.ghostteam.findetect.data.local.entity.TransactionDeletionEntity
import com.ghostteam.findetect.data.local.dao.XpDao
import com.ghostteam.findetect.data.local.entity.XpEventEntity
import com.ghostteam.findetect.data.local.entity.XpStateEntity
import com.ghostteam.findetect.data.local.dao.AchievementDao
import com.ghostteam.findetect.data.local.entity.AchievementEntity
import com.ghostteam.findetect.data.local.entity.AchievementStateEntity
import com.ghostteam.findetect.data.local.entity.AchievementTransactionFactEntity

@Database(
    entities = [AccountEntity::class, CategoryEntity::class, TransactionEntity::class, AdjustmentEntity::class,
        AccountDeletionEntity::class, BalanceConfirmationEntity::class, TransactionDeletionEntity::class,
        ActivityDayEntity::class, NotificationDayEntity::class, XpEventEntity::class, XpStateEntity::class,
        AchievementEntity::class, AchievementStateEntity::class, AchievementTransactionFactEntity::class, PersonalSettings::class,
        ForecastDay::class, ForecastBalance::class, ForecastPlanRow::class],
    version = 10,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun forecastPlanDao(): ForecastPlanDao
    abstract fun forecastHistoryDao(): ForecastHistoryDao
    abstract fun personalSettingsDao(): PersonalSettingsDao
    abstract fun achievementDao(): AchievementDao
    abstract fun xpDao(): XpDao
    abstract fun activityDayDao(): ActivityDayDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionDeletionDao(): TransactionDeletionDao
    abstract fun adjustmentDao(): AdjustmentDao
    abstract fun balanceDao(): BalanceDao
    abstract fun accountDeletionDao(): AccountDeletionDao
    abstract fun balanceConfirmationDao(): BalanceConfirmationDao
}
