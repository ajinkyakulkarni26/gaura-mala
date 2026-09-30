package com.gauramala.wear.presentation.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.gauramala.wear.presentation.theme.GauraGold
import com.gauramala.wear.presentation.theme.OnSurfaceWhite
import com.gauramala.wear.presentation.theme.SurfaceDark

private const val PRIVACY_CONTACT_URL = "https://github.com/ajinkyakulkarni26/gaura-mala/issues"

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().hierarchicalFocusGroup(active = true),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 18.dp),
        rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Privacy Policy",
                style = MaterialTheme.typography.titleMedium,
                color = GauraGold,
                fontWeight = FontWeight.Bold
            )
        }
        item { PolicySection("Gaura Mala", "Effective date: September 30, 2026") }
        item {
            PolicySection(
                "Information and storage",
                "Gaura Mala does not collect or send personal information to a server. The app has no account, ads, or analytics. It stores your bead count, completed rounds, daily goal, and app settings locally on your watch so your progress and preferences are available when you reopen the app."
            )
        }
        item {
            PolicySection(
                "Daily reset and deletion",
                "The daily bead and round count resets when the local date changes. Your preferences remain on the watch until you clear the app's storage or uninstall Gaura Mala. You can also reset today's count in Settings. Android app backup is disabled for Gaura Mala."
            )
        }
        item {
            PolicySection(
                "Sharing and permissions",
                "The app does not share this locally stored information with the developer or other companies. The app requests vibration access for haptic feedback. It does not request access to your location, contacts, microphone, camera, or health data."
            )
        }
        item {
            PolicySection(
                "Children and changes",
                "Gaura Mala does not knowingly collect personal information from children or adults. If the app's data practices change, this policy will be updated before the change takes effect."
            )
        }
        item {
            PolicySection(
                "Contact",
                "For privacy questions, contact the developer through the Gaura Mala GitHub issue page. Please do not post private information in a public issue."
            )
        }
        item {
            Button(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_CONTACT_URL)))
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = OnSurfaceWhite)
            ) {
                Text("Contact developer", fontSize = 12.sp)
            }
        }
        item {
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GauraGold)
            ) {
                Text("Back to settings", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Text(title, color = GauraGold, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    Text(body, color = OnSurfaceWhite, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
}
