package com.gauramala.wear

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.gauramala.wear.complication.GauraMalaComplicationService
import com.gauramala.wear.tile.GauraMalaTileService

/** Requests fresh progress on the Wear OS tile and any active watch-face complications. */
object WearSurfaceUpdater {
    fun requestUpdate(context: Context, updateComplications: Boolean = true) {
        val appContext = context.applicationContext
        TileService.getUpdater(appContext).requestUpdate(GauraMalaTileService::class.java)
        if (updateComplications) {
            ComplicationDataSourceUpdateRequester.create(
                appContext,
                ComponentName(appContext, GauraMalaComplicationService::class.java)
            ).requestUpdateAll()
        }
    }
}
