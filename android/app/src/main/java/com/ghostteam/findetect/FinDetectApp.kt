package com.ghostteam.findetect

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ghostteam.findetect.domain.repository.AuthRepository
import com.ghostteam.findetect.data.reminder.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class FinDetectApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject lateinit var reminderScheduler: ReminderScheduler

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        reminderScheduler.start(applicationScope)
        applicationScope.launch {
            try {
                authRepository.restore()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("FinDetectApp", "Local data initialization failed", error)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
