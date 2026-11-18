package com.ghostteam.findetect.core.data

import javax.inject.Inject
import javax.inject.Singleton
import com.ghostteam.findetect.data.local.AppDatabase

/** Release startup never inserts debug demo fixtures. */
@Singleton
class LocalDataInitializer @Inject constructor() {
    suspend fun initialize(database: AppDatabase, userId: String) = Unit
}
