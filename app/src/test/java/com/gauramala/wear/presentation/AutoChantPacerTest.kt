package com.gauramala.wear.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoChantPacerTest {
    @Test
    fun calibrationRejectsInvalidElapsedTimeOrChantCount() {
        assertNull(AutoChantTiming.averagePaceMillis(0L, 1))
        assertNull(AutoChantTiming.averagePaceMillis(-1L, 1))
        assertNull(AutoChantTiming.averagePaceMillis(1_000L, 0))
        assertNull(AutoChantTiming.averagePaceMillis(1_000L, 11))
    }

    @Test
    fun calibrationAveragesSamplesAndClampsToSupportedPaceRange() {
        assertEquals(2_000L, AutoChantTiming.averagePaceMillis(10_000L, 5))
        assertEquals(
            AutoChantTiming.MIN_PACE_MILLIS,
            AutoChantTiming.averagePaceMillis(100L, 1)
        )
        assertEquals(
            AutoChantTiming.MAX_PACE_MILLIS,
            AutoChantTiming.averagePaceMillis(100_000L, 1)
        )
    }

    @Test
    fun threeTrialCalibrationUsesTheMiddlePaceAndRejectsIncompleteOrInvalidSamples() {
        assertTrue(AutoChantTiming.isValidPace(AutoChantTiming.MIN_PACE_MILLIS))
        assertTrue(AutoChantTiming.isValidPace(AutoChantTiming.MAX_PACE_MILLIS))
        assertFalse(AutoChantTiming.isValidPace(AutoChantTiming.MIN_PACE_MILLIS - 1))
        assertFalse(AutoChantTiming.isValidPace(AutoChantTiming.MAX_PACE_MILLIS + 1))

        assertEquals(6_000L, AutoChantTiming.medianPaceMillis(listOf(9_000L, 6_000L, 3_000L)))
        assertNull(AutoChantTiming.medianPaceMillis(listOf(3_000L, 6_000L)))
        assertNull(AutoChantTiming.medianPaceMillis(listOf(3_000L, 6_000L, 16_000L)))
    }

    @Test
    fun pacerRequiresAValidConfiguredPace() {
        val pacer = AutoChantPacer()

        assertFalse(pacer.start(1_000L))
        assertNull(pacer.timeUntilNextCount(1_000L))
        assertFalse(pacer.consumeDueCount(1_000L))
        assertFalse(pacer.configure(AutoChantTiming.MIN_PACE_MILLIS - 1L))
        assertFalse(pacer.configure(AutoChantTiming.MAX_PACE_MILLIS + 1L))
    }

    @Test
    fun pacerCountsAtTheConfiguredIntervalAndDoesNotCatchUpAfterDelay() {
        val pacer = AutoChantPacer()
        assertTrue(pacer.configure(2_000L))
        assertTrue(pacer.start(5_000L))

        assertEquals(2_000L, pacer.timeUntilNextCount(5_000L))
        assertFalse(pacer.consumeDueCount(6_999L))
        assertTrue(pacer.consumeDueCount(7_500L))
        assertEquals(2_000L, pacer.timeUntilNextCount(7_500L))
        assertFalse(pacer.consumeDueCount(9_499L))
        assertTrue(pacer.consumeDueCount(9_500L))
    }

    @Test
    fun pauseStopsScheduledCountsUntilStartedAgain() {
        val pacer = AutoChantPacer()
        pacer.configure(1_000L)
        pacer.start(0L)
        pacer.pause()

        assertFalse(pacer.isRunning)
        assertNull(pacer.timeUntilNextCount(1_000L))
        assertFalse(pacer.consumeDueCount(10_000L))
        assertTrue(pacer.start(10_000L))
        assertEquals(1_000L, pacer.timeUntilNextCount(10_000L))
    }

    @Test
    fun configuringNewPaceCancelsAnExistingSchedule() {
        val pacer = AutoChantPacer()
        pacer.configure(1_000L)
        pacer.start(0L)

        assertTrue(pacer.configure(3_000L))

        assertFalse(pacer.isRunning)
        assertNull(pacer.timeUntilNextCount(1_000L))
    }
}
