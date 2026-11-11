package com.ghostteam.findetect.core.data

import javax.inject.Inject
import javax.inject.Singleton

/** Release startup never inserts debug demo fixtures. */
@Singleton
class LocalDataInitializer @Inject constructor() {
    suspend fun initialize() = Unit
}
