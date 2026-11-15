package com.ghostteam.findetect.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `account_deletions` (
                `accountId` TEXT NOT NULL, `name` TEXT NOT NULL,
                `deletedAt` TEXT NOT NULL, `undoUntil` TEXT NOT NULL,
                `previousSyncStatus` TEXT NOT NULL, PRIMARY KEY(`accountId`),
                FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)""",
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `balance_confirmations` (
                `requestId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `currency` TEXT NOT NULL,
                `actualMinor` INTEGER NOT NULL, `adjustmentId` TEXT, PRIMARY KEY(`requestId`),
                FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)""",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_balance_confirmations_accountId` ON `balance_confirmations` (`accountId`)")
    }
}
