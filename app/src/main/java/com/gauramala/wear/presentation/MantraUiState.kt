package com.gauramala.wear.presentation

import com.gauramala.wear.data.UserPreferences

data class MantraUiState(
    val beadCount: Int = 0,               // 0 to 107; 108 completes the round
    val completedRounds: Int = 0,         // e.g. 0 to 16+
    val dailyGoalRounds: Int = 16,        // ISKCON standard vow: 16 rounds
    val isPinchGestureEnabled: Boolean = true,
    val supportsOneHandedGestures: Boolean = false,
    val isScreenTapEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val isMilestonesEnabled: Boolean = true,
    val keepScreenOn: Boolean = false,
    val lastRecordedDate: String = "",
    val canUndo: Boolean = false,
    val showGoalAchievedDialog: Boolean = false
) {
    val totalMantrasChanted: Int
        get() = (completedRounds * 108) + beadCount

    val roundProgressFraction: Float
        get() = (beadCount.toFloat() / 108f).coerceIn(0f, 1f)

    val dailyProgressFraction: Float
        get() = if (dailyGoalRounds > 0) {
            (completedRounds.toFloat() / dailyGoalRounds.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val isGoalAchieved: Boolean
        get() = completedRounds >= dailyGoalRounds
}

/** Merges a stored snapshot without letting delayed writes roll back active in-memory progress. */
internal fun MantraUiState.withPreferences(
    preferences: UserPreferences,
    preserveLocalProgress: Boolean
): MantraUiState {
    val isNewDay = lastRecordedDate.isNotEmpty() &&
        lastRecordedDate != preferences.lastRecordedDate
    val useSavedProgress = !preserveLocalProgress || isNewDay

    return copy(
        beadCount = if (useSavedProgress) preferences.beadCount else beadCount,
        completedRounds = if (useSavedProgress) preferences.completedRounds else completedRounds,
        dailyGoalRounds = preferences.dailyGoalRounds,
        isPinchGestureEnabled = preferences.gesturePinchEnabled,
        isScreenTapEnabled = preferences.screenTapEnabled,
        isHapticsEnabled = preferences.hapticFeedbackEnabled,
        isMilestonesEnabled = preferences.milestoneVibrationsEnabled,
        keepScreenOn = preferences.keepScreenOn,
        lastRecordedDate = preferences.lastRecordedDate,
        canUndo = if (isNewDay) false else canUndo,
        showGoalAchievedDialog = if (isNewDay) false else showGoalAchievedDialog
    )
}
