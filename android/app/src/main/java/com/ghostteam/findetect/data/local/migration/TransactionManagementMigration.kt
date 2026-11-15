package com.ghostteam.findetect.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `transaction_deletions` (
                `transactionId` TEXT NOT NULL, `deletedAt` TEXT NOT NULL, `undoUntil` TEXT NOT NULL,
                `previousSyncStatus` TEXT NOT NULL, PRIMARY KEY(`transactionId`),
                FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)""",
        )
    }
}
