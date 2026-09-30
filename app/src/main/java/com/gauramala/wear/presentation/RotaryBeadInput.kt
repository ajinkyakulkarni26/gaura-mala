package com.gauramala.wear.presentation

/** Converts rotary pixel deltas into bead steps without storing scroll pixels in Compose state. */
internal class RotaryBeadInput(
    private val thresholdPx: Float,
    private val idleResetMs: Long = DEFAULT_IDLE_RESET_MS
) {
    private var accumulatedPx = 0f
    private var lastEventUptimeMs: Long? = null

    init {
        require(thresholdPx > 0f && thresholdPx.isFinite())
        require(idleResetMs >= 0L)
    }

    fun consume(deltaPx: Float, eventUptimeMs: Long): Int {
        val previousEventTime = lastEventUptimeMs
        if (previousEventTime != null &&
            (eventUptimeMs < previousEventTime || eventUptimeMs - previousEventTime > idleResetMs)
        ) {
            accumulatedPx = 0f
        }
        lastEventUptimeMs = eventUptimeMs

        if (!deltaPx.isFinite() || deltaPx <= 0f) {
            accumulatedPx = 0f
            return 0
        }

        accumulatedPx += deltaPx
        val steps = (accumulatedPx / thresholdPx).toInt()
        accumulatedPx -= steps * thresholdPx
        return steps
    }

    fun reset() {
        accumulatedPx = 0f
        lastEventUptimeMs = null
    }

    private companion object {
        const val DEFAULT_IDLE_RESET_MS = 180L
    }
}
