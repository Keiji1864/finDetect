package com.ghostteam.findetect.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val Context.deviceSettings by preferencesDataStore(name = "settings")

/** Device currency and notification quota survive logout; personal settings live in user files. */
@Singleton
class DevicePreferences @Inject constructor(@ApplicationContext private val context: Context) {
    val store = context.deviceSettings

    suspend fun discardLegacyData() = withContext(Dispatchers.IO) {
        if (store.data.first()[legacyDiscarded] == true) return@withContext
        val oldDatabase = context.getDatabasePath("findetective.db")
        if (oldDatabase.exists()) check(context.deleteDatabase("findetective.db"))
        check(context.getSharedPreferences("pin_security", Context.MODE_PRIVATE).edit().clear().commit())
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("findetect.pin.v1") }
        store.edit {
            it.remove(booleanPreferencesKey("game_elements_enabled"))
            it.remove(booleanPreferencesKey("daily_reminder_enabled"))
            it[legacyDiscarded] = true
        }
    }

    suspend fun claimReminder(date: String): Boolean {
        var claimed = false
        store.edit { preferences ->
            val days = preferences[reminderDates].orEmpty()
            if (date !in days) {
                preferences[reminderDates] = days + date
                claimed = true
            }
        }
        return claimed
    }

    suspend fun blockSession(sessionId: String) { store.edit { it[blockedSession] = sessionId } }
    suspend fun isSessionBlocked(sessionId: String): Boolean = store.data.first()[blockedSession] == sessionId

    suspend fun pendingCleanups(): Set<String> = store.data.first()[cleanupUsers].orEmpty()
    suspend fun rememberCleanup(userId: String) { store.edit { it[cleanupUsers] = it[cleanupUsers].orEmpty() + userId } }
    suspend fun finishCleanups(userIds: Set<String>) {
        if (userIds.isNotEmpty()) store.edit { it[cleanupUsers] = it[cleanupUsers].orEmpty() - userIds }
    }

    private companion object {
        val legacyDiscarded = booleanPreferencesKey("session_legacy_discarded_v1")
        val reminderDates = stringSetPreferencesKey("daily_reminder_dates")
        val blockedSession = stringPreferencesKey("blocked_session_id")
        val cleanupUsers = stringSetPreferencesKey("pending_user_cleanup")
    }
}
