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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.SwitchButtonDefaults
import androidx.wear.compose.material3.Text
import com.gauramala.wear.presentation.MantraCounterViewModel
import com.gauramala.wear.presentation.MantraUiState
import com.gauramala.wear.presentation.theme.AlertRed
import com.gauramala.wear.presentation.theme.GauraGold
import com.gauramala.wear.presentation.theme.OnSurfaceMuted
import com.gauramala.wear.presentation.theme.OnSurfaceWhite
import com.gauramala.wear.presentation.theme.SurfaceDark

@Composable
fun SettingsDialog(
    state: MantraUiState,
    viewModel: MantraCounterViewModel,
    onDismiss: () -> Unit
) {
    var pendingReset by remember { mutableStateOf<ResetTarget?>(null) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    BackHandler(enabled = showPrivacyPolicy) {
        showPrivacyPolicy = false
    }

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
                text = if (isDailyReset) "Reset today's count?" else "Reset this round?",
                style = MaterialTheme.typography.titleMedium,
                color = AlertRed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isDailyReset) {
                    "Clears today's rounds and beads."
                } else {
                    "Clears beads in this round."
                },
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(0.75f),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = { pendingReset = null },
                    modifier = Modifier.size(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceDark,
                        contentColor = OnSurfaceWhite
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel reset", modifier = Modifier.size(22.dp))
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
                    modifier = Modifier.size(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Confirm reset", modifier = Modifier.size(22.dp))
                }
            }
        }
        return
    }

    if (showPrivacyPolicy) {
        PrivacyPolicyScreen(onBack = { showPrivacyPolicy = false })
        return
    }

    val scrollState = rememberScrollState()
    val rotaryFocusRequester = remember { FocusRequester() }
    val switchButtonColors = SwitchButtonDefaults.switchButtonColors(
        checkedContainerColor = SurfaceDark,
        checkedContentColor = OnSurfaceWhite,
        checkedSecondaryContentColor = OnSurfaceMuted,
        checkedThumbColor = GauraGold,
        checkedTrackColor = GauraGold.copy(alpha = 0.55f),
        uncheckedContainerColor = SurfaceDark,
        uncheckedContentColor = OnSurfaceWhite,
        uncheckedSecondaryContentColor = OnSurfaceMuted,
        uncheckedThumbColor = OnSurfaceMuted,
        uncheckedTrackColor = OnSurfaceMuted.copy(alpha = 0.35f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .hierarchicalFocusGroup(active = true)
            .requestFocusOnHierarchyActive()
            .rotaryScrollable(
                behavior = RotaryScrollableDefaults.behavior(scrollableState = scrollState),
                focusRequester = rotaryFocusRequester
            )
            .verticalScroll(scrollState)
            .padding(horizontal = 6.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleMedium,
            color = GauraGold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Daily goal", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.updateDailyGoal(state.dailyGoalRounds - 1) },
                    enabled = state.dailyGoalRounds > 1,
                    modifier = Modifier.size(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceDark,
                        contentColor = OnSurfaceWhite
                    )
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease daily goal",
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = "${state.dailyGoalRounds} rounds",
                    modifier = Modifier.weight(1f),
                    color = GauraGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Button(
                    onClick = { viewModel.updateDailyGoal(state.dailyGoalRounds + 1) },
                    enabled = state.dailyGoalRounds < 64,
                    modifier = Modifier.size(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceDark,
                        contentColor = OnSurfaceWhite
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase daily goal",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Text(
                "1–64 rounds",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }

        // 1. Gesture Pinch Toggle
        SwitchButton(
            checked = state.isPinchGestureEnabled && state.supportsOneHandedGestures,
            onCheckedChange = { viewModel.togglePinchGesture(it) },
            label = { Text("Double Pinch", fontSize = 12.sp) },
            secondaryLabel = {
                Text(
                    if (state.supportsOneHandedGestures) {
                        "Works in dim mode"
                    } else {
                        "Not supported here"
                    },
                    fontSize = 10.sp
                )
            },
            enabled = state.supportsOneHandedGestures,
            colors = switchButtonColors,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )

        // 2. Screen Tap Toggle
        SwitchButton(
            checked = state.isScreenTapEnabled,
            onCheckedChange = { viewModel.toggleScreenTap(it) },
            label = { Text("Screen Tap", fontSize = 12.sp) },
            secondaryLabel = { Text("Tap to count", fontSize = 10.sp) },
            colors = switchButtonColors,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )

        // 3. Haptics Toggle
        SwitchButton(
            checked = state.isHapticsEnabled,
            onCheckedChange = { viewModel.toggleHaptics(it) },
            label = { Text("Haptics", fontSize = 12.sp) },
            secondaryLabel = { Text("Bead and milestone cues", fontSize = 10.sp) },
            colors = switchButtonColors,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )

        // 4. Milestone Vibrations
        SwitchButton(
            checked = state.isMilestonesEnabled,
            onCheckedChange = { viewModel.toggleMilestones(it) },
            label = { Text("Milestones", fontSize = 12.sp) },
            secondaryLabel = { Text("Ticks at 27, 54, 81", fontSize = 10.sp) },
            colors = switchButtonColors,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )

        // 5. Keep Screen Awake
        SwitchButton(
            checked = state.keepScreenOn,
            onCheckedChange = { viewModel.toggleKeepScreenOn(it) },
            label = { Text("Keep Awake", fontSize = 12.sp) },
            secondaryLabel = { Text("Keeps screen on; uses more power", fontSize = 10.sp) },
            colors = switchButtonColors,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )

        // 6. Reset Current Round
        Button(
            onClick = {
                pendingReset = ResetTarget.CURRENT_ROUND
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceDark,
                contentColor = OnSurfaceWhite
            )
        ) {
            Text("Reset Round", fontSize = 12.sp)
        }

        // 7. Reset Entire Day
        Button(
            onClick = { pendingReset = ResetTarget.DAILY_COUNT },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AlertRed.copy(alpha = 0.2f))
        ) {
            Text("Reset Today", color = AlertRed, fontSize = 12.sp)
        }

        Button(
            onClick = { showPrivacyPolicy = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceDark,
                contentColor = OnSurfaceWhite
            )
        ) {
            Text("Privacy Policy", fontSize = 12.sp)
        }

        // Close / Back button
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GauraGold)
        ) {
            Text("Done", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private enum class ResetTarget {
    CURRENT_ROUND,
    DAILY_COUNT
}
