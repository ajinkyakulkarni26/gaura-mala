package com.gauramala.wear.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.gauramala.wear.presentation.MantraUiState
import com.gauramala.wear.presentation.theme.GauraGold
import com.gauramala.wear.presentation.theme.SuccessGreen

@Composable
fun SummaryDialog(
    state: MantraUiState,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    RoundScrollContainer(
        scrollState = scrollState,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.Center,
        scrollIndicatorTag = "summary-scroll-indicator"
    ) {
        Text(
            text = "Hari Bol! 🙏",
            style = MaterialTheme.typography.titleMedium,
            color = GauraGold,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.roundScrollItem()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Daily goal reached",
            style = MaterialTheme.typography.bodyMedium,
            color = SuccessGreen,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.roundScrollItem()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${state.completedRounds} Rounds",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.roundScrollItem()
        )
        Text(
            text = "${state.totalMantrasChanted} mantras",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.roundScrollItem()
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .roundScrollItem(),
            colors = ButtonDefaults.buttonColors(containerColor = GauraGold)
        ) {
            Text(
                "Continue",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
