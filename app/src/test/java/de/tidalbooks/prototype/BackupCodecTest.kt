package de.tidalbooks.prototype

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class BackupCodecTest {
    private val tracks = listOf(Track("a", "Äußere Welt", 60_000, 1, 1), Track("b", "Zweite Disc", 90_000, 2, 1))
    private val book = StoredBook("123", "Ein Buch", "Autor", "https://resources.tidal.com/images/a/640x640.jpg",
        TrackCodec.encode(tracks), 150_000, "b", 12_345, 72_345, false, 10, 20)
    private val mark = StoredBookmark("mark-1", "123", "a", 4_321, 4_321, "Äußere Welt", "Eine Stelle", 30)
    private val anchor = StoredSleep(startBookId = "123", startTrackId = "b", startPositionMs = 10_000,
        active = true, deadlineElapsedMs = 500_000, endTrackId = null, bootCount = 4)
    private val backup = LibraryBackup(listOf(book), listOf(mark), anchor, "123")
    @Test fun roundTripPreservesPositionsMetadataAndIndependentBookmarks() {
        val restored = BackupCodec.read(ByteArrayInputStream(BackupCodec.encode(backup).toByteArray()))
        assertEquals(listOf(book), restored.books)
        assertEquals(listOf(mark), restored.bookmarks)
        assertEquals("123", restored.lastBookId)
        assertEquals(anchor.inactive(), restored.sleep)
    }
    @Test fun emptyLibraryAndCompletedBookRoundTrip() {
        val empty = LibraryBackup(emptyList(), emptyList(), null, null)
        assertEquals(empty, BackupCodec.decode(BackupCodec.encode(empty)))
        val complete = book.copy(progressMs = 90_000, elapsedMs = 150_000, completed = true)
        assertEquals(complete, BackupCodec.decode(BackupCodec.encode(backup.copy(books = listOf(complete)))).books.single())
    }
    @Test fun backupNeverExportsActiveTimerOrCredentials() {
        val json = BackupCodec.encode(backup)
        for (name in listOf("deadline", "bootCount", "active", "token", "password", "manifest", "streamUrl")) assertFalse(json.contains(name))
    }
    @Test fun rejectsUnknownVersionAndWrongFormat() {
        reject { it.put("version", 99) }
        reject { it.put("format", "another-app") }
    }
    @Test fun rejectsDuplicateBooksAndOrphanedBookmarks() {
        reject { it.getJSONArray("books").put(it.getJSONArray("books").getJSONObject(0)) }
        reject { it.getJSONArray("bookmarks").getJSONObject(0).put("bookId", "missing") }
    }
    @Test fun rejectsMissingTracksAndInvalidOffsets() {
        reject { it.getJSONArray("books").getJSONObject(0).put("progressTrackId", "missing") }
        reject { it.getJSONArray("books").getJSONObject(0).put("progressMs", -1) }
        reject { it.getJSONArray("bookmarks").getJSONObject(0).put("positionMs", 99_999) }
        reject { it.getJSONObject("sleepAnchor").put("trackId", "missing") }
    }
    @Test fun rejectsInvalidDurationsAndCoverUrls() {
        reject { it.getJSONArray("books").getJSONObject(0).getJSONArray("tracks").getJSONObject(0).put("duration", Long.MAX_VALUE) }
        reject { it.getJSONArray("books").getJSONObject(0).put("coverUrl", "https://example.com/private") }
        reject { it.getJSONArray("books").getJSONObject(0).put("coverUrl", "https://resources.tidal.com/images/a?token=secret") }
    }
    @Test fun rejectsOversizedInputBeforeParsing() {
        try { BackupCodec.read(ByteArrayInputStream(ByteArray(BackupCodec.MAX_BYTES + 1))); fail() }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun olderBackupsWithoutListeningHistoryRemainReadable() {
        val json = JSONObject(BackupCodec.encode(backup))
        json.remove("history")
        json.getJSONArray("books").getJSONObject(0).remove("lastPlayedAt")
        val restored = BackupCodec.decode(json.toString())
        assertEquals(book, restored.books.single())
        assertTrue(restored.history.isEmpty())
    }
    private fun reject(change: (JSONObject) -> Unit) {
        val json = JSONObject(BackupCodec.encode(backup)).also(change)
        try { BackupCodec.decode(json.toString()); fail("Invalid backup accepted") }
        catch (_: IllegalArgumentException) { }
    }
}
