package com.gauramala.wear

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gauramala.wear.data.MantraPreferences
import com.gauramala.wear.data.dataStore
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CounterInputIntegrationTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val clearStoredProgress = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            // Seed a visible marker so @Before can wait until the Activity has consumed its
            // first DataStore emission. The counter surface itself renders before that load
            // completes, which can otherwise make the first simulated input disappear.
            runBlocking {
                context.dataStore.edit {
                    it.clear()
                    it[MantraPreferences.KEY_COMPLETED_ROUNDS] = 1
                    it[MantraPreferences.KEY_LAST_DATE] = LocalDate.now().toString()
                }
            }
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(clearStoredProgress).around(composeRule)

    @Before
    fun waitForCounterScreen() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag("counter-surface", useUnmergedTree = true)
                    .assertIsDisplayed()
            }.isSuccess
        }

        // Round 2 is the seeded marker; seeing it proves preferences have loaded. Clear it
        // only then, and wait for the clean baseline before the test performs any input.
        awaitRound("Round 2 of 16")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking { context.dataStore.edit { it.clear() } }
        awaitRound("Round 1 of 16")
        awaitBeadCount("0")
    }

    @Test
    fun counterStartsAtZeroOnRoundOne() {
        awaitBeadCount("0")
        composeRule.onNodeWithText("/ 108").assertIsDisplayed()
        awaitRound("Round 1 of 16")
    }

    @Test
    fun undoIsDisabledBeforeAnyBeadIsCounted() {
        composeRule.onNodeWithContentDescription("Undo Bead").assertIsNotEnabled()
    }

    @Test
    fun tappingCounterAdvancesOneBeadAndEnablesUndo() {
        tapCounter()
        awaitBeadCount("1")
        composeRule.onNodeWithContentDescription("Undo Bead").assertIsEnabled()
    }

    @Test
    fun undoReturnsCounterToPreviousBead() {
        tapCounter()
        awaitBeadCount("1")

        composeRule.onNodeWithContentDescription("Undo Bead").performClick()

        awaitBeadCount("0")
        composeRule.onNodeWithContentDescription("Undo Bead").assertIsNotEnabled()
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun rotaryInputAdvancesCounter() {
        composeRule.onNodeWithTag("counter-surface", useUnmergedTree = true)
            .performRotaryScrollInput {
                rotateToScrollVertically(48f)
            }

        awaitBeadCount("1")
    }

    @Test
    fun roundHeaderOpensManualRoundAdjustment() {
        openRoundAdjustment()

        composeRule.onNodeWithText("Rounds").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Decrease completed rounds").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Increase completed rounds").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cancel round adjustment").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Set completed rounds").assertIsDisplayed()
    }

    @Test
    fun roundPickerPlusAndMinusChangeTheNextRound() {
        openRoundAdjustment()

        composeRule.onNodeWithContentDescription("Increase completed rounds").performClick()
        composeRule.onNodeWithText("Next round: 2").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Decrease completed rounds").performClick()
        composeRule.onNodeWithText("Next round: 1").assertIsDisplayed()
    }

    @Test
    fun cancellingRoundAdjustmentKeepsPartialBeadProgress() {
        tapCounter()
        awaitBeadCount("1")
        openRoundAdjustment()
        composeRule.onNodeWithText("Saving clears 1/108 beads.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Increase completed rounds").performClick()

        composeRule.onNodeWithContentDescription("Cancel round adjustment").performClick()

        awaitBeadCount("1")
        awaitRound("Round 1 of 16")
    }

    @Test
    fun savingManualRoundCountClearsPartialProgressAndStartsFollowingRound() {
        tapCounter()
        awaitBeadCount("1")
        openRoundAdjustment()
        repeat(3) {
            composeRule.onNodeWithContentDescription("Increase completed rounds").performClick()
        }
        composeRule.onNodeWithText("Next round: 4").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Set completed rounds").performClick()

        awaitBeadCount("0")
        awaitRound("Round 4 of 16")
        composeRule.onNodeWithContentDescription("Undo Bead").assertIsNotEnabled()
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun crownStepFromLastBeadCompletesRoundAndAdvancesToRoundTwo() {
        setBeadCount(107)
        awaitBeadCount("107")

        val oneBeadRotationPx = 24f * composeRule.activity.resources.displayMetrics.density
        composeRule.onNodeWithTag("counter-surface", useUnmergedTree = true)
            .performRotaryScrollInput {
                rotateToScrollVertically(oneBeadRotationPx)
            }

        awaitRound("Round 2 of 16")
        awaitBeadCount("0")
    }

    @Test
    fun settingsScreenShowsCounterPreferencesAndResetActions() {
        openSettings()

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Daily goal").assertIsDisplayed()
        composeRule.onNodeWithText("Double Pinch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Screen Tap").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings-haptics").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings-keep-awake").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reset Round").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reset Today").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dailyGoalIncreaseAndDecreaseButtonsUpdateTheGoal() {
        openSettings()

        composeRule.onNodeWithContentDescription("Increase daily goal").performClick()
        composeRule.onNodeWithTag("daily-goal-value").assertTextEquals("17 rounds")
        composeRule.onNodeWithContentDescription("Decrease daily goal").performClick()
        composeRule.onNodeWithTag("daily-goal-value").assertTextEquals("16 rounds")
    }

    @Test
    fun hapticsSettingCanBeTurnedOffAndBackOn() {
        openSettings()
        val hapticsSwitch = composeRule.onNodeWithTag("settings-haptics").performScrollTo()

        hapticsSwitch.assertIsOn().performTouchInput { click() }
        awaitSwitchOff("settings-haptics")
        composeRule.onNodeWithTag("settings-haptics").performScrollTo().performTouchInput { click() }
        awaitSwitchOn("settings-haptics")
    }

    @Test
    fun keepAwakeSettingCanBeEnabled() {
        openSettings()
        val keepAwakeSwitch = composeRule.onNodeWithTag("settings-keep-awake").performScrollTo()

        keepAwakeSwitch.assertIsOff().performClick()
        awaitSwitchOn("settings-keep-awake")
    }

    @Test
    fun primaryCounterSettingsAndRoundPickerActionsHave48DpTargets() {
        assertAtLeast48Dp(
            "Counter surface",
            composeRule.onNodeWithTag("counter-surface", useUnmergedTree = true)
        )
        assertAtLeast48Dp("Round progress", composeRule.onNodeWithTag("round-progress-button"))
        assertAtLeast48Dp("Undo", composeRule.onNodeWithTag("undo-button"))
        assertAtLeast48Dp("Open Settings", composeRule.onNodeWithTag("open-settings-button"))

        openSettings()
        assertAtLeast48Dp("Decrease daily goal", composeRule.onNodeWithTag("daily-goal-decrease"))
        assertAtLeast48Dp("Increase daily goal", composeRule.onNodeWithTag("daily-goal-increase"))
        listOf(
            "settings-double-pinch",
            "settings-screen-tap",
            "settings-haptics",
            "settings-milestones",
            "settings-keep-awake"
        ).forEach { tag ->
            assertAtLeast48Dp(tag, composeRule.onNodeWithTag(tag).performScrollTo())
        }

        assertAtLeast48Dp("Reset Round", composeRule.onNodeWithText("Reset Round").performScrollTo())
        assertAtLeast48Dp("Reset Today", composeRule.onNodeWithText("Reset Today").performScrollTo())
        assertAtLeast48Dp("Privacy Policy", composeRule.onNodeWithText("Privacy Policy").performScrollTo())
        composeRule.onNodeWithText("Reset Round").performScrollTo().performClick()
        assertAtLeast48Dp("Cancel reset", composeRule.onNodeWithTag("reset-confirm-cancel"))
        assertAtLeast48Dp("Confirm reset", composeRule.onNodeWithTag("reset-confirm-accept"))
        composeRule.onNodeWithContentDescription("Cancel reset").performClick()
        assertAtLeast48Dp("Done", composeRule.onNodeWithText("Done").performScrollTo())
        composeRule.onNodeWithText("Done").performScrollTo().performClick()

        openRoundAdjustment()
        assertAtLeast48Dp("Decrease completed rounds", composeRule.onNodeWithTag("round-picker-decrease"))
        assertAtLeast48Dp("Increase completed rounds", composeRule.onNodeWithTag("round-picker-increase"))
        assertAtLeast48Dp("Cancel round adjustment", composeRule.onNodeWithTag("round-picker-cancel"))
        assertAtLeast48Dp("Set completed rounds", composeRule.onNodeWithTag("round-picker-confirm"))
        composeRule.onNodeWithContentDescription("Cancel round adjustment").performClick()
    }

    @Test
    fun cancellingResetRoundPreservesProgress() {
        tapCounter()
        awaitBeadCount("1")
        openSettings()
        composeRule.onNodeWithText("Reset Round").performScrollTo().performClick()
        composeRule.onNodeWithText("Clears beads in this round.").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Cancel reset").performClick()
        composeRule.onNodeWithText("Done").performScrollTo().performClick()

        awaitBeadCount("1")
    }

    @Test
    fun confirmingResetRoundClearsBeadsButKeepsCompletedRounds() {
        setCompletedRounds(2)
        tapCounter()
        awaitBeadCount("1")
        openSettings()
        composeRule.onNodeWithText("Reset Round").performScrollTo().performClick()

        composeRule.onNodeWithContentDescription("Confirm reset").performClick()

        awaitBeadCount("0")
        awaitRound("Round 3 of 16")
    }

    @Test
    fun confirmingResetTodayClearsRoundsAndBeads() {
        setStoredProgress(beadCount = 5, completedRounds = 2)
        awaitBeadCount("5")
        awaitRound("Round 3 of 16")

        openSettings()
        composeRule.onNodeWithText("Reset Today").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Confirm reset").performClick()

        awaitBeadCount("0")
        awaitRound("Round 1 of 16")
    }

    private fun tapCounter() {
        composeRule.onNodeWithTag("counter-surface", useUnmergedTree = true)
            .performTouchInput { click() }
    }

    private fun openRoundAdjustment() {
        composeRule.onNodeWithTag("round-progress-button").performClick()
    }

    private fun openSettings() {
        composeRule.onNodeWithContentDescription("Open Settings").performClick()
    }

    private fun assertAtLeast48Dp(label: String, target: SemanticsNodeInteraction) {
        val bounds = target.fetchSemanticsNode().boundsInRoot
        val density = composeRule.density.density
        val minimumPixels = 48f * density
        assertTrue(
            "$label target width ${bounds.width}px (${bounds.width / density}dp) is below 48dp",
            bounds.width + 0.5f >= minimumPixels
        )
        assertTrue(
            "$label target height ${bounds.height}px (${bounds.height / density}dp) is below 48dp",
            bounds.height + 0.5f >= minimumPixels
        )
    }

    private fun setCompletedRounds(count: Int) {
        openRoundAdjustment()
        repeat(count) {
            composeRule.onNodeWithContentDescription("Increase completed rounds").performClick()
        }
        composeRule.onNodeWithContentDescription("Set completed rounds").performClick()
        awaitRound("Round ${count + 1} of 16")
    }

    private fun setBeadCount(count: Int) {
        setStoredProgress(beadCount = count, completedRounds = 0)
    }

    private fun setStoredProgress(beadCount: Int, completedRounds: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[MantraPreferences.KEY_BEAD_COUNT] = beadCount
                preferences[MantraPreferences.KEY_COMPLETED_ROUNDS] = completedRounds
                preferences[MantraPreferences.KEY_LAST_DATE] = LocalDate.now().toString()
            }
        }
    }

    private fun awaitBeadCount(expected: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            runCatching {
                composeRule.onNodeWithTag("bead-count", useUnmergedTree = true)
                    .assertTextEquals(expected)
            }.isSuccess
        }
    }

    private fun awaitRound(expected: String, timeoutMillis: Long = 10_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            runCatching {
                composeRule.onNodeWithTag("round-progress-button")
                    .assertTextEquals(expected)
            }.isSuccess
        }
    }

    private fun awaitSwitchOn(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching { composeRule.onNodeWithTag(tag).assertIsOn() }.isSuccess
        }
    }

    private fun awaitSwitchOff(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching { composeRule.onNodeWithTag(tag).assertIsOff() }.isSuccess
        }
    }
}
