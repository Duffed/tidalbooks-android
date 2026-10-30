package de.tidalbooks.prototype

/** No credentials or network data; position changes are capped to one widget update per 15 s. */
internal data class WidgetSnapshot(
    val title: String, val coverUrl: String?, val hasBook: Boolean, val completed: Boolean,
    val pauseAction: Boolean, val chapter: Int, val chapters: Int, val elapsedMs: Long,
    val durationMs: Long, val timerActive: Boolean, val deadlineMs: Long?, val lastTimerMinutes: Int, val skipSeconds: Int = 30,
) {
    companion object {
        fun from(s: ProbeState): WidgetSnapshot {
            val book = s.album
            return WidgetSnapshot(book?.title ?: translated("Hörbuch auswählen"),book?.coverUrl,book != null,s.completed,
                s.requestedPlay && !s.completed,s.index+1,book?.tracks?.size ?: 0,
                if (book != null) elapsedBookMs(book.tracks,s.index,s.positionMs)/15_000*15_000 else 0,
                book?.tracks?.sumOf { it.durationMs } ?: 0,s.sleepSession?.active == true,
                s.sleepSession?.takeIf { it.active }?.deadlineElapsedMs,s.lastTimerMinutes.coerceIn(1,180),s.skipSeconds)
        }
    }
}
