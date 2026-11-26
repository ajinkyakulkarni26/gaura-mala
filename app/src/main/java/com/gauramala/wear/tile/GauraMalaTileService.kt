package com.gauramala.wear.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.TypeBuilders
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.gauramala.wear.AutoCountSessionStore
import com.gauramala.wear.MainActivity
import com.gauramala.wear.R
import com.gauramala.wear.WearSurfaceUpdater
import com.gauramala.wear.data.MantraPreferences
import com.gauramala.wear.data.UserPreferences
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future

private const val RESOURCES_VERSION = "1"
private const val BEADS_PER_ROUND = 108
private const val MAX_DYNAMIC_SECONDS = 2_000_000L

/**
 * Glanceable Wear OS Tile allowing devotees to inspect their daily chanting
 * status with a single swipe without launching the full application.
 */
class GauraMalaTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest) = serviceScope.future {
        val targetVersion = WearSurfaceUpdater.currentStateVersion()
        val liveSessionPrefs = AutoCountSessionStore.peek()?.currentTilePreferencesSnapshot()
        val userPrefs = if (liveSessionPrefs != null) {
            liveSessionPrefs
        } else {
            val prefs = MantraPreferences(this@GauraMalaTileService)
            prefs.resetIfNewDay()
            prefs.userPreferencesFlow.first()
        }
        val tileContent = GauraMalaTileContent.from(userPrefs)
        val liveTotalBeads = buildLiveAutoCountTotalBeads(
            preferences = userPrefs,
            deviceConfiguration = requestParams.deviceConfiguration
        )
        WearSurfaceUpdater.onTileRendered(targetVersion)

        val roundTextElement = if (liveTotalBeads != null) {
            val dynamicRound = DynamicString.constant("Round ")
                .concat(liveTotalBeads.div(BEADS_PER_ROUND).format())
                .concat(DynamicString.constant(" / ${userPrefs.dailyGoalRounds}"))
            Text.Builder(
                this@GauraMalaTileService,
                TypeBuilders.StringProp.Builder(tileContent.roundProgress)
                    .setDynamicValue(dynamicRound)
                    .build(),
                TypeBuilders.StringLayoutConstraint.Builder("Round 999 / ${userPrefs.dailyGoalRounds}")
                    .setAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
                    .build()
            )
                .setColor(argb(0xFFFFFFFF.toInt()))
                .setTypography(androidx.wear.protolayout.material.Typography.TYPOGRAPHY_TITLE2)
                .build()
        } else {
            Text.Builder(this@GauraMalaTileService, tileContent.roundProgress)
                .setColor(argb(0xFFFFFFFF.toInt()))
                .setTypography(androidx.wear.protolayout.material.Typography.TYPOGRAPHY_TITLE2)
                .build()
        }

        val beadTextElement = if (liveTotalBeads != null) {
            val dynamicBead = DynamicString.constant("Auto Count · ")
                .concat(liveTotalBeads.rem(BEADS_PER_ROUND).format())
                .concat(DynamicString.constant(" / $BEADS_PER_ROUND"))
            Text.Builder(
                this@GauraMalaTileService,
                TypeBuilders.StringProp.Builder(tileContent.beadProgress)
                    .setDynamicValue(dynamicBead)
                    .build(),
                TypeBuilders.StringLayoutConstraint.Builder("Auto Count · 108 / 108")
                    .setAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
                    .build()
            )
                .setColor(argb(0xFFFFE082.toInt()))
                .setTypography(androidx.wear.protolayout.material.Typography.TYPOGRAPHY_BODY2)
                .build()
        } else {
            Text.Builder(this@GauraMalaTileService, tileContent.beadProgress)
                .setColor(argb(0xFFFFE082.toInt()))
                .setTypography(androidx.wear.protolayout.material.Typography.TYPOGRAPHY_BODY2)
                .build()
        }

        val primaryLayout = materialScope(
            context = this@GauraMalaTileService,
            deviceConfiguration = requestParams.deviceConfiguration
        ) {
            primaryLayout(
                titleSlot = {
                    Text.Builder(this@GauraMalaTileService, getString(R.string.tile_name))
                        .setColor(argb(0xFFFFB300.toInt()))
                        .setTypography(androidx.wear.protolayout.material.Typography.TYPOGRAPHY_CAPTION1)
                        .build()
                },
                mainSlot = {
                    LayoutElementBuilders.Column.Builder()
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .addContent(roundTextElement)
                        .addContent(beadTextElement)
                        .build()
                },
                bottomSlot = {
                    CompactChip.Builder(
                        this@GauraMalaTileService,
                        tileContent.actionLabel,
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
                }
            )
        }

        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(if (userPrefs.autoCountRunning) 30_000L else 0L)
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

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onTileEnterEvent(requestParams: EventBuilders.TileEnterEvent) {
        super.onTileEnterEvent(requestParams)
        WearSurfaceUpdater.requestTileEnterRefresh(this)
    }

    override fun onRecentInteractionEventsAsync(
        events: MutableList<EventBuilders.TileInteractionEvent>
    ) = serviceScope.future<Void?> {
        if (events.any { it.eventType == EventBuilders.TileInteractionEvent.ENTER }) {
            WearSurfaceUpdater.requestTileEnterRefresh(this@GauraMalaTileService)
        }
        null
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest) = serviceScope.future {
        ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
    }

    private fun buildLiveAutoCountTotalBeads(
        preferences: UserPreferences,
        deviceConfiguration: DeviceParametersBuilders.DeviceParameters
    ): DynamicInt32? {
        if (!preferences.autoCountActive || !preferences.autoCountRunning) return null
        val intervalMillis = preferences.autoCountIntervalMillis?.takeIf { it > 0 } ?: return null
        val anchorEpochMillis = preferences.autoCountAnchorEpochMillis.takeIf { it > 0L } ?: return null
        val schema = deviceConfiguration.rendererSchemaVersion
        if (schema.major < 1 || (schema.major == 1 && schema.minor < 200)) return null

        val baseTotalBeads = (preferences.completedRounds.coerceIn(0, 999) * BEADS_PER_ROUND) +
            preferences.beadCount.coerceIn(0, BEADS_PER_ROUND - 1)
        val maxTotalBeads = if (preferences.completedRounds < preferences.dailyGoalRounds) {
            preferences.dailyGoalRounds.coerceIn(1, 64) * BEADS_PER_ROUND
        } else {
            (999 * BEADS_PER_ROUND) + (BEADS_PER_ROUND - 1)
        }
        val maxAddedBeads = (maxTotalBeads - baseTotalBeads).coerceAtLeast(0)
        if (maxAddedBeads == 0) return DynamicInt32.constant(baseTotalBeads)

        val anchorSeconds = anchorEpochMillis / 1_000L
        val subSecondMillis = (anchorEpochMillis % 1_000L).toInt().coerceAtLeast(0)
        val capThresholdMillis = (maxAddedBeads.toLong() * intervalMillis.toLong()) + subSecondMillis.toLong()
        val capSeconds = ((capThresholdMillis + 999L) / 1_000L)
            .coerceIn(1L, MAX_DYNAMIC_SECONDS)
            .toInt()

        val elapsedSeconds = DynamicInstant.withSecondsPrecision(
            Instant.ofEpochSecond(anchorSeconds)
        )
            .durationUntil(DynamicInstant.platformTimeWithSecondsPrecision())
            .toIntSeconds()

        val addedBeads = DynamicInt32.onCondition(elapsedSeconds.lte(0))
            .use(0)
            .elseUse(
                DynamicInt32.onCondition(elapsedSeconds.gte(capSeconds))
                    .use(maxAddedBeads)
                    .elseUse(
                        elapsedSeconds
                            .times(1_000)
                            .minus(subSecondMillis)
                            .div(intervalMillis)
                    )
            )
        return addedBeads.plus(baseTotalBeads)
    }
}
