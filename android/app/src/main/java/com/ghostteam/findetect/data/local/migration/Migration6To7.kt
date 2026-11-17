package com.ghostteam.findetect.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** No historical timing facts are fabricated; the user scope recognizes provable legacy badges. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS achievements (
            code TEXT NOT NULL PRIMARY KEY, earnedAt TEXT NOT NULL, earnedOn TEXT NOT NULL,
            isLegacy INTEGER NOT NULL, xpEventId TEXT, createdAt TEXT, syncStatus TEXT NOT NULL)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_achievements_syncStatus ON achievements (syncStatus)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_achievements_isLegacy_xpEventId ON achievements (isLegacy, xpEventId)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS achievement_state (
            id INTEGER NOT NULL PRIMARY KEY, startedAt TEXT NOT NULL,
            startedOn TEXT NOT NULL, zoneId TEXT NOT NULL, freshStartDate TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS achievement_transaction_facts (
            transactionId TEXT NOT NULL PRIMARY KEY, enteredOn TEXT NOT NULL,
            transactionOn TEXT NOT NULL, zoneId TEXT NOT NULL, enteredAt TEXT NOT NULL)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_achievement_transaction_facts_enteredOn ON achievement_transaction_facts (enteredOn)")
    }
}
