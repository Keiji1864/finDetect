package com.ghostteam.findetect.feature.achievements

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.ghostteam.findetect.R
import com.ghostteam.findetect.domain.model.AchievementCode
import com.ghostteam.findetect.domain.model.AchievementRules

@StringRes
internal fun AchievementCode.nameRes(): Int = when (this) {
    AchievementCode.FIRST_CLUE -> R.string.achievement_first_clue
    AchievementCode.STREAK_3 -> R.string.achievement_streak_3
    AchievementCode.STREAK_7 -> R.string.achievement_streak_7
    AchievementCode.STREAK_14 -> R.string.achievement_streak_14
    AchievementCode.STREAK_30 -> R.string.achievement_streak_30
    AchievementCode.STREAK_100 -> R.string.achievement_streak_100
    AchievementCode.BACK_IN_ACTION -> R.string.achievement_back_in_action
    AchievementCode.FRESH_CLUES -> R.string.achievement_fresh_clues
    AchievementCode.WITHIN_LIMIT -> R.string.achievement_within_limit
    AchievementCode.REACHED_INCOME -> R.string.achievement_reached_income
    AchievementCode.FORECASTER -> R.string.achievement_forecaster
}

@DrawableRes
internal fun AchievementCode.iconRes(): Int = when (this) {
    AchievementCode.FIRST_CLUE -> R.drawable.ic_trophy
    AchievementCode.STREAK_3, AchievementCode.STREAK_7, AchievementCode.STREAK_14,
    AchievementCode.STREAK_30, AchievementCode.STREAK_100 -> R.drawable.ic_flame
    AchievementCode.BACK_IN_ACTION -> R.drawable.ic_spark
    AchievementCode.FRESH_CLUES, AchievementCode.FORECASTER -> R.drawable.ic_calendar
    AchievementCode.WITHIN_LIMIT -> R.drawable.ic_chart
    AchievementCode.REACHED_INCOME -> R.drawable.ic_wallet
}

@Composable
internal fun achievementCondition(code: AchievementCode): String {
    AchievementRules.streakTargets[code]?.let { target ->
        return pluralStringResource(R.plurals.achievement_streak_condition, target, target)
    }
    return stringResource(when (code) {
        AchievementCode.FIRST_CLUE -> R.string.achievement_first_clue_condition
        AchievementCode.BACK_IN_ACTION -> R.string.achievement_back_in_action_condition
        AchievementCode.FRESH_CLUES -> R.string.achievement_fresh_clues_condition
        AchievementCode.WITHIN_LIMIT -> R.string.achievement_within_limit_condition
        AchievementCode.REACHED_INCOME -> R.string.achievement_reached_income_condition
        AchievementCode.FORECASTER -> R.string.achievement_forecaster_condition
        else -> error("Streak condition must have a configured target")
    })
}
