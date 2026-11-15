package com.ghostteam.findetect

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.navigation.AppNavHost
import com.ghostteam.findetect.core.security.AppLockManager
import com.ghostteam.findetect.data.reminder.ReminderNotifications
import com.ghostteam.findetect.feature.security.AppLockHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var lock: AppLockManager
    private var homeRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lock.onActivityCreated(restored = savedInstanceState != null)
        enableEdgeToEdge()
        homeRequest = savedInstanceState?.getInt(HOME_REQUEST) ?: if (intent.action == ReminderNotifications.ACTION_OPEN_HOME) 1 else 0
        setContent {
            FinDetectTheme {
                AppLockHost(lock, onExit = { moveTaskToBack(true) }) { AppNavHost(homeRequest = homeRequest) }
            }
        }
    }

    override fun onStart() {
        lock.onForeground()
        super.onStart()
    }

    override fun onStop() {
        if (!isChangingConfigurations) lock.onBackground()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ReminderNotifications.ACTION_OPEN_HOME) homeRequest += 1
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(HOME_REQUEST, homeRequest)
        super.onSaveInstanceState(outState)
    }

    private companion object { const val HOME_REQUEST = "notification_home_request" }
}
