package com.gauramala.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gauramala.wear.WearSurfaceUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gaura_mala_prefs")

data class UserPreferences(
    val beadCount: Int = 0,
    val completedRounds: Int = 0,
    val dailyGoalRounds: Int = 16,
    val gesturePinchEnabled: Boolean = true,
    val screenTapEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val milestoneVibrationsEnabled: Boolean = true,
    val keepScreenOn: Boolean = false,
    val lastRecordedDate: String = ""
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
        val KEY_LAST_DATE = stringPreferencesKey("key_last_date")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            beadCount = preferences[KEY_BEAD_COUNT] ?: 0,
            completedRounds = preferences[KEY_COMPLETED_ROUNDS] ?: 0,
            dailyGoalRounds = preferences[KEY_DAILY_GOAL] ?: 16,
            gesturePinchEnabled = preferences[KEY_GESTURE_PINCH] ?: true,
            screenTapEnabled = preferences[KEY_SCREEN_TAP] ?: true,
            hapticFeedbackEnabled = preferences[KEY_HAPTIC_ENABLED] ?: true,
            milestoneVibrationsEnabled = preferences[KEY_MILESTONE_ENABLED] ?: true,
            keepScreenOn = preferences[KEY_KEEP_SCREEN_ON] ?: false,
            lastRecordedDate = preferences[KEY_LAST_DATE] ?: ""
        )
    }

    /** Starts a fresh daily count when saved progress belongs to an earlier local calendar day. */
    suspend fun resetIfNewDay() {
        val today = LocalDate.now().toString()
        var didReset = false
        context.dataStore.edit { prefs ->
            if (prefs[KEY_LAST_DATE] != today) {
                prefs[KEY_BEAD_COUNT] = 0
                prefs[KEY_COMPLETED_ROUNDS] = 0
                prefs[KEY_LAST_DATE] = today
                didReset = true
            }
        }
        if (didReset) WearSurfaceUpdater.requestUpdate(context)
    }

    suspend fun saveCounts(beadCount: Int, completedRounds: Int) {
        val today = LocalDate.now().toString()
        var roundsChanged = false
        context.dataStore.edit { prefs ->
            roundsChanged = (prefs[KEY_COMPLETED_ROUNDS] ?: 0) != completedRounds
            prefs[KEY_BEAD_COUNT] = beadCount
            prefs[KEY_COMPLETED_ROUNDS] = completedRounds
            prefs[KEY_LAST_DATE] = today
        }
        WearSurfaceUpdater.requestUpdate(context, updateComplications = roundsChanged)
    }

    suspend fun updateDailyGoal(goal: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DAILY_GOAL] = goal
        }
        WearSurfaceUpdater.requestUpdate(context)
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

    suspend fun resetDay() {
        context.dataStore.edit { prefs ->
            prefs[KEY_BEAD_COUNT] = 0
            prefs[KEY_COMPLETED_ROUNDS] = 0
            prefs[KEY_LAST_DATE] = LocalDate.now().toString()
        }
        WearSurfaceUpdater.requestUpdate(context)
    }
}
