package com.gauramala.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gauramala.wear.AutoCountSessionStore
import com.gauramala.wear.WearSurfaceUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gaura_mala_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

data class UserPreferences(
    val beadCount: Int = 0,
    val completedRounds: Int = 0,
    val dailyGoalRounds: Int = 16,
    val gesturePinchEnabled: Boolean = true,
    val screenTapEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val milestoneVibrationsEnabled: Boolean = true,
    val keepScreenOn: Boolean = false,
    val remindersEnabled: Boolean = false,
    val lastReminderTemplateIndex: Int = -1,
    val lastRecordedDate: String = "",
    val autoChantPaceMillis: Int? = null,
    val autoCountActive: Boolean = false,
    val autoCountRunning: Boolean = false,
    val autoCountAnchorEpochMillis: Long = 0L,
    val autoCountIntervalMillis: Int? = null
)

class MantraPreferences(private val context: Context) {

    companion object {
        val KEY_BEAD_COUNT = intPreferencesKey("key_bead_count")
        val KEY_COMPLETED_ROUNDS = intPreferencesKey("key_completed_rounds")
        val KEY_DAILY_GOAL = intPreferencesKey("key_daily_goal")
        val KEY_GESTURE_PINCH = booleanPreferencesKey("key_gesture_pinch")
        val KEY_SCREEN_TAP = booleanPreferencesKey("key_screen_tap")
        val KEY_HAPTIC_ENABLED = booleanPreferencesKey("key_haptic_enabled")
        val KEY_MILESTONE_ENABLED = booleanPreferencesKey("key_milestone_enabled")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("key_keep_screen_on")
        val KEY_REMINDERS_ENABLED = booleanPreferencesKey("key_reminders_enabled")
        val KEY_LAST_REMINDER_TEMPLATE_INDEX = intPreferencesKey("key_last_reminder_template_index")
        val KEY_LAST_DATE = stringPreferencesKey("key_last_date")
        val KEY_AUTO_CHANT_PACE_MILLIS = intPreferencesKey("key_auto_chant_pace_millis")
        val KEY_AUTO_COUNT_ACTIVE = booleanPreferencesKey("key_auto_count_active")
        val KEY_AUTO_COUNT_RUNNING = booleanPreferencesKey("key_auto_count_running")
        val KEY_AUTO_COUNT_ANCHOR_EPOCH_MILLIS = longPreferencesKey("key_auto_count_anchor_epoch_millis")
        val KEY_AUTO_COUNT_INTERVAL_MILLIS = intPreferencesKey("key_auto_count_interval_millis")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val isAutoCountActive = preferences[KEY_AUTO_COUNT_ACTIVE] ?: false
        val isAutoCountRunning = isAutoCountActive && (preferences[KEY_AUTO_COUNT_RUNNING] ?: false)
        UserPreferences(
            beadCount = preferences[KEY_BEAD_COUNT] ?: 0,
            completedRounds = preferences[KEY_COMPLETED_ROUNDS] ?: 0,
            dailyGoalRounds = preferences[KEY_DAILY_GOAL] ?: 16,
            gesturePinchEnabled = preferences[KEY_GESTURE_PINCH] ?: true,
            screenTapEnabled = preferences[KEY_SCREEN_TAP] ?: true,
            hapticFeedbackEnabled = preferences[KEY_HAPTIC_ENABLED] ?: true,
            milestoneVibrationsEnabled = preferences[KEY_MILESTONE_ENABLED] ?: true,
            keepScreenOn = preferences[KEY_KEEP_SCREEN_ON] ?: false,
            remindersEnabled = preferences[KEY_REMINDERS_ENABLED] ?: false,
            lastReminderTemplateIndex = preferences[KEY_LAST_REMINDER_TEMPLATE_INDEX] ?: -1,
            lastRecordedDate = preferences[KEY_LAST_DATE] ?: "",
            autoChantPaceMillis = preferences[KEY_AUTO_CHANT_PACE_MILLIS]?.takeIf { it > 0 },
            autoCountActive = isAutoCountActive,
            autoCountRunning = isAutoCountRunning,
            autoCountAnchorEpochMillis = if (isAutoCountRunning) {
                preferences[KEY_AUTO_COUNT_ANCHOR_EPOCH_MILLIS] ?: 0L
            } else {
                0L
            },
            autoCountIntervalMillis = preferences[KEY_AUTO_COUNT_INTERVAL_MILLIS]?.takeIf { it > 0 }
                ?: preferences[KEY_AUTO_CHANT_PACE_MILLIS]?.takeIf { it > 0 }
        )
    }

    /** Starts a fresh daily count when saved progress belongs to an earlier local calendar day. */
    suspend fun resetIfNewDay(today: String = LocalDate.now().toString()) {
        val keepRunningAutoCount = AutoCountSessionStore.peek()?.uiState?.value?.autoCountRunning == true
        var didReset = false
        context.dataStore.edit { prefs ->
            if (prefs[KEY_LAST_DATE] != today) {
                prefs[KEY_BEAD_COUNT] = 0
                prefs[KEY_COMPLETED_ROUNDS] = 0
                if (keepRunningAutoCount) {
                    prefs[KEY_AUTO_COUNT_ACTIVE] = true
                    prefs[KEY_AUTO_COUNT_RUNNING] = true
                    prefs[KEY_AUTO_COUNT_ANCHOR_EPOCH_MILLIS] = System.currentTimeMillis()
                } else {
                    prefs[KEY_AUTO_COUNT_ACTIVE] = false
                    prefs[KEY_AUTO_COUNT_RUNNING] = false
                    prefs.remove(KEY_AUTO_COUNT_ANCHOR_EPOCH_MILLIS)
                    prefs.remove(KEY_AUTO_COUNT_INTERVAL_MILLIS)
                }
                prefs[KEY_LAST_DATE] = today
                didReset = true
            }
        }
        if (didReset) WearSurfaceUpdater.requestUpdate(context)
    }

    suspend fun saveCounts(
        beadCount: Int,
        completedRounds: Int,
        autoCountAnchorEpochMillis: Long? = null
    ) {
        val today = LocalDate.now().toString()
        var roundsChanged = false
        context.dataStore.edit { prefs ->
            roundsChanged = (prefs[KEY_COMPLETED_ROUNDS] ?: 0) != completedRounds
            prefs[KEY_BEAD_COUNT] = beadCount
            prefs[KEY_COMPLETED_ROUNDS] = completedRounds
            if (autoCountAnchorEpochMillis != null && autoCountAnchorEpochMillis > 0L) {
                prefs[KEY_AUTO_COUNT_ANCHOR_EPOCH_MILLIS] = autoCountAnchorEpochMillis
            }
            prefs[KEY_LAST_DATE] = today
        }
        if (roundsChanged || autoCountAnchorEpochMillis != null) {
            WearSurfaceUpdater.requestUpdate(
                context,
                updateComplications = roundsChanged,
                forceTileUpdate = roundsChanged
            )
        }
    }

    suspend fun updateDailyGoal(goal: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DAILY_GOAL] = goal
        }
        WearSurfaceUpdater.requestUpdate(context)
    }

    suspend fun updateAutoChantPace(paceMillis: Int?) {
        context.dataStore.edit { prefs ->
            if (paceMillis == null) {
                prefs.remove(KEY_AUTO_CHANT_PACE_MILLIS)
            } else {
                prefs[KEY_AUTO_CHANT_PACE_MILLIS] = paceMillis
            }
        }
    }

    suspend fun updateAutoCountState(
        active: Boolean,
        running: Boolean,
        anchorEpochMillis: Long = 0L,
        intervalMillis: Int? = null,
        requestSurfaceUpdate: Boolean = true
    ) {
        val effectiveRunning = active && running
        var changed = false
        context.dataStore.edit { prefs ->
            val prevActive = prefs[KEY_AUTO_COUNT_ACTIVE] ?: false
            val prevRunning = prefs[KEY_AUTO_COUNT_RUNNING] ?: false
            changed = prevActive != active || prevRunning != effectiveRunning
            prefs[KEY_AUTO_COUNT_ACTIVE] = active
            prefs[KEY_AUTO_COUNT_RUNNING] = effectiveRunning
            if (active && prefs[KEY_LAST_DATE].isNullOrEmpty()) {
                prefs[KEY_LAST_DATE] = LocalDate.now().toString()
            }
            if (effectiveRunning && anchorEpochMillis > 0L) {
                prefs[KEY_AUTO_COUNT_ANCHOR_EPOCH_MILLIS] = anchorEpochMillis
            } else if (!effectiveRunning) {
                prefs.remove(KEY_AUTO_COUNT_ANCHOR_EPOCH_MILLIS)
            }
            if (effectiveRunning && intervalMillis != null && intervalMillis > 0) {
                prefs[KEY_AUTO_COUNT_INTERVAL_MILLIS] = intervalMillis
            } else if (!active) {
                prefs.remove(KEY_AUTO_COUNT_INTERVAL_MILLIS)
            }
        }
        if (requestSurfaceUpdate && (changed || (effectiveRunning && anchorEpochMillis > 0L))) {
            WearSurfaceUpdater.requestUpdate(context, updateComplications = false, forceTileUpdate = true)
        }
    }

    suspend fun toggleGesturePinch(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GESTURE_PINCH] = enabled
        }
    }

    suspend fun toggleScreenTap(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SCREEN_TAP] = enabled
        }
    }

    suspend fun toggleHaptics(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAPTIC_ENABLED] = enabled
        }
    }

    suspend fun toggleMilestones(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MILESTONE_ENABLED] = enabled
        }
    }

    suspend fun toggleKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_KEEP_SCREEN_ON] = enabled
        }
    }

    suspend fun toggleReminders(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMINDERS_ENABLED] = enabled
        }
    }

    suspend fun updateLastReminderTemplateIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_REMINDER_TEMPLATE_INDEX] = index
        }
    }

    /** Marks the user's daily round goal as completed today (for example, from physical beads). */
    suspend fun markDailyGoalCompleted() {
        val today = LocalDate.now().toString()
        context.dataStore.edit { prefs ->
            val goal = (prefs[KEY_DAILY_GOAL] ?: 16).coerceIn(1, 64)
            val currentRounds = if (prefs[KEY_LAST_DATE] == today) {
                prefs[KEY_COMPLETED_ROUNDS] ?: 0
            } else {
                0
            }
            prefs[KEY_BEAD_COUNT] = 0
            prefs[KEY_COMPLETED_ROUNDS] = maxOf(currentRounds, goal)
            prefs[KEY_AUTO_COUNT_ACTIVE] = false
            prefs[KEY_AUTO_COUNT_RUNNING] = false
            prefs.remove(KEY_AUTO_COUNT_ANCHOR_EPOCH_MILLIS)
            prefs.remove(KEY_AUTO_COUNT_INTERVAL_MILLIS)
            prefs[KEY_LAST_DATE] = today
        }
        WearSurfaceUpdater.requestUpdate(context, updateComplications = true)
    }

    suspend fun resetDay() {
        context.dataStore.edit { prefs ->
            prefs[KEY_BEAD_COUNT] = 0
            prefs[KEY_COMPLETED_ROUNDS] = 0
            prefs[KEY_AUTO_COUNT_ACTIVE] = false
            prefs[KEY_AUTO_COUNT_RUNNING] = false
            prefs.remove(KEY_AUTO_COUNT_ANCHOR_EPOCH_MILLIS)
            prefs.remove(KEY_AUTO_COUNT_INTERVAL_MILLIS)
            prefs[KEY_LAST_DATE] = LocalDate.now().toString()
        }
        WearSurfaceUpdater.requestUpdate(context)
    }
}
