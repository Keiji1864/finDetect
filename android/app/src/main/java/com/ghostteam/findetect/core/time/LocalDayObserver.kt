package com.ghostteam.findetect.core.time

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.ghostteam.findetect.domain.model.LocalDay
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Watches midnight, system clock/zone changes and foreground returns; releases listeners on cancellation. */
class LocalDayObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    fun current(): LocalDay {
        val zone = TimeZone.currentSystemDefault()
        return LocalDay(clock.now().toLocalDateTime(zone).date, zone)
    }

    fun observe(): Flow<LocalDay> = channelFlow {
        val changes = Channel<Unit>(Channel.CONFLATED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                changes.trySend(Unit)
            }
        }
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) { changes.trySend(Unit) }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        val application = context.applicationContext as Application
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        try {
            application.registerActivityLifecycleCallbacks(callbacks)
            try {
                while (isActive) {
                    val zone = TimeZone.currentSystemDefault()
                    val now = clock.now()
                    val today = now.toLocalDateTime(zone).date
                    send(LocalDay(today, zone))
                    val midnight = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
                    val waitMillis = (midnight - now).inWholeMilliseconds.coerceAtLeast(1L)
                    withTimeoutOrNull(waitMillis) { changes.receive() }
                }
            } finally {
                application.unregisterActivityLifecycleCallbacks(callbacks)
            }
        } finally {
            context.unregisterReceiver(receiver)
            changes.close()
        }
    }.distinctUntilChanged()
}
