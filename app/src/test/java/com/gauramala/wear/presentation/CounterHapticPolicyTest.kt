package com.gauramala.wear.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterHapticPolicyTest {
    private val stateMachine = MantraCounterStateMachine()

    @Test
    fun gestureKeepsFrameworkFeedbackForOrdinaryBeads() {
        val state = MantraUiState(beadCount = 10)
        val transition = stateMachine.increment(state)

        assertNull(selectHapticCue(state, transition, BeadInputSource.GESTURE))
        assertEquals(HapticCue.BEAD, selectHapticCue(state, transition, BeadInputSource.TAP))
    }

    @Test
    fun gestureGetsMilestoneCueAtQuarterRound() {
        val state = MantraUiState(beadCount = 26)
        val transition = stateMachine.increment(state)

        assertEquals(HapticCue.MILESTONE, selectHapticCue(state, transition, BeadInputSource.GESTURE))
    }

    @Test
    fun gestureGetsRoundCompletionCue() {
        val state = MantraUiState(beadCount = 107, completedRounds = 2)
        val transition = stateMachine.increment(state)

        assertEquals(HapticCue.ROUND_COMPLETED, selectHapticCue(state, transition, BeadInputSource.GESTURE))
    }

    @Test
    fun gestureGetsDailyGoalCueWhenItCompletesTheGoalRound() {
        val state = MantraUiState(beadCount = 107, dailyGoalRounds = 1)
        val transition = stateMachine.increment(state)

        assertEquals(
            HapticCue.DAILY_GOAL_ACHIEVED,
            selectHapticCue(state, transition, BeadInputSource.GESTURE)
        )
    }

    @Test
    fun disabledHapticsSuppressGestureCues() {
        val state = MantraUiState(beadCount = 26, isHapticsEnabled = false)
        val transition = stateMachine.increment(state)

        assertNull(selectHapticCue(state, transition, BeadInputSource.GESTURE))
    }

    @Test
    fun disabledMilestonesKeepNormalGestureFeedback() {
        val state = MantraUiState(beadCount = 26, isMilestonesEnabled = false)
        val transition = stateMachine.increment(state)

        assertNull(selectHapticCue(state, transition, BeadInputSource.GESTURE))
    }
}
