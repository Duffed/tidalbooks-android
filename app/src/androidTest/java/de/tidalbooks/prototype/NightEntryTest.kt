package de.tidalbooks.prototype

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NightEntryTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun widgetNightIntentOpensTheDialogWithoutStartingPlayback() {
        val originalIntent = Intent(compose.activity.intent)
        try {
            compose.runOnUiThread {
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(compose.activity,
                    Intent(compose.activity,MainActivity::class.java).putExtra("openPlayer",true).putExtra("openNightMode",true))
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithContentDescription("Nachtmodus schließen").fetchSemanticsNodes().isNotEmpty()
            }
            assertFalse((compose.activity.application as ProbeApp).probe.state.value.requestedPlay)
            compose.onNodeWithContentDescription("Nachtmodus schließen").performClick()
            compose.onNodeWithContentDescription("Nachtmodus schließen").assertDoesNotExist()
        } finally {
            // ActivityScenario identifies lifecycle callbacks using the launch intent's filter.
            compose.runOnUiThread { compose.activity.intent = originalIntent }
        }
    }
}
