package com.gauramala.wear.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.gauramala.wear.presentation.theme.SurfaceDark

@Composable
fun SettingsDialog(
    state: MantraUiState,
    viewModel: MantraCounterViewModel,
    onDismiss: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    if (showResetConfirm) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Reset All Rounds?",
                style = MaterialTheme.typography.titleMedium,
                color = AlertRed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This resets your daily count to 0.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { showResetConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
                ) {
                    Text("Cancel", fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        viewModel.resetDailyCount()
                        showResetConfirm = false
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

        // 1. Gesture Pinch Toggle
        item {
            SwitchButton(
                checked = state.isPinchGestureEnabled,
                onCheckedChange = { viewModel.togglePinchGesture(it) },
                label = { Text("Double Pinch") },
                secondaryLabel = { Text("Hands-free gesture") },
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
                secondaryLabel = { Text("Prevent screen sleep") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }

        // 6. Reset Current Round
        item {
            Button(
                onClick = {
                    viewModel.resetCurrentRound()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
            ) {
                Text("Reset Current Round", fontSize = 12.sp)
            }
        }

        // 7. Reset Entire Day
        item {
            Button(
                onClick = { showResetConfirm = true },
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
