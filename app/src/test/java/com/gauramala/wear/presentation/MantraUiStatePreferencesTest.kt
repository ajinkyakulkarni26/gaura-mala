package com.gauramala.wear.presentation

import com.gauramala.wear.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MantraUiStatePreferencesTest {
    @Test
    fun initialSnapshotLoadsSavedProgressAndSettings() {
        val state = MantraUiState().withPreferences(
            preferences = UserPreferences(
                beadCount = 37,
                completedRounds = 2,
                dailyGoalRounds = 12,
                gesturePinchEnabled = false
            ),
            preserveLocalProgress = false
        )

        assertEquals(37, state.beadCount)
        assertEquals(2, state.completedRounds)
        assertEquals(12, state.dailyGoalRounds)
        assertFalse(state.isPinchGestureEnabled)
    }

    @Test
    fun storedSnapshotCannotRollBackProgressAfterLocalChange() {
        val state = MantraUiState(
            beadCount = 17,
            completedRounds = 3,
            lastRecordedDate = "2026-09-29"
        ).withPreferences(
            preferences = UserPreferences(
                beadCount = 15,
                completedRounds = 2,
                dailyGoalRounds = 10,
                gesturePinchEnabled = false,
                lastRecordedDate = "2026-09-29"
            ),
            preserveLocalProgress = true
        )

        assertEquals(17, state.beadCount)
        assertEquals(3, state.completedRounds)
        assertEquals(10, state.dailyGoalRounds)
        assertFalse(state.isPinchGestureEnabled)
    }

    @Test
    fun newDaySnapshotResetsProgressAndUndoState() {
        val state = MantraUiState(
            beadCount = 20,
            completedRounds = 4,
            lastRecordedDate = "2026-09-28",
            canUndo = true,
            showGoalAchievedDialog = true
        ).withPreferences(
            preferences = UserPreferences(
                beadCount = 0,
                completedRounds = 0,
                lastRecordedDate = "2026-09-29"
            ),
            preserveLocalProgress = true
        )

        assertEquals(0, state.beadCount)
        assertEquals(0, state.completedRounds)
        assertFalse(state.canUndo)
        assertFalse(state.showGoalAchievedDialog)
        assertEquals("2026-09-29", state.lastRecordedDate)
    }
}
