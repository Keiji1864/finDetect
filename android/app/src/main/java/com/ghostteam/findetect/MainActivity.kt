package com.ghostteam.findetect

import android.os.Bundle
import android.content.Intent
import android.content.Context
import com.ghostteam.findetect.core.util.ApplicationLanguage
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.navigation.SessionNavHost
import com.ghostteam.findetect.domain.model.SessionIdentity
import androidx.compose.runtime.mutableStateOf
import com.ghostteam.findetect.core.security.AppLockManager
import com.ghostteam.findetect.data.reminder.ReminderNotifications
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var lock: AppLockManager
    @Inject lateinit var reminders: ReminderNotifications
    private var homeRequest by mutableIntStateOf(0)
    private var notificationOwner by mutableStateOf<SessionIdentity?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        applyOverrideConfiguration(ApplicationLanguage.configuration(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminders.refreshExistingChannel()
        lock.onActivityCreated(restored = savedInstanceState != null)
        enableEdgeToEdge()
        homeRequest = savedInstanceState?.getInt(HOME_REQUEST) ?: if (intent.action == ReminderNotifications.ACTION_OPEN_HOME) 1 else 0
        notificationOwner = ownerFrom(intent)
        setContent {
            FinDetectTheme {
                SessionNavHost(lock, homeRequest, notificationOwner, onExit = { moveTaskToBack(true) })
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
        if (intent.action == ReminderNotifications.ACTION_OPEN_HOME) { notificationOwner = ownerFrom(intent); homeRequest += 1 }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(HOME_REQUEST, homeRequest)
        super.onSaveInstanceState(outState)
    }

    private companion object { const val HOME_REQUEST = "notification_home_request" }

    private fun ownerFrom(intent: Intent): SessionIdentity? {
        if (intent.action != ReminderNotifications.ACTION_OPEN_HOME) return null
        val user = intent.getStringExtra(ReminderNotifications.EXTRA_USER_ID) ?: return null
        val session = intent.getStringExtra(ReminderNotifications.EXTRA_SESSION_ID) ?: return null
        return SessionIdentity(user, session)
    }
}
