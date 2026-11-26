package com.gauramala.wear

import android.content.Intent
import android.content.LocusId
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.rememberAmbientModeManager
import com.gauramala.wear.presentation.MantraCounterViewModel
import com.gauramala.wear.presentation.theme.GauraMalaTheme
import com.gauramala.wear.presentation.ui.MantraCounterScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MantraCounterViewModel by lazy {
        AutoCountSessionStore.getOrCreate(application)
    }
    private var openRoundPickerOnLaunch by mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        viewModel.refreshDailySession()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    override fun onStop() {
        viewModel.cancelVoiceRecording()
        WearSurfaceUpdater.flushPendingTileUpdate(this)
        super.onStop()
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            AutoCountSessionStore.releaseIfInactive()
        }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLocusContext(LocusId(AutoCountOngoingActivity.LOCUS_ID), null)
        handleLaunchIntent(intent)

        // Observe screen wake preference; Auto Count ongoing activity and reminders are
        // managed directly by MantraCounterViewModel so they stay active in the background.
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        setContent {
            GauraMalaTheme {
                val ambientModeManager = rememberAmbientModeManager()
                CompositionLocalProvider(
                    LocalAmbientModeManager provides ambientModeManager
                ) {
                    MantraCounterScreen(
                        viewModel = viewModel,
                        openRoundPickerInitially = openRoundPickerOnLaunch,
                        onRoundPickerLaunchConsumed = { openRoundPickerOnLaunch = false }
                    )
                }
            }
        }
    }

    internal fun handleLaunchIntent(launchIntent: Intent?) {
        if (launchIntent?.getBooleanExtra(ChantReminderScheduler.EXTRA_OPEN_ROUND_PICKER, false) == true) {
            launchIntent.removeExtra(ChantReminderScheduler.EXTRA_OPEN_ROUND_PICKER)
            openRoundPickerOnLaunch = true
        }
    }
}
