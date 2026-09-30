package com.gauramala.wear.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MantraCounterStateMachineTest {
    private val machine = MantraCounterStateMachine()

    @Test
    fun initialStateHasNoProgressAndDefaultGoal() {
        val state = MantraUiState()

        assertEquals(0, state.totalMantrasChanted)
        assertEquals(0f, state.roundProgressFraction)
        assertEquals(0f, state.dailyProgressFraction)
        assertEquals(16, state.dailyGoalRounds)
        assertFalse(state.supportsOneHandedGestures)
        assertFalse(state.isGoalAchieved)
    }

    @Test
    fun incrementAdvancesBeadAndProgress() {
        val result = machine.increment(MantraUiState(beadCount = 26))

        assertEquals(27, result.state.beadCount)
        assertEquals(27, result.state.totalMantrasChanted)
        assertEquals(27f / 108f, result.state.roundProgressFraction)
        assertTrue(result.state.canUndo)
        assertFalse(result.completedRound)
        assertFalse(result.goalJustReached)
    }

    @Test
    fun hundredEighthBeadCompletesRoundAndRaisesGoalOnce() {
        val result = machine.increment(MantraUiState(beadCount = 107, dailyGoalRounds = 1))

        assertEquals(0, result.state.beadCount)
        assertEquals(1, result.state.completedRounds)
        assertEquals(108, result.state.totalMantrasChanted)
        assertEquals(1f, result.state.dailyProgressFraction)
        assertTrue(result.state.isGoalAchieved)
        assertTrue(result.state.showGoalAchievedDialog)
        assertTrue(result.state.canUndo)
        assertTrue(result.completedRound)
        assertTrue(result.goalJustReached)
    }

    @Test
    fun laterRoundDoesNotRaiseGoalAgain() {
        val result = machine.increment(
            MantraUiState(beadCount = 107, completedRounds = 1, dailyGoalRounds = 1)
        )

        assertEquals(2, result.state.completedRounds)
        assertFalse(result.goalJustReached)
        assertFalse(result.state.showGoalAchievedDialog)
    }

    @Test
    fun undoRestoresPreviousBeadAndCanOnlyBeUsedOnce() {
        val incremented = machine.increment(MantraUiState(beadCount = 53)).state
        val undone = machine.undo(incremented)

        assertEquals(53, undone.beadCount)
        assertFalse(undone.canUndo)
        assertSame(undone, machine.undo(undone))
    }

    @Test
    fun undoRestoresCompletedRoundAndClearsGoalDialogWhenGoalIsNoLongerMet() {
        val incremented = machine.increment(
            MantraUiState(beadCount = 107, completedRounds = 0, dailyGoalRounds = 1)
        ).state
        val undone = machine.undo(incremented)

        assertEquals(107, undone.beadCount)
        assertEquals(0, undone.completedRounds)
        assertFalse(undone.isGoalAchieved)
        assertFalse(undone.showGoalAchievedDialog)
        assertFalse(undone.canUndo)
    }

    @Test
    fun resetCurrentRoundPreservesCompletedRoundsAndPreferences() {
        val state = MantraUiState(
            beadCount = 44,
            completedRounds = 3,
            dailyGoalRounds = 12,
            isHapticsEnabled = false,
            canUndo = true
        )

        val reset = machine.resetCurrentRound(state)

        assertEquals(0, reset.beadCount)
        assertEquals(3, reset.completedRounds)
        assertEquals(12, reset.dailyGoalRounds)
        assertFalse(reset.isHapticsEnabled)
        assertFalse(reset.canUndo)
    }

    @Test
    fun dailyResetClearsCountsAndDialogButKeepsGoalAndSettings() {
        val state = MantraUiState(
            beadCount = 20,
            completedRounds = 4,
            dailyGoalRounds = 8,
            isPinchGestureEnabled = false,
            showGoalAchievedDialog = true,
            canUndo = true
        )

        val reset = machine.resetDailyCount(state, "2026-09-29")

        assertEquals(0, reset.beadCount)
        assertEquals(0, reset.completedRounds)
        assertEquals(8, reset.dailyGoalRounds)
        assertFalse(reset.isPinchGestureEnabled)
        assertFalse(reset.showGoalAchievedDialog)
        assertFalse(reset.canUndo)
        assertEquals("2026-09-29", reset.lastRecordedDate)
    }

    @Test
    fun dailyGoalAcceptsSupportedRangeAndRejectsOutOfRangeValues() {
        assertEquals(1, machine.updateDailyGoal(MantraUiState(), 1).dailyGoalRounds)
        assertEquals(64, machine.updateDailyGoal(MantraUiState(), 64).dailyGoalRounds)

        val original = MantraUiState(dailyGoalRounds = 16)
        assertSame(original, machine.updateDailyGoal(original, 0))
        assertSame(original, machine.updateDailyGoal(original, 65))
    }
}
