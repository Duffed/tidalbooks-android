package de.tidalbooks.prototype

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ExpandablePlayerTest {
    @get:Rule val compose = createComposeRule()

    private fun showPlayer(scrollable: Boolean = false) {
        compose.setContent {
            MaterialTheme {
                var expanded by remember { mutableStateOf(false) }
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val bounds = with(density) {
                        Rect(0f, (maxHeight - 160.dp).toPx(), maxWidth.toPx(), (maxHeight - 79.dp).toPx())
                    }
                    Box(Modifier.fillMaxSize().background(Color.Blue).testTag("library"))
                    ExpandablePlayer(expanded, { expanded = it }, bounds,
                        miniContent = { Text("Öffnen", Modifier.fillMaxSize().clickable { expanded = true }) },
                    ) {
                        if (scrollable) LazyColumn(Modifier.fillMaxSize().testTag("player_content")) {
                            item { Box(Modifier.fillMaxWidth().height(240.dp).testTag("cover")) }
                            item {
                                var position by remember { mutableFloatStateOf(.1f) }
                                ExpressiveSeekBar(position,{ position = it },{},Modifier.fillMaxWidth().testTag("seek"))
                            }
                            items(20) { Text("Kapitel $it",Modifier.fillMaxWidth().height(80.dp)) }
                        } else Text("Minimieren", Modifier.fillMaxSize().clickable { expanded = false })
                    }
                }
            }
        }
    }

    @Test fun slowDragFollowsFingerAndKeepsLibraryInPlace() {
        showPlayer()
        val panel = compose.onNodeWithTag("expandable_player")
        val collapsed = panel.fetchSemanticsNode().boundsInRoot
        val library = compose.onNodeWithTag("library").fetchSemanticsNode().boundsInRoot
        // Finger bleibt unten: es darf noch keine selbstständige Öffnungsanimation laufen.
        compose.onRoot().performTouchInput {
            down(collapsed.center)
            moveBy(Offset(0f, -collapsed.top * .3f), delayMillis = 800)
        }
        val partial = panel.fetchSemanticsNode().boundsInRoot
        assertTrue(partial.height > collapsed.height)
        assertTrue(partial.top > 0f)
        assertEquals(library, compose.onNodeWithTag("library").fetchSemanticsNode().boundsInRoot)
        // Langsam zurückziehen und loslassen: zurück zum Mini-Player.
        compose.onRoot().performTouchInput {
            moveBy(Offset(0f, collapsed.top * .3f), delayMillis = 800)
            advanceEventTime(500)
            up()
        }
        compose.waitForIdle()
        assertEquals(collapsed.top, panel.fetchSemanticsNode().boundsInRoot.top, 2f)
    }

    @Test fun tapExpandsAndDownwardDragCollapses() {
        showPlayer()
        val panel = compose.onNodeWithTag("expandable_player")
        val collapsed = panel.fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("Öffnen").performClick()
        compose.waitForIdle()
        assertEquals(0f, panel.fetchSemanticsNode().boundsInRoot.top, 2f)
        compose.onRoot().performTouchInput {
            down(Offset(collapsed.center.x, 60f))
            moveBy(Offset(0f, collapsed.top * .8f), delayMillis = 1200)
            advanceEventTime(500)
            up()
        }
        compose.waitForIdle()
        assertEquals(collapsed.top, panel.fetchSemanticsNode().boundsInRoot.top, 2f)
    }

    @Test fun downwardDragOnScrollableCoverMinimizesPlayer() {
        showPlayer(scrollable = true)
        val panel = compose.onNodeWithTag("expandable_player")
        val collapsed = panel.fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("Öffnen").performClick()
        val cover = compose.onNodeWithTag("cover").fetchSemanticsNode().boundsInRoot
        compose.onRoot().performTouchInput {
            down(cover.center)
            moveBy(Offset(0f, collapsed.top * .3f), delayMillis = 800)
        }
        assertTrue(panel.fetchSemanticsNode().boundsInRoot.top > 0f)
        compose.onRoot().performTouchInput { advanceEventTime(500); up() }
        compose.waitForIdle()
        assertEquals(collapsed.top, panel.fetchSemanticsNode().boundsInRoot.top, 2f)
    }

    @Test fun horizontalSeekDoesNotMinimizePlayer() {
        showPlayer(scrollable = true)
        compose.onNodeWithText("Öffnen").performClick()
        val seek = compose.onNodeWithTag("seek")
        seek.performTouchInput { swipe(Offset(width * .1f, centerY), Offset(width * .8f, centerY), 800) }
        assertTrue(seek.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current > .6f)
        assertEquals(0f, compose.onNodeWithTag("expandable_player").fetchSemanticsNode().boundsInRoot.top, 2f)
    }

    @Test fun upwardContentScrollStaysInsideExpandedPlayer() {
        showPlayer(scrollable = true)
        compose.onNodeWithText("Öffnen").performClick()
        compose.onNodeWithTag("player_content").performTouchInput { swipeUp(durationMillis = 800) }
        assertEquals(0f, compose.onNodeWithTag("expandable_player").fetchSemanticsNode().boundsInRoot.top, 2f)
        compose.onNodeWithTag("cover").assertIsNotDisplayed()
    }
}
