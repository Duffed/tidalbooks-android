package de.tidalbooks.prototype

import org.junit.Assert.*
import org.junit.Test

class ProbeModelsTest {
    @Test
    fun acceptsAlbumLinksAndIds() {
        assertEquals("123", albumId("123"))
        assertEquals("123", albumId("https://tidal.com/browse/album/123?u"))
        assertEquals("123", albumId("https://listen.tidal.com/album/123"))
        assertEquals("123", albumId("https://www.tidal.com/album/123/"))
    }

    @Test
    fun rejectsOtherHostsAndResources() {
        for (input in
            listOf(
                "https://tidal.com.evil/album/123",
                "http://tidal.com/album/123",
                "https://tidal.com/track/123",
                "https://example.com/album/123",
                "abc",
            )) {
            assertThrows(IllegalArgumentException::class.java) { albumId(input) }
        }
    }

    @Test
    fun usesElapsedTimeEvenWhenPaused() {
        val timer = SleepDeadline()

        timer.start(1, 500)
        assertEquals(60_000L, timer.remaining(500))
        assertFalse(timer.expired(60_499))
        assertTrue(timer.expired(60_500))
        assertEquals(0L, timer.remaining(70_000))
    }

    @Test
    fun acceptsOnlySupportedDurations() {
        val timer = SleepDeadline()

        assertThrows(IllegalArgumentException::class.java) { timer.start(0, 0) }
        assertThrows(IllegalArgumentException::class.java) { timer.start(181, 0) }
        timer.start(180, 0)
        assertEquals(10_800_000L, timer.remaining(0))
    }

    @Test
    fun extendingAddsTenMinutesToExistingDeadline() {
        val timer = SleepDeadline()

        timer.start(1, 100)
        timer.extend()
        assertEquals(660_000L, timer.remaining(100))
    }

    @Test
    fun trackEndAndTimeAreExclusive() {
        val timer = SleepDeadline()

        timer.start(1, 100)
        timer.endOfTrack("track-a")
        assertNull(timer.deadlineMs)
        assertEquals("track-a", timer.endTrackId)
        assertFalse(timer.expired(Long.MAX_VALUE))
        timer.start(1, 100)
        assertNull(timer.endTrackId)
    }

    @Test
    fun cancelledTimerDoesNotRearmThroughExtend() {
        val timer = SleepDeadline()

        timer.start(1, 0)
        timer.cancel()
        timer.extend()
        assertNull(timer.remaining(0))
        assertFalse(timer.expired(Long.MAX_VALUE))
        assertNull(timer.endTrackId)
    }
}
