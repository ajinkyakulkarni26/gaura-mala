package com.gauramala.wear.presentation

import com.gauramala.wear.data.UserPreferences
import java.time.LocalDateTime

internal data class ChantReminderContent(
    val title: String,
    val body: String,
    val templateIndex: Int
)

/**
 * Pure policy for gentle chanting reminders after 2.5 hours of daytime inactivity once a user
 * has started their daily rounds.
 */
internal object ChantReminderPolicy {
    const val REMINDER_DELAY_MINUTES = 150L
    const val QUIET_HOURS_START_HOUR = 21
    const val QUIET_HOURS_END_HOUR = 6
    const val TEMPLATE_COUNT = 5

    fun isEligibleForReminder(state: MantraUiState): Boolean =
        state.isRemindersEnabled &&
            (state.completedRounds > 0 || state.beadCount > 0) &&
            !state.isGoalAchieved &&
            !state.autoCountRunning

    fun shouldDeliverReminderNow(
        preferences: UserPreferences,
        today: String,
        hourOfDay: Int
    ): Boolean =
        preferences.remindersEnabled &&
            preferences.lastRecordedDate == today &&
            (preferences.completedRounds > 0 || preferences.beadCount > 0) &&
            preferences.completedRounds < preferences.dailyGoalRounds &&
            hourOfDay in QUIET_HOURS_END_HOUR until QUIET_HOURS_START_HOUR

    /**
     * Returns the next reminder time on the same calendar day outside quiet hours (9 PM – 6 AM),
     * or null if the reminder would cross into nighttime quiet hours or the next calendar day.
     */
    fun nextReminderTime(from: LocalDateTime): LocalDateTime? {
        val candidate = from.plusMinutes(REMINDER_DELAY_MINUTES)
        if (!candidate.toLocalDate().isEqual(from.toLocalDate())) {
            return null
        }
        if (candidate.hour >= QUIET_HOURS_START_HOUR) {
            return null
        }
        if (candidate.hour < QUIET_HOURS_END_HOUR) {
            return candidate.toLocalDate().atTime(QUIET_HOURS_END_HOUR, 0)
        }
        return candidate
    }

    fun buildReminderContent(
        completedRounds: Int,
        dailyGoalRounds: Int,
        previousTemplateIndex: Int = -1
    ): ChantReminderContent {
        val safeCompleted = completedRounds.coerceAtLeast(0)
        val safeGoal = dailyGoalRounds.coerceAtLeast(1)
        val remainingRounds = (safeGoal - safeCompleted).coerceAtLeast(1)
        val nextIndex = if (previousTemplateIndex < 0) {
            0
        } else {
            (previousTemplateIndex + 1).mod(TEMPLATE_COUNT)
        }

        return when (nextIndex) {
            0 -> ChantReminderContent(
                title = "Time for a round?",
                body = "You're at $safeCompleted of $safeGoal rounds today. Ready for one peaceful round?",
                templateIndex = 0
            )
            1 -> ChantReminderContent(
                title = "Keep your mala flowing",
                body = "$safeCompleted of $safeGoal rounds done! How about chanting one gentle round now?",
                templateIndex = 1
            )
            2 -> ChantReminderContent(
                title = "One more round today?",
                body = "$remainingRounds rounds left for your daily goal. Let's chant a round with joy!",
                templateIndex = 2
            )
            3 -> ChantReminderContent(
                title = "Pause for a chant?",
                body = "Could now be a good moment for a round? Every chant brings you closer to $safeGoal.",
                templateIndex = 3
            )
            else -> ChantReminderContent(
                title = "Chanted on your beads?",
                body = "At $safeCompleted of $safeGoal on watch. Chant a round now, or tap below if you finished on your mala!",
                templateIndex = 4
            )
        }
    }
}

