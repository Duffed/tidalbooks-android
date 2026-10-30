package de.tidalbooks.prototype

import org.junit.Assert.*
import org.junit.Test

class WidgetSnapshotTest {
    private val album = Album("b","Ein Hörbuch",listOf(Track("a","A",60_000,1,1),Track("b","B",120_000,1,2)))

    @Test fun refreshOfRestoredBookNeverSuggestsAnAutomaticPauseOrPlayback() {
        val s = WidgetSnapshot.from(ProbeState(album = album,index = 1,positionMs = 31_000))
        assertTrue(s.hasBook); assertFalse(s.pauseAction)
        assertEquals("Ein Hörbuch",s.title)
        assertEquals(2,s.chapter); assertEquals(180_000L,s.durationMs)
    }
    @Test fun bufferingCanBeCancelledButCompletedBookCannotResume() {
        assertTrue(WidgetSnapshot.from(ProbeState(album = album,requestedPlay = true,loading = true)).pauseAction)
        assertFalse(WidgetSnapshot.from(ProbeState(album = album,requestedPlay = true,completed = true)).pauseAction)
        assertFalse(WidgetSnapshot.from(ProbeState()).hasBook)
    }
    @Test fun frequentPlaybackTicksDoNotCauseFrequentRemoteViewUpdates() {
        val s = ProbeState(album = album,positionMs = 31_000)
        assertEquals(WidgetSnapshot.from(s),WidgetSnapshot.from(s.copy(positionMs = 44_999)))
        assertNotEquals(WidgetSnapshot.from(s),WidgetSnapshot.from(s.copy(positionMs = 45_000)))
        assertNotEquals(WidgetSnapshot.from(s),WidgetSnapshot.from(s.copy(requestedPlay = true)))
    }
    @Test fun timerRetainsMonotonicDeadlineWhilePausedAndUsesLastDuration() {
        val timer = StoredSleep(startBookId = "b",startTrackId = "a",startPositionMs = 5000,
            active = true,deadlineElapsedMs = 100_000,endTrackId = null,bootCount = 1)
        val s = WidgetSnapshot.from(ProbeState(album = album,sleepSession = timer,lastTimerMinutes = 45))
        assertTrue(s.timerActive); assertEquals(100_000L,s.deadlineMs); assertEquals(45,s.lastTimerMinutes)
        val cancelled = WidgetSnapshot.from(ProbeState(album = album,sleepSession = timer.copy(active = false)))
        assertFalse(cancelled.timerActive); assertNull(cancelled.deadlineMs)
    }
}
