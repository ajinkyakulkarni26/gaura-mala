package com.gauramala.wear.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TimeTextDefaults
import androidx.wear.compose.material3.timeTextCurvedText
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureAction
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicatorState
import androidx.wear.compose.material3.onehandedgesture.OneHandedGesturePriority
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureIndicatorSize
import androidx.wear.compose.material3.onehandedgesture.oneHandedGesture
import androidx.wear.compose.material3.onehandedgesture.rememberOneHandedGestureConfiguration
import kotlinx.coroutines.launch
import com.gauramala.wear.presentation.MantraCounterViewModel
import com.gauramala.wear.presentation.RotaryBeadInput
import com.gauramala.wear.presentation.theme.GauraGold
import com.gauramala.wear.presentation.theme.GauraGoldLight
import com.gauramala.wear.presentation.theme.OnSurfaceMuted
import com.gauramala.wear.presentation.theme.SurfaceDark

@Composable
fun MantraCounterScreen(
    viewModel: MantraCounterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val canUsePinchGesture = state.isPinchGestureEnabled && state.supportsOneHandedGestures
    var showSettings by remember { mutableStateOf(false) }
    var showAutoChantSetup by remember { mutableStateOf(false) }
    var notificationPermissionDeclined by remember { mutableStateOf(false) }
    var showRoundProgress by remember { mutableStateOf(false) }
    val isAmbient = LocalAmbientModeManager.current?.currentAmbientMode is AmbientMode.Ambient
    val gestureConfiguration = rememberOneHandedGestureConfiguration(
        action = OneHandedGestureAction.Primary,
        gestureId = "gaura-mala-count-bead",
        priority = OneHandedGesturePriority.Clickable
    )
    val gestureIndicatorState = remember { OneHandedGestureClickIndicatorState() }
    val coroutineScope = rememberCoroutineScope()
    var hasPromptedPinchGesture by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val rotaryThreshold = with(LocalDensity.current) { 24.dp.toPx() }
    val rotaryBeadInput = remember(rotaryThreshold) { RotaryBeadInput(rotaryThreshold) }

    LaunchedEffect(
        focusRequester,
        showSettings,
        showAutoChantSetup,
        showRoundProgress,
        state.showGoalAchievedDialog,
        state.autoCountActive,
        state.autoCountRunning
    ) {
        if (
            !showSettings &&
            !showAutoChantSetup &&
            !showRoundProgress &&
            !state.showGoalAchievedDialog
        ) {
            focusRequester.requestFocus()
        }
    }

    BackHandler(enabled = showSettings && !isAmbient) {
        showSettings = false
    }
    BackHandler(enabled = showRoundProgress && !isAmbient) {
        showRoundProgress = false
    }
    BackHandler(enabled = showAutoChantSetup && !isAmbient) {
        showAutoChantSetup = false
    }
    BackHandler(enabled = state.showGoalAchievedDialog && !isAmbient) {
        viewModel.dismissGoalDialog()
    }

    // Dialog Overlays
    if (showSettings && !isAmbient) {
        SettingsDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = { showSettings = false },
            onAutoChant = {
                showSettings = false
                showAutoChantSetup = true
            }
        )
        return
    }

    if (showAutoChantSetup && !isAmbient) {
        AutoChantSetupScreen(
            state = state,
            viewModel = viewModel,
            onBack = { showAutoChantSetup = false },
            onStartCount = { showAutoChantSetup = false },
            notificationPermissionDeclined = notificationPermissionDeclined,
            onNotificationPermissionDeclined = { notificationPermissionDeclined = true }
        )
        return
    }

    if (showRoundProgress && !isAmbient) {
        RoundProgressDialog(
            state = state,
            onDismiss = { showRoundProgress = false },
            onSelectionTick = { viewModel.roundAdjustmentSelectionTick() },
            onSetRounds = { rounds ->
                viewModel.setCompletedRounds(rounds)
                showRoundProgress = false
            }
        )
        return
    }

    if (state.showGoalAchievedDialog && !isAmbient) {
        SummaryDialog(
            state = state,
            onDismiss = { viewModel.dismissGoalDialog() }
        )
        return
    }

    // Build gesture and interaction modifiers conditionally
    var containerModifier = modifier
        .fillMaxSize()
        .onRotaryScrollEvent { event ->
            // Keep raw pixel accumulation out of Compose state to avoid redrawing for tiny deltas.
            if (isAmbient || state.autoCountRunning) {
                rotaryBeadInput.reset()
                state.autoCountRunning
            } else {
                val steps = rotaryBeadInput.consume(
                    deltaPx = event.verticalScrollPixels,
                    eventUptimeMs = event.uptimeMillis
                )
                repeat(steps) { viewModel.incrementBeadFromRotary() }
                true
            }
        }
        .focusRequester(focusRequester)
        .focusable()

    if (state.isScreenTapEnabled && !isAmbient && !state.autoCountRunning) {
        containerModifier = containerModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null // Silent indication for battery preservation
        ) {
            viewModel.incrementBead()
        }
    }

    if (canUsePinchGesture && !state.autoCountRunning) {
        containerModifier = containerModifier.oneHandedGesture(
            gestureConfiguration = gestureConfiguration,
            onGestureLabel = "count a bead",
            enabledInAmbient = true,
            onGestureAvailable = {
                if (!hasPromptedPinchGesture) {
                    hasPromptedPinchGesture = true
                    coroutineScope.launch { gestureIndicatorState.showIndicator() }
                }
            },
            onGesture = {
                hasPromptedPinchGesture = true
                viewModel.incrementBeadFromGesture()
            }
        )
    }

    BoxWithConstraints(
        modifier = containerModifier.testTag("counter-surface"),
        contentAlignment = Alignment.Center
    ) {
        val compactCounterLayout = maxHeight < 220.dp || LocalDensity.current.fontScale > 1.15f
        val largeSystemFont = LocalDensity.current.fontScale > 1.15f

        // Outer Circular Bead & Round Progress Tracks
        BeadProgressRing(
            beadCount = state.beadCount,
            completedRounds = state.completedRounds,
            dailyGoal = state.dailyGoalRounds,
            isAmbient = isAmbient
        )

        // Center Content & Digital Bead Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(
                horizontal = 24.dp,
                vertical = if (compactCounterLayout) 0.dp else 8.dp
            )
        ) {
            // Round Header Indicator
            if (isAmbient) {
                Text(
                    text = if (state.isGoalAchieved) {
                        "Goal"
                    } else {
                        "R ${state.completedRounds + 1}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .clickable(
                            onClickLabel = "Adjust rounds completed today",
                            role = Role.Button
                        ) { showRoundProgress = true }
                        .testTag("round-progress-button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceDark)
                            .height(32.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.isGoalAchieved) {
                                "Daily goal reached"
                            } else {
                                "Round ${state.completedRounds + 1} of ${state.dailyGoalRounds}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = GauraGoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = GauraGoldLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Main Bead Number (Large, high-contrast, easily visible at arm's length)
            Text(
                text = "${state.beadCount}",
                modifier = Modifier.testTag("bead-count"),
                style = MaterialTheme.typography.displayLarge,
                color = if (isAmbient) Color.White else GauraGold,
                fontSize = if (compactCounterLayout) 44.sp else 46.sp,
                lineHeight = if (compactCounterLayout) 44.sp else 48.sp,
                fontWeight = FontWeight.Bold
            )

            // Bead Sub-label
            Text(
                text = "/ 108",
                style = MaterialTheme.typography.labelSmall,
                color = if (isAmbient) Color.Gray else OnSurfaceMuted,
                fontSize = 12.sp,
                lineHeight = if (compactCounterLayout) 12.sp else 14.sp
            )

            if (!isAmbient) {
                val inputHint = when {
                    state.autoCountRunning && state.autoCountUsesVoiceGuide -> "Following your chant"
                    state.autoCountRunning -> state.autoChantPaceMillis?.let {
                        "Auto count · ${String.format(java.util.Locale.US, "%.1f", it / 1000f)} sec"
                    } ?: "Auto count running"
                    state.autoCountActive -> "Auto count paused"
                    canUsePinchGesture && state.isScreenTapEnabled -> "Tap or double pinch to count"
                    canUsePinchGesture -> "Double pinch to count"
                    state.isScreenTapEnabled -> "Tap to count"
                    else -> "Turn the crown to count"
                }
                if (canUsePinchGesture && !state.autoCountRunning) {
                    OneHandedGestureClickIndicator(
                        gestureConfiguration = gestureConfiguration,
                        state = gestureIndicatorState,
                        gestureIndicatorSize = OneHandedGestureIndicatorSize.Small,
                        gestureIndicatorTint = GauraGoldLight
                    ) {
                        Text(
                            text = inputHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceMuted,
                            fontSize = 10.sp,
                            lineHeight = if (compactCounterLayout) 12.sp else 14.sp
                        )
                    }
                } else {
                    Text(
                        text = inputHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceMuted,
                        fontSize = 10.sp,
                        lineHeight = if (compactCounterLayout) 12.sp else 14.sp
                    )
                }
            }

            // Bottom Control Action Row (Hidden during ambient mode)
            if (!isAmbient) {
                Spacer(modifier = Modifier.height(if (compactCounterLayout) 0.dp else 6.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.autoCountActive) {
                        Button(
                            onClick = {
                                if (state.autoCountRunning) viewModel.pauseAutoCount()
                                else viewModel.resumeAutoCount()
                            },
                            modifier = Modifier.size(48.dp).testTag("auto-count-toggle"),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
                        ) {
                            Icon(
                                imageVector = if (state.autoCountRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.autoCountRunning) "Pause auto count" else "Resume auto count",
                                tint = GauraGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.undoLastBead() },
                            enabled = state.canUndo,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("undo-button")
                                .alpha(if (state.canUndo) 1f else 0.3f),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo Bead",
                                tint = if (state.canUndo) GauraGold else OnSurfaceMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))

                    // Settings Button
                    Button(
                        onClick = { showSettings = true },
                        modifier = Modifier.size(48.dp).testTag("open-settings-button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings",
                            tint = GauraGoldLight,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        val timeTextStyle = if (largeSystemFont) {
            TimeTextDefaults.timeTextStyle(fontSize = 10.sp)
        } else {
            null
        }
        TimeText(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .testTag("counter-time-text")
                .zIndex(1f),
            content = { time ->
                timeTextCurvedText(time, style = timeTextStyle)
            }
        )
    }
}
