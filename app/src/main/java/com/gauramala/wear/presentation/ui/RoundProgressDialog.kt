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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onSetRounds: (Int) -> Unit
) {
    var selectedRounds by remember(state.completedRounds) {
        mutableIntStateOf(state.completedRounds.coerceAtMost(MAX_COMPLETED_ROUNDS))
    }
    val maxRounds = maxOf(MAX_COMPLETED_ROUNDS, state.completedRounds)

    BackHandler(onBack = onDismiss)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Set today's rounds",
            style = MaterialTheme.typography.labelLarge,
            color = GauraGold,
            fontSize = 14.sp,
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
                onClick = { selectedRounds = (selectedRounds - 1).coerceAtLeast(0) },
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
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Button(
                onClick = { selectedRounds = (selectedRounds + 1).coerceAtMost(maxRounds) },
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
            text = "Next watch round: ${selectedRounds + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        if (state.beadCount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Setting this will clear ${state.beadCount}/108 beads in the current round.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceDark,
                    contentColor = OnSurfaceWhite
                )
            ) {
                Text("Cancel", fontSize = 11.sp, maxLines = 1)
            }
            Button(
                onClick = { onSetRounds(selectedRounds) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GauraGold)
            ) {
                Text("Set", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private const val MAX_COMPLETED_ROUNDS = 999
