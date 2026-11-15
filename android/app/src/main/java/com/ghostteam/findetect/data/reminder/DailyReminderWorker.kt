package com.ghostteam.findetect.data.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ghostteam.findetect.domain.repository.ActivityRepository
import com.ghostteam.findetect.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** WorkManager is approximate. A late job outside the chosen hour is skipped, never sent at night. */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val settings: SettingsRepository,
    private val activity: ActivityRepository,
    private val notifications: ReminderNotifications,
    private val clock: Clock,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        val reminder = settings.settings.first().dailyReminder
        if (reminder.enabled) notifications.createChannel()
        val zone = TimeZone.currentSystemDefault()
        val localNow = clock.now().toLocalDateTime(zone)
        if (!reminder.enabled || localNow.hour != reminder.hour || !notifications.allowed() || isStopped) {
            Result.success()
        } else {
            // Read again before the durable claim: an in-flight settings change must not send an old reminder.
            val latest = settings.settings.first().dailyReminder
            if (latest == reminder && notifications.allowed() && !isStopped &&
                activity.claimDailyReminder(localNow.date, zone)) {
                val current = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
                if (current.date == localNow.date && current.hour == reminder.hour && !isStopped &&
                    settings.settings.first().dailyReminder == reminder) notifications.show()
            }
            Result.success()
        }
    } catch (error: CancellationException) { throw error }
    catch (_: Exception) { Result.retry() }
}
