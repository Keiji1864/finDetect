package com.ghostteam.findetect.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS personal_settings (
            id INTEGER NOT NULL PRIMARY KEY, gameEnabled INTEGER NOT NULL, reminderEnabled INTEGER NOT NULL,
            reminderHour INTEGER NOT NULL, modifiedAt TEXT NOT NULL, nextIncomeDate TEXT, incomeRepeat TEXT)""")
    }
}
