package de.tidalbooks.prototype

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

class ListeningPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "listening_features_test.db"
    private val tracks = listOf(Track("a", "Anfang", 60_000, 1, 1))
    @Before fun before() { context.deleteDatabase(name) }
    @After fun after() { context.deleteDatabase(name) }
    @Test fun migrationFromRealV1SchemaPreservesProgressBookmarksAndSleepAnchor() = runBlocking {
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("de.tidalbooks.prototype.LibraryDatabase/1.json").bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val file = context.getDatabasePath(name); file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}", table))
            }
            old.execSQL("INSERT INTO books VALUES ('book','Titel','Autor',NULL,?,60000,'a',12345,12345,0,1,2)", arrayOf(TrackCodec.encode(tracks)))
            old.execSQL("INSERT INTO bookmarks VALUES ('mark','book','a',5000,5000,'Anfang','Merken',1)")
            old.execSQL("INSERT INTO sleep_session VALUES (1,'book','a',4000,0,NULL,NULL,1)")
            old.version = 1
        }
        val db = Room.databaseBuilder(context, LibraryDatabase::class.java, name).addMigrations(LibraryDatabase.MIGRATION_1_2).build()
        try {
            assertEquals(12_345L, db.library().book("book")!!.progressMs)
            assertEquals(0L, db.library().book("book")!!.lastPlayedAt)
            assertEquals(5000L, db.library().allBookmarks().single().positionMs)
            assertEquals(4000L, db.library().sleep()!!.startPositionMs)
            assertTrue(db.library().allHistory().isEmpty())
        } finally { db.close() }
    }
    @Test fun historyIsLimitedPerBookSurvivesReopenAndIsBackedUp() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        var store = LibraryStore(context, scope, name) { }
        try {
            store.add(Album("book", "Titel", tracks)); store.add(Album("other", "Anderes", tracks))
            repeat(14) { n -> store.write {
                store.dao.insertHistory(StoredJump("j$n", "book", "a", n*1000L, n.toLong(), "Vor dem Spulen"))
                store.dao.trimHistory("book")
            } }
            store.write { store.dao.insertHistory(StoredJump("other-jump", "other", "a", 500, 20, "Vor dem Spulen")) }.await()
            val snapshot = store.snapshot("book")
            assertEquals(11, snapshot.history.size)
            assertEquals((4..13).map { "j$it" }.toSet(), snapshot.history.filter { it.bookId == "book" }.map { it.id }.toSet())
            val backup = BackupCodec.decode(BackupCodec.encode(snapshot))
            store.database.close(); store = LibraryStore(context, scope, name) { }
            assertEquals(snapshot.history, store.dao.allHistory())
            store.restore(backup)
            assertEquals(snapshot.history.toSet(), store.dao.allHistory().toSet())
            store.write { store.dao.deleteHistory("j13") }.await()
            assertEquals("j12", store.dao.allHistory().first { it.bookId == "book" }.id)
            store.write { store.dao.delete("book") }.await()
            assertEquals("other-jump", store.dao.allHistory().single().id)
        } finally { store.database.close(); scope.cancel() }
    }
}
