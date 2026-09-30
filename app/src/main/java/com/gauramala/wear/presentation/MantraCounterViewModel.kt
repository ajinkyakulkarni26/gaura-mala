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
import java.time.LocalDate

class MantraCounterViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = MantraPreferences(application)
    private val hapticHelper = HapticHelper(application)

    private val _uiState = MutableStateFlow(
        MantraUiState(
            supportsOneHandedGestures = application.packageManager.hasSystemFeature(
                FEATURE_WEAR_GESTURE_DETECTION
            )
        )
    )
    val uiState: StateFlow<MantraUiState> = _uiState.asStateFlow()
    private val counterStateMachine = MantraCounterStateMachine()

    // Debounce timestamp to prevent accidental double-pinches or jitter
    private var lastIncrementTimestamp: Long = 0L
    private val debounceWindowMs: Long = 280L

    init {
        // Collect saved preferences
        viewModelScope.launch {
            preferences.resetIfNewDay()
            preferences.userPreferencesFlow.collect { prefs ->
                _uiState.update { current ->
                    val isNewDay = current.lastRecordedDate.isNotEmpty() &&
                        current.lastRecordedDate != prefs.lastRecordedDate
                    current.copy(
                        beadCount = prefs.beadCount,
                        completedRounds = prefs.completedRounds,
                        dailyGoalRounds = prefs.dailyGoalRounds,
                        isPinchGestureEnabled = prefs.gesturePinchEnabled,
                        isScreenTapEnabled = prefs.screenTapEnabled,
                        isHapticsEnabled = prefs.hapticFeedbackEnabled,
                        isMilestonesEnabled = prefs.milestoneVibrationsEnabled,
                        keepScreenOn = prefs.keepScreenOn,
                        lastRecordedDate = prefs.lastRecordedDate,
                        canUndo = if (isNewDay) false else current.canUndo,
                        showGoalAchievedDialog = if (isNewDay) false else current.showGoalAchievedDialog
                    )
                }
            }
        }
    }

    /**
     * Increment by 1 bead.
     * Advances bead progress (0 through 107) and completes a round on the next count.
     */
    fun incrementBead() = incrementBead(applyDebounce = true)

    /** The gesture framework already provides its own success haptic. */
    fun incrementBeadFromGesture() = incrementBead(applyDebounce = true, playAppHaptics = false)

    /** Crown rotations are already discrete input and should not share the tap/pinch debounce. */
    fun incrementBeadFromRotary() = incrementBead(applyDebounce = false, playAppHaptics = true)

    private fun incrementBead(applyDebounce: Boolean, playAppHaptics: Boolean = true) {
        if (applyDebounce) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastIncrementTimestamp < debounceWindowMs) {
                return
            }
            lastIncrementTimestamp = now
        }

        resetMemoryForNewDayIfNeeded()
        val currentState = _uiState.value
        val transition = counterStateMachine.increment(currentState)
        val nextState = transition.state
        _uiState.value = nextState
        saveCounts(nextState.beadCount, nextState.completedRounds)

        if (playAppHaptics && currentState.isHapticsEnabled) {
            if (transition.completedRound) {
                if (transition.goalJustReached) {
                    hapticHelper.dailyGoalAchievedAlert()
                } else {
                    hapticHelper.roundCompletedAlert()
                }
            } else if (
                currentState.isMilestonesEnabled &&
                (nextState.beadCount == 27 || nextState.beadCount == 54 || nextState.beadCount == 81)
            ) {
                hapticHelper.milestoneAlert()
            } else {
                hapticHelper.beadClick()
            }
        }
    }

    /**
     * Undoes the last chant action in case of accidental gesture or tap.
     */
    fun undoLastBead() {
        resetMemoryForNewDayIfNeeded()
        val currentState = _uiState.value
        if (!currentState.canUndo) return

        val nextState = counterStateMachine.undo(currentState)
        _uiState.value = nextState
        saveCounts(nextState.beadCount, nextState.completedRounds)
        if (_uiState.value.isHapticsEnabled) {
            hapticHelper.undoAlert()
        }
    }

    fun resetCurrentRound() {
        resetMemoryForNewDayIfNeeded()
        val nextState = counterStateMachine.resetCurrentRound(_uiState.value)
        _uiState.value = nextState
        saveCounts(nextState.beadCount, nextState.completedRounds)
        if (_uiState.value.isHapticsEnabled) {
            hapticHelper.undoAlert()
        }
    }

    fun resetDailyCount() {
        viewModelScope.launch {
            preferences.resetDay()
            _uiState.value = counterStateMachine.resetDailyCount(
                state = _uiState.value,
                recordedDate = LocalDate.now().toString()
            )
        }
    }

    fun dismissGoalDialog() {
        _uiState.update { it.copy(showGoalAchievedDialog = false) }
    }

    fun refreshDailySession() {
        viewModelScope.launch { preferences.resetIfNewDay() }
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
            _uiState.value = counterStateMachine.updateDailyGoal(_uiState.value, newGoal)
            viewModelScope.launch { preferences.updateDailyGoal(newGoal) }
        }
    }

    private fun saveCounts(beads: Int, rounds: Int) {
        viewModelScope.launch {
            preferences.saveCounts(beads, rounds)
        }
    }

    private fun resetMemoryForNewDayIfNeeded() {
        val today = LocalDate.now().toString()
        if (_uiState.value.lastRecordedDate.isNotEmpty() && _uiState.value.lastRecordedDate != today) {
            counterStateMachine.clearUndoHistory()
            _uiState.update {
                it.copy(
                    beadCount = 0,
                    completedRounds = 0,
                    canUndo = false,
                    showGoalAchievedDialog = false,
                    lastRecordedDate = today
                )
            }
            viewModelScope.launch { preferences.resetIfNewDay() }
        }
    }

    private companion object {
        const val FEATURE_WEAR_GESTURE_DETECTION = "com.google.wear.feature.GESTURE_DETECTION"
    }
}
