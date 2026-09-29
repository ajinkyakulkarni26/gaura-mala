package com.gauramala.wear.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.gauramala.wear.MainActivity
import com.gauramala.wear.data.MantraPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future

private const val RESOURCES_VERSION = "1"

/**
 * Glanceable Wear OS Tile allowing devotees to inspect their daily chanting
 * status with a single swipe without launching the full application.
 */
class GauraMalaTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest) = serviceScope.future {
        val prefs = MantraPreferences(this@GauraMalaTileService)
        val userPrefs = prefs.userPreferencesFlow.first()

        val primaryLayout = PrimaryLayout.Builder(requestParams.deviceConfiguration)
            .setPrimaryLabelTextContent(
                Text.Builder(this@GauraMalaTileService, "GauraMala 📿")
                    .setColor(argb(0xFFFFB300.toInt()))
                    .setTypography(androidx.wear.protolayout.material.Typography.TYPOGRAPHY_CAPTION1)
                    .build()
            )
            .setContent(
                LayoutElementBuilders.Column.Builder()
                    .addContent(
                        Text.Builder(this@GauraMalaTileService, "Round ${userPrefs.completedRounds} / ${userPrefs.dailyGoalRounds}")
                            .setColor(argb(0xFFFFFFFF.toInt()))
                            .setTypography(androidx.wear.protolayout.material.Typography.TYPOGRAPHY_TITLE2)
                            .build()
                    )
                    .addContent(
                        Text.Builder(this@GauraMalaTileService, "${userPrefs.beadCount} / 108 Beads")
                            .setColor(argb(0xFFFFE082.toInt()))
                            .setTypography(androidx.wear.protolayout.material.Typography.TYPOGRAPHY_BODY2)
                            .build()
                    )
                    .build()
            )
            .setPrimaryChipContent(
                CompactChip.Builder(
                    this@GauraMalaTileService,
                    "Chant",
                    ModifiersBuilders.Clickable.Builder()
                        .setOnClick(
                            ActionBuilders.LaunchAction.Builder()
                                .setAndroidActivity(
                                    ActionBuilders.AndroidActivity.Builder()
                                        .setPackageName(packageName)
                                        .setClassName(MainActivity::class.java.name)
                                        .build()
                                )
                                .build()
                        )
                        .build(),
                    requestParams.deviceConfiguration
                )
                .build()
            )
            .build()

        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(primaryLayout)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest) = serviceScope.future {
        ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
    }
}
