package com.ghostteam.findetect.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ghostteam.findetect.MainActivity
import com.ghostteam.findetect.core.util.ApplicationLanguage
import com.ghostteam.findetect.R
import com.ghostteam.findetect.domain.model.SessionIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotifications @Inject constructor(@ApplicationContext private val context: Context) {
    fun refreshExistingChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL) != null) {
            createChannel()
        }
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val strings = ApplicationLanguage.localized(context)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, strings.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = strings.getString(R.string.reminder_channel_description)
                },
            )
        }
    }

    fun allowed(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 26 || context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun settingsIntent(): Intent = if (Build.VERSION.SDK_INT >= 26) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))

    @Suppress("MissingPermission") // Rechecked immediately before posting, including the channel switch.
    fun show(owner: SessionIdentity) {
        if (!allowed()) return
        val strings = ApplicationLanguage.localized(context)
        val intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_HOME)
            .putExtra(EXTRA_USER_ID, owner.userId)
            .putExtra(EXTRA_SESSION_ID, owner.sessionId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(context, 20, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_wallet)
            .setContentTitle(strings.getString(R.string.reminder_title))
            .setContentText(strings.getString(R.string.reminder_message))
            .setStyle(NotificationCompat.BigTextStyle().bigText(strings.getString(R.string.reminder_message)))
            .setContentIntent(pendingIntent).setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun cancel() = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    companion object {
        const val ACTION_OPEN_HOME = "com.ghostteam.findetect.OPEN_HOME_FROM_REMINDER"
        const val EXTRA_USER_ID = "notification_user_id"
        const val EXTRA_SESSION_ID = "notification_session_id"
        private const val CHANNEL = "daily_accounting"
        private const val NOTIFICATION_ID = 20
    }
}
