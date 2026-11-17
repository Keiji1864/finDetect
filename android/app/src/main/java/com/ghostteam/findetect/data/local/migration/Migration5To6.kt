package com.ghostteam.findetect.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds an empty XP journal. The user scope freezes the legacy streak before accepting actions. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS xp_events (
            id TEXT NOT NULL PRIMARY KEY, reason TEXT NOT NULL, sourceId TEXT NOT NULL,
            reasonRef TEXT, amount INTEGER NOT NULL, counted INTEGER NOT NULL,
            localDate TEXT NOT NULL, occurredAt TEXT NOT NULL, createdAt TEXT, syncStatus TEXT NOT NULL)""")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_xp_events_reason_sourceId ON xp_events (reason, sourceId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_xp_events_localDate_reason_counted ON xp_events (localDate, reason, counted)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_xp_events_syncStatus ON xp_events (syncStatus)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS xp_state (
            id INTEGER NOT NULL PRIMARY KEY, startedAt TEXT NOT NULL, legacyBestStreak INTEGER NOT NULL)""")
    }
}
