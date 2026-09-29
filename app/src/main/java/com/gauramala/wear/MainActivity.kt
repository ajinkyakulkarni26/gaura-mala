package com.gauramala.wear

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.gauramala.wear.presentation.MantraCounterViewModel
import com.gauramala.wear.presentation.theme.GauraMalaTheme
import com.gauramala.wear.presentation.ui.MantraCounterScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MantraCounterViewModel by viewModels()

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
            }
        }

        setContent {
            GauraMalaTheme {
                MantraCounterScreen(viewModel = viewModel)
            }
        }
    }
}
