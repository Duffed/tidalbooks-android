package de.tidalbooks.prototype

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "books")
data class StoredBook(
    @PrimaryKey val id: String,
    val title: String, val author: String, val coverUrl: String?, val tracksJson: String,
    val durationMs: Long, val progressTrackId: String?, val progressMs: Long = 0,
    val elapsedMs: Long = 0, val completed: Boolean = false, val addedAt: Long, val updatedAt: Long,
    @ColumnInfo(defaultValue = "0") val lastPlayedAt: Long = 0,
) {
    fun album() = Album(id, title, TrackCodec.decode(tracksJson), author, coverUrl)
}

data class LibraryBook(
    val id: String, val title: String, val author: String, val coverUrl: String?,
    val durationMs: Long, val elapsedMs: Long, val completed: Boolean, val updatedAt: Long,
    val addedAt: Long = 0, val lastPlayedAt: Long = 0,
) {
    val fraction get() = if (durationMs > 0) (elapsedMs.toFloat()/durationMs).coerceIn(0f, 1f) else 0f
    val status get() = if (completed) "Beendet" else if (elapsedMs > 0) "Angefangen" else "Nicht begonnen"
}

@Entity(tableName = "bookmarks", foreignKeys = [ForeignKey(entity = StoredBook::class,
    parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)], indices = [Index("bookId")])
data class StoredBookmark(
    @PrimaryKey val id: String, val bookId: String, val trackId: String,
    val positionMs: Long, val elapsedMs: Long, val chapterTitle: String,
    val label: String?, val createdAt: Long,
)
data class BookmarkItem(@Embedded val bookmark: StoredBookmark, val bookTitle: String, val bookCover: String?)

@Entity(tableName = "sleep_session")
data class StoredSleep(
    @PrimaryKey val id: Int = 1,
    val startBookId: String, val startTrackId: String, val startPositionMs: Long,
    val active: Boolean, val deadlineElapsedMs: Long?, val endTrackId: String?, val bootCount: Int,
)

@Entity(tableName = "position_history", foreignKeys = [ForeignKey(entity = StoredBook::class,
    parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)], indices = [Index("bookId")])
data class StoredJump(@PrimaryKey val id: String, val bookId: String, val trackId: String,
    val positionMs: Long, val createdAt: Long, val reason: String)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM position_history ORDER BY createdAt DESC, rowid DESC") fun observeHistory(): Flow<List<StoredJump>>
    @Query("SELECT * FROM position_history ORDER BY createdAt DESC, rowid DESC") suspend fun allHistory(): List<StoredJump>
    @Upsert suspend fun insertHistory(jump: StoredJump)
    @Query("DELETE FROM position_history WHERE bookId=:bookId AND id NOT IN (SELECT id FROM position_history WHERE bookId=:bookId ORDER BY createdAt DESC, rowid DESC LIMIT 10)")
    suspend fun trimHistory(bookId: String)
    @Query("DELETE FROM position_history WHERE id=:id") suspend fun deleteHistory(id: String)
    @Query("DELETE FROM position_history WHERE bookId=:bookId") suspend fun clearHistory(bookId: String)
    @Query("UPDATE books SET lastPlayedAt=:time WHERE id=:id") suspend fun heard(id: String, time: Long)

    @Query("SELECT * FROM books ORDER BY addedAt, id") suspend fun allBooks(): List<StoredBook>
    @Query("SELECT * FROM bookmarks ORDER BY createdAt, id") suspend fun allBookmarks(): List<StoredBookmark>
    @Upsert suspend fun restoreBooks(books: List<StoredBook>)
    @Upsert suspend fun restoreBookmarks(bookmarks: List<StoredBookmark>)
    @Query("SELECT id,title,author,coverUrl,durationMs,elapsedMs,completed,updatedAt,addedAt,lastPlayedAt FROM books ORDER BY updatedAt DESC, addedAt DESC")
    fun observeBooks(): Flow<List<LibraryBook>>
    @Query("SELECT * FROM books WHERE id=:id") suspend fun book(id: String): StoredBook?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(book: StoredBook)
    @Query("UPDATE books SET progressTrackId=:trackId,progressMs=:positionMs,elapsedMs=:elapsedMs,completed=:completed,updatedAt=:updatedAt WHERE id=:id")
    suspend fun progress(id: String, trackId: String, positionMs: Long, elapsedMs: Long, completed: Boolean, updatedAt: Long)
    @Query("DELETE FROM books WHERE id=:id") suspend fun delete(id: String)
    @Query("SELECT bookmarks.*, books.title AS bookTitle, books.coverUrl AS bookCover FROM bookmarks JOIN books ON books.id=bookmarks.bookId ORDER BY bookmarks.createdAt DESC")
    fun observeBookmarks(): Flow<List<BookmarkItem>>
    @Insert suspend fun bookmark(mark: StoredBookmark)
    @Query("UPDATE bookmarks SET label=:label WHERE id=:id") suspend fun renameBookmark(id: String, label: String?)
    @Query("DELETE FROM bookmarks WHERE id=:id") suspend fun deleteBookmark(id: String)
    @Query("SELECT * FROM sleep_session WHERE id=1") suspend fun sleep(): StoredSleep?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun sleep(value: StoredSleep)
    @Query("DELETE FROM sleep_session WHERE startBookId=:id") suspend fun deleteSleepForBook(id: String)
}

@Database(entities = [StoredBook::class, StoredBookmark::class, StoredSleep::class, StoredJump::class], version = 2, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun library(): LibraryDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN lastPlayedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS position_history (id TEXT NOT NULL, bookId TEXT NOT NULL, trackId TEXT NOT NULL, positionMs INTEGER NOT NULL, createdAt INTEGER NOT NULL, reason TEXT NOT NULL, PRIMARY KEY(id), FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_position_history_bookId ON position_history(bookId)")
            }
        }
    }
}

object TrackCodec {
    fun encode(tracks: List<Track>): String = JSONArray().apply {
        tracks.forEach { put(JSONObject().put("id",it.id).put("title",it.title).put("duration",it.durationMs)
            .put("disc",it.disc).put("number",it.number)) }
    }.toString()
    fun decode(value: String): List<Track> {
        val array = JSONArray(value)
        return (0 until array.length()).map { index -> array.getJSONObject(index).let {
            Track(it.getString("id"),it.getString("title"),it.getLong("duration"),it.getInt("disc"),it.getInt("number"))
        } }
    }
}

/** One ordered writer prevents an older periodic snapshot from overtaking a pause or seek. */
class LibraryStore(context: Context, scope: CoroutineScope, name: String = "audiobooks.db", onError: (String) -> Unit) {
    val database = Room.databaseBuilder(context, LibraryDatabase::class.java, name).addMigrations(LibraryDatabase.MIGRATION_1_2).build()
    val dao = database.library()
    private data class Write(val action: suspend () -> Unit, val done: CompletableDeferred<Unit>)
    private val writes = Channel<Write>(Channel.UNLIMITED)
    init {
        scope.launch(Dispatchers.IO) {
            for (write in writes) {
                try { write.action(); write.done.complete(Unit) }
                catch (e: CancellationException) { write.done.cancel(e); throw e }
                catch (e: Exception) {
                    write.done.completeExceptionally(e)
                    withContext(Dispatchers.Main) { onError("Änderung konnte nicht gespeichert werden. Bitte Speicherplatz prüfen.") }
                }
            }
        }
    }
    fun write(action: suspend () -> Unit): Deferred<Unit> {
        val done = CompletableDeferred<Unit>()
        if (writes.trySend(Write(action,done)).isFailure) done.completeExceptionally(IllegalStateException("Speicher nicht verfügbar."))
        return done
    }
    suspend fun flush() { write {}.await() }
    suspend fun snapshot(lastBookId: String?): LibraryBackup {
        var snapshot: LibraryBackup? = null
        write { database.withTransaction {
            val books = dao.allBooks()
            snapshot = LibraryBackup(books, dao.allBookmarks(), dao.sleep()?.inactive(),
                lastBookId?.takeIf { id -> books.any { it.id == id } }, dao.allHistory())
        } }.await()
        return requireNotNull(snapshot)
    }
    suspend fun restore(backup: LibraryBackup) {
        // Freeze and validate before entering the ordered writer. Upsert avoids REPLACE's cascades.
        val frozen = withContext(Dispatchers.IO) { BackupCodec.decode(BackupCodec.encode(backup)) }
        write { database.withTransaction {
            val importedIds = frozen.books.map { it.id }.toSet()
            val existingMarks = dao.allBookmarks()
            val importedMarks = frozen.bookmarks.associateBy { it.id }
            require(existingMarks.none { old -> importedMarks[old.id]?.let { it.bookId != old.bookId } == true }) {
                "Die Sicherung enthält widersprüchliche Lesezeichen. Es wurde nichts wiederhergestellt."
            }
            // Retained bookmarks must remain usable with the restored track metadata.
            val restoredTracks = frozen.books.associate { it.id to it.album().tracks.associate { t -> t.id to t.durationMs } }
            require(existingMarks.none { mark ->
                mark.bookId in importedIds && restoredTracks.getValue(mark.bookId)[mark.trackId]
                    ?.let { mark.positionMs in 0..it } != true
            }) {
                "Die Abschnittsliste passt nicht zu vorhandenen Lesezeichen. Es wurde nichts wiederhergestellt."
            }
            dao.restoreBooks(frozen.books)
            // Old history may refer to a changed track list; restored books get their backed-up history.
            frozen.books.forEach { dao.clearHistory(it.id) }
            frozen.history.sortedBy { it.createdAt }.forEach { dao.insertHistory(it); dao.trimHistory(it.bookId) }
            dao.restoreBookmarks(frozen.bookmarks)
            (frozen.sleep ?: dao.sleep())?.let { dao.sleep(it.inactive()) }
        } }.await()
    }
    suspend fun add(album: Album) {
        val now = System.currentTimeMillis()
        val book = StoredBook(album.id, album.title, album.author, album.coverUrl, TrackCodec.encode(album.tracks),
            album.tracks.sumOf { it.durationMs }, album.tracks.firstOrNull()?.id, addedAt = now, updatedAt = now)
        write { dao.insert(book) }.await()
    }
}

internal fun StoredSleep.inactive() = copy(active = false, deadlineElapsedMs = null, endTrackId = null, bootCount = -1)
