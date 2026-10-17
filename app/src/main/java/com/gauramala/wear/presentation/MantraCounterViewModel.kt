package com.gauramala.wear.presentation

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gauramala.wear.data.MantraPreferences
import com.gauramala.wear.haptics.HapticHelper
import com.gauramala.wear.media.VoiceChantAudio
import java.time.LocalDate
import java.util.ArrayDeque
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class MantraCounterViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = MantraPreferences(application)
    private val hapticHelper = HapticHelper(application)
    private val voiceChantAudio = VoiceChantAudio(application)

    private val _uiState = MutableStateFlow(
        MantraUiState(
            supportsOneHandedGestures = application.packageManager.hasSystemFeature(
                FEATURE_WEAR_GESTURE_DETECTION
            ),
            hasVoiceChantRecording = voiceChantAudio.hasRecording()
        )
    )
    val uiState: StateFlow<MantraUiState> = _uiState.asStateFlow()
    private val counterStateMachine = MantraCounterStateMachine()
    private val autoChantPacer = AutoChantPacer()
    private var autoCountJob: Job? = null

    // Debounce timestamp to prevent accidental double-pinches or jitter
    private var lastIncrementTimestamp: Long = 0L
    private val debounceWindowMs: Long = 280L
    private var preferencesLoaded = false
    private var hasLocalProgressChanges = false
    private val pendingBeadInputs = ArrayDeque<PendingBeadInput>()
    private val countSaveRequests = Channel<CountSnapshot>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            for (snapshot in countSaveRequests) {
                preferences.saveCounts(snapshot.beadCount, snapshot.completedRounds)
            }
        }

        // Collect saved preferences
        viewModelScope.launch {
            preferences.resetIfNewDay()
            preferences.userPreferencesFlow.collect { prefs ->
                _uiState.update { current ->
                    current.withPreferences(
                        preferences = prefs,
                        preserveLocalProgress = hasLocalProgressChanges
                    )
                }
                if (!preferencesLoaded) {
                    preferencesLoaded = true
                    while (pendingBeadInputs.isNotEmpty()) {
                        applyBeadInput(pendingBeadInputs.removeFirst())
                    }
                }
            }
        }
    }

    /**
     * Increment by 1 bead.
     * Advances bead progress (0 through 107) and completes a round on the next count.
     */
    fun incrementBead() = incrementBead(applyDebounce = true, source = BeadInputSource.TAP)

    /** The gesture framework supplies normal feedback; app haptics are reserved for special cues. */
    fun incrementBeadFromGesture() =
        incrementBead(applyDebounce = true, source = BeadInputSource.GESTURE)

    /** Crown rotations are already discrete input and should not share the tap/pinch debounce. */
    fun incrementBeadFromRotary() =
        incrementBead(applyDebounce = false, source = BeadInputSource.ROTARY)

    private fun incrementBead(applyDebounce: Boolean, source: BeadInputSource) {
        val input = PendingBeadInput(
            applyDebounce = applyDebounce,
            source = source,
            timestampMs = SystemClock.elapsedRealtime()
        )
        if (!preferencesLoaded) {
            pendingBeadInputs.addLast(input)
            return
        }

        applyBeadInput(input)
    }

    private fun applyBeadInput(input: PendingBeadInput) {
        if (input.applyDebounce) {
            if (input.timestampMs - lastIncrementTimestamp < debounceWindowMs) {
                return
            }
            lastIncrementTimestamp = input.timestampMs
        }

        hasLocalProgressChanges = true
        resetMemoryForNewDayIfNeeded()
        val currentState = _uiState.value
        val transition = counterStateMachine.increment(currentState)
        val nextState = transition.state
        _uiState.value = nextState
        saveCounts(nextState.beadCount, nextState.completedRounds)

        when (selectHapticCue(currentState, transition, input.source)) {
            HapticCue.BEAD -> hapticHelper.beadClick()
            HapticCue.MILESTONE -> hapticHelper.milestoneAlert()
            HapticCue.ROUND_COMPLETED -> hapticHelper.roundCompletedAlert()
            HapticCue.DAILY_GOAL_ACHIEVED -> hapticHelper.dailyGoalAchievedAlert()
            null -> Unit
        }
    }

    /**
     * Undoes the last chant action in case of accidental gesture or tap.
     */
    fun undoLastBead() {
        if (!preferencesLoaded) return

        resetMemoryForNewDayIfNeeded()
        val currentState = _uiState.value
        if (!currentState.canUndo) return

        hasLocalProgressChanges = true
        val nextState = counterStateMachine.undo(currentState)
        _uiState.value = nextState
        saveCounts(nextState.beadCount, nextState.completedRounds)
        if (_uiState.value.isHapticsEnabled) {
            hapticHelper.undoAlert()
        }
    }

    fun resetCurrentRound() {
        if (!preferencesLoaded) return

        pauseAutoCount()
        hasLocalProgressChanges = true
        resetMemoryForNewDayIfNeeded()
        val nextState = counterStateMachine.resetCurrentRound(_uiState.value)
        _uiState.value = nextState
        saveCounts(nextState.beadCount, nextState.completedRounds)
        if (_uiState.value.isHapticsEnabled) {
            hapticHelper.undoAlert()
        }
    }

    /** Sets completed rounds from chanting done with physical beads; no chanting haptics are played. */
    fun setCompletedRounds(completedRounds: Int) {
        if (!preferencesLoaded || completedRounds !in 0..MAX_COMPLETED_ROUNDS) return

        stopAutoCount()
        resetMemoryForNewDayIfNeeded()
        hasLocalProgressChanges = true
        val nextState = counterStateMachine.setCompletedRounds(_uiState.value, completedRounds)
        _uiState.value = nextState
        saveCounts(nextState.beadCount, nextState.completedRounds)
    }

    fun roundAdjustmentSelectionTick() {
        hapticHelper.selectionTick(enabled = _uiState.value.isHapticsEnabled)
    }

    /** Gives a clear start cue during guided pace and voice trials. */
    fun autoChantCalibrationCue() {
        hapticHelper.autoCountStarted(enabled = _uiState.value.isHapticsEnabled)
    }

    fun saveAutoChantPace(paceMillis: Long) {
        if (!preferencesLoaded || !autoChantPacer.configure(paceMillis)) return
        val savedPace = paceMillis.toInt()
        stopAutoCount()
        _uiState.update { it.copy(autoChantPaceMillis = savedPace) }
        viewModelScope.launch { preferences.updateAutoChantPace(savedPace) }
    }

    fun prepareVoiceChantSetup() {
        _uiState.update { it.copy(voiceRecordingFinished = null, voiceChantMessage = null) }
    }

    fun startAutoCount(useVoiceGuide: Boolean = false) {
        val state = _uiState.value
        if (!preferencesLoaded) return
        if (useVoiceGuide) {
            if (!state.hasVoiceChantRecording || voiceChantAudio.recordingDurationMillis() == null) return
        } else {
            val paceMillis = state.autoChantPaceMillis?.toLong() ?: return
            if (!autoChantPacer.configure(paceMillis)) return
        }
        autoCountJob?.cancel()
        if (!useVoiceGuide && !autoChantPacer.start(SystemClock.elapsedRealtime())) return

        _uiState.update {
            it.copy(
                autoCountActive = true,
                autoCountRunning = true,
                autoCountUsesVoiceGuide = useVoiceGuide,
                isVoiceChantPreviewing = false
            )
        }
        hapticHelper.autoCountStarted(enabled = state.isHapticsEnabled)
        if (useVoiceGuide) {
            if (!voiceChantAudio.playLoop(::onVoiceChantCompleted)) {
                stopAutoCount()
                _uiState.update {
                    it.copy(voiceChantMessage = "Couldn't play the recording. Record it again.")
                }
            }
            return
        }

        autoCountJob = viewModelScope.launch {
            while (isActive && _uiState.value.autoCountRunning) {
                val waitMillis = autoChantPacer.timeUntilNextCount(SystemClock.elapsedRealtime()) ?: break
                delay(waitMillis.coerceAtLeast(1L))
                if (_uiState.value.autoCountRunning &&
                    autoChantPacer.consumeDueCount(SystemClock.elapsedRealtime()) &&
                    !incrementBeadFromAutoCount()
                ) {
                    stopAutoCount()
                    break
                }
            }
        }
    }

    fun pauseAutoCount() {
        if (!_uiState.value.autoCountActive || !_uiState.value.autoCountRunning) return
        autoCountJob?.cancel()
        autoCountJob = null
        autoChantPacer.pause()
        voiceChantAudio.stopPlayback()
        _uiState.update { it.copy(autoCountRunning = false) }
    }

    fun resumeAutoCount() {
        if (!_uiState.value.autoCountActive || _uiState.value.autoCountRunning) return
        startAutoCount(useVoiceGuide = _uiState.value.autoCountUsesVoiceGuide)
    }

    fun stopAutoCount() {
        autoCountJob?.cancel()
        autoCountJob = null
        autoChantPacer.pause()
        voiceChantAudio.stopPlayback()
        _uiState.update {
            it.copy(
                autoCountActive = false,
                autoCountRunning = false,
                autoCountUsesVoiceGuide = false
            )
        }
    }

    private fun incrementBeadFromAutoCount(): Boolean {
        if (!preferencesLoaded) return false

        hasLocalProgressChanges = true
        resetMemoryForNewDayIfNeeded()
        val currentState = _uiState.value
        val transition = counterStateMachine.increment(currentState)
        val nextState = transition.state
        _uiState.value = nextState
        saveCounts(nextState.beadCount, nextState.completedRounds)

        when (selectHapticCue(currentState, transition, BeadInputSource.AUTO)) {
            HapticCue.BEAD -> hapticHelper.beadClick()
            HapticCue.MILESTONE -> hapticHelper.milestoneAlert()
            HapticCue.ROUND_COMPLETED -> hapticHelper.roundCompletedAlert()
            HapticCue.DAILY_GOAL_ACHIEVED -> hapticHelper.dailyGoalAchievedAlert()
            null -> Unit
        }
        return !nextState.showGoalAchievedDialog
    }

    fun startVoiceRecording(): Boolean {
        if (!preferencesLoaded || _uiState.value.isVoiceChantRecording) return false
        _uiState.update {
            it.copy(
                voiceChantMessage = null,
                voiceRecordingFinished = null,
                isVoiceChantPreviewing = false
            )
        }
        val started = voiceChantAudio.startRecording {
            finishVoiceRecording()
        }
        _uiState.update { it.copy(isVoiceChantRecording = started) }
        if (!started) {
            _uiState.update {
                it.copy(voiceChantMessage = "Recording couldn't start. Check microphone access and try again.")
            }
        }
        return started
    }

    fun finishVoiceRecording() {
        val stopped = voiceChantAudio.finishRecording()
        val duration = if (stopped) voiceChantAudio.temporaryRecordingDurationMillis() else null
        val validDuration = duration?.takeIf(AutoChantTiming::isValidPace)
        val saved = validDuration != null && voiceChantAudio.publishTemporaryRecording()
        if (!saved) voiceChantAudio.discardTemporaryRecording()
        if (saved) saveAutoChantPace(validDuration)
        _uiState.update {
            it.copy(
                isVoiceChantRecording = false,
                voiceRecordingFinished = saved,
                hasVoiceChantRecording = it.hasVoiceChantRecording || saved,
                voiceChantMessage = if (saved) {
                    "Chant saved on this watch."
                } else if (duration != null) {
                    "Try a chant between 1 and 15 seconds."
                } else {
                    "Couldn't save that chant. Please try again."
                }
            )
        }
    }

    /** Discards only an unfinished take; an older saved chant remains intact. */
    fun cancelVoiceChantSetup() {
        cancelVoiceRecording()
        _uiState.update { it.copy(voiceRecordingFinished = null, voiceChantMessage = null) }
    }

    /** Discards an unfinished take; any previous saved recording remains available. */
    fun cancelVoiceRecording() {
        voiceChantAudio.cancelRecording()
        if (_uiState.value.isVoiceChantPreviewing) voiceChantAudio.stopPlayback()
        _uiState.update { it.copy(isVoiceChantRecording = false, isVoiceChantPreviewing = false) }
    }

    fun previewVoiceChant() {
        if (!_uiState.value.hasVoiceChantRecording) return
        _uiState.update { it.copy(voiceChantMessage = null, isVoiceChantPreviewing = true) }
        val started = voiceChantAudio.preview {
            _uiState.update { it.copy(isVoiceChantPreviewing = false) }
        }
        if (!started) {
            _uiState.update {
                it.copy(
                    isVoiceChantPreviewing = false,
                    voiceChantMessage = "Couldn't play the recording. Record it again."
                )
            }
        }
    }

    fun stopVoiceChantPreview() {
        if (!_uiState.value.isVoiceChantPreviewing) return
        voiceChantAudio.stopPlayback()
        _uiState.update { it.copy(isVoiceChantPreviewing = false) }
    }

    fun deleteVoiceChantRecording() {
        if (_uiState.value.autoCountUsesVoiceGuide) stopAutoCount()
        voiceChantAudio.deleteRecording()
        _uiState.update {
            it.copy(
                hasVoiceChantRecording = false,
                isVoiceChantRecording = false,
                isVoiceChantPreviewing = false,
                voiceRecordingFinished = null,
                voiceChantMessage = "Recording deleted from this watch."
            )
        }
    }

    private fun onVoiceChantCompleted(): Boolean {
        val shouldContinue = incrementBeadFromAutoCount() && _uiState.value.autoCountRunning
        if (!shouldContinue) {
            _uiState.update { it.copy(autoCountRunning = false, autoCountActive = false) }
        }
        return shouldContinue
    }

    fun resetDailyCount() {
        if (!preferencesLoaded) return

        stopAutoCount()
        hasLocalProgressChanges = true
        val nextState = counterStateMachine.resetDailyCount(
            state = _uiState.value,
            recordedDate = LocalDate.now().toString()
        )
        _uiState.value = nextState
        saveCounts(nextState.beadCount, nextState.completedRounds)
    }

    fun dismissGoalDialog() {
        _uiState.update { it.copy(showGoalAchievedDialog = false) }
    }

    fun refreshDailySession() {
        viewModelScope.launch { preferences.resetIfNewDay() }
    }

    fun togglePinchGesture(enabled: Boolean) {
        if (!preferencesLoaded) return
        viewModelScope.launch { preferences.toggleGesturePinch(enabled) }
    }

    fun toggleScreenTap(enabled: Boolean) {
        if (!preferencesLoaded) return
        viewModelScope.launch { preferences.toggleScreenTap(enabled) }
    }

    fun toggleHaptics(enabled: Boolean) {
        if (!preferencesLoaded) return
        viewModelScope.launch { preferences.toggleHaptics(enabled) }
    }

    fun toggleMilestones(enabled: Boolean) {
        if (!preferencesLoaded) return
        viewModelScope.launch { preferences.toggleMilestones(enabled) }
    }

    fun toggleKeepScreenOn(enabled: Boolean) {
        if (!preferencesLoaded) return
        viewModelScope.launch { preferences.toggleKeepScreenOn(enabled) }
    }

    fun updateDailyGoal(newGoal: Int) {
        if (!preferencesLoaded) return
        if (newGoal in 1..64) {
            _uiState.value = counterStateMachine.updateDailyGoal(_uiState.value, newGoal)
            viewModelScope.launch { preferences.updateDailyGoal(newGoal) }
        }
    }

    private fun saveCounts(beads: Int, rounds: Int) {
        countSaveRequests.trySend(CountSnapshot(beads, rounds))
    }

    override fun onCleared() {
        stopAutoCount()
        voiceChantAudio.release()
        super.onCleared()
    }

    private fun resetMemoryForNewDayIfNeeded() {
        val today = LocalDate.now().toString()
        if (_uiState.value.lastRecordedDate.isNotEmpty() && _uiState.value.lastRecordedDate != today) {
            hasLocalProgressChanges = true
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
            saveCounts(beads = 0, rounds = 0)
        }
    }

    private companion object {
        const val FEATURE_WEAR_GESTURE_DETECTION = "com.google.wear.feature.GESTURE_DETECTION"
        const val MAX_COMPLETED_ROUNDS = 999
    }

    private data class CountSnapshot(val beadCount: Int, val completedRounds: Int)

    private data class PendingBeadInput(
        val applyDebounce: Boolean,
        val source: BeadInputSource,
        val timestampMs: Long
    )
}
