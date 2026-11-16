package com.ghostteam.findetect.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS activity_days (
            date TEXT NOT NULL PRIMARY KEY, hadTransaction INTEGER NOT NULL,
            noSpendingConfirmed INTEGER NOT NULL, enteredAt TEXT NOT NULL, modifiedAt TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS notification_days (
            date TEXT NOT NULL PRIMARY KEY, claimedAt TEXT NOT NULL)""")
        // Historical entries did not retain their original zone. Use the device zone at migration.
        // Include deleted transactions: deleting an entry must not revoke the original activity.
        db.execSQL("""INSERT OR IGNORE INTO activity_days
            (date, hadTransaction, noSpendingConfirmed, enteredAt, modifiedAt)
            SELECT date(substr(enteredAt, 1, 19) || 'Z', 'localtime'), 1, 0, MIN(enteredAt), MAX(modifiedAt)
            FROM transactions
            WHERE date(substr(enteredAt, 1, 19) || 'Z', 'localtime') IS NOT NULL
            GROUP BY date(substr(enteredAt, 1, 19) || 'Z', 'localtime')""")
    }
}
