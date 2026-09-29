package com.gauramala.wear.presentation

data class MantraUiState(
    val beadCount: Int = 0,               // 0 to 108
    val completedRounds: Int = 0,         // e.g. 0 to 16+
    val dailyGoalRounds: Int = 16,        // ISKCON standard vow: 16 rounds
    val isPinchGestureEnabled: Boolean = true,
    val isScreenTapEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val isMilestonesEnabled: Boolean = true,
    val keepScreenOn: Boolean = false,
    val isAmbient: Boolean = false,
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
