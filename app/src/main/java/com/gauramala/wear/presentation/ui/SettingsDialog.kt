package com.gauramala.wear.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import com.gauramala.wear.presentation.MantraCounterViewModel
import com.gauramala.wear.presentation.MantraUiState
import com.gauramala.wear.presentation.theme.AlertRed
import com.gauramala.wear.presentation.theme.GauraGold
import com.gauramala.wear.presentation.theme.OnSurfaceWhite
import com.gauramala.wear.presentation.theme.SurfaceDark

@Composable
fun SettingsDialog(
    state: MantraUiState,
    viewModel: MantraCounterViewModel,
    onDismiss: () -> Unit
) {
    var pendingReset by remember { mutableStateOf<ResetTarget?>(null) }

    BackHandler(enabled = pendingReset != null) {
        pendingReset = null
    }

    if (pendingReset != null) {
        val isDailyReset = pendingReset == ResetTarget.DAILY_COUNT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isDailyReset) "Reset Today's Count?" else "Reset Current Round?",
                style = MaterialTheme.typography.titleMedium,
                color = AlertRed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isDailyReset) {
                    "This clears today's completed rounds and bead progress."
                } else {
                    "This clears bead progress for the current round."
                },
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { pendingReset = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceDark,
                        contentColor = OnSurfaceWhite
                    )
                ) {
                    Text("Cancel", fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        if (isDailyReset) {
                            viewModel.resetDailyCount()
                        } else {
                            viewModel.resetCurrentRound()
                        }
                        pendingReset = null
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp)
                }
            }
        }
        return
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                color = GauraGold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Daily goal", color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.updateDailyGoal(state.dailyGoalRounds - 1) },
                        enabled = state.dailyGoalRounds > 1,
                        modifier = Modifier.size(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceDark,
                            contentColor = OnSurfaceWhite
                        )
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease daily goal")
                    }
                    Text(
                        text = "${state.dailyGoalRounds} rounds",
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = GauraGold,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { viewModel.updateDailyGoal(state.dailyGoalRounds + 1) },
                        enabled = state.dailyGoalRounds < 64,
                        modifier = Modifier.size(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceDark,
                            contentColor = OnSurfaceWhite
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase daily goal")
                    }
                }
                Text(
                    "Choose from 1 to 64 rounds",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }

        // 1. Gesture Pinch Toggle
        item {
            SwitchButton(
                checked = state.isPinchGestureEnabled && state.supportsOneHandedGestures,
                onCheckedChange = { viewModel.togglePinchGesture(it) },
                label = { Text("Double Pinch") },
                secondaryLabel = {
                    Text(
                        if (state.supportsOneHandedGestures) {
                            "Enabled while the screen is dimmed"
                        } else {
                            "Unavailable on this watch or emulator"
                        }
                    )
                },
                enabled = state.supportsOneHandedGestures,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }

        // 2. Screen Tap Toggle
        item {
            SwitchButton(
                checked = state.isScreenTapEnabled,
                onCheckedChange = { viewModel.toggleScreenTap(it) },
                label = { Text("Screen Tap") },
                secondaryLabel = { Text("Tap watch to count") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }

        // 3. Haptics Toggle
        item {
            SwitchButton(
                checked = state.isHapticsEnabled,
                onCheckedChange = { viewModel.toggleHaptics(it) },
                label = { Text("Haptic Vibration") },
                secondaryLabel = { Text("Tactile feedback") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }

        // 4. Milestone Vibrations
        item {
            SwitchButton(
                checked = state.isMilestonesEnabled,
                onCheckedChange = { viewModel.toggleMilestones(it) },
                label = { Text("Milestones") },
                secondaryLabel = { Text("Ticks at 27, 54, 81") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }

        // 5. Keep Screen Awake
        item {
            SwitchButton(
                checked = state.keepScreenOn,
                onCheckedChange = { viewModel.toggleKeepScreenOn(it) },
                label = { Text("Keep Awake") },
                secondaryLabel = { Text("Prevents ambient mode; uses more battery") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }

        // 6. Reset Current Round
        item {
            Button(
                onClick = {
                    pendingReset = ResetTarget.CURRENT_ROUND
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceDark,
                    contentColor = OnSurfaceWhite
                )
            ) {
                Text("Reset Current Round", fontSize = 12.sp)
            }
        }

        // 7. Reset Entire Day
        item {
            Button(
                onClick = { pendingReset = ResetTarget.DAILY_COUNT },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed.copy(alpha = 0.2f))
            ) {
                Text("Reset Today's Rounds", color = AlertRed, fontSize = 12.sp)
            }
        }

        // Close / Back button
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GauraGold)
            ) {
                Text("Done", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private enum class ResetTarget {
    CURRENT_ROUND,
    DAILY_COUNT
}
