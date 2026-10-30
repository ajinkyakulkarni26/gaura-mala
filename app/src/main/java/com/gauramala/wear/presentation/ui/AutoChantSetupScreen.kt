package com.gauramala.wear.presentation.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.gauramala.wear.AutoCountOngoingActivity
import com.gauramala.wear.presentation.AutoChantTiming
import com.gauramala.wear.presentation.MantraCounterViewModel
import com.gauramala.wear.presentation.MantraUiState
import com.gauramala.wear.presentation.theme.AlertRed
import com.gauramala.wear.presentation.theme.GauraGold
import com.gauramala.wear.presentation.theme.OnSurfaceMuted
import com.gauramala.wear.presentation.theme.OnSurfaceWhite
import kotlinx.coroutines.delay
import java.util.Locale

private enum class AutoChantPage { CHOOSE, OPTIONS, PACE, VOICE, NOTIFICATIONS }
private enum class GuidedStep { INTRO, COUNTDOWN, CHANTING, TAKE_SAVED, RESULT }
private val AutoChantSecondary = Color(0xFF303030)

@Composable
fun AutoChantSetupScreen(
    state: MantraUiState,
    viewModel: MantraCounterViewModel,
    onBack: () -> Unit,
    onStartCount: () -> Unit,
    notificationPermissionDeclined: Boolean,
    onNotificationPermissionDeclined: () -> Unit
) {
    val context = LocalContext.current
    val notificationPermissionMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    var page by remember { mutableStateOf(AutoChantPage.CHOOSE) }
    var returnPage by remember { mutableStateOf(AutoChantPage.CHOOSE) }
    var paceStep by remember { mutableStateOf(GuidedStep.INTRO) }
    var voiceStep by remember { mutableStateOf(GuidedStep.INTRO) }
    val paceTrials = remember { mutableStateListOf<Long>() }
    var countdown by remember { mutableIntStateOf(3) }
    var chantStartedAt by remember { mutableStateOf<Long?>(null) }
    var elapsedMillis by remember { mutableStateOf(0L) }
    var calibrationMessage by remember { mutableStateOf<String?>(null) }
    var microphoneMessage by remember { mutableStateOf<String?>(null) }
    var pendingVoiceGuide by remember { mutableStateOf<Boolean?>(null) }
    var notificationReturnPage by remember { mutableStateOf(AutoChantPage.CHOOSE) }
    var confirmVoiceRecordingDeletion by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val compactPaceIntro = page == AutoChantPage.PACE && paceStep == GuidedStep.INTRO
    val compactGuideChooser = page == AutoChantPage.CHOOSE &&
        state.autoChantPaceMillis == null && !state.hasVoiceChantRecording
    val compactNotificationPrompt = page == AutoChantPage.NOTIFICATIONS
    val compactNavigationPage = compactPaceIntro || compactGuideChooser || compactNotificationPrompt

    fun startCount(useVoiceGuide: Boolean) {
        pendingVoiceGuide = null
        viewModel.startAutoCount(useVoiceGuide)
        onStartCount()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val useVoiceGuide = pendingVoiceGuide
        pendingVoiceGuide = null
        if (granted && AutoCountOngoingActivity.notificationsEnabled(context)) {
            if (useVoiceGuide == null) page = notificationReturnPage
            else startCount(useVoiceGuide)
        } else {
            onNotificationPermissionDeclined()
            page = notificationReturnPage
        }
    }

    val notificationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val useVoiceGuide = pendingVoiceGuide
        pendingVoiceGuide = null
        if (AutoCountOngoingActivity.notificationsEnabled(context)) {
            if (useVoiceGuide == null) page = notificationReturnPage
            else startCount(useVoiceGuide)
        } else {
            onNotificationPermissionDeclined()
            page = notificationReturnPage
        }
    }

    fun requestCountStart(useVoiceGuide: Boolean) {
        if (notificationPermissionMissing && !notificationPermissionDeclined) {
            pendingVoiceGuide = useVoiceGuide
            notificationReturnPage = page
            page = AutoChantPage.NOTIFICATIONS
        } else {
            startCount(useVoiceGuide)
        }
    }

    fun declineNotificationPermission() {
        onNotificationPermissionDeclined()
        pendingVoiceGuide = null
        page = notificationReturnPage
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            microphoneMessage = null
            voiceStep = GuidedStep.COUNTDOWN
        } else {
            microphoneMessage = "Microphone access is off. Nothing was recorded."
        }
    }

    fun beginVoiceSetup() {
        microphoneMessage = null
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (permissionGranted) {
            voiceStep = GuidedStep.COUNTDOWN
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun openPaceSetup() {
        returnPage = page
        paceTrials.clear()
        calibrationMessage = null
        paceStep = GuidedStep.INTRO
        page = AutoChantPage.PACE
    }

    fun openVoiceSetup() {
        returnPage = page
        viewModel.prepareVoiceChantSetup()
        voiceStep = GuidedStep.INTRO
        page = AutoChantPage.VOICE
    }

    fun goBack() {
        if (confirmVoiceRecordingDeletion) {
            confirmVoiceRecordingDeletion = false
            return
        }
        when (page) {
            AutoChantPage.CHOOSE -> onBack()
            AutoChantPage.OPTIONS -> page = AutoChantPage.CHOOSE
            AutoChantPage.PACE -> {
                chantStartedAt = null
                paceTrials.clear()
                calibrationMessage = null
                paceStep = GuidedStep.INTRO
                page = returnPage
                returnPage = AutoChantPage.CHOOSE
            }
            AutoChantPage.VOICE -> {
                viewModel.cancelVoiceChantSetup()
                voiceStep = GuidedStep.INTRO
                page = returnPage
                returnPage = AutoChantPage.CHOOSE
            }
            AutoChantPage.NOTIFICATIONS -> declineNotificationPermission()
        }
    }

    BackHandler { goBack() }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancelVoiceChantSetup()
            viewModel.stopVoiceChantPreview()
        }
    }
    LaunchedEffect(chantStartedAt) {
        while (chantStartedAt != null) {
            elapsedMillis = SystemClock.elapsedRealtime() - (chantStartedAt ?: return@LaunchedEffect)
            delay(100)
        }
    }
    LaunchedEffect(paceStep) {
        if (paceStep == GuidedStep.COUNTDOWN) {
            for (number in 3 downTo 1) {
                countdown = number
                delay(800)
            }
            viewModel.autoChantCalibrationCue()
            chantStartedAt = SystemClock.elapsedRealtime()
            paceStep = GuidedStep.CHANTING
        }
    }
    LaunchedEffect(voiceStep) {
        if (voiceStep == GuidedStep.COUNTDOWN) {
            for (number in 3 downTo 1) {
                countdown = number
                delay(800)
            }
            viewModel.autoChantCalibrationCue()
            if (viewModel.startVoiceRecording()) {
                chantStartedAt = SystemClock.elapsedRealtime()
                voiceStep = GuidedStep.CHANTING
            } else {
                chantStartedAt = null
                microphoneMessage = "Recording could not start. Try again."
                voiceStep = GuidedStep.INTRO
            }
        }
    }
    LaunchedEffect(state.voiceRecordingFinished) {
        when (state.voiceRecordingFinished) {
            true -> {
                chantStartedAt = null
                voiceStep = GuidedStep.RESULT
            }
            false -> {
                chantStartedAt = null
                voiceStep = GuidedStep.INTRO
            }
            null -> Unit
        }
    }
    LaunchedEffect(page, paceStep, voiceStep) { scrollState.scrollTo(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .hierarchicalFocusGroup(active = true)
            .requestFocusOnHierarchyActive()
            .rotaryScrollable(
                behavior = RotaryScrollableDefaults.behavior(scrollableState = scrollState),
                focusRequester = focusRequester
            )
            .verticalScroll(scrollState)
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = when {
                    compactPaceIntro -> 16.dp
                    compactNotificationPrompt -> 8.dp
                    else -> 12.dp
                },
                bottom = if (compactNavigationPage) 4.dp else 12.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            when {
                compactPaceIntro -> 4.dp
                compactGuideChooser -> 2.dp
                compactNotificationPrompt -> 4.dp
                else -> 8.dp
            }
        )
    ) {
        if (state.autoCountActive) {
            SessionPage(
                state = state,
                onToggle = {
                    if (state.autoCountRunning) viewModel.pauseAutoCount()
                    else viewModel.resumeAutoCount()
                },
                onStop = {
                    viewModel.stopAutoCount()
                    onBack()
                }
            )
        } else {
            when (page) {
                AutoChantPage.CHOOSE -> {
                    PageTitle("Auto Count")
                    val hasPace = state.autoChantPaceMillis != null
                    val hasGuide = state.hasVoiceChantRecording
                    if (hasPace || hasGuide) {
                        Spacer(Modifier.height(2.dp))
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }

                    if (hasPace || hasGuide) {
                        if (hasPace) {
                            Text(
                                "Pace · ${paceDescription(state.autoChantPaceMillis)}",
                                color = OnSurfaceMuted,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("auto-chant-saved-pace")
                            )
                            ActionButton(
                                text = "Start with my pace",
                                tag = "auto-chant-start-count",
                                onClick = { requestCountStart(useVoiceGuide = false) }
                            )
                        }
                        if (hasGuide) {
                            ActionButton(
                                text = "Count with my chant",
                                tag = "auto-chant-start-voice-count",
                                onClick = { requestCountStart(useVoiceGuide = true) }
                            )
                        }
                        ActionButton(
                            text = "Change setup",
                            tag = "auto-chant-open-options",
                            onClick = { page = AutoChantPage.OPTIONS }
                        )
                    } else {
                        ActionButton(
                            text = "Set my pace",
                            tag = "auto-chant-open-pace",
                            onClick = ::openPaceSetup
                        )
                        ActionButton(
                            text = "Record a chant",
                            tag = "auto-chant-open-voice",
                            onClick = ::openVoiceSetup,
                            color = AutoChantSecondary
                        )
                    }
                }

                AutoChantPage.NOTIFICATIONS -> {
                    PageTitle("Notifications", compact = true)
                    Text(
                        "Return shortcut for Auto Count.",
                        color = OnSurfaceMuted,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                    ActionButton(
                        text = if (notificationPermissionMissing && !notificationPermissionDeclined) {
                            "Allow notifications"
                        } else {
                            "Open settings"
                        },
                        tag = "auto-count-allow-notifications",
                        onClick = {
                            if (notificationPermissionMissing && !notificationPermissionDeclined) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationSettingsLauncher.launch(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }
                        }
                    )
                }

                AutoChantPage.OPTIONS -> {
                    if (confirmVoiceRecordingDeletion) {
                        PageTitle("Delete saved chant?", "Are you sure?")
                        Row(
                            modifier = Modifier.fillMaxWidth(0.75f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                        ) {
                            Button(
                                onClick = { confirmVoiceRecordingDeletion = false },
                                modifier = Modifier.size(48.dp).testTag("voice-recording-delete-cancel"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AutoChantSecondary,
                                    contentColor = OnSurfaceWhite
                                )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel chant deletion",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.deleteVoiceChantRecording()
                                    confirmVoiceRecordingDeletion = false
                                },
                                modifier = Modifier.size(48.dp).testTag("voice-recording-delete-confirm"),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Confirm chant deletion",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    } else {
                        PageTitle("Auto Count", "Choose or change a guide")
                        ActionButton(
                            text = if (state.autoChantPaceMillis == null) "Set my pace" else "Change my pace",
                            tag = "auto-chant-open-pace",
                            onClick = ::openPaceSetup
                        )
                        ActionButton(
                            text = if (state.hasVoiceChantRecording) "Record a new chant" else "Record a chant",
                            tag = "auto-chant-open-voice",
                            onClick = ::openVoiceSetup
                        )
                        if (state.hasVoiceChantRecording) {
                            ActionButton(
                                text = "Delete saved chant",
                                tag = "voice-recording-delete",
                                onClick = { confirmVoiceRecordingDeletion = true },
                                color = AlertRed
                            )
                        }
                        if (notificationPermissionMissing ||
                            !AutoCountOngoingActivity.notificationsEnabled(context)
                        ) {
                            ActionButton(
                                text = "Notifications",
                                tag = "auto-chant-notifications",
                                onClick = {
                                    pendingVoiceGuide = null
                                    notificationReturnPage = page
                                    page = AutoChantPage.NOTIFICATIONS
                                }
                            )
                        }
                    }
                }

                AutoChantPage.PACE -> when (paceStep) {
                    GuidedStep.INTRO -> {
                        PageTitle(
                            if (paceTrials.isEmpty()) "Set your pace" else "Chant ${paceTrials.size + 1} of ${AutoChantTiming.CALIBRATION_TRIALS}",
                            "Tap timer. Chant on cue.",
                            compact = true
                        )
                        Text(
                            "Timing only · no audio.",
                            color = OnSurfaceMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                        IconActionButton(
                            icon = Icons.Filled.Timer,
                            description = "Start pace timing; no audio is recorded",
                            tag = "auto-chant-start-timing",
                            onClick = {
                                calibrationMessage = null
                                paceStep = GuidedStep.COUNTDOWN
                            }
                        )
                        calibrationMessage?.let {
                            Text(it, color = OnSurfaceMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                    }
                    GuidedStep.COUNTDOWN -> CountdownPage(countdown)
                    GuidedStep.CHANTING -> {
                        PageTitle("Chant now", "Tap stop to finish.")
                        Text(
                            formatDuration(elapsedMillis),
                            color = GauraGold,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("auto-chant-calibration-status")
                        )
                        IconActionButton(
                            icon = Icons.Filled.Stop,
                            description = "Stop pace timing",
                            tag = "auto-chant-finish-trial",
                            onClick = {
                                val startedAt = chantStartedAt ?: return@IconActionButton
                                val duration = SystemClock.elapsedRealtime() - startedAt
                                chantStartedAt = null
                                if (!AutoChantTiming.isValidPace(duration)) {
                                    calibrationMessage = "Try again. Chant once at your usual pace."
                                    paceStep = GuidedStep.INTRO
                                } else {
                                    calibrationMessage = null
                                    paceTrials.add(duration)
                                    if (paceTrials.size == AutoChantTiming.CALIBRATION_TRIALS) {
                                        val middlePace = AutoChantTiming.medianPaceMillis(paceTrials)
                                        if (middlePace != null) viewModel.saveAutoChantPace(middlePace)
                                        paceStep = GuidedStep.RESULT
                                    } else {
                                        paceStep = GuidedStep.TAKE_SAVED
                                    }
                                }
                            }
                        )
                    }
                    GuidedStep.TAKE_SAVED -> {
                        PageTitle("Chant saved", "${paceTrials.size} of ${AutoChantTiming.CALIBRATION_TRIALS}")
                        ActionButton(
                            text = "Next chant",
                            tag = "auto-chant-next-trial",
                            onClick = { paceStep = GuidedStep.COUNTDOWN },
                            color = GauraGold,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    GuidedStep.RESULT -> {
                        PageTitle("Pace ready")
                        Text(
                            paceDescription(state.autoChantPaceMillis),
                            color = OnSurfaceWhite,
                            fontSize = 14.sp,
                            modifier = Modifier.testTag("auto-chant-saved-pace")
                        )
                        ActionButton(
                            text = "Start counting",
                            tag = "auto-chant-start-count",
                            onClick = { requestCountStart(useVoiceGuide = false) },
                            color = GauraGold,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                        ActionButton(
                            text = "Try again",
                            tag = "auto-chant-retry-calibration",
                            onClick = {
                                paceTrials.clear()
                                calibrationMessage = null
                                paceStep = GuidedStep.INTRO
                            },
                            color = AutoChantSecondary
                        )
                    }
                }

                AutoChantPage.VOICE -> when (voiceStep) {
                    GuidedStep.INTRO -> {
                        PageTitle("Record a chant", "Save a guide for Auto Count.")
                        Text(
                            "Records one chant, up to 20 seconds, as an Auto Count guide. It stays in this watch's private app storage until deleted. Gaura Mala doesn't upload or share it.",
                            color = OnSurfaceMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("voice-recording-disclosure")
                        )
                        Text(
                            "Only record a voice you have permission to record and use.",
                            color = OnSurfaceMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "For a child, a parent or guardian should decide and help with recording and deletion.",
                            color = OnSurfaceMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        IconActionButton(
                            icon = Icons.Filled.Mic,
                            description = "Agree and begin recording a chant",
                            tag = "voice-recording-start",
                            onClick = ::beginVoiceSetup,
                        )
                        Text("Agree & begin", color = OnSurfaceWhite, fontSize = 10.sp, textAlign = TextAlign.Center)
                        state.voiceChantMessage?.let {
                            Text(it, color = OnSurfaceMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                        microphoneMessage?.let {
                            Text(it, color = AlertRed, fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                    }
                    GuidedStep.COUNTDOWN -> CountdownPage(countdown)
                    GuidedStep.CHANTING -> {
                        PageTitle("Chant now", "Tap stop to finish.")
                        Text("Recording", color = AlertRed, fontSize = 12.sp)
                        Text(
                            formatDuration(elapsedMillis),
                            color = GauraGold,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("voice-recording-duration")
                        )
                        IconActionButton(
                            icon = Icons.Filled.Stop,
                            description = "Stop chant recording",
                            tag = "voice-recording-stop",
                            onClick = {
                                chantStartedAt = null
                                viewModel.finishVoiceRecording()
                            }
                        )
                    }
                    GuidedStep.TAKE_SAVED -> Unit
                    GuidedStep.RESULT -> {
                        PageTitle("Chant ready")
                        Text(
                            paceDescription(state.autoChantPaceMillis),
                            color = OnSurfaceWhite,
                            fontSize = 13.sp
                        )
                        WatchMediaVolumeControl(context)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                        ) {
                            VoiceGuideIconButton(
                                icon = if (state.isVoiceChantPreviewing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                description = if (state.isVoiceChantPreviewing) "Stop chant preview" else "Play saved chant",
                                tag = "voice-recording-preview",
                                onClick = {
                                    if (state.isVoiceChantPreviewing) viewModel.stopVoiceChantPreview()
                                    else viewModel.previewVoiceChant()
                                }
                            )
                            VoiceGuideIconButton(
                                icon = Icons.Filled.Mic,
                                description = "Record another chant",
                                tag = "voice-recording-again",
                                onClick = {
                                    viewModel.stopVoiceChantPreview()
                                    voiceStep = GuidedStep.COUNTDOWN
                                }
                            )
                        }
                        ActionButton(
                            text = "Count with this chant",
                            tag = "auto-chant-start-voice-count",
                            onClick = { requestCountStart(useVoiceGuide = true) },
                            color = GauraGold,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Button(
                    onClick = ::goBack,
                    modifier = Modifier.size(48.dp).testTag("auto-chant-back"),
                    colors = ButtonDefaults.buttonColors(containerColor = AutoChantSecondary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (
                            (page == AutoChantPage.VOICE && voiceStep == GuidedStep.INTRO) ||
                            page == AutoChantPage.NOTIFICATIONS
                        ) {
                            "Not now"
                        } else {
                            "Back"
                        },
                        tint = GauraGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if ((page == AutoChantPage.VOICE && voiceStep == GuidedStep.INTRO) ||
                    page == AutoChantPage.NOTIFICATIONS
                ) {
                    Text("Not now", color = OnSurfaceWhite, fontSize = 9.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun CountdownPage(countdown: Int) {
    PageTitle("Get ready", "Chant when you feel the cue.")
    Text(
        countdown.toString(),
        color = GauraGold,
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag("auto-chant-countdown")
    )
}

@Composable
private fun PageTitle(title: String, hint: String? = null, compact: Boolean = false) {
    Text(
        title,
        modifier = Modifier.fillMaxWidth(if (compact) 0.9f else 0.72f).padding(horizontal = 4.dp),
        style = if (compact) MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
        else MaterialTheme.typography.titleMedium,
        color = GauraGold,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = if (compact) 1 else 2
    )
    if (!hint.isNullOrBlank()) {
        Text(
            hint,
            modifier = Modifier.fillMaxWidth().padding(horizontal = if (compact) 4.dp else 12.dp),
            color = OnSurfaceMuted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            maxLines = if (compact) 1 else 2
        )
    }
}

@Composable
private fun SessionPage(
    state: MantraUiState,
    onToggle: () -> Unit,
    onStop: () -> Unit
) {
    PageTitle("Auto Count", if (state.autoCountRunning) "Your chant is counting" else "Paused")
    Text(
        if (state.autoCountUsesVoiceGuide) "Following your chant" else paceDescription(state.autoChantPaceMillis),
        color = OnSurfaceMuted,
        fontSize = 10.sp,
        textAlign = TextAlign.Center
    )
    ActionButton(
        text = if (state.autoCountRunning) "Pause" else "Resume",
        tag = "auto-count-pause-resume",
        onClick = onToggle,
        color = GauraGold,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
    ActionButton("Stop counting", "auto-count-stop", onStop, color = AlertRed)
}

@Composable
private fun ActionButton(
    text: String,
    tag: String,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color = AutoChantSecondary,
    contentColor: androidx.compose.ui.graphics.Color = OnSurfaceWhite
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .heightIn(min = 48.dp)
            .testTag(tag),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = contentColor)
    ) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun IconActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(56.dp).testTag(tag),
        colors = ButtonDefaults.buttonColors(
            containerColor = AutoChantSecondary,
            contentColor = GauraGold
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun VoiceGuideIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(48.dp).testTag(tag),
        colors = ButtonDefaults.buttonColors(
            containerColor = AutoChantSecondary,
            contentColor = GauraGold
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun WatchMediaVolumeControl(context: Context) {
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val volumeIsFixed = audioManager.isVolumeFixed || maxVolume <= 0
    var volume by remember(audioManager) {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }

    val currentFraction = if (maxVolume > 0) volume.toFloat() / maxVolume else 0f
    Text(
        text = if (volumeIsFixed) "Volume fixed" else "Volume ${volume * 100 / maxVolume}%",
        color = OnSurfaceWhite,
        fontSize = 10.sp,
        textAlign = TextAlign.Center
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                val next = (volume - 1).coerceAtLeast(0)
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    next,
                    0
                )
                volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            },
            enabled = !volumeIsFixed && volume > 0,
            modifier = Modifier.size(48.dp).testTag("voice-volume-down"),
            colors = ButtonDefaults.buttonColors(
                containerColor = AutoChantSecondary,
                contentColor = GauraGold
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = "Lower watch media volume",
                modifier = Modifier.size(22.dp)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .background(AutoChantSecondary, RoundedCornerShape(3.dp))
                .testTag("voice-volume-level")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(currentFraction)
                    .height(6.dp)
                    .background(GauraGold, RoundedCornerShape(3.dp))
            )
        }
        Button(
            onClick = {
                val next = (volume + 1).coerceAtMost(maxVolume)
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    next,
                    0
                )
                volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            },
            enabled = !volumeIsFixed && volume < maxVolume,
            modifier = Modifier.size(48.dp).testTag("voice-volume-up"),
            colors = ButtonDefaults.buttonColors(
                containerColor = AutoChantSecondary,
                contentColor = GauraGold
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Raise watch media volume",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun paceDescription(paceMillis: Int?): String = paceMillis?.let {
    String.format(Locale.US, "%.1f sec per chant", it / 1000f)
} ?: "No pace saved"

private fun formatDuration(durationMillis: Long): String =
    String.format(Locale.US, "%.1f sec", durationMillis / 1000f)
