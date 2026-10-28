package de.tidalbooks.prototype

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NightPlayerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val state = ProbeState(album = Album("book","Ein Hörbuch",listOf(Track("a","A",60_000,1,1))),libraryReady = true)

    @Test fun entryIsPausedAndLargeButtonsDispatchOnlyExplicitActions() {
        var plays = 0; var skips = 0L; var marks = 0; var minutes = 0
        compose.setContent {
            NightPlayer(state,{}, { plays++ }, { skips = it }, { marks++ }, { minutes = it }, {}, {}, {}, {})
        }
        assertEquals(0,plays)
        compose.onNodeWithContentDescription("Abspielen").performScrollTo().performClick()
        compose.onNodeWithContentDescription("30 Sekunden zurück").performScrollTo().performClick()
        compose.onNodeWithText("Stelle merken").performScrollTo().performClick()
        compose.onNodeWithText("30 Min.").performScrollTo().performClick()
        assertEquals(1,plays); assertEquals(-30_000L,skips); assertEquals(1,marks); assertEquals(30,minutes)
    }

    @Test fun closingRestoresWindowBrightnessWithoutChangingTheSystemSetting() {
        val visible = mutableStateOf(true)
        var original = -1f
        compose.runOnUiThread { original = compose.activity.window.attributes.screenBrightness }
        compose.setContent {
            if (visible.value) NightPlayer(state,{ visible.value = false },{}, {}, {}, {}, {}, {}, {}, {})
            else Text("Normalansicht")
        }
        compose.runOnIdle { assertEquals(.08f,compose.activity.window.attributes.screenBrightness,.001f) }
        compose.onNodeWithContentDescription("Nachtmodus schließen").performScrollTo().performClick()
        compose.onNodeWithText("Normalansicht").assertExists()
        compose.runOnIdle { assertEquals(original,compose.activity.window.attributes.screenBrightness,.001f) }
    }

    @Test fun activeTimerAllowsExtendCancelAndReturnToTheSleepAnchor() {
        var extended = false; var cancelled = false; var returned = false
        val timer = StoredSleep(startBookId = "book",startTrackId = "a",startPositionMs = 5000,
            active = true,deadlineElapsedMs = 100_000,endTrackId = null,bootCount = 1)
        compose.setContent { NightPlayer(state.copy(sleepSession = timer,timerRemainingMs = 45_000),{}, {}, {}, {}, {}, {},
            { extended = true },{ cancelled = true },{ returned = true }) }
        compose.onNodeWithText("0:45").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("+10 Minuten").performScrollTo().performClick()
        compose.onNodeWithText("Timer ausschalten").performScrollTo().performClick()
        compose.onNodeWithText("Zur Stelle vor dem Einschlafen").performScrollTo().performClick()
        assertTrue(extended && cancelled && returned)
    }
}
