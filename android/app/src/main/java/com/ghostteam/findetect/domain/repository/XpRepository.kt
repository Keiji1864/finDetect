package com.ghostteam.findetect.domain.repository

import kotlinx.coroutines.flow.Flow

/** User-scoped lifetime XP. Awards are recorded together with their originating local action. */
interface XpRepository {
    fun observeTotalXp(): Flow<Long>
}
