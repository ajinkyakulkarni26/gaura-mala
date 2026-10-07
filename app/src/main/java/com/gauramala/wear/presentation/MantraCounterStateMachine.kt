package com.gauramala.wear.presentation

/** Pure counter transitions, kept separate from Android and persistence for straightforward testing. */
internal class MantraCounterStateMachine {
    private var previousBeadCount = 0
    private var previousCompletedRounds = 0

    fun increment(state: MantraUiState): CounterTransition {
        previousBeadCount = state.beadCount
        previousCompletedRounds = state.completedRounds

        if (state.beadCount < LAST_BEAD_COUNT) {
            val nextBead = state.beadCount + 1
            return CounterTransition(
                state = state.copy(beadCount = nextBead, canUndo = true),
                completedRound = false
            )
        }

        val nextRounds = state.completedRounds + 1
        val goalJustReached = !state.isGoalAchieved && nextRounds >= state.dailyGoalRounds
        return CounterTransition(
            state = state.copy(
                beadCount = 0,
                completedRounds = nextRounds,
                canUndo = true,
                showGoalAchievedDialog = goalJustReached
            ),
            completedRound = true,
            goalJustReached = goalJustReached
        )
    }

    fun undo(state: MantraUiState): MantraUiState {
        if (!state.canUndo) return state
        return state.copy(
            beadCount = previousBeadCount,
            completedRounds = previousCompletedRounds,
            showGoalAchievedDialog = state.showGoalAchievedDialog &&
                previousCompletedRounds >= state.dailyGoalRounds,
            canUndo = false
        )
    }

    fun resetCurrentRound(state: MantraUiState): MantraUiState {
        clearUndoHistory()
        return state.copy(beadCount = 0, canUndo = false)
    }

    fun setCompletedRounds(state: MantraUiState, completedRounds: Int): MantraUiState {
        if (completedRounds !in MIN_COMPLETED_ROUNDS..MAX_COMPLETED_ROUNDS) return state
        clearUndoHistory()
        return state.copy(
            beadCount = 0,
            completedRounds = completedRounds,
            canUndo = false,
            showGoalAchievedDialog = false
        )
    }

    fun resetDailyCount(state: MantraUiState, recordedDate: String): MantraUiState {
        clearUndoHistory()
        return state.copy(
            beadCount = 0,
            completedRounds = 0,
            canUndo = false,
            showGoalAchievedDialog = false,
            lastRecordedDate = recordedDate
        )
    }

    fun updateDailyGoal(state: MantraUiState, newGoal: Int): MantraUiState =
        if (newGoal in MIN_DAILY_GOAL..MAX_DAILY_GOAL) {
            state.copy(dailyGoalRounds = newGoal)
        } else {
            state
        }

    fun clearUndoHistory() {
        previousBeadCount = 0
        previousCompletedRounds = 0
    }

    private companion object {
        const val LAST_BEAD_COUNT = 107
        const val MIN_DAILY_GOAL = 1
        const val MAX_DAILY_GOAL = 64
        const val MIN_COMPLETED_ROUNDS = 0
        const val MAX_COMPLETED_ROUNDS = 999
    }
}

internal data class CounterTransition(
    val state: MantraUiState,
    val completedRound: Boolean,
    val goalJustReached: Boolean = false
)
