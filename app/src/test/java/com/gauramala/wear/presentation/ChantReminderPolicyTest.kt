package com.gauramala.wear.presentation

import com.gauramala.wear.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ChantReminderPolicyTest {
    @Test
    fun eligibilityRequiresRemindersEnabledProgressStartedGoalIncompleteAndNoActiveAutoCount() {
        assertFalse(
            ChantReminderPolicy.isEligibleForReminder(
                MantraUiState(isRemindersEnabled = false, completedRounds = 4, dailyGoalRounds = 16)
            )
        )
        assertFalse(
            ChantReminderPolicy.isEligibleForReminder(
                MantraUiState(isRemindersEnabled = true, completedRounds = 0, beadCount = 0)
            )
        )
        assertTrue(
            ChantReminderPolicy.isEligibleForReminder(
                MantraUiState(isRemindersEnabled = true, completedRounds = 0, beadCount = 12)
            )
        )
        assertTrue(
            ChantReminderPolicy.isEligibleForReminder(
                MantraUiState(isRemindersEnabled = true, completedRounds = 4, dailyGoalRounds = 16)
            )
        )
        assertFalse(
            ChantReminderPolicy.isEligibleForReminder(
                MantraUiState(isRemindersEnabled = true, completedRounds = 16, dailyGoalRounds = 16)
            )
        )
        assertFalse(
            ChantReminderPolicy.isEligibleForReminder(
                MantraUiState(
                    isRemindersEnabled = true,
                    completedRounds = 4,
                    dailyGoalRounds = 16,
                    autoCountRunning = true
                )
            )
        )
    }

    @Test
    fun nextReminderTimeSchedulesTwoAndAHalfHoursAheadAndRespectsQuietHours() {
        val morning = LocalDateTime.of(2026, 11, 2, 8, 30)
        assertEquals(
            LocalDateTime.of(2026, 11, 2, 11, 0),
            ChantReminderPolicy.nextReminderTime(morning)
        )

        val earlyMorning = LocalDateTime.of(2026, 11, 2, 3, 0)
        assertEquals(
            LocalDateTime.of(2026, 11, 2, 6, 0),
            ChantReminderPolicy.nextReminderTime(earlyMorning)
        )

        val eveningEnteringQuietHours = LocalDateTime.of(2026, 11, 2, 18, 35)
        assertNull(ChantReminderPolicy.nextReminderTime(eveningEnteringQuietHours))

        val lateNightCrossingMidnight = LocalDateTime.of(2026, 11, 2, 22, 30)
        assertNull(ChantReminderPolicy.nextReminderTime(lateNightCrossingMidnight))
    }

    @Test
    fun shouldDeliverReminderNowChecksCurrentDayProgressGoalAndDaytimeWindow() {
        val basePrefs = UserPreferences(
            completedRounds = 4,
            dailyGoalRounds = 16,
            remindersEnabled = true,
            lastRecordedDate = "2026-11-02"
        )
        assertTrue(ChantReminderPolicy.shouldDeliverReminderNow(basePrefs, "2026-11-02", 11))
        assertTrue(
            ChantReminderPolicy.shouldDeliverReminderNow(
                basePrefs.copy(completedRounds = 0, beadCount = 15),
                "2026-11-02",
                6
            )
        )
        assertTrue(
            ChantReminderPolicy.shouldDeliverReminderNow(
                basePrefs,
                "2026-11-02",
                20
            )
        )
        assertFalse(ChantReminderPolicy.shouldDeliverReminderNow(basePrefs, "2026-11-03", 11))
        assertFalse(ChantReminderPolicy.shouldDeliverReminderNow(basePrefs, "2026-11-02", 5))
        assertFalse(ChantReminderPolicy.shouldDeliverReminderNow(basePrefs, "2026-11-02", 21))
        assertFalse(
            ChantReminderPolicy.shouldDeliverReminderNow(
                basePrefs.copy(remindersEnabled = false),
                "2026-11-02",
                11
            )
        )
        assertFalse(
            ChantReminderPolicy.shouldDeliverReminderNow(
                basePrefs.copy(completedRounds = 0, beadCount = 0),
                "2026-11-02",
                11
            )
        )
        assertFalse(
            ChantReminderPolicy.shouldDeliverReminderNow(
                basePrefs.copy(completedRounds = 16),
                "2026-11-02",
                11
            )
        )
    }

    @Test
    fun buildReminderContentRotatesAcrossAllTemplatesWithoutRepeatingConsecutively() {
        var previousIndex = -1
        var previousTitle = ""
        for (expectedIndex in 0 until ChantReminderPolicy.TEMPLATE_COUNT) {
            val content = ChantReminderPolicy.buildReminderContent(
                completedRounds = 4,
                dailyGoalRounds = 16,
                previousTemplateIndex = previousIndex
            )
            assertEquals(expectedIndex, content.templateIndex)
            assertNotEquals(previousTitle, content.title)
            assertTrue(content.body.isNotBlank())
            previousIndex = content.templateIndex
            previousTitle = content.title
        }

        val wrapped = ChantReminderPolicy.buildReminderContent(
            completedRounds = 4,
            dailyGoalRounds = 16,
            previousTemplateIndex = ChantReminderPolicy.TEMPLATE_COUNT - 1
        )
        assertEquals(0, wrapped.templateIndex)

        val defaultFirst = ChantReminderPolicy.buildReminderContent(
            completedRounds = 4,
            dailyGoalRounds = 16
        )
        assertEquals(0, defaultFirst.templateIndex)
    }
}

