package de.tidalbooks.prototype

const val SLEEP_FADE_MS = 5_000L

/** Linear amplitude envelope, driven by the original monotonic timer deadline. */
internal fun sleepFadeGain(remainingMs: Long): Float =
    (remainingMs.toFloat() / SLEEP_FADE_MS).coerceIn(0f, 1f)

internal fun sleepResumePosition(tracks: List<Track>, index: Int, positionMs: Long, fadeStartMs: Long?): BookPosition {
    val elapsed = elapsedBookMs(tracks, index, positionMs)
    val target = minOf(elapsed - SLEEP_FADE_MS, fadeStartMs ?: elapsed).coerceAtLeast(0)
    return bookPosition(tracks, target)
}

internal fun sleepExpiredWhileAway(record: StoredSleep?, bootCount: Int, nowMs: Long): Boolean =
    record?.active == true && bootCount >= 0 && record.bootCount == bootCount &&
        record.deadlineElapsedMs?.let { nowMs >= it } == true
