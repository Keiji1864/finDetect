package com.ghostteam.findetect.data.repository

import androidx.room.withTransaction
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.local.dao.AchievementDao
import com.ghostteam.findetect.data.local.entity.AchievementEntity
import com.ghostteam.findetect.data.local.entity.AchievementStateEntity
import com.ghostteam.findetect.data.local.entity.AchievementTransactionFactEntity
import com.ghostteam.findetect.domain.model.AchievementActivityDay
import com.ghostteam.findetect.domain.model.AchievementCandidate
import com.ghostteam.findetect.domain.model.AchievementCode
import com.ghostteam.findetect.domain.model.AchievementTransactionFact
import com.ghostteam.findetect.domain.model.EarnedAchievement
import com.ghostteam.findetect.domain.model.AchievementEvaluation
import com.ghostteam.findetect.domain.model.AchievementProgress
import com.ghostteam.findetect.domain.model.AchievementRules
import com.ghostteam.findetect.domain.model.AchievementsProgress
import com.ghostteam.findetect.domain.repository.AchievementRepository
import com.ghostteam.findetect.domain.usecase.CalculateAchievementsUseCase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class LocalAchievementRepository @Inject constructor(
    private val database: AppDatabase,
    private val dao: AchievementDao,
    private val clock: Clock,
    private val calculate: CalculateAchievementsUseCase,
    private val xp: LocalXpRepository,
) : AchievementRepository {
    override fun observeEarned() = dao.observeEarned().map { rows ->
        rows.map { EarnedAchievement(it.code, it.earnedAt, LocalDate.parse(it.earnedOn), it.isLegacy,
            rewardPending = !it.isLegacy && it.xpEventId == null) }
    }.distinctUntilChanged()

    override fun observeProgress(today: LocalDate) = database.invalidationTracker.createFlow(
        "achievements", "achievement_state", "activity_days", "achievement_transaction_facts",
    ).map {
        // Read all evidence inside one transaction, rather than combining results from different commits.
        database.withTransaction {
            val state = checkNotNull(dao.getState()) { "Achievements have not been initialized" }
            val evaluation = evaluate(state, today)
            val earned = dao.getEarned().associateBy { it.code }
            AchievementsProgress(AchievementCode.entries.map { code ->
                val row = earned[code]
                val achievement = row?.let {
                    EarnedAchievement(it.code, it.earnedAt, LocalDate.parse(it.earnedOn), it.isLegacy,
                        rewardPending = !it.isLegacy && it.xpEventId == null)
                }
                val target = AchievementRules.target(code)
                AchievementProgress(code, achievement, target?.let {
                    if (achievement != null) it else evaluation.progress.getValue(code).coerceIn(0, it)
                })
            })
        }
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    /** Recognizes only provable legacy conditions and freezes the boundary before accepting writes. */
    internal suspend fun initialize() = database.withTransaction {
        val state = dao.getState()
        if (state != null) {
            reconcileInside(state)
            return@withTransaction
        }
        val now = clock.now()
        val zone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(zone).date
        val activity = database.activityDayDao().getActiveDays()
        val hadUntrackedTransactionsToday = activity.any { it.date == today.toString() && it.hadTransaction } ||
            database.activityDayDao().hasEnteredTransaction(today.atStartOfDayIn(zone), today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone))
        val freshStart = if (hadUntrackedTransactionsToday) today.plus(1, DateTimeUnit.DAY) else today
        dao.insertState(AchievementStateEntity(startedAt = now, startedOn = today.toString(), zoneId = zone.id,
            freshStartDate = freshStart.toString()))
        val days = activity.map { AchievementActivityDay(LocalDate.parse(it.date), it.enteredAt) }
        val legacy = calculate(days, emptyList(), today, freshStart).candidates.toMutableList()
        val firstTransaction = database.transactionDao().getFirstEntered(now)
        if (firstTransaction != null) {
            legacy += AchievementCandidate(AchievementCode.FIRST_CLUE,
                firstTransaction.enteredAt.toLocalDateTime(zone).date, firstTransaction.enteredAt)
        } else {
            // Activity survives hard deletion as well; its timestamp is the earliest surviving proof.
            activity.firstOrNull { it.hadTransaction && LocalDate.parse(it.date) <= today }?.let {
                legacy += AchievementCandidate(AchievementCode.FIRST_CLUE, LocalDate.parse(it.date), it.enteredAt)
            }
        }
        for (candidate in legacy) saveCandidate(candidate, isLegacy = true, now = now, today = today)
        awardPendingRewards()
    }

    internal suspend fun recordIncomeSchedule(today: LocalDate, now: Instant) = database.withTransaction {
        saveCandidate(AchievementCandidate(AchievementCode.FORECASTER, today, now), false, now, today)
        awardPendingRewards()
    }

    internal suspend fun recordForecastReward(code: AchievementCode, date: LocalDate, now: Instant) {
        saveCandidate(AchievementCandidate(code, date, now), false, now, date)
        awardPendingRewards()
    }

    internal suspend fun recordTransaction(id: String, enteredOn: LocalDate, transactionOn: LocalDate, zone: TimeZone, at: Instant) =
        database.withTransaction {
            val state = checkNotNull(dao.getState()) { "Achievements must be initialized before user actions" }
            dao.insertTransactionFact(AchievementTransactionFactEntity(id, enteredOn.toString(), transactionOn.toString(), zone.id, at))
            reconcileInside(state)
        }

    /** Called after activity changes, at calendar changes, and on startup; no screen dependency. */
    internal suspend fun reconcile() = database.withTransaction {
        reconcileInside(checkNotNull(dao.getState()) { "Achievements must be initialized before reconciliation" })
    }

    private suspend fun reconcileInside(state: AchievementStateEntity) {
        val now = clock.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val result = evaluate(state, today)
        for (candidate in result.candidates) saveCandidate(candidate, isLegacy = false, now = now, today = today)
        awardPendingRewards()
    }

    private suspend fun evaluate(state: AchievementStateEntity, today: LocalDate): AchievementEvaluation {
        val activity = database.activityDayDao().getActiveDays()
            .map { AchievementActivityDay(LocalDate.parse(it.date), it.enteredAt) }
        val facts = dao.getTransactionFacts().map {
            AchievementTransactionFact(it.transactionId, LocalDate.parse(it.enteredOn), LocalDate.parse(it.transactionOn), it.zoneId, it.enteredAt)
        }
        return calculate(activity, facts, today, LocalDate.parse(state.freshStartDate))
    }

    private suspend fun awardPendingRewards() {
        for (achievement in dao.getPendingRewards()) {
            val eventId = xp.recordAchievement(achievement.code, LocalDate.parse(achievement.earnedOn), achievement.earnedAt)
            check(dao.markRewarded(achievement.code, eventId) == 1) { "Achievement reward changed during transaction" }
        }
    }

    private suspend fun saveCandidate(candidate: AchievementCandidate, isLegacy: Boolean, now: Instant, today: LocalDate) {
        // Completed-day conditions are timestamped when recognized, after their qualifying day ended.
        dao.insertEarned(AchievementEntity(
            code = candidate.code,
            earnedAt = candidate.occurredAt ?: now,
            earnedOn = if (candidate.occurredAt == null) today.toString() else candidate.qualifyingDate.toString(),
            isLegacy = isLegacy,
        ))
    }
}
