package com.gauramala.wear

import androidx.compose.ui.test.ExperimentalTestApi
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
            runBlocking { context.dataStore.edit { it.clear() } }
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

    private fun awaitBeadCount(expected: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
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
