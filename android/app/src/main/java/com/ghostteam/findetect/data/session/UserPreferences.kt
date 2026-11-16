package com.ghostteam.findetect.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One DataStore instance per file, including when an account logs out and returns. */
@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val stores = mutableMapOf<String, DataStore<Preferences>>()

    @Synchronized
    fun forUser(userId: String): DataStore<Preferences> {
        val id = canonicalUserId(userId)
        return stores.getOrPut(id) {
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("user_$id") }
        }
    }
}

internal fun canonicalUserId(value: String): String = UUID.fromString(value).toString().also {
    require(it.equals(value, ignoreCase = true)) { "Invalid user ID" }
}

/** A revoked session cannot write PIN or preferences after cleanup. */
class UserAccessGate {
    private val mutex = Mutex()
    @Volatile private var active = true
    val isActive: Boolean get() = active

    suspend fun <T> use(block: suspend () -> T): T = mutex.withLock {
        if (!active) throw CancellationException("Session closed")
        block()
    }

    suspend fun revoke() = mutex.withLock { active = false }
}
