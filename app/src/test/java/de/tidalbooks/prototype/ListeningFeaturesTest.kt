package de.tidalbooks.prototype

import org.junit.Assert.*
import org.junit.Test

class ListeningFeaturesTest {
    private fun book(id: String, title: String, author: String, added: Long, heard: Long, elapsed: Long = 0) =
        LibraryBook(id, title, author, null, 100_000, elapsed, false, 999, added, heard)
    private val books = listOf(book("1", "Zweite Welt", "Anna", 10, 30, 70_000),
        book("2", "Äpfel im Winter", "Berta", 30, 10), book("3", "Abend", "Claus", 20, 20, 20_000))
    @Test fun searchCombinesTitleAuthorAndStatusWithoutChangingSource() {
        assertEquals(listOf("1"), filterBooks(books, "  WELT anna ", "Angefangen", BookSort.TITLE).map { it.id })
        assertTrue(filterBooks(books, "Welt", "Beendet", BookSort.TITLE).isEmpty())
        assertEquals(3, books.size)
    }
    @Test fun sortUsesActualListeningAndImportTimestamps() {
        assertEquals(listOf("1", "3", "2"), filterBooks(books, "", "Alle", BookSort.LAST_PLAYED).map { it.id })
        assertEquals(listOf("2", "3", "1"), filterBooks(books, "", "Alle", BookSort.ADDED).map { it.id })
        assertEquals(listOf("3", "2", "1"), filterBooks(books, "", "Alle", BookSort.TITLE).map { it.id })
        assertEquals(listOf("1", "3", "2"), filterBooks(books, "", "Alle", BookSort.REMAINING).map { it.id })
    }
    @Test fun resumeRequiresOptInFiveMinutesAndSameBoot() {
        assertEquals(10_000L, resumeOffsetMs(10, 1000, 4, 301_000, 4))
        assertEquals(0L, resumeOffsetMs(10, 1000, 4, 300_999, 4))
        assertEquals(0L, resumeOffsetMs(0, 1000, 4, 900_000, 4))
        assertEquals(0L, resumeOffsetMs(10, 1000, 4, 900_000, 5))
        assertEquals(0L, resumeOffsetMs(10, -1, 4, 900_000, 4))
        assertEquals(0L, resumeOffsetMs(10, 1000, 4, 500, 4))
    }
    @Test fun errorMessagesDoNotExposeServerBodiesOrCredentials() {
        val secret = "https://example.com/?token=private"
        assertFalse(friendlyFailure(java.io.IOException(secret)).contains(secret))
        assertTrue(friendlyFailure(CatalogFailure(401)).contains("Anmeldung"))
        assertTrue(friendlyFailure(CatalogFailure(404)).contains("nicht verfügbar"))
        assertTrue(friendlyFailure(CatalogFailure(429)).contains("warte"))
    }
}
