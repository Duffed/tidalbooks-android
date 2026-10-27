package de.tidalbooks.prototype

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "library_persistence_test.db"
    private var database: LibraryDatabase? = null
    private fun open(): LibraryDatabase = Room.databaseBuilder(context,LibraryDatabase::class.java,name).build().also { database = it }
    private val tracks = listOf(Track("track-a","Anfang",60_000,1,1),Track("track-b","Weiter",90_000,1,2))
    private fun book(id: String) = StoredBook(id,"Buch $id","Autor",null,TrackCodec.encode(tracks),150_000,"track-a",addedAt = 1,updatedAt = 1)
    @Before fun before() { context.deleteDatabase(name) }
    @After fun after() { database?.close(); context.deleteDatabase(name) }

    @Test fun twoBooksKeepIndependentProgressAfterDatabaseReopen() = runBlocking {
        var db = open(); val dao = db.library()
        dao.insert(book("one")); dao.insert(book("two"))
        dao.progress("one","track-b",12_345,72_345,false,2)
        dao.progress("two","track-a",23_456,23_456,false,3)
        db.close(); db = open()
        assertEquals("track-b",db.library().book("one")!!.progressTrackId)
        assertEquals(12_345,db.library().book("one")!!.progressMs)
        assertEquals(23_456,db.library().book("two")!!.progressMs)
    }
    @Test fun duplicateImportsDoNotResetProgress() = runBlocking {
        val dao = open().library()
        dao.insert(book("one")); dao.progress("one","track-b",42_000,102_000,false,2)
        dao.insert(book("one"))
        assertEquals(42_000,dao.book("one")!!.progressMs)
    }
    @Test fun bookmarksSurviveReopenRenameAndCascadeWithTheirBook() = runBlocking {
        var db = open(); db.library().insert(book("one"))
        db.library().bookmark(StoredBookmark("mark","one","track-b",4_321,64_321,"Weiter",null,10))
        db.close(); db = open()
        db.library().renameBookmark("mark","Wichtige Stelle")
        val restored = db.library().observeBookmarks().first().single()
        assertEquals("Wichtige Stelle",restored.bookmark.label)
        assertEquals(4_321,restored.bookmark.positionMs)
        assertEquals("Buch one",restored.bookTitle)
        db.library().delete("one")
        assertTrue(db.library().observeBookmarks().first().isEmpty())
    }
    @Test fun sleepAnchorAndDeadlineSurviveDatabaseReopen() = runBlocking {
        var db = open()
        val sleep = StoredSleep(startBookId = "one",startTrackId = "track-b",startPositionMs = 12_345,
            active = true,deadlineElapsedMs = 100_000,endTrackId = null,bootCount = 4)
        db.library().sleep(sleep); db.close(); db = open()
        assertEquals(sleep,db.library().sleep())
        assertFalse(restoredTimer(db.library().sleep(),5,1_000)!!.active)
        assertEquals(12_345,db.library().sleep()!!.startPositionMs)
    }
    @Test fun queuedPeriodicAndImmediateSavesRemainInSubmissionOrder() = runBlocking {
        val writerScope = CoroutineScope(SupervisorJob()+Dispatchers.Main)
        val errors = java.util.Collections.synchronizedList(mutableListOf<String>())
        val store = LibraryStore(context,writerScope,name) { errors.add(it) }
        database = store.database
        try {
            store.write { store.dao.insert(book("one")) }.await()
            repeat(20) { index ->
                val position = index*1000L
                store.write { store.dao.progress("one","track-b",position,60_000+position,false,index.toLong()) }
            }
            store.flush()
            assertEquals(19_000L,store.dao.book("one")!!.progressMs)
            assertTrue(errors.isEmpty())
        } finally { writerScope.cancel() }
    }
}
