package com.gauramala.wear

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
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

    override fun onResume() {
        super.onResume()
        viewModel.refreshDailySession()
    }

    override fun onStop() {
        viewModel.pauseAutoCount()
        viewModel.cancelVoiceRecording()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Observe screen wake preference
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
            }
        }

        setContent {
            GauraMalaTheme {
                val ambientModeManager = rememberAmbientModeManager()
                CompositionLocalProvider(
                    LocalAmbientModeManager provides ambientModeManager
                ) {
                    MantraCounterScreen(viewModel = viewModel)
                }
            }
        }
    }
}
