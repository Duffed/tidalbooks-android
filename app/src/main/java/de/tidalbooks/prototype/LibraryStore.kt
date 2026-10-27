package de.tidalbooks.prototype

import android.content.Context
import androidx.room.*
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
) {
    fun album() = Album(id, title, TrackCodec.decode(tracksJson), author, coverUrl)
}

data class LibraryBook(
    val id: String, val title: String, val author: String, val coverUrl: String?,
    val durationMs: Long, val elapsedMs: Long, val completed: Boolean, val updatedAt: Long,
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

@Dao
interface LibraryDao {
    @Query("SELECT id,title,author,coverUrl,durationMs,elapsedMs,completed,updatedAt FROM books ORDER BY updatedAt DESC, addedAt DESC")
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

@Database(entities = [StoredBook::class, StoredBookmark::class, StoredSleep::class], version = 1, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() { abstract fun library(): LibraryDao }

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
    val database = Room.databaseBuilder(context, LibraryDatabase::class.java, name).build()
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
    suspend fun add(album: Album) {
        val now = System.currentTimeMillis()
        val book = StoredBook(album.id, album.title, album.author, album.coverUrl, TrackCodec.encode(album.tracks),
            album.tracks.sumOf { it.durationMs }, album.tracks.firstOrNull()?.id, addedAt = now, updatedAt = now)
        write { dao.insert(book) }.await()
    }
}
