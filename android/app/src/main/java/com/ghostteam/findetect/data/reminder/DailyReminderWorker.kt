package com.ghostteam.findetect.data.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ghostteam.findetect.data.session.DevicePreferences
import com.ghostteam.findetect.data.session.UserDataScopes
import com.ghostteam.findetect.domain.model.SessionState
import com.ghostteam.findetect.domain.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Late jobs and jobs from another login do nothing. Quota is shared by all accounts on the device. */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val users: UserDataScopes,
    private val auth: AuthRepository,
    private val device: DevicePreferences,
    private val notifications: ReminderNotifications,
    private val clock: Clock,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        if (auth.state.value == SessionState.Restoring) return Result.retry()
        val user = users.current.value ?: return Result.success()
        if (inputData.getString(ReminderScheduler.USER_ID) != user.identity.userId ||
            inputData.getString(ReminderScheduler.SESSION_ID) != user.identity.sessionId) return Result.success()
        return try {
            user.gate.use {
                fun current() = users.current.value === user && !isStopped
                val reminder = user.settings.settings.first().dailyReminder
                if (reminder.enabled) notifications.createChannel()
                val zone = TimeZone.currentSystemDefault()
                val localNow = clock.now().toLocalDateTime(zone)
                if (current() && reminder.enabled && localNow.hour == reminder.hour && notifications.allowed() &&
                    user.settings.settings.first().dailyReminder == reminder &&
                    user.activity.claimDailyReminder(localNow.date, zone) && device.claimReminder(localNow.date.toString())) {
                    val now = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    if (current() && now.date == localNow.date && now.hour == reminder.hour &&
                        user.settings.settings.first().dailyReminder == reminder) notifications.show(user.identity)
                }
                Result.success()
            }
        } catch (error: CancellationException) {
            if (users.current.value !== user) Result.success() else throw error
        } catch (_: Exception) { Result.retry() }
    }
}
