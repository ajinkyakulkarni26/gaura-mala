package com.gauramala.wear

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.gauramala.wear.presentation.MantraCounterViewModel

/**
 * Retains the active counter ViewModel across Wear OS back-swipe activity dismissal while an
 * Auto Count ongoing activity is active, so counting, audio, and the Ongoing Activity indicator
 * remain alive when the user visits the watch face, recent apps, or tile carousel.
 */
internal object AutoCountSessionStore {
    private var store: ViewModelStore? = null
    private var activeViewModel: MantraCounterViewModel? = null

    @Synchronized
    fun getOrCreate(application: Application): MantraCounterViewModel {
        val current = activeViewModel
        if (current != null) return current

        val nextStore = ViewModelStore()
        val nextViewModel = ViewModelProvider(
            nextStore,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MantraCounterViewModel::class.java]
        store = nextStore
        activeViewModel = nextViewModel
        return nextViewModel
    }

    @Synchronized
    fun releaseIfInactive() {
        val current = activeViewModel ?: return
        if (!current.uiState.value.autoCountActive) {
            clear()
        }
    }

    @Synchronized
    fun clear() {
        activeViewModel?.stopAutoCount()
        store?.clear()
        store = null
        activeViewModel = null
    }
}

