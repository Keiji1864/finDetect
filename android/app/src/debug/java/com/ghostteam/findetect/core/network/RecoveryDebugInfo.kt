package com.ghostteam.findetect.core.network

import android.content.Context
import com.ghostteam.findetect.BuildConfig
import com.ghostteam.findetect.R
import com.ghostteam.findetect.data.demo.DebugProfileStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Mock-only mailbox preview; never reads or logs real email codes. */
class RecoveryDebugInfo @Inject constructor(@ApplicationContext private val context: Context, private val profiles: DebugProfileStore) {
    suspend fun hint(requestId: String): String? = if (BuildConfig.USE_MOCK_API)
        profiles.previewResetCode(requestId)?.let { context.getString(R.string.recovery_mock_code, it) } else null
}
