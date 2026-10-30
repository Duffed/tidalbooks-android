package de.tidalbooks.prototype

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ExpressiveProgressTest {
    @get:Rule val compose = createComposeRule()

    private fun inkHeight(): Int {
        val pixels = compose.onNodeWithTag("progress").captureToImage().toPixelMap()
        // Pixel's display accessibility settings may invert black and white.
        // Measure contrast against the background instead of assuming black ink.
        val background = pixels[pixels.width - 1, 0]
        val rows =
            (0 until pixels.height).filter { y ->
                (8 until (pixels.width * .55f).toInt()).any { x ->
                    val c = pixels[x, y]

                    abs(c.red - background.red) +
                        abs(c.green - background.green) +
                        abs(c.blue - background.blue) > 1.5f && c.alpha > .9f
                }
            }

        assertTrue("The active progress stroke must remain visible", rows.isNotEmpty())

        return rows.last() - rows.first() + 1
    }

    @Test
    fun pauseDrawsFlatOnTheNextFrameWithoutWaitingForAnAmplitudeAnimation() {
        val playing = mutableStateOf(true)

        compose.setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(primary = Color.Black, outlineVariant = Color.White)
            ) {
                ExpressiveProgress(
                    .7f,
                    Modifier.width(260.dp)
                        .height(16.dp)
                        .background(Color.White)
                        .testTag("progress"),
                    playing.value,
                )
            }
        }
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(1200)
        val flatLimit = ceil(2 * compose.density.density).toInt() + 2

        assertTrue("Playback draws a wave", inkHeight() > flatLimit)
        compose.runOnUiThread { playing.value = false }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        val height = inkHeight()

        assertTrue("Pause stroke height $height exceeds $flatLimit", height <= flatLimit)
    }

    @Test
    fun repeatedPlayPauseAndBufferingNeverLeaveAWaveBehind() {
        val state = mutableStateOf(ProbeState())

        compose.setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(primary = Color.Black, outlineVariant = Color.White)
            ) {
                ExpressiveProgress(
                    .7f,
                    Modifier.width(260.dp)
                        .height(16.dp)
                        .background(Color.White)
                        .testTag("progress"),
                    state.value.showPlaybackWave,
                )
            }
        }
        compose.mainClock.autoAdvance = false
        val flatLimit = ceil(2 * compose.density.density).toInt() + 2

        repeat(4) {
            compose.runOnUiThread { state.value = ProbeState(playing = true, requestedPlay = true) }
            compose.mainClock.advanceTimeBy(100)
            compose.runOnUiThread { state.value = state.value.copy(requestedPlay = false) }
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            val height = inkHeight()

            assertTrue("Pause stroke height $height exceeds $flatLimit", height <= flatLimit)
            compose.runOnUiThread {
                state.value = ProbeState(playing = true, requestedPlay = true, loading = true)
            }
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            assertTrue(inkHeight() <= flatLimit)
        }
    }

    @Test
    fun playGrowsFromFlatWithoutJumpingToTheFullWaveOrMovingTheBaseline() {
        val playing = mutableStateOf(false)

        compose.setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(primary = Color.Black, outlineVariant = Color.White)
            ) {
                ExpressiveProgress(
                    .7f,
                    Modifier.width(260.dp)
                        .height(16.dp)
                        .background(Color.White)
                        .testTag("progress"),
                    playing.value,
                )
            }
        }
        compose.mainClock.autoAdvance = false
        val bounds = compose.onNodeWithTag("progress").fetchSemanticsNode().boundsInRoot
        val flatLimit = ceil(2 * compose.density.density).toInt() + 2

        compose.runOnUiThread { playing.value = true }
        compose.mainClock.advanceTimeByFrame()
        val start = inkHeight()

        compose.mainClock.advanceTimeBy(150)
        val early = inkHeight()

        compose.mainClock.advanceTimeBy(1200)
        val settled = inkHeight()

        assertTrue("Playback must start with a nearly straight stroke", start <= flatLimit)
        assertTrue("The wave must grow instead of appearing at full height", early < settled)
        assertTrue("Playback eventually draws the full wave", settled > flatLimit)
        assertEquals(
            "The progress container must stay in place",
            bounds,
            compose.onNodeWithTag("progress").fetchSemanticsNode().boundsInRoot,
        )
    }
}
