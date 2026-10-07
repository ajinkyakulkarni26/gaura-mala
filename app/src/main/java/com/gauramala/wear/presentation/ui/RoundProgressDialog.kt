package com.gauramala.wear.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.gauramala.wear.presentation.MantraUiState
import com.gauramala.wear.presentation.theme.GauraGold
import com.gauramala.wear.presentation.theme.OnSurfaceWhite
import com.gauramala.wear.presentation.theme.SurfaceDark

@Composable
fun RoundProgressDialog(
    state: MantraUiState,
    onDismiss: () -> Unit,
    onSelectionTick: () -> Unit,
    onSetRounds: (Int) -> Unit
) {
    var selectedRounds by remember(state.completedRounds) {
        mutableIntStateOf(state.completedRounds.coerceAtMost(MAX_COMPLETED_ROUNDS))
    }
    val maxRounds = maxOf(MAX_COMPLETED_ROUNDS, state.completedRounds)
    val rotaryThreshold = with(LocalDensity.current) { 16.dp.toPx() }
    val rotaryInput = remember(rotaryThreshold) { RoundAdjustmentRotaryInput(rotaryThreshold) }
    val rotaryFocusRequester = remember { FocusRequester() }

    BackHandler(onBack = onDismiss)
    LaunchedEffect(rotaryFocusRequester) {
        rotaryFocusRequester.requestFocus()
    }

    fun adjustRoundsBy(delta: Int) {
        val updatedRounds = (selectedRounds.toLong() + delta)
            .coerceIn(0L, maxRounds.toLong())
            .toInt()
        if (updatedRounds != selectedRounds) {
            selectedRounds = updatedRounds
            onSelectionTick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onRotaryScrollEvent { event ->
                val roundSteps = rotaryInput.consume(
                    deltaPx = event.verticalScrollPixels,
                    eventUptimeMs = event.uptimeMillis
                )
                if (roundSteps != 0) {
                    adjustRoundsBy(roundSteps)
                }
                true
            }
            .focusRequester(rotaryFocusRequester)
            .focusable()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Rounds",
            style = MaterialTheme.typography.labelSmall,
            color = GauraGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { adjustRoundsBy(-1) },
                enabled = selectedRounds > 0,
                modifier = Modifier.size(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceDark,
                    contentColor = OnSurfaceWhite
                )
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease completed rounds", modifier = Modifier.size(26.dp))
            }

            Text(
                text = "$selectedRounds",
                modifier = Modifier.weight(1f).testTag("selected-completed-rounds"),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Button(
                onClick = { adjustRoundsBy(1) },
                enabled = selectedRounds < maxRounds,
                modifier = Modifier.size(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceDark,
                    contentColor = OnSurfaceWhite
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase completed rounds", modifier = Modifier.size(26.dp))
            }
        }

        Text(
            text = "Next round: ${selectedRounds + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        if (state.beadCount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Saving clears ${state.beadCount}/108 beads.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.75f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceDark,
                    contentColor = OnSurfaceWhite
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel round adjustment", modifier = Modifier.size(22.dp))
            }
            Button(
                onClick = { onSetRounds(selectedRounds) },
                modifier = Modifier.size(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GauraGold)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Set completed rounds",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private const val MAX_COMPLETED_ROUNDS = 999

private class RoundAdjustmentRotaryInput(
    private val thresholdPx: Float,
    private val idleResetMs: Long = 180L
) {
    private var accumulatedPx = 0f
    private var lastEventUptimeMs: Long? = null

    init {
        require(thresholdPx > 0f && thresholdPx.isFinite())
        require(idleResetMs >= 0L)
    }

    fun consume(deltaPx: Float, eventUptimeMs: Long): Int {
        val previousEventTime = lastEventUptimeMs
        if (previousEventTime != null &&
            (eventUptimeMs < previousEventTime || eventUptimeMs - previousEventTime > idleResetMs)
        ) {
            accumulatedPx = 0f
        }
        lastEventUptimeMs = eventUptimeMs

        if (!deltaPx.isFinite()) {
            accumulatedPx = 0f
            return 0
        }
        if (deltaPx == 0f) return 0
        if (accumulatedPx != 0f && accumulatedPx.sign != deltaPx.sign) {
            accumulatedPx = 0f
        }

        accumulatedPx += deltaPx
        val direction = accumulatedPx.sign.toInt()
        val steps = (kotlin.math.abs(accumulatedPx) / thresholdPx).toInt()
        if (steps == 0) return 0

        accumulatedPx -= direction * steps * thresholdPx
        return direction * steps
    }

    private val Float.sign: Float
        get() = when {
            this > 0f -> 1f
            this < 0f -> -1f
            else -> 0f
        }
}
