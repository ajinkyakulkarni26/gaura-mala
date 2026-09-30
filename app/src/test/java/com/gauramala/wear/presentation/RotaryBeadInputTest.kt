package com.gauramala.wear.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class RotaryBeadInputTest {
    @Test
    fun accumulatesDeltasUntilOneStepAndKeepsRemainder() {
        val input = RotaryBeadInput(thresholdPx = 10f)

        assertEquals(0, input.consume(deltaPx = 4f, eventUptimeMs = 1L))
        assertEquals(0, input.consume(deltaPx = 5f, eventUptimeMs = 20L))
        assertEquals(1, input.consume(deltaPx = 3f, eventUptimeMs = 40L))
        assertEquals(0, input.consume(deltaPx = 7f, eventUptimeMs = 60L))
        assertEquals(1, input.consume(deltaPx = 3f, eventUptimeMs = 80L))
    }

    @Test
    fun returnsMultipleStepsForOneLargeDelta() {
        val input = RotaryBeadInput(thresholdPx = 10f)

        assertEquals(3, input.consume(deltaPx = 35f, eventUptimeMs = 1L))
        assertEquals(0, input.consume(deltaPx = 4f, eventUptimeMs = 20L))
        assertEquals(1, input.consume(deltaPx = 2f, eventUptimeMs = 40L))
    }

    @Test
    fun clearsPartialDeltaAfterIdleGap() {
        val input = RotaryBeadInput(thresholdPx = 10f, idleResetMs = 100L)

        assertEquals(0, input.consume(deltaPx = 7f, eventUptimeMs = 10L))
        assertEquals(0, input.consume(deltaPx = 4f, eventUptimeMs = 111L))
        assertEquals(0, input.consume(deltaPx = 5f, eventUptimeMs = 120L))
        assertEquals(1, input.consume(deltaPx = 1f, eventUptimeMs = 130L))
    }

    @Test
    fun nonForwardDeltaClearsPendingMovement() {
        val input = RotaryBeadInput(thresholdPx = 10f)

        assertEquals(0, input.consume(deltaPx = 7f, eventUptimeMs = 10L))
        assertEquals(0, input.consume(deltaPx = -2f, eventUptimeMs = 20L))
        assertEquals(0, input.consume(deltaPx = 4f, eventUptimeMs = 30L))
    }

    @Test
    fun resetClearsPendingMovementAndEventHistory() {
        val input = RotaryBeadInput(thresholdPx = 10f)

        assertEquals(0, input.consume(deltaPx = 7f, eventUptimeMs = 10L))
        input.reset()

        assertEquals(0, input.consume(deltaPx = 4f, eventUptimeMs = 20L))
        assertEquals(1, input.consume(deltaPx = 6f, eventUptimeMs = 40L))
    }
}
