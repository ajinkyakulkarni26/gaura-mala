package com.gauramala.wear.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureAction
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicatorState
import androidx.wear.compose.material3.onehandedgesture.OneHandedGesturePriority
import androidx.wear.compose.material3.onehandedgesture.oneHandedGesture
import androidx.wear.compose.material3.onehandedgesture.rememberOneHandedGestureConfiguration
import kotlinx.coroutines.launch
import com.gauramala.wear.presentation.MantraCounterViewModel
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
    var showSettings by remember { mutableStateOf(false) }
    val isAmbient = LocalAmbientModeManager.current?.currentAmbientMode is AmbientMode.Ambient
    val gestureConfiguration = rememberOneHandedGestureConfiguration(
        action = OneHandedGestureAction.Primary,
        gestureId = "gaura-mala-count-bead",
        priority = OneHandedGesturePriority.Clickable
    )
    val gestureIndicatorState = remember { OneHandedGestureClickIndicatorState() }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var rotaryPixels by remember { mutableFloatStateOf(0f) }
    val rotaryThreshold = with(LocalDensity.current) { 24.dp.toPx() }

    LaunchedEffect(focusRequester, showSettings, state.showGoalAchievedDialog) {
        if (!showSettings && !state.showGoalAchievedDialog) {
            focusRequester.requestFocus()
        }
    }

    BackHandler(enabled = showSettings && !isAmbient) {
        showSettings = false
    }
    BackHandler(enabled = state.showGoalAchievedDialog && !isAmbient) {
        viewModel.dismissGoalDialog()
    }

    // Dialog Overlays
    if (showSettings && !isAmbient) {
        SettingsDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = { showSettings = false }
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
            // Advance once per rotary detent while ignoring reverse rotation.
            val delta = event.verticalScrollPixels
            if (isAmbient || delta <= 0f) {
                rotaryPixels = 0f
                false
            } else {
                rotaryPixels += delta
                val steps = (rotaryPixels / rotaryThreshold).toInt()
                repeat(steps) { viewModel.incrementBeadFromRotary() }
                rotaryPixels -= steps * rotaryThreshold
                true
            }
        }
        .focusRequester(focusRequester)
        .focusable()

    if (state.isScreenTapEnabled && !isAmbient) {
        containerModifier = containerModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null // Silent indication for battery preservation
        ) {
            viewModel.incrementBead()
        }
    }

    if (state.isPinchGestureEnabled && !isAmbient) {
        containerModifier = containerModifier.oneHandedGesture(
            gestureConfiguration = gestureConfiguration,
            onGestureLabel = "count a bead",
            onGestureAvailable = {
                coroutineScope.launch { gestureIndicatorState.showIndicator() }
            },
            onGesture = { viewModel.incrementBead() }
        )
    }

    Box(
        modifier = containerModifier,
        contentAlignment = Alignment.Center
    ) {
        TimeText(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
        )

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
            modifier = Modifier.padding(24.dp)
        ) {
            // Round Header Indicator
            Text(
                text = if (isAmbient) {
                    "R ${state.completedRounds}"
                } else if (state.isGoalAchieved) {
                    "Daily goal reached"
                } else {
                    "Round ${state.completedRounds + 1} of ${state.dailyGoalRounds}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isAmbient) Color.White else GauraGoldLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Main Bead Number (Large, high-contrast, easily visible at arm's length)
            Text(
                text = "${state.beadCount}",
                style = MaterialTheme.typography.displayLarge,
                color = if (isAmbient) Color.White else GauraGold,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold
            )

            // Bead Sub-label
            Text(
                text = "/ 108",
                style = MaterialTheme.typography.labelSmall,
                color = if (isAmbient) Color.Gray else OnSurfaceMuted,
                fontSize = 12.sp
            )

            if (!isAmbient) {
                val inputHint = when {
                    state.isPinchGestureEnabled && state.isScreenTapEnabled -> "Tap or double pinch to count"
                    state.isPinchGestureEnabled -> "Double pinch to count"
                    state.isScreenTapEnabled -> "Tap to count"
                    else -> "Turn the crown to count"
                }
                if (state.isPinchGestureEnabled) {
                    OneHandedGestureClickIndicator(
                        gestureConfiguration = gestureConfiguration,
                        state = gestureIndicatorState,
                        gestureIndicatorTint = GauraGold
                    ) {
                        Text(
                            text = inputHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceMuted,
                            fontSize = 9.sp
                        )
                    }
                } else {
                    Text(
                        text = inputHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceMuted,
                        fontSize = 9.sp
                    )
                }
            }

            // Bottom Control Action Row (Hidden during ambient mode)
            if (!isAmbient) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Undo Button
                    Button(
                        onClick = { viewModel.undoLastBead() },
                        enabled = state.canUndo,
                        modifier = Modifier
                            .size(48.dp)
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

                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))

                    // Settings Button
                    Button(
                        onClick = { showSettings = true },
                        modifier = Modifier.size(48.dp),
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
    }
}
