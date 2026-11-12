package com.ghostteam.findetect.core.data

import androidx.room.withTransaction
import com.ghostteam.findetect.data.demo.DemoData
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.mapper.toEntity
import com.ghostteam.findetect.domain.model.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton

/** Fills a completely empty debug database once, preserving all existing records and tombstones. */
@Singleton
class LocalDataInitializer @Inject constructor(
    private val database: AppDatabase,
    private val data: DemoData,
) {
    suspend fun initialize() = database.withTransaction {
        val hasData = database.openHelper.writableDatabase.query(
            """SELECT EXISTS(SELECT 1 FROM categories)
                OR EXISTS(SELECT 1 FROM accounts)
                OR EXISTS(SELECT 1 FROM transactions)
                OR EXISTS(SELECT 1 FROM balance_adjustments)""",
        ).use { cursor -> cursor.moveToFirst() && cursor.getInt(0) != 0 }
        if (hasData) return@withTransaction

        database.categoryDao().upsertAll(data.categories.map { it.toEntity() })
        data.accounts.forEach { account ->
            database.accountDao().insert(account.toEntity().copy(
                createdAt = null,
                updatedAt = null,
                syncStatus = SyncStatus.PENDING_CREATE,
            ))
        }
        data.transactions.forEach { transaction ->
            database.transactionDao().insert(transaction.toEntity().copy(
                createdAt = null,
                updatedAt = null,
                syncStatus = SyncStatus.PENDING_CREATE,
            ))
        }
        data.adjustments.forEach { adjustment ->
            database.adjustmentDao().insert(adjustment.toEntity().copy(
                createdAt = null,
                syncStatus = SyncStatus.PENDING_CREATE,
            ))
        }
    }
}
