package de.tidalbooks.prototype

import org.junit.Assert.*
import org.junit.Test

class DecoderRecoveryTest {
    @Test
    fun pausedDecoderLossDoesNotShowALoadFailureOrStartPlayback() {
        val recovery = DecoderRecovery()

        assertEquals(DecoderRecovery.Action.INVALIDATE_PAUSED, recovery.onError("-1:3:4006", false))
        assertEquals(DecoderRecovery.Action.RETRY, recovery.onError("-1:3:4006", true))
    }

    @Test
    fun requestedPlaybackRetriesOnceThenReportsPersistentDecoderFailure() {
        val recovery = DecoderRecovery()

        assertEquals(DecoderRecovery.Action.RETRY, recovery.onError("-1:3:4006", true))
        assertEquals(DecoderRecovery.Action.REPORT, recovery.onError("-1:3:4006", true))
        recovery.newRequest()
        assertEquals(DecoderRecovery.Action.RETRY, recovery.onError("-1:3:4006", true))
    }

    @Test
    fun networkSubscriptionAndOtherDecoderErrorsRemainVisible() {
        val recovery = DecoderRecovery()

        listOf("2:2:2001", "0:401:11003", "-1:3:4003", "bad:4006").forEach { code ->
            assertEquals(code, DecoderRecovery.Action.REPORT, recovery.onError(code, true))
            assertEquals(code, DecoderRecovery.Action.REPORT, recovery.onError(code, false))
        }
    }

    @Test
    fun pauseWinsEvenIfEngineStillReportsPlaying() {
        val active = ProbeState(playing = true, requestedPlay = true)

        assertTrue(active.showPlaybackWave)
        assertFalse(active.copy(requestedPlay = false).showPlaybackWave)
        assertFalse(active.copy(playing = false).showPlaybackWave)
        assertFalse(active.copy(loading = true).showPlaybackWave)
        assertFalse(active.copy(playbackProblem = "Fehler").showPlaybackWave)
        assertFalse(active.copy(completed = true).showPlaybackWave)
    }
}
