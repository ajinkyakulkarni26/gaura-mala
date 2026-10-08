package com.gauramala.wear.tile

import com.gauramala.wear.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class GauraMalaTileContentTest {

    @Test
    fun defaultPreferencesShowZeroRoundsAndBeads() {
        val content = GauraMalaTileContent.from(UserPreferences())

        assertEquals("Round 0 / 16", content.roundProgress)
        assertEquals("0 / 108 Beads", content.beadProgress)
        assertEquals("Chant", content.actionLabel)
    }

    @Test
    fun partialProgressShowsSavedRoundsAndBeads() {
        val content = GauraMalaTileContent.from(
            UserPreferences(completedRounds = 4, beadCount = 37, dailyGoalRounds = 12)
        )

        assertEquals("Round 4 / 12", content.roundProgress)
        assertEquals("37 / 108 Beads", content.beadProgress)
    }

    @Test
    fun completedDailyGoalRemainsVisibleInTileSummary() {
        val content = GauraMalaTileContent.from(
            UserPreferences(completedRounds = 16, beadCount = 0, dailyGoalRounds = 16)
        )

        assertEquals("Round 16 / 16", content.roundProgress)
        assertEquals("0 / 108 Beads", content.beadProgress)
    }
}
