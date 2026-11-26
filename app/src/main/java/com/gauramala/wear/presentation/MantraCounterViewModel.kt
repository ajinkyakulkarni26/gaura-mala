package com.gauramala.wear.presentation

import android.app.Application
import android.os.PowerManager
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gauramala.wear.AutoCountOngoingActivity
import com.gauramala.wear.ChantReminderScheduler
import com.gauramala.wear.WearSurfaceUpdater
import com.gauramala.wear.data.MantraPreferences
import com.gauramala.wear.data.UserPreferences
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class MantraCounterViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = MantraPreferences(application)
    private val hapticHelper = HapticHelper(application)
    private val voiceChantAudio = VoiceChantAudio(application)
    private val autoCountOngoingActivity = AutoCountOngoingActivity(application)
    private val chantReminderScheduler = ChantReminderScheduler(application)
    private val autoCountWakeLock = application
        .getSystemService(PowerManager::class.java)
        .newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "${application.packageName}:auto_count"
        )
        .apply { setReferenceCounted(false) }

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
    private var lastSyncedAutoCountState: Pair<Boolean, Boolean> = false to false
    @Volatile
    private var activeAutoCountAnchorEpochMs: Long = 0L
    @Volatile
    private var activeAutoCountIntervalMs: Int? = null
    private val pendingBeadInputs = ArrayDeque<PendingBeadInput>()
    private val countSaveRequests = Channel<CountSnapshot>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            for (snapshot in countSaveRequests) {
                preferences.saveCounts(
                    beadCount = snapshot.beadCount,
                    completedRounds = snapshot.completedRounds,
                    autoCountAnchorEpochMillis = snapshot.autoCountAnchorEpochMillis
                )
            }
        }

        // Keep Ongoing Activity, Tile status, and chanting reminders synced even while MainActivity
        // is in the background or dismissed to the watch face.
        viewModelScope.launch {
            uiState.collectLatest { state ->
                autoCountOngoingActivity.update(
                    isActive = state.autoCountActive,
                    isRunning = state.autoCountRunning,
                    beadCount = state.beadCount,
                    completedRounds = state.completedRounds
                )
                chantReminderScheduler.sync(state)
                val autoCountState = state.autoCountActive to state.autoCountRunning
                if (autoCountState != lastSyncedAutoCountState) {
                    lastSyncedAutoCountState = autoCountState
                    preferences.updateAutoCountState(
                        active = state.autoCountActive,
                        running = state.autoCountRunning,
                        anchorEpochMillis = activeAutoCountAnchorEpochMs,
                        intervalMillis = activeAutoCountIntervalMs,
                        requestSurfaceUpdate = false
                    )
                }
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
        val resolvedIntervalMs: Int = if (useVoiceGuide) {
            if (!state.hasVoiceChantRecording) return
            val recordingDuration = voiceChantAudio.recordingDurationMillis() ?: return
            recordingDuration.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
        } else {
            val paceMillis = state.autoChantPaceMillis?.toLong() ?: return
            if (!autoChantPacer.configure(paceMillis)) return
            paceMillis.toInt()
        }
        autoCountJob?.cancel()
        releaseAutoCountWakeLock()
        if (!useVoiceGuide && !autoChantPacer.start(SystemClock.elapsedRealtime())) return
        acquireAutoCountWakeLock()
        activeAutoCountAnchorEpochMs = System.currentTimeMillis()
        activeAutoCountIntervalMs = resolvedIntervalMs

        _uiState.update {
            it.copy(
                autoCountActive = true,
                autoCountRunning = true,
                autoCountUsesVoiceGuide = useVoiceGuide,
                isVoiceChantPreviewing = false,
                lastRecordedDate = it.lastRecordedDate.ifEmpty { LocalDate.now().toString() }
            )
        }
        WearSurfaceUpdater.requestUpdate(
            context = getApplication(),
            updateComplications = false,
            forceTileUpdate = true
        )
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
        releaseAutoCountWakeLock()
        activeAutoCountAnchorEpochMs = 0L
        _uiState.update { it.copy(autoCountRunning = false) }
        WearSurfaceUpdater.requestUpdate(
            context = getApplication(),
            updateComplications = false,
            forceTileUpdate = true
        )
    }

    fun resumeAutoCount() {
        if (resetMemoryForNewDayIfNeeded()) return
        if (!_uiState.value.autoCountActive || _uiState.value.autoCountRunning) return
        startAutoCount(useVoiceGuide = _uiState.value.autoCountUsesVoiceGuide)
    }

    fun stopAutoCount() {
        val wasActive = _uiState.value.autoCountActive || _uiState.value.autoCountRunning
        autoCountJob?.cancel()
        autoCountJob = null
        autoChantPacer.pause()
        voiceChantAudio.stopPlayback()
        releaseAutoCountWakeLock()
        activeAutoCountAnchorEpochMs = 0L
        activeAutoCountIntervalMs = null
        _uiState.update {
            it.copy(
                autoCountActive = false,
                autoCountRunning = false,
                autoCountUsesVoiceGuide = false
            )
        }
        if (wasActive) {
            WearSurfaceUpdater.requestUpdate(
                context = getApplication(),
                updateComplications = false,
                forceTileUpdate = true
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
        val nowEpochMs = System.currentTimeMillis()
        activeAutoCountAnchorEpochMs = nowEpochMs
        _uiState.value = nextState
        saveCounts(
            beads = nextState.beadCount,
            rounds = nextState.completedRounds,
            autoCountAnchorEpochMillis = nowEpochMs
        )

        when (selectHapticCue(currentState, transition, BeadInputSource.AUTO)) {
            HapticCue.BEAD -> hapticHelper.beadClick()
            HapticCue.MILESTONE -> hapticHelper.milestoneAlert()
            HapticCue.ROUND_COMPLETED -> hapticHelper.roundCompletedAlert()
            HapticCue.DAILY_GOAL_ACHIEVED -> hapticHelper.dailyGoalAchievedAlert()
            null -> Unit
        }
        return !nextState.showGoalAchievedDialog
    }

    private fun acquireAutoCountWakeLock() {
        if (!autoCountWakeLock.isHeld) autoCountWakeLock.acquire()
    }

    private fun releaseAutoCountWakeLock() {
        if (autoCountWakeLock.isHeld) autoCountWakeLock.release()
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
            stopAutoCount()
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

    fun refreshDailySession(today: String = LocalDate.now().toString()) {
        resetMemoryForNewDayIfNeeded(today)
        viewModelScope.launch { preferences.resetIfNewDay(today) }
        val currentState = _uiState.value
        autoCountOngoingActivity.update(
            isActive = currentState.autoCountActive,
            isRunning = currentState.autoCountRunning,
            beadCount = currentState.beadCount,
            completedRounds = currentState.completedRounds
        )
        chantReminderScheduler.sync(currentState)
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

    fun toggleReminders(enabled: Boolean) {
        if (!preferencesLoaded) return
        _uiState.update { it.copy(isRemindersEnabled = enabled) }
        viewModelScope.launch { preferences.toggleReminders(enabled) }
    }

    fun updateDailyGoal(newGoal: Int) {
        if (!preferencesLoaded) return
        if (newGoal in 1..64) {
            _uiState.value = counterStateMachine.updateDailyGoal(_uiState.value, newGoal)
            viewModelScope.launch { preferences.updateDailyGoal(newGoal) }
        }
    }

    internal fun currentTilePreferencesSnapshot(
        today: String = LocalDate.now().toString()
    ): UserPreferences? {
        if (!preferencesLoaded) return null
        resetMemoryForNewDayIfNeeded(today)
        val current = _uiState.value
        return UserPreferences(
            beadCount = current.beadCount,
            completedRounds = current.completedRounds,
            dailyGoalRounds = current.dailyGoalRounds,
            gesturePinchEnabled = current.isPinchGestureEnabled,
            screenTapEnabled = current.isScreenTapEnabled,
            hapticFeedbackEnabled = current.isHapticsEnabled,
            milestoneVibrationsEnabled = current.isMilestonesEnabled,
            keepScreenOn = current.keepScreenOn,
            remindersEnabled = current.isRemindersEnabled,
            lastRecordedDate = current.lastRecordedDate,
            autoChantPaceMillis = current.autoChantPaceMillis,
            autoCountActive = current.autoCountActive,
            autoCountRunning = current.autoCountRunning,
            autoCountAnchorEpochMillis = if (current.autoCountRunning) activeAutoCountAnchorEpochMs else 0L,
            autoCountIntervalMillis = activeAutoCountIntervalMs ?: current.autoChantPaceMillis
        )
    }

    private fun saveCounts(
        beads: Int,
        rounds: Int,
        autoCountAnchorEpochMillis: Long? = null
    ) {
        WearSurfaceUpdater.markStateChanged()
        countSaveRequests.trySend(CountSnapshot(beads, rounds, autoCountAnchorEpochMillis))
    }

    override fun onCleared() {
        stopAutoCount()
        autoCountOngoingActivity.stop()
        voiceChantAudio.release()
        super.onCleared()
    }

    private fun resetMemoryForNewDayIfNeeded(today: String = LocalDate.now().toString()): Boolean {
        val current = _uiState.value
        if (current.lastRecordedDate.isEmpty() || current.lastRecordedDate == today) {
            return false
        }
        val wasPausedAutoCount = current.autoCountActive && !current.autoCountRunning
        if (wasPausedAutoCount) {
            stopAutoCount()
        }
        hasLocalProgressChanges = true
        counterStateMachine.clearUndoHistory()
        val nowEpochMs = System.currentTimeMillis()
        if (current.autoCountRunning) {
            activeAutoCountAnchorEpochMs = nowEpochMs
        }
        _uiState.update {
            it.copy(
                beadCount = 0,
                completedRounds = 0,
                canUndo = false,
                showGoalAchievedDialog = false,
                lastRecordedDate = today,
                autoCountActive = if (wasPausedAutoCount) false else it.autoCountActive,
                autoCountRunning = if (wasPausedAutoCount) false else it.autoCountRunning,
                autoCountUsesVoiceGuide = if (wasPausedAutoCount) false else it.autoCountUsesVoiceGuide
            )
        }
        saveCounts(
            beads = 0,
            rounds = 0,
            autoCountAnchorEpochMillis = if (current.autoCountRunning) nowEpochMs else null
        )
        return true
    }

    private companion object {
        const val FEATURE_WEAR_GESTURE_DETECTION = "com.google.wear.feature.GESTURE_DETECTION"
        const val MAX_COMPLETED_ROUNDS = 999
    }

    private data class CountSnapshot(
        val beadCount: Int,
        val completedRounds: Int,
        val autoCountAnchorEpochMillis: Long? = null
    )

    private data class PendingBeadInput(
        val applyDebounce: Boolean,
        val source: BeadInputSource,
        val timestampMs: Long
    )
}
