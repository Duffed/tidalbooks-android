package de.tidalbooks.prototype

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "backup_roundtrip_test.db"
    private lateinit var store: LibraryStore
    private lateinit var writerScope: CoroutineScope
    private val tracks = listOf(Track("a", "Anfang", 60_000, 1, 1), Track("b", "Weiter", 90_000, 2, 1))
    private fun book(id: String) = StoredBook(id, "Buch $id", "Autor", null, TrackCodec.encode(tracks),
        150_000, "b", 12_345, 72_345, false, 1, 2)
    @Before fun before() {
        context.deleteDatabase(name)
        writerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        store = LibraryStore(context, writerScope, name) { }
    }
    @After fun after() { writerScope.cancel(); store.database.close(); context.deleteDatabase(name) }
    @Test fun snapshotWaitsForQueuedWritesAndRoundTripRestoresWithoutDeletingOtherBooksOrMarks() = runBlocking {
        store.write {
            store.dao.insert(book("one")); store.dao.insert(book("other"))
            store.dao.bookmark(StoredBookmark("old", "one", "a", 5_000, 5_000, "Anfang", null, 3))
        }.await()
        store.write { store.dao.progress("one", "b", 22_222, 82_222, false, 4) }
        val backup = store.snapshot("one")
        assertEquals(22_222, backup.books.first { it.id == "one" }.progressMs)
        val mark = StoredBookmark("new", "one", "b", 3_333, 63_333, "Weiter", "Merken", 5)
        val anchor = StoredSleep(startBookId = "one", startTrackId = "a", startPositionMs = 8_000,
            active = true, deadlineElapsedMs = 90_000, endTrackId = null, bootCount = 3)
        store.restore(LibraryBackup(listOf(book("one")), listOf(mark), anchor, "one"))
        store.database.close()
        store = LibraryStore(context, writerScope, name) { }
        assertEquals(12_345, store.dao.book("one")!!.progressMs)
        assertNotNull(store.dao.book("other"))
        assertEquals(setOf("old", "new"), store.dao.allBookmarks().map { it.id }.toSet())
        assertEquals(anchor.inactive(), store.dao.sleep())
        store.restore(LibraryBackup(listOf(book("one")), listOf(mark), anchor, "one"))
        assertEquals(2, store.dao.allBookmarks().size)
    }
    @Test fun invalidBackupLeavesExistingProgressUntouched() = runBlocking {
        store.write { store.dao.insert(book("one")) }.await()
        try {
            store.restore(LibraryBackup(listOf(book("one").copy(progressMs = -1)), emptyList(), null, "one"))
            fail("Invalid backup restored")
        } catch (_: IllegalArgumentException) { }
        assertEquals(12_345, store.dao.book("one")!!.progressMs)
    }
    @Test fun conflictingBookmarkIdentityRejectsEntireRestore() = runBlocking {
        store.write {
            store.dao.insert(book("one")); store.dao.insert(book("other"))
            store.dao.bookmark(StoredBookmark("same", "other", "a", 0, 0, "Anfang", null, 1))
        }.await()
        val mark = StoredBookmark("same", "one", "a", 0, 0, "Anfang", null, 1)
        try {
            store.restore(LibraryBackup(listOf(book("one").copy(progressMs = 20_000)), listOf(mark), null, "one"))
            fail("Conflicting backup restored")
        } catch (_: IllegalArgumentException) { }
        assertEquals(12_345, store.dao.book("one")!!.progressMs)
        assertEquals("other", store.dao.allBookmarks().single().bookId)
    }
    @Test fun databaseFailureRollsBackBooksAlreadyWrittenInTheTransaction() = runBlocking {
        store.write {
            store.dao.insert(book("one"))
            store.database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_backup_mark BEFORE INSERT ON bookmarks BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        }.await()
        val mark = StoredBookmark("new", "one", "a", 0, 0, "Anfang", null, 1)
        try {
            store.restore(LibraryBackup(listOf(book("one").copy(progressMs = 20_000)), listOf(mark), null, "one"))
            fail("Database failure was ignored")
        } catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals(12_345, store.dao.book("one")!!.progressMs)
        assertTrue(store.dao.allBookmarks().isEmpty())
    }

}
