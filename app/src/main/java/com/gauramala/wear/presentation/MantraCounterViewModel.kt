package com.gauramala.wear.presentation

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gauramala.wear.data.MantraPreferences
import com.gauramala.wear.haptics.HapticHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MantraCounterViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = MantraPreferences(application)
    private val hapticHelper = HapticHelper(application)

    private val _uiState = MutableStateFlow(MantraUiState())
    val uiState: StateFlow<MantraUiState> = _uiState.asStateFlow()

    // Debounce timestamp to prevent accidental double-pinches or jitter
    private var lastIncrementTimestamp: Long = 0L
    private val debounceWindowMs: Long = 280L

    // Stack to support undoing last chant
    private var previousBeadCount: Int = 0
    private var previousCompletedRounds: Int = 0

    init {
        // Collect saved preferences
        viewModelScope.launch {
            preferences.userPreferencesFlow.collect { prefs ->
                _uiState.update { current ->
                    current.copy(
                        beadCount = prefs.beadCount,
                        completedRounds = prefs.completedRounds,
                        dailyGoalRounds = prefs.dailyGoalRounds,
                        isPinchGestureEnabled = prefs.gesturePinchEnabled,
                        isScreenTapEnabled = prefs.screenTapEnabled,
                        isHapticsEnabled = prefs.hapticFeedbackEnabled,
                        isMilestonesEnabled = prefs.milestoneVibrationsEnabled,
                        keepScreenOn = prefs.keepScreenOn
                    )
                }
            }
        }
    }

    /**
     * Increment by 1 bead.
     * Handles bead count from 1 to 108, round completions, and haptic feedback.
     */
    fun incrementBead() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastIncrementTimestamp < debounceWindowMs) {
            return
        }
        lastIncrementTimestamp = now

        val currentState = _uiState.value
        previousBeadCount = currentState.beadCount
        previousCompletedRounds = currentState.completedRounds

        if (currentState.beadCount < 107) {
            val nextBead = currentState.beadCount + 1
            _uiState.update {
                it.copy(
                    beadCount = nextBead,
                    canUndo = true
                )
            }
            saveCounts(nextBead, currentState.completedRounds)

            // Tactile feedback
            if (currentState.isHapticsEnabled) {
                if (currentState.isMilestonesEnabled && (nextBead == 27 || nextBead == 54 || nextBead == 81)) {
                    hapticHelper.milestoneAlert()
                } else {
                    hapticHelper.beadClick()
                }
            }
        } else {
            // 108th bead completed -> Round finished!
            val nextRounds = currentState.completedRounds + 1
            val goalReached = nextRounds == currentState.dailyGoalRounds

            _uiState.update {
                it.copy(
                    beadCount = 0,
                    completedRounds = nextRounds,
                    canUndo = true,
                    showGoalAchievedDialog = goalReached
                )
            }
            saveCounts(0, nextRounds)

            if (currentState.isHapticsEnabled) {
                if (goalReached) {
                    hapticHelper.dailyGoalAchievedAlert()
                } else {
                    hapticHelper.roundCompletedAlert()
                }
            }
        }
    }

    /**
     * Undoes the last chant action in case of accidental gesture or tap.
     */
    fun undoLastBead() {
        if (!_uiState.value.canUndo) return

        _uiState.update {
            it.copy(
                beadCount = previousBeadCount,
                completedRounds = previousCompletedRounds,
                canUndo = false
            )
        }
        saveCounts(previousBeadCount, previousCompletedRounds)
        if (_uiState.value.isHapticsEnabled) {
            hapticHelper.undoAlert()
        }
    }

    fun resetCurrentRound() {
        _uiState.update {
            it.copy(beadCount = 0, canUndo = false)
        }
        saveCounts(0, _uiState.value.completedRounds)
        if (_uiState.value.isHapticsEnabled) {
            hapticHelper.undoAlert()
        }
    }

    fun resetDailyCount() {
        viewModelScope.launch {
            preferences.resetDay()
            _uiState.update {
                it.copy(
                    beadCount = 0,
                    completedRounds = 0,
                    canUndo = false,
                    showGoalAchievedDialog = false
                )
            }
        }
    }

    fun dismissGoalDialog() {
        _uiState.update { it.copy(showGoalAchievedDialog = false) }
    }

    fun setAmbientMode(isAmbient: Boolean) {
        _uiState.update { it.copy(isAmbient = isAmbient) }
    }

    fun togglePinchGesture(enabled: Boolean) {
        viewModelScope.launch { preferences.toggleGesturePinch(enabled) }
    }

    fun toggleScreenTap(enabled: Boolean) {
        viewModelScope.launch { preferences.toggleScreenTap(enabled) }
    }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch { preferences.toggleHaptics(enabled) }
    }

    fun toggleMilestones(enabled: Boolean) {
        viewModelScope.launch { preferences.toggleMilestones(enabled) }
    }

    fun toggleKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { preferences.toggleKeepScreenOn(enabled) }
    }

    fun updateDailyGoal(newGoal: Int) {
        if (newGoal in 1..64) {
            viewModelScope.launch { preferences.updateDailyGoal(newGoal) }
        }
    }

    private fun saveCounts(beads: Int, rounds: Int) {
        viewModelScope.launch {
            preferences.saveCounts(beads, rounds)
        }
    }
}
