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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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

    // Dialog Overlays
    if (showSettings) {
        SettingsDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = { showSettings = false }
        )
        return
    }

    if (state.showGoalAchievedDialog) {
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
            // Advance bead when user turns the watch crown downwards
            if (event.verticalScrollPixels > 20f) {
                viewModel.incrementBead()
                true
            } else false
        }

    // Full-screen tap / primary gesture binding (also triggered by Wear OS accessibility gestures)
    if ((state.isScreenTapEnabled || state.isPinchGestureEnabled) && !state.isAmbient) {
        containerModifier = containerModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null // Silent indication for battery preservation
        ) {
            viewModel.incrementBead()
        }
    }

    Box(
        modifier = containerModifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer Circular Bead & Round Progress Tracks
        BeadProgressRing(
            beadCount = state.beadCount,
            completedRounds = state.completedRounds,
            dailyGoal = state.dailyGoalRounds,
            isAmbient = state.isAmbient
        )

        // Center Content & Digital Bead Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Round Header Indicator
            Text(
                text = if (state.isAmbient) {
                    "R ${state.completedRounds}"
                } else {
                    "Round ${state.completedRounds + 1} of ${state.dailyGoalRounds}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (state.isAmbient) Color.White else GauraGoldLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Main Bead Number (Large, high-contrast, easily visible at arm's length)
            Text(
                text = "${state.beadCount}",
                style = MaterialTheme.typography.displayLarge,
                color = if (state.isAmbient) Color.White else GauraGold,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold
            )

            // Bead Sub-label
            Text(
                text = "/ 108",
                style = MaterialTheme.typography.labelSmall,
                color = if (state.isAmbient) Color.Gray else OnSurfaceMuted,
                fontSize = 12.sp
            )

            // Bottom Control Action Row (Hidden during ambient mode)
            if (!state.isAmbient) {
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
                            .size(34.dp)
                            .alpha(if (state.canUndo) 1f else 0.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo Bead",
                            tint = if (state.canUndo) GauraGold else OnSurfaceMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))

                    // Settings Button
                    Button(
                        onClick = { showSettings = true },
                        modifier = Modifier.size(34.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings",
                            tint = GauraGoldLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
