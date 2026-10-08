package com.gauramala.wear

import android.content.ComponentName
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.StateBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.connection.DefaultTileClient
import com.gauramala.wear.data.MantraPreferences
import com.gauramala.wear.data.dataStore
import com.gauramala.wear.tile.GauraMalaTileService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TileServiceIntegrationTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearPreferences() {
        runBlocking {
            context.dataStore.edit { it.clear() }
        }
    }

    @Test
    fun tileServiceReturnsCurrentRoundsBeadProgressAndLaunchAction() = runBlocking {
        context.dataStore.edit { preferences ->
            preferences[MantraPreferences.KEY_COMPLETED_ROUNDS] = 4
            preferences[MantraPreferences.KEY_BEAD_COUNT] = 37
            preferences[MantraPreferences.KEY_DAILY_GOAL] = 12
            preferences[MantraPreferences.KEY_LAST_DATE] = LocalDate.now().toString()
        }

        val tile = requestTile()
        val tilePayload = tile.toString()

        assertEquals("1", tile.resourcesVersion)
        assertTrue("Tile should include the saved round count", tilePayload.contains("Round 4 / 12"))
        assertTrue("Tile should include the partial bead count", tilePayload.contains("37 / 108 Beads"))
        assertTrue("Tile action should open the counter activity", tilePayload.contains(MainActivity::class.java.name))
    }

    @Test
    fun tileServiceResetsCountsFromAnEarlierLocalDay() = runBlocking {
        context.dataStore.edit { preferences ->
            preferences[MantraPreferences.KEY_COMPLETED_ROUNDS] = 6
            preferences[MantraPreferences.KEY_BEAD_COUNT] = 24
            preferences[MantraPreferences.KEY_DAILY_GOAL] = 16
            preferences[MantraPreferences.KEY_LAST_DATE] = LocalDate.now().minusDays(1).toString()
        }

        val tilePayload = requestTile().toString()

        assertTrue("A previous day's rounds must not appear in today's Tile", tilePayload.contains("Round 0 / 16"))
        assertTrue("A previous day's beads must not appear in today's Tile", tilePayload.contains("0 / 108 Beads"))
    }

    private fun requestTile(): androidx.wear.tiles.TileBuilders.Tile {
        val deviceParameters = DeviceParametersBuilders.DeviceParameters.Builder()
            .setScreenWidthDp(192)
            .setScreenHeightDp(192)
            .setScreenDensity(context.resources.displayMetrics.density)
            .setScreenShape(DeviceParametersBuilders.SCREEN_SHAPE_ROUND)
            .setDevicePlatform(DeviceParametersBuilders.DEVICE_PLATFORM_WEAR_OS)
            .build()
        val request = RequestBuilders.TileRequest.Builder()
            .setDeviceConfiguration(deviceParameters)
            .setCurrentState(StateBuilders.State.Builder().build())
            .build()
        val client = DefaultTileClient(
            context,
            ComponentName(context, GauraMalaTileService::class.java),
            Executor { command -> command.run() }
        )
        return client.requestTile(request).get(20, TimeUnit.SECONDS)
    }
}
