package com.ghostteam.findetect.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE forecast_plans (
            currency TEXT NOT NULL PRIMARY KEY, savingsMinor INTEGER, paymentsMinor INTEGER,
            paymentDate TEXT, manualDailyMinor INTEGER, modifiedAt TEXT NOT NULL)""")
        // An upgraded device must also receive plans uploaded before its current sync cursor.
        val hasShadow = db.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name='sync_shadow'")
            .use { it.moveToFirst() }
        if (hasShadow) db.execSQL("UPDATE sync_shadow SET content='0' WHERE section='cursor'")
    }
}
