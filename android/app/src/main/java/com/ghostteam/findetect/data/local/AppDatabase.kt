package com.ghostteam.findetect.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ghostteam.findetect.data.local.dao.AccountDao
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

@Database(
    entities = [AccountEntity::class, CategoryEntity::class, TransactionEntity::class, AdjustmentEntity::class,
        AccountDeletionEntity::class, BalanceConfirmationEntity::class, TransactionDeletionEntity::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionDeletionDao(): TransactionDeletionDao
    abstract fun adjustmentDao(): AdjustmentDao
    abstract fun balanceDao(): BalanceDao
    abstract fun accountDeletionDao(): AccountDeletionDao
    abstract fun balanceConfirmationDao(): BalanceConfirmationDao
}
