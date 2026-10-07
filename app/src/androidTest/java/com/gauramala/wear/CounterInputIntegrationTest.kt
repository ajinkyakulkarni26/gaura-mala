package com.gauramala.wear

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import com.gauramala.wear.data.dataStore
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

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

    @Test
    fun tappingCounterAdvancesOneBead() {
        composeRule.onNodeWithTag("bead-count", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("0")
        composeRule.onNodeWithTag("bead-count", useUnmergedTree = true)
            .performTouchInput { click() }

        awaitBeadCount("1")
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

    private fun awaitBeadCount(expected: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("bead-count", useUnmergedTree = true)
                    .assertTextEquals(expected)
            }.isSuccess
        }
    }
}
