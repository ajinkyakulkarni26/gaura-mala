package com.gauramala.wear.presentation.ui

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.gauramala.wear.presentation.theme.GauraGold
import com.gauramala.wear.presentation.theme.OnSurfaceWhite
import com.gauramala.wear.presentation.theme.SurfaceDark

private const val PRIVACY_CONTACT_URL = "https://github.com/ajinkyakulkarni26/gaura-mala/issues"

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val remoteActivityHelper = remember(context) {
        RemoteActivityHelper(context, mainExecutor)
    }
    var showPhoneFallback by remember { mutableStateOf(false) }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().hierarchicalFocusGroup(active = true),
        state = listState,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
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
        item { PolicySection("Gaura Mala", "Effective: Sep 30, 2026") }
        item {
            PolicySection(
                "Information and storage",
                "Gaura Mala has no account, ads, or analytics and sends no personal data to a server. Your bead count, rounds, daily goal, and settings stay on this watch."
            )
        }
        item {
            PolicySection(
                "Daily reset and deletion",
                "Counts reset each new day. Settings remain until you clear app storage or uninstall. Backups are off."
            )
        }
        item {
            PolicySection(
                "Sharing and permissions",
                "We don't share local data. Vibration access is used for haptics. The app doesn't access location, contacts, microphone, camera, or health data."
            )
        }
        item {
            PolicySection(
                "Children and changes",
                "We don't knowingly collect personal information from children or adults. If data practices change, this policy will be updated first."
            )
        }
        item {
            PolicySection(
                "Contact",
                "For privacy questions, open a GitHub issue. Don't post private details in public issues."
            )
        }
        item {
            Button(
                onClick = {
                    showPhoneFallback = false
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_CONTACT_URL))
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                    try {
                        val launch = remoteActivityHelper.startRemoteActivity(intent)
                        launch.addListener(
                            {
                                showPhoneFallback = runCatching { launch.get() }.isFailure
                            },
                            mainExecutor
                        )
                    } catch (_: Exception) {
                        showPhoneFallback = true
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = OnSurfaceWhite)
            ) {
                Text("Open GitHub on phone", fontSize = 12.sp)
            }
        }
        if (showPhoneFallback) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Phone unavailable. Open on phone:",
                        color = OnSurfaceWhite,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "github.com/ajinkyakulkarni26/gaura-mala/issues",
                        color = GauraGold,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { showPhoneFallback = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceDark,
                            contentColor = OnSurfaceWhite
                        )
                    ) {
                        Text("Dismiss", fontSize = 12.sp)
                    }
                }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            title,
            color = GauraGold,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Text(
            body,
            color = OnSurfaceWhite,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
