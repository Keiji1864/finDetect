package com.ghostteam.findetect.data.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ghostteam.findetect.data.session.UserDataScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val users: UserDataScopes,
    private val notifications: ReminderNotifications,
) {
    /** Work and notification navigation carry the login instance, not just the selected user. */
    fun start(scope: CoroutineScope) = scope.launch {
        users.current.collectLatest { user ->
            val work = WorkManager.getInstance(context)
            if (user == null) {
                work.cancelUniqueWork(WORK_NAME).result.get()
                notifications.cancel()
            } else {
                user.settings.settings.map { it.dailyReminder.enabled }.distinctUntilChanged()
                    .onEach { enabled ->
                        if (enabled && users.current.value === user) {
                            notifications.createChannel()
                            val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(15, TimeUnit.MINUTES)
                                .setInputData(workDataOf(USER_ID to user.identity.userId, SESSION_ID to user.identity.sessionId)).build()
                            work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request).result.get()
                        } else {
                            work.cancelUniqueWork(WORK_NAME).result.get()
                            notifications.cancel()
                        }
                    }.retryWhen { error, _ ->
                        if (error is CancellationException) throw error
                        delay(30_000)
                        true
                    }.collect()
            }
        }
    }

    companion object {
        const val USER_ID = "reminder_user_id"
        const val SESSION_ID = "reminder_session_id"
        private const val WORK_NAME = "daily-accounting-reminder"
    }
}
