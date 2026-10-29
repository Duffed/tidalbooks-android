package de.tidalbooks.prototype

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.URI

data class LibraryBackup(
    val books: List<StoredBook>, val bookmarks: List<StoredBookmark>,
    val sleep: StoredSleep?, val lastBookId: String?, val history: List<StoredJump> = emptyList(),
)

/** Versioned allowlist format: no credentials, player manifests, or stream addresses. */
object BackupCodec {
    const val MAX_BYTES = 10 * 1024 * 1024
    private const val FORMAT = "tidalbooks-library"
    private const val VERSION = 1
    private fun JSONObject.optional(key: String): String? = if (isNull(key)) null else getString(key)
    private fun id(value: String): String = value.also {
        require(it.matches(Regex("[A-Za-z0-9_-]{1,128}"))) { translated("Ungültige Kennung in der Sicherung.") }
    }
    private fun text(value: String, max: Int = 2000) = value.also {
        require(it.length <= max) { translated("Ein Text in der Sicherung ist zu lang.") }
    }
    private fun nonnegative(value: Long) = value.also { require(it >= 0) { translated("Ungültiger Zahlenwert in der Sicherung.") } }
    private fun JSONArray.objects(max: Int): List<JSONObject> {
        require(length() <= max) { translated("Die Sicherung enthält zu viele Einträge.") }
        return (0 until length()).map { getJSONObject(it) }
    }
    fun read(input: InputStream): LibraryBackup {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= MAX_BYTES) { translated("Die Sicherung ist größer als 10 MB.") }
            output.write(buffer, 0, count)
        }
        return decode(output.toString(Charsets.UTF_8.name()))
    }
    fun encode(value: LibraryBackup): String = JSONObject().apply {
        put("format", FORMAT); put("version", VERSION)
        put("lastBookId", value.lastBookId ?: JSONObject.NULL)
        put("books", JSONArray().apply { value.books.forEach { b -> put(JSONObject().apply {
            put("id", b.id); put("title", b.title); put("author", b.author)
            put("coverUrl", b.coverUrl ?: JSONObject.NULL); put("tracks", JSONArray(b.tracksJson))
            put("progressTrackId", b.progressTrackId ?: JSONObject.NULL); put("progressMs", b.progressMs)
            put("lastPlayedAt", b.lastPlayedAt); put("completed", b.completed); put("addedAt", b.addedAt); put("updatedAt", b.updatedAt)
        }) } })
        put("bookmarks", JSONArray().apply { value.bookmarks.forEach { m -> put(JSONObject().apply {
            put("id", m.id); put("bookId", m.bookId); put("trackId", m.trackId)
            put("positionMs", m.positionMs); put("label", m.label ?: JSONObject.NULL); put("createdAt", m.createdAt)
        }) } })
        put("history", JSONArray().apply { value.history.forEach { j -> put(JSONObject()
            .put("id", j.id).put("bookId", j.bookId).put("trackId", j.trackId).put("positionMs", j.positionMs)
            .put("createdAt", j.createdAt).put("reason", j.reason)) } })
        put("sleepAnchor", value.sleep?.let { a -> JSONObject().apply {
            put("bookId", a.startBookId); put("trackId", a.startTrackId); put("positionMs", a.startPositionMs)
        } } ?: JSONObject.NULL)
    }.toString(2).also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { translated("Die Sicherung ist größer als 10 MB.") } }

    fun decode(json: String): LibraryBackup {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { translated("Die Sicherung ist größer als 10 MB.") }
        val root = JSONObject(json)
        require(root.getString("format") == FORMAT) { translated("Das ist keine Hörbuch-Sicherung.") }
        require(root.getInt("version") == VERSION) { translated("Diese Sicherungsversion wird noch nicht unterstützt.") }
        var trackCount = 0
        val books = root.getJSONArray("books").objects(2000).map { b ->
            val tracks = b.getJSONArray("tracks").objects(20000).map { t ->
                val duration = t.getLong("duration")
                require(duration in 1..604_800_000L) { translated("Ungültige Kapiteldauer.") }
                Track(id(t.getString("id")), text(t.getString("title")), duration,
                    t.getInt("disc").also { require(it in 1..10000) }, t.getInt("number").also { require(it in 1..100000) })
            }
            trackCount += tracks.size
            require(tracks.isNotEmpty() && trackCount <= 100000 && tracks.map { it.id }.toSet().size == tracks.size) {
                translated("Ungültige Kapitelliste in der Sicherung.")
            }
            val trackId = b.optional("progressTrackId") ?: tracks.first().id
            val trackIndex = tracks.indexOfFirst { it.id == trackId }
            require(trackIndex >= 0) { translated("Der Hörstand verweist auf ein fehlendes Kapitel.") }
            val position = b.getLong("progressMs")
            require(position in 0..tracks[trackIndex].durationMs) { translated("Ungültiger Hörstand.") }
            val cover = b.optional("coverUrl")?.also {
                val uri = URI(it)
                require(it.length <= 2048 && uri.scheme == "https" && uri.host == "resources.tidal.com" &&
                    uri.path.startsWith("/images/") && uri.userInfo == null && uri.query == null && uri.fragment == null &&
                    uri.port in listOf(-1, 443)) { translated("Ungültige Coveradresse in der Sicherung.") }
            }
            StoredBook(id(b.getString("id")), text(b.getString("title")), text(b.getString("author")), cover,
                TrackCodec.encode(tracks), tracks.sumOf { it.durationMs }, trackId, position,
                elapsedBookMs(tracks, trackIndex, position), b.getBoolean("completed"),
                nonnegative(b.getLong("addedAt")), nonnegative(b.getLong("updatedAt")), nonnegative(b.optLong("lastPlayedAt", 0)))
        }
        val byId = books.associateBy { it.id }
        require(byId.size == books.size) { translated("Die Sicherung enthält doppelte Bücher.") }
        val locations = books.associate { book ->
            var elapsed = 0L
            book.id to book.album().tracks.associate { track ->
                val start = elapsed
                elapsed += track.durationMs
                track.id to (track to start)
            }
        }
        fun position(bookId: String, trackId: String, offset: Long): Pair<Track, Long> {
            val (track, start) = requireNotNull(locations[bookId]?.get(trackId)) { translated("Ein Lesezeichen verweist auf ein fehlendes Kapitel.") }
            require(offset in 0..track.durationMs) { translated("Ungültige gespeicherte Stelle.") }
            return track to (start + offset)
        }
        val marks = root.getJSONArray("bookmarks").objects(50000).map { m ->
            val bookId = id(m.getString("bookId")); val trackId = id(m.getString("trackId")); val offset = m.getLong("positionMs")
            val (track, elapsed) = position(bookId, trackId, offset)
            StoredBookmark(id(m.getString("id")), bookId, trackId, offset, elapsed, track.title,
                m.optional("label")?.let { text(it, 120) }, nonnegative(m.getLong("createdAt")))
        }
        require(marks.map { it.id }.toSet().size == marks.size) { translated("Die Sicherung enthält doppelte Lesezeichen.") }
        val anchor = if (root.isNull("sleepAnchor")) null else root.getJSONObject("sleepAnchor").let { a ->
            val bookId = id(a.getString("bookId")); val trackId = id(a.getString("trackId")); val offset = a.getLong("positionMs")
            position(bookId, trackId, offset)
            StoredSleep(startBookId = bookId, startTrackId = trackId, startPositionMs = offset,
                active = false, deadlineElapsedMs = null, endTrackId = null, bootCount = -1)
        }
        val lastBook = root.optional("lastBookId")
        require(lastBook == null || lastBook in byId) { translated("Das zuletzt geöffnete Buch fehlt in der Sicherung.") }
        val history = (root.optJSONArray("history") ?: JSONArray()).objects(20000).map { j ->
            val bookId = id(j.getString("bookId")); val trackId = id(j.getString("trackId")); val offset = j.getLong("positionMs")
            position(bookId, trackId, offset)
            StoredJump(id(j.getString("id")), bookId, trackId, offset, nonnegative(j.getLong("createdAt")), text(j.getString("reason"), 120))
        }
        require(history.map { it.id }.toSet().size == history.size && history.groupBy { it.bookId }.values.all { it.size <= 10 }) {
            translated("Ungültiger Positionsverlauf.")
        }
        return LibraryBackup(books, marks, anchor, lastBook, history)
    }
}
