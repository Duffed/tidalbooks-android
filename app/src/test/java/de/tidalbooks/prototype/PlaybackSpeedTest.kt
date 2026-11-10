package de.tidalbooks.prototype

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedTest {
    @Test fun invalidStoredSpeedsFallBackToNormal() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 0f, 0.1f, 3f).forEach {
            assertEquals(1f, validPlaybackSpeed(it), 0f)
        }
    }

    @Test fun allOfferedSpeedsSurviveRestoration() {
        playbackSpeeds.forEach { assertEquals(it, validPlaybackSpeed(it), 0f) }
    }
}
