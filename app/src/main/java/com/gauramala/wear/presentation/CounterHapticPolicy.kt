package com.gauramala.wear.presentation

internal enum class BeadInputSource {
    TAP,
    ROTARY,
    GESTURE
}

internal enum class HapticCue {
    BEAD,
    MILESTONE,
    ROUND_COMPLETED,
    DAILY_GOAL_ACHIEVED
}

internal fun selectHapticCue(
    currentState: MantraUiState,
    transition: CounterTransition,
    source: BeadInputSource
): HapticCue? {
    if (!currentState.isHapticsEnabled) return null

    return when {
        transition.completedRound && transition.goalJustReached -> HapticCue.DAILY_GOAL_ACHIEVED
        transition.completedRound -> HapticCue.ROUND_COMPLETED
        currentState.isMilestonesEnabled && transition.state.beadCount in MILESTONE_BEADS ->
            HapticCue.MILESTONE
        source == BeadInputSource.GESTURE -> null
        else -> HapticCue.BEAD
    }
}

private val MILESTONE_BEADS = setOf(27, 54, 81)
