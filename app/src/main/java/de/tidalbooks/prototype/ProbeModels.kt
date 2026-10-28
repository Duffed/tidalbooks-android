package de.tidalbooks.prototype

import java.net.URI

data class Track(val id: String, val title: String, val durationMs: Long, val disc: Int, val number: Int)
data class Album(val id: String, val title: String, val tracks: List<Track>, val author: String = "", val coverUrl: String? = null)
data class ProbeState(
    val loggedIn: Boolean = false,
    val busy: Boolean = false,
    val album: Album? = null,
    val index: Int = 0,
    val positionMs: Long = 0,
    val playing: Boolean = false,
    val requestedPlay: Boolean = false,
    val loading: Boolean = false,
    val presentation: String = "Noch nicht geprüft",
    val fullObserved: Boolean = false,
    val continuousPlayedMs: Long = 0,
    val transitions: Int = 0,
    val seekObserved: Boolean = false,
    val timerRemainingMs: Long? = null,
    val message: String = "",
    val legacyReport: String? = null,
    val alternativeCode: String? = null,
    val alternativeUrl: String? = null,
    val alternativeActive: Boolean = false,
    val books: List<LibraryBook> = emptyList(),
    val bookmarks: List<BookmarkItem> = emptyList(),
    val completed: Boolean = false,
    val sleepSession: StoredSleep? = null,
    val libraryReady: Boolean = false,
    val history: List<StoredJump> = emptyList(),
    val resumeRewindSeconds: Int = 0,
    val lastTimerMinutes: Int = 30,
    val playbackProblem: String? = null,

) {
    val showPlaybackWave: Boolean
        get() = requestedPlay && playing && !loading && playbackProblem == null && !completed
}

fun albumId(input: String): String {
    val value = input.trim()
    if (value.matches(Regex("[0-9]+"))) return value
    val uri = runCatching { URI(value) }.getOrNull()
    require(uri != null && uri.scheme == "https" && uri.host in setOf("tidal.com", "www.tidal.com", "listen.tidal.com")) {
        "Bitte einen TIDAL-Albumlink oder eine numerische Album-ID eingeben."
    }
    return Regex("/(?:browse/)?album/([0-9]+)(?:/|$)").find(uri.path)?.groupValues?.get(1)
        ?: throw IllegalArgumentException("Der Link muss auf ein Album zeigen.")
}

/** All deadlines use elapsedRealtime, never the adjustable wall clock. */
class SleepDeadline {
    var deadlineMs: Long? = null
        private set
    var endTrackId: String? = null
        private set
    fun start(minutes: Int, nowMs: Long) {
        require(minutes in 1..180)
        deadlineMs = nowMs + minutes * 60_000L
        endTrackId = null
    }
    fun endOfTrack(id: String) { deadlineMs = null; endTrackId = id }
    fun restore(deadlineMs: Long?, endTrackId: String?) { this.deadlineMs = deadlineMs; this.endTrackId = endTrackId }
    fun remaining(nowMs: Long): Long? = deadlineMs?.let { (it - nowMs).coerceAtLeast(0) }
    fun expired(nowMs: Long) = deadlineMs?.let { nowMs >= it } ?: false
    fun extend() { deadlineMs = deadlineMs?.plus(600_000) }
    fun cancel() { deadlineMs = null; endTrackId = null }
}
