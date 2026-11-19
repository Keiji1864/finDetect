package com.ghostteam.findetect.core.network

import javax.inject.Inject

/** Production has no mock mailbox, fixed code, or verification bypass. */
class RecoveryDebugInfo @Inject constructor() {
    suspend fun hint(@Suppress("UNUSED_PARAMETER") requestId: String): String? = null
}
