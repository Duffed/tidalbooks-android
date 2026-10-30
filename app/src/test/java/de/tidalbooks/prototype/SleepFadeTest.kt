package de.tidalbooks.prototype

import org.junit.Assert.*
import org.junit.Test

class SleepFadeTest {
    private val tracks = listOf(Track("a", "A", 60_000, 1, 1), Track("b", "B", 60_000, 2, 1))

    @Test
    fun envelopeFallsFromNormalGainToSilenceInFiveSeconds() {
        assertEquals(1f, sleepFadeGain(5000), .0001f)
        assertEquals(.5f, sleepFadeGain(2500), .0001f)
        assertEquals(0f, sleepFadeGain(0), .0001f)
        assertEquals(0f, sleepFadeGain(-100), .0001f)
        assertEquals(1f, sleepFadeGain(60_000), .0001f)
    }

    @Test
    fun rewindCrossesTrackAndDiscBoundaries() {
        assertEquals(BookPosition(0, 58_000), sleepResumePosition(tracks, 1, 3000, 58_000))
    }

    @Test
    fun delayedStopNeverSkipsAnyFadedAudio() {
        assertEquals(BookPosition(0, 50_000), sleepResumePosition(tracks, 0, 58_000, 50_000))
    }

    @Test
    fun pausedOrBufferingTimerStillRewindsAtLeastFiveSeconds() {
        assertEquals(BookPosition(0, 45_000), sleepResumePosition(tracks, 0, 50_000, 50_000))
        assertEquals(BookPosition(0, 45_000), sleepResumePosition(tracks, 0, 50_000, null))
    }

    @Test
    fun clampsToBeginningAndRewindsAtBookEnd() {
        assertEquals(BookPosition(0, 0), sleepResumePosition(tracks, 0, 2000, null))
        assertEquals(BookPosition(1, 55_000), sleepResumePosition(tracks, 1, 60_000, 115_000))
    }

    @Test
    fun expiredProcessRestartRewindsButCancelledTimersAndRebootsDoNot() {
        val timer =
            StoredSleep(
                startBookId = "book",
                startTrackId = "a",
                startPositionMs = 0,
                active = true,
                deadlineElapsedMs = 60_000,
                endTrackId = null,
                bootCount = 4,
            )

        assertTrue(sleepExpiredWhileAway(timer, 4, 60_001))
        assertFalse(sleepExpiredWhileAway(timer, 4, 59_000))
        assertFalse(sleepExpiredWhileAway(timer.copy(active = false), 4, 60_001))
        assertFalse(sleepExpiredWhileAway(timer, 5, 60_001))
        assertFalse(
            sleepExpiredWhileAway(timer.copy(deadlineElapsedMs = null, endTrackId = "a"), 4, 60_001)
        )
    }
}
