package de.tidalbooks.prototype

data class BookPosition(val index: Int, val positionMs: Long)

fun bookPosition(tracks: List<Track>, absoluteMs: Long): BookPosition {
    require(tracks.isNotEmpty()) { translated("Das Hörbuch enthält keine Tracks.") }
    var left = absoluteMs.coerceIn(0, (tracks.sumOf { it.durationMs } - 1).coerceAtLeast(0))

    tracks.forEachIndexed { index, track ->
        if (left < track.durationMs || index == tracks.lastIndex)
            return BookPosition(index, left.coerceAtLeast(0))

        left -= track.durationMs
    }

    return BookPosition(0, 0)
}

fun restoreBookPosition(tracks: List<Track>, trackId: String?, positionMs: Long): BookPosition {
    val found = tracks.indexOfFirst { it.id == trackId }
    val index = found.coerceAtLeast(0)

    return BookPosition(
        index,
        if (found < 0) 0 else positionMs.coerceIn(0, tracks.getOrNull(index)?.durationMs ?: 0),
    )
}

fun elapsedBookMs(tracks: List<Track>, index: Int, positionMs: Long): Long =
    tracks.take(index).sumOf { it.durationMs } +
        positionMs.coerceIn(0, tracks.getOrNull(index)?.durationMs ?: 0)

fun restoredTimer(record: StoredSleep?, bootCount: Int, nowElapsedMs: Long): StoredSleep? =
    record?.let {
        if (
            it.active &&
                (bootCount < 0 ||
                    it.bootCount < 0 ||
                    it.bootCount != bootCount ||
                    (it.deadlineElapsedMs?.let { deadline -> deadline <= nowElapsedMs } == true))
        )
            it.copy(active = false, deadlineElapsedMs = null, endTrackId = null)
        else it
    }
