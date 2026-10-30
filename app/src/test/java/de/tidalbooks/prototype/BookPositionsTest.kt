package de.tidalbooks.prototype

import org.junit.Assert.*
import org.junit.Test

class BookPositionsTest {
    private val tracks =
        listOf(
            Track("a", "Erster Abschnitt", 60_000, 1, 1),
            Track("b", "Zweiter Abschnitt", 45_000, 1, 2),
            Track("c", "Disc zwei", 90_000, 2, 1),
        )

    @Test
    fun crossesBothTrackAndDiscBoundaries() {
        assertEquals(BookPosition(1, 0), bookPosition(tracks, 60_000))
        assertEquals(BookPosition(2, 1_000), bookPosition(tracks, 106_000))
        assertEquals(BookPosition(0, 59_000), bookPosition(tracks, 59_000))
    }

    @Test
    fun seekIsClampedAtBothEnds() {
        assertEquals(BookPosition(0, 0), bookPosition(tracks, -30_000))
        assertEquals(BookPosition(2, 89_999), bookPosition(tracks, 999_999))
    }

    @Test
    fun restoringUsesTrackIdentityRatherThanTheOldOrdinal() {
        val reordered = listOf(tracks[2], tracks[0], tracks[1])

        assertEquals(BookPosition(2, 12_345), restoreBookPosition(reordered, "b", 12_345))
    }

    @Test
    fun missingTracksRestartSafelyAndOffsetsAreClamped() {
        assertEquals(BookPosition(0, 0), restoreBookPosition(tracks, "removed", 50_000))
        assertEquals(BookPosition(1, 45_000), restoreBookPosition(tracks, "b", 100_000))
        assertEquals(BookPosition(1, 0), restoreBookPosition(tracks, "b", -100))
    }

    @Test
    fun totalsRetainMillisecondAccuracy() {
        assertEquals(106_234, elapsedBookMs(tracks, 2, 1_234))
    }

    @Test
    fun metadataRoundTripPreservesTextOrderingAndDurations() {
        val input = tracks + Track("d", "„Eine Stelle“ – ÄÖÜ \\ /", 1_234, 2, 2)

        assertEquals(input, TrackCodec.decode(TrackCodec.encode(input)))
    }

    private val sleep =
        StoredSleep(
            startBookId = "book",
            startTrackId = "b",
            startPositionMs = 12_345,
            active = true,
            deadlineElapsedMs = 60_000,
            endTrackId = null,
            bootCount = 7,
        )

    @Test
    fun processRestartKeepsUnexpiredMonotonicDeadline() {
        assertEquals(sleep, restoredTimer(sleep, 7, 30_000))
    }

    @Test
    fun expiryAndRebootCancelTheTimerButKeepItsStartPoint() {
        for (restored in listOf(restoredTimer(sleep, 7, 60_001), restoredTimer(sleep, 8, 1_000))) {
            assertFalse(requireNotNull(restored).active)
            assertEquals("b", restored.startTrackId)
            assertEquals(12_345, restored.startPositionMs)
            assertNull(restored.deadlineElapsedMs)
        }
    }
}
