package com.ghostteam.findetect.data.session

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import com.ghostteam.findetect.core.data.LocalDataInitializer
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.local.migration.MIGRATION_1_2
import com.ghostteam.findetect.data.local.migration.MIGRATION_2_3
import com.ghostteam.findetect.data.local.migration.MIGRATION_3_4
import com.ghostteam.findetect.data.local.migration.MIGRATION_4_5
import com.ghostteam.findetect.data.local.migration.MIGRATION_5_6
import com.ghostteam.findetect.data.local.migration.MIGRATION_7_8
import com.ghostteam.findetect.data.local.migration.MIGRATION_6_7
import com.ghostteam.findetect.data.pin.PinStorage
import com.ghostteam.findetect.data.reminder.ReminderNotifications
import com.ghostteam.findetect.data.repository.*
import com.ghostteam.findetect.domain.model.OnboardingStep
import com.ghostteam.findetect.domain.model.SessionIdentity
import com.ghostteam.findetect.domain.usecase.CalculateStreakUseCase
import com.ghostteam.findetect.domain.usecase.CalculateAchievementsUseCase
import com.ghostteam.findetect.data.local.migration.MIGRATION_8_9
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.core.time.LocalDayObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/** Immutable dependencies for one login. Existing ViewModels can never switch to another user's DB. */
class UserDataScope internal constructor(
    val identity: SessionIdentity,
    val database: AppDatabase,
    val gate: UserAccessGate,
    val pin: PinStorage,
    device: DevicePreferences,
    preferences: DataStore<Preferences>,
    clock: Clock,
    calculateStreak: CalculateStreakUseCase,
    calculateAchievements: CalculateAchievementsUseCase,
) {
    private val mutableSyncFailed = MutableStateFlow(false)
    val syncFailed = mutableSyncFailed.asStateFlow()
    internal fun syncFailed(value: Boolean) { mutableSyncFailed.value = value }
    val xp = LocalXpRepository(database, database.xpDao(), clock, calculateStreak)
    val achievements = LocalAchievementRepository(database, database.achievementDao(), clock, calculateAchievements, xp)
    private val history = ForecastHistory(database, clock, achievements) {
        val saved = device.store.data.first()[LocalSettingsRepository.currencyKey]
        AppCurrency.entries.firstOrNull { it.name == saved } ?: AppCurrency.RUB
    }
    internal val exchange = com.ghostteam.findetect.data.sync.RoomExchange(database, gate, history)
    val settings = LocalSettingsRepository(device, preferences, database, gate, achievements, clock, history)
    val accounts = LocalAccountRepository(database, database.accountDao(), database.accountDeletionDao(), clock, history)
    val categories = LocalCategoryRepository(database, database.categoryDao())
    val transactions = LocalTransactionRepository(database, database.transactionDao(), database.accountDao(),
        database.categoryDao(), database.transactionDeletionDao(), clock, history, xp, achievements)
    val adjustments = LocalBalanceAdjustmentRepository(database, database.adjustmentDao(), database.accountDao(),
        database.balanceDao(), database.balanceConfirmationDao(), clock, history)
    val balances = LocalBalanceRepository(database.balanceDao())
    val activity = LocalActivityRepository(database, database.activityDayDao(), clock, history, xp, achievements)

    private val lifetime = SupervisorJob()
    private val background = CoroutineScope(Dispatchers.IO + lifetime)

    /** Keeps completed-day recognition alive without a Home/Profile/Streak navigation owner. */
    internal fun startAchievementChecks(days: LocalDayObserver) {
        background.launch {
            days.observe().onEach {
                gate.use {
                    history.checkpoint()
                    achievements.reconcile()
                }
            }
                .retryWhen { error, _ ->
                    if (error is CancellationException) throw error
                    Log.w("AchievementChecks", "Cannot reconcile achievements; retrying", error)
                    delay(5_000)
                    true
                }.collect()
        }
    }

    internal suspend fun stopAchievementChecks() { lifetime.cancelAndJoin() }

    suspend fun hasPendingChanges(): Boolean = withContext(Dispatchers.IO) {
        database.openHelper.readableDatabase.query("""
            SELECT EXISTS(SELECT 1 FROM accounts WHERE syncStatus != 'SYNCED')
                OR EXISTS(SELECT 1 FROM transactions WHERE syncStatus != 'SYNCED')
                OR EXISTS(SELECT 1 FROM balance_adjustments WHERE syncStatus != 'SYNCED')
                OR EXISTS(SELECT 1 FROM xp_events WHERE syncStatus != 'SYNCED')
                OR EXISTS(SELECT 1 FROM achievements WHERE syncStatus != 'SYNCED')
        """.trimIndent()).use { it.moveToFirst() && it.getInt(0) != 0 }
    }
}

@Singleton
class UserDataScopes @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferences,
    private val device: DevicePreferences,
    private val initializer: LocalDataInitializer,
    private val clock: Clock,
    private val calculateStreak: CalculateStreakUseCase,
    private val calculateAchievements: CalculateAchievementsUseCase,
    private val days: LocalDayObserver,
    private val notifications: ReminderNotifications,
) {
    private val mutableCurrent = MutableStateFlow<UserDataScope?>(null)
    val current = mutableCurrent.asStateFlow()
    fun requireCurrent(): UserDataScope = checkNotNull(current.value) { "No active user data scope" }

    internal suspend fun open(identity: SessionIdentity): UserDataScope = withContext(Dispatchers.IO) {
        val id = canonicalUserId(identity.userId)
        val gate = UserAccessGate()
        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName(id))
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9).build()
        try {
            initializer.initialize(database, id)
            val known = database.openHelper.readableDatabase.query("SELECT id FROM categories").use { rows ->
                    buildSet { while (rows.moveToNext()) add(rows.getString(0)) }
                }
            database.categoryDao().upsertAll(com.ghostteam.findetect.data.local.defaultCategories.filter { it.id !in known })
            val pin = PinStorage(context, id, gate)
            pin.refresh()
            UserDataScope(identity, database, gate, pin, device, preferences.forUser(id),
                clock, calculateStreak, calculateAchievements).also {
                it.settings.initialize()
                it.xp.initialize()
                it.achievements.initialize()
            }
        } catch (error: Exception) {
            database.close()
            throw error
        }
    }

    internal fun publish(scope: UserDataScope) {
        check(current.value == null)
        mutableCurrent.value = scope
        scope.startAchievementChecks(days)
    }

    internal suspend fun close() {
        val previous = current.value ?: return
        mutableCurrent.value = null
        previous.gate.revoke()
        notifications.cancel()
        withContext(NonCancellable + Dispatchers.IO) {
            previous.stopAchievementChecks()
            previous.database.close()
        }
    }

    internal suspend fun onboarding(userId: String, isRegistration: Boolean): OnboardingStep {
        val stored = preferences.forUser(userId).data.first()[onboardingKey]
        return OnboardingStep.entries.firstOrNull { it.name == stored }
            ?: if (isRegistration) OnboardingStep.INCOME else OnboardingStep.DONE
    }

    internal suspend fun saveOnboarding(userId: String, step: OnboardingStep) {
        preferences.forUser(userId).edit { it[onboardingKey] = step.name }
    }

    internal suspend fun resetPin(userId: String) = withContext(Dispatchers.IO) {
        PinStorage.clearStored(context, userId)
        saveOnboarding(userId, OnboardingStep.PIN)
    }

    internal suspend fun deleteUserData(userId: String) = withContext(Dispatchers.IO) {
        val name = databaseName(canonicalUserId(userId))
        if (context.getDatabasePath(name).exists()) check(context.deleteDatabase(name))
        PinStorage.clearStored(context, userId)
        preferences.forUser(userId).edit { it.clear() }
    }

    private fun databaseName(id: String) = "findetective_user_$id.db"
    private companion object { val onboardingKey = stringPreferencesKey("onboarding_step") }
}
