package com.gauramala.wear

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.gauramala.wear.complication.GauraMalaComplicationService
import com.gauramala.wear.tile.GauraMalaTileService
import java.util.concurrent.atomic.AtomicLong

/**
 * Requests fresh progress on the Wear OS tile and any active watch-face complications.
 *
 * Wear OS throttles back-to-back [TileService.getUpdater] calls (~20 seconds). Tracking state vs.
 * requested/rendered versions prevents duplicate bursts (such as firing on Pause, on `onStop`, and
 * again on Tile `ENTER`) from delaying urgent Auto Count transitions.
 */
object WearSurfaceUpdater {
    private const val TILE_BEAD_UPDATE_INTERVAL_MS = 25_000L
    private const val TILE_ENTER_DEBOUNCE_MS = 2_000L

    private val stateVersion = AtomicLong(1L)
    private val requestedTileVersion = AtomicLong(0L)
    private val renderedTileVersion = AtomicLong(0L)

    @Volatile
    private var lastTileUpdateElapsedMs: Long = 0L

    fun markStateChanged(): Long = stateVersion.incrementAndGet()

    fun currentStateVersion(): Long = stateVersion.get()

    fun onTileRendered(version: Long = stateVersion.get()) {
        renderedTileVersion.updateAndGet { current -> maxOf(current, version) }
        requestedTileVersion.updateAndGet { current -> maxOf(current, version) }
        lastTileUpdateElapsedMs = SystemClock.elapsedRealtime()
    }

    fun requestTileEnterRefresh(context: Context) {
        val currentVersion = stateVersion.get()
        if (renderedTileVersion.get() >= currentVersion) return
        val nowElapsedMs = SystemClock.elapsedRealtime()
        if (lastTileUpdateElapsedMs == 0L ||
            (nowElapsedMs - lastTileUpdateElapsedMs) >= TILE_ENTER_DEBOUNCE_MS
        ) {
            requestTileForVersion(context.applicationContext, currentVersion, nowElapsedMs)
        }
    }

    fun flushPendingTileUpdate(context: Context) {
        val currentVersion = stateVersion.get()
        if (requestedTileVersion.get() >= currentVersion) return
        requestTileForVersion(
            context.applicationContext,
            currentVersion,
            SystemClock.elapsedRealtime()
        )
    }

    fun requestUpdate(
        context: Context,
        updateComplications: Boolean = true,
        forceTileUpdate: Boolean = true
    ) {
        val appContext = context.applicationContext
        val version = stateVersion.incrementAndGet()
        val nowElapsedMs = SystemClock.elapsedRealtime()
        val shouldUpdateTile = forceTileUpdate ||
            lastTileUpdateElapsedMs == 0L ||
            (nowElapsedMs - lastTileUpdateElapsedMs) >= TILE_BEAD_UPDATE_INTERVAL_MS

        if (shouldUpdateTile) {
            requestTileForVersion(appContext, version, nowElapsedMs)
        }
        if (updateComplications) {
            ComplicationDataSourceUpdateRequester.create(
                appContext,
                ComponentName(appContext, GauraMalaComplicationService::class.java)
            ).requestUpdateAll()
        }
    }

    private fun requestTileForVersion(appContext: Context, version: Long, nowElapsedMs: Long) {
        requestedTileVersion.updateAndGet { current -> maxOf(current, version) }
        lastTileUpdateElapsedMs = nowElapsedMs
        TileService.getUpdater(appContext).requestUpdate(GauraMalaTileService::class.java)
    }
}
