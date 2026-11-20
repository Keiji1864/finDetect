package com.ghostteam.findetect.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS forecast_days (id TEXT NOT NULL PRIMARY KEY, currency TEXT NOT NULL, day TEXT NOT NULL, stamp INTEGER NOT NULL, accountIds TEXT NOT NULL, fromUtc TEXT NOT NULL, untilUtc TEXT NOT NULL, limitMinor INTEGER)")
        db.execSQL("CREATE TABLE IF NOT EXISTS forecast_balances (id TEXT NOT NULL PRIMARY KEY, currency TEXT NOT NULL, stamp INTEGER NOT NULL, localDay TEXT NOT NULL, targetDay TEXT, rule TEXT, accountIds TEXT NOT NULL, balanceMinor INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX index_forecast_days_currency_day ON forecast_days(currency, day)")
        db.execSQL("CREATE INDEX index_forecast_balances_currency_stamp ON forecast_balances(currency, stamp)")
        val hasCursorTable = db.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name='sync_shadow'")
            .use { it.moveToFirst() }
        if (hasCursorTable) db.execSQL("UPDATE sync_shadow SET content='0' WHERE section='cursor'")
    }
}
