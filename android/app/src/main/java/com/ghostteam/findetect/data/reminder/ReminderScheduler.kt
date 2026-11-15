package com.ghostteam.findetect.data.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ghostteam.findetect.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val notifications: ReminderNotifications,
) {
    /** One persistent job reads local date/time each run, including after reboot and time zone changes. */
    fun start(scope: CoroutineScope) = scope.launch {
        settings.settings.map { it.dailyReminder.enabled }.distinctUntilChanged()
            .onEach { enabled ->
                val work = WorkManager.getInstance(context)
                if (enabled) {
                    notifications.createChannel()
                    work.enqueueUniquePeriodicWork(
                        WORK_NAME, ExistingPeriodicWorkPolicy.KEEP,
                        PeriodicWorkRequestBuilder<DailyReminderWorker>(15, TimeUnit.MINUTES).build(),
                    ).result.get()
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

    private companion object { const val WORK_NAME = "daily-accounting-reminder" }
}
