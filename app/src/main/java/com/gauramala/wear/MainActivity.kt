package com.gauramala.wear

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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

    private val viewModel: MantraCounterViewModel by viewModels()
    private val autoCountOngoingActivity by lazy { AutoCountOngoingActivity(this) }
    private val chantReminderScheduler by lazy { ChantReminderScheduler(this) }
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
        viewModel.pauseAutoCount()
        viewModel.cancelVoiceRecording()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleLaunchIntent(intent)

        // Observe screen wake preference, Auto Count ongoing notification, and chanting reminders
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                autoCountOngoingActivity.update(
                    // A paused session keeps a return notification, but pausing has already
                    // stopped the timer/audio and released the Auto Count wake lock.
                    isActive = state.autoCountActive,
                    isRunning = state.autoCountRunning
                )
                chantReminderScheduler.sync(state)
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
