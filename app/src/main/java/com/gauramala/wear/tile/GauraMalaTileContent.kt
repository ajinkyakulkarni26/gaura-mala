package com.gauramala.wear.tile

import com.gauramala.wear.data.UserPreferences

/** Text shown by the Tile, kept independent from Android so its mapping can be unit-tested. */
internal data class GauraMalaTileContent(
    val roundProgress: String,
    val beadProgress: String,
    val actionLabel: String = "Chant"
) {
    companion object {
        fun from(preferences: UserPreferences) = GauraMalaTileContent(
            roundProgress = "Round ${preferences.completedRounds} / ${preferences.dailyGoalRounds}",
            beadProgress = "${preferences.beadCount} / 108 Beads"
        )
    }
}
