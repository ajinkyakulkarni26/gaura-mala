package com.gauramala.wear.presentation

/** Validates a measured chant pace and schedules one count per beat without catch-up bursts. */
internal object AutoChantTiming {
    const val MIN_PACE_MILLIS = 750L
    const val MAX_PACE_MILLIS = 15_000L

    fun averagePaceMillis(totalElapsedMillis: Long, chantCount: Int): Long? {
        if (totalElapsedMillis <= 0L || chantCount !in 1..10) return null
        return (totalElapsedMillis / chantCount).coerceIn(MIN_PACE_MILLIS, MAX_PACE_MILLIS)
    }
}

internal class AutoChantPacer {
    private var paceMillis: Long? = null
    private var nextCountAtMillis: Long? = null

    val isRunning: Boolean
        get() = nextCountAtMillis != null

    fun configure(paceMillis: Long): Boolean {
        if (paceMillis !in AutoChantTiming.MIN_PACE_MILLIS..AutoChantTiming.MAX_PACE_MILLIS) {
            return false
        }
        this.paceMillis = paceMillis
        nextCountAtMillis = null
        return true
    }

    fun start(nowMillis: Long): Boolean {
        val pace = paceMillis ?: return false
        nextCountAtMillis = nowMillis + pace
        return true
    }

    fun pause() {
        nextCountAtMillis = null
    }

    fun timeUntilNextCount(nowMillis: Long): Long? =
        nextCountAtMillis?.let { (it - nowMillis).coerceAtLeast(0L) }

    /** Counts at most once per call; late timer delivery never produces rapid catch-up counts. */
    fun consumeDueCount(nowMillis: Long): Boolean {
        val pace = paceMillis ?: return false
        val nextCountAt = nextCountAtMillis ?: return false
        if (nowMillis < nextCountAt) return false

        nextCountAtMillis = nowMillis + pace
        return true
    }
}
