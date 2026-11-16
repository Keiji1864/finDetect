package com.ghostteam.findetect.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import com.ghostteam.findetect.core.data.LocalDataInitializer
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.local.migration.MIGRATION_1_2
import com.ghostteam.findetect.data.local.migration.MIGRATION_2_3
import com.ghostteam.findetect.data.local.migration.MIGRATION_3_4
import com.ghostteam.findetect.data.local.migration.MIGRATION_4_5
import com.ghostteam.findetect.data.pin.PinStorage
import com.ghostteam.findetect.data.reminder.ReminderNotifications
import com.ghostteam.findetect.data.repository.*
import com.ghostteam.findetect.domain.model.OnboardingStep
import com.ghostteam.findetect.domain.model.SessionIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/** Immutable dependencies for one login. Existing ViewModels can never switch to another user's DB. */
class UserDataScope internal constructor(
    val identity: SessionIdentity,
    val database: AppDatabase,
    val gate: UserAccessGate,
    val pin: PinStorage,
    val settings: LocalSettingsRepository,
    clock: Clock,
) {
    val accounts = LocalAccountRepository(database, database.accountDao(), database.accountDeletionDao(), clock)
    val categories = LocalCategoryRepository(database, database.categoryDao())
    val transactions = LocalTransactionRepository(database, database.transactionDao(), database.accountDao(),
        database.categoryDao(), database.transactionDeletionDao(), clock)
    val adjustments = LocalBalanceAdjustmentRepository(database, database.adjustmentDao(), database.accountDao(),
        database.balanceDao(), database.balanceConfirmationDao(), clock)
    val balances = LocalBalanceRepository(database.balanceDao())
    val activity = LocalActivityRepository(database, database.activityDayDao(), clock)

    suspend fun hasPendingChanges(): Boolean = withContext(Dispatchers.IO) {
        database.openHelper.readableDatabase.query("""
            SELECT EXISTS(SELECT 1 FROM accounts WHERE syncStatus != 'SYNCED')
                OR EXISTS(SELECT 1 FROM transactions WHERE syncStatus != 'SYNCED')
                OR EXISTS(SELECT 1 FROM balance_adjustments WHERE syncStatus != 'SYNCED')
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
    private val notifications: ReminderNotifications,
) {
    private val mutableCurrent = MutableStateFlow<UserDataScope?>(null)
    val current = mutableCurrent.asStateFlow()
    fun requireCurrent(): UserDataScope = checkNotNull(current.value) { "No active user data scope" }

    internal suspend fun open(identity: SessionIdentity): UserDataScope = withContext(Dispatchers.IO) {
        val id = canonicalUserId(identity.userId)
        val gate = UserAccessGate()
        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName(id))
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
        try {
            initializer.initialize(database, id)
            val pin = PinStorage(context, id, gate)
            pin.refresh()
            UserDataScope(identity, database, gate, pin, LocalSettingsRepository(device, preferences.forUser(id), gate), clock)
        } catch (error: Exception) {
            database.close()
            throw error
        }
    }

    internal fun publish(scope: UserDataScope) { check(current.value == null); mutableCurrent.value = scope }

    internal suspend fun close() {
        val previous = current.value ?: return
        mutableCurrent.value = null
        previous.gate.revoke()
        notifications.cancel()
        withContext(Dispatchers.IO) { previous.database.close() }
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
