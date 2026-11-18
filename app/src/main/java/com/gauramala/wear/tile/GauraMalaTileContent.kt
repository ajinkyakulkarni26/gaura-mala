package com.gauramala.wear.tile

import com.gauramala.wear.data.UserPreferences

/** Text shown by the Tile, kept independent from Android so its mapping can be unit-tested. */
internal data class GauraMalaTileContent(
    val roundProgress: String,
    val beadProgress: String,
    val actionLabel: String
) {
    companion object {
        fun from(preferences: UserPreferences): GauraMalaTileContent {
            val roundProgress = "Round ${preferences.completedRounds} / ${preferences.dailyGoalRounds}"
            return when {
                preferences.autoCountActive && preferences.autoCountRunning -> GauraMalaTileContent(
                    roundProgress = roundProgress,
                    beadProgress = "Auto Count · ${preferences.beadCount} / 108",
                    actionLabel = "Auto Count"
                )
                preferences.autoCountActive -> GauraMalaTileContent(
                    roundProgress = roundProgress,
                    beadProgress = "Paused · ${preferences.beadCount} / 108",
                    actionLabel = "Auto Count"
                )
                else -> GauraMalaTileContent(
                    roundProgress = roundProgress,
                    beadProgress = "${preferences.beadCount} / 108 Beads",
                    actionLabel = "Chant"
                )
            }
        }
    }
}
