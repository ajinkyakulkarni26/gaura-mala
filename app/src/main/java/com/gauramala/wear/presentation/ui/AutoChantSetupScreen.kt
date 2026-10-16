package com.gauramala.wear.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.gauramala.wear.presentation.AutoChantTiming
import com.gauramala.wear.presentation.MantraCounterViewModel
import com.gauramala.wear.presentation.MantraUiState
import com.gauramala.wear.presentation.theme.AlertRed
import com.gauramala.wear.presentation.theme.GauraGold
import com.gauramala.wear.presentation.theme.OnSurfaceMuted
import com.gauramala.wear.presentation.theme.OnSurfaceWhite
import com.gauramala.wear.presentation.theme.SurfaceDark
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun AutoChantSetupScreen(
    state: MantraUiState,
    viewModel: MantraCounterViewModel,
    onBack: () -> Unit,
    onStartCount: () -> Unit
) {
    val context = LocalContext.current
    var chantStartedAt by remember { mutableStateOf<Long?>(null) }
    var elapsedMillis by remember { mutableStateOf(0L) }
    var calibrationMessage by remember { mutableStateOf<String?>(null) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionMessage = null
            viewModel.startVoiceRecording()
        } else {
            permissionMessage = "Microphone access is off. Nothing was recorded."
        }
    }

    BackHandler {
        viewModel.cancelVoiceRecording()
        viewModel.stopVoiceChantPreview()
        onBack()
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancelVoiceRecording()
            viewModel.stopVoiceChantPreview()
        }
    }
    LaunchedEffect(chantStartedAt) {
        while (chantStartedAt != null) {
            elapsedMillis = SystemClock.elapsedRealtime() - (chantStartedAt ?: return@LaunchedEffect)
            delay(250)
        }
    }

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
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            "Auto Count",
            style = MaterialTheme.typography.titleMedium,
            color = GauraGold,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Count hands-free at your pace, with the usual bead and round cues.",
            color = OnSurfaceWhite,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        if (state.autoCountActive) {
            Text(
                if (state.autoCountRunning) "Auto count is running" else "Auto count is paused",
                color = GauraGold,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("auto-count-status")
            )
            if (state.autoCountUsesVoiceGuide) {
                Text("Following your recorded chant", color = OnSurfaceMuted, fontSize = 10.sp)
            } else {
                Text(paceDescription(state.autoChantPaceMillis), color = OnSurfaceMuted, fontSize = 10.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                ActionButton(
                    text = if (state.autoCountRunning) "Pause" else "Resume",
                    tag = "auto-count-pause-resume",
                    onClick = {
                        if (state.autoCountRunning) viewModel.pauseAutoCount() else viewModel.resumeAutoCount()
                    }
                )
                ActionButton(
                    text = "Stop",
                    tag = "auto-count-stop",
                    onClick = {
                        viewModel.stopAutoCount()
                        onBack()
                    },
                    color = AlertRed
                )
            }
        } else {
            Text(
                "Set your pace",
                color = GauraGold,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
            Text(
                "Start, chant once, then tap Finish.",
                color = OnSurfaceMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
            if (chantStartedAt != null) {
                Text(
                    "Timing · ${formatDuration(elapsedMillis)}",
                    color = GauraGold,
                    fontSize = 11.sp,
                    modifier = Modifier.testTag("auto-chant-calibration-status")
                )
            } else if (state.autoChantPaceMillis != null) {
                Text(
                    "Saved pace: ${paceDescription(state.autoChantPaceMillis)}",
                    color = OnSurfaceMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.testTag("auto-chant-saved-pace")
                )
            }
            ActionButton(
                text = if (chantStartedAt == null) "Start timing" else "Finish chant",
                tag = if (chantStartedAt == null) "auto-chant-start-timing" else "auto-chant-finish-timing",
                onClick = {
                    val startedAt = chantStartedAt
                    if (startedAt == null) {
                        calibrationMessage = null
                        elapsedMillis = 0L
                        chantStartedAt = SystemClock.elapsedRealtime()
                    } else {
                        val duration = SystemClock.elapsedRealtime() - startedAt
                        chantStartedAt = null
                        val pace = AutoChantTiming.averagePaceMillis(duration, chantCount = 1)
                        if (pace == null) {
                            calibrationMessage = "Too quick to time. Try again."
                        } else {
                            viewModel.saveAutoChantPace(pace)
                            calibrationMessage = "Pace saved: ${paceDescription(pace.toInt())}"
                        }
                    }
                }
            )
            calibrationMessage?.let { Text(it, color = OnSurfaceMuted, fontSize = 10.sp, textAlign = TextAlign.Center) }
            if (state.autoChantPaceMillis != null) {
                ActionButton(
                    text = "Start auto count",
                    tag = "auto-chant-start-count",
                    onClick = {
                        viewModel.startAutoCount(useVoiceGuide = false)
                        onStartCount()
                    },
                    color = GauraGold,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }

            Text("Optional voice guide", color = GauraGold, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(
                "Watch mic, up to 20 sec. Stays on this watch; never uploaded, shared, or backed up. Delete anytime. Record a voice you have permission to use.",
                color = OnSurfaceWhite,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .testTag("voice-recording-disclosure")
            )
            if (state.isVoiceChantRecording) {
                Text(
                    "Recording · tap stop when one chant is complete",
                    color = AlertRed,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("voice-recording-indicator")
                )
                ActionButton(
                    text = "Stop recording",
                    tag = "voice-recording-stop",
                    onClick = viewModel::finishVoiceRecording,
                    color = AlertRed
                )
            } else {
                ActionButton(
                    text = "Record a chant",
                    tag = "voice-recording-start",
                    onClick = {
                        permissionMessage = null
                        val permissionGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (permissionGranted) {
                            viewModel.startVoiceRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }
            permissionMessage?.let {
                Text(it, color = OnSurfaceMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
            }
            if (state.hasVoiceChantRecording && !state.isVoiceChantRecording) {
                Text("Saved on this watch", color = OnSurfaceMuted, fontSize = 10.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    ActionButton(
                        text = if (state.isVoiceChantPreviewing) "Stop preview" else "Preview",
                        tag = "voice-recording-preview",
                        onClick = {
                            if (state.isVoiceChantPreviewing) viewModel.stopVoiceChantPreview()
                            else viewModel.previewVoiceChant()
                        }
                    )
                    ActionButton(
                        text = "Delete",
                        tag = "voice-recording-delete",
                        onClick = viewModel::deleteVoiceChantRecording,
                        color = AlertRed
                    )
                }
                ActionButton(
                    text = "Start with voice guide",
                    tag = "auto-chant-start-voice-count",
                    onClick = {
                        viewModel.startAutoCount(useVoiceGuide = true)
                        onStartCount()
                    },
                    color = GauraGold,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
            state.voiceChantMessage?.let {
                Text(it, color = OnSurfaceMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        ActionButton(text = "Back", tag = "auto-chant-back", onClick = onBack)
    }
}

@Composable
private fun ActionButton(
    text: String,
    tag: String,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color = SurfaceDark,
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
        Text(text, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

private fun paceDescription(paceMillis: Int?): String = paceMillis?.let {
    String.format(Locale.US, "%.1f seconds per chant", it / 1000f)
} ?: "No pace saved yet"

private fun formatDuration(durationMillis: Long): String =
    String.format(Locale.US, "%.1f seconds", durationMillis / 1000f)
