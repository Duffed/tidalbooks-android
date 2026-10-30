package de.tidalbooks.prototype

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*

class ArtworkStoreTest {
    private val directory = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "artwork-isolated-test")
    private val url = "https://resources.tidal.com/images/test/640x640.jpg"
    private fun image(): ByteArray = ByteArrayOutputStream().use { output ->
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        output.toByteArray()
    }
    @Before fun before() { directory.deleteRecursively(); directory.mkdirs() }
    @After fun after() { directory.deleteRecursively() }

    @Test fun simultaneousViewsShareDownloadAndNewStoreReadsDiskOffline() = runBlocking {
        val calls = AtomicInteger()
        val bytes = image()
        val store = ArtworkStore(directory) { calls.incrementAndGet(); Thread.sleep(100); bytes }
        val results = (1..12).map { async { store.load(url) } }.awaitAll()
        assertNotNull(results.first())
        results.forEach { assertSame(results.first(), it) }
        assertEquals(1, calls.get())
        val reopened = ArtworkStore(directory) { error("Offline: must use disk") }
        assertEquals(32, reopened.load(url)!!.width)
    }

    @Test fun corruptCacheIsReplacedAndInvalidDownloadsAreNotStored() = runBlocking {
        val bytes = image()
        assertNotNull(ArtworkStore(directory) { bytes }.load(url))
        directory.listFiles()!!.single().writeText("broken")
        val calls = AtomicInteger()
        assertNotNull(ArtworkStore(directory) { calls.incrementAndGet(); bytes }.load(url))
        assertEquals(1, calls.get())
        assertNull(ArtworkStore(directory) { byteArrayOf(1, 2) }.load(url + "?invalid"))
        assertEquals(1, directory.listFiles()!!.size)
    }

    @Test fun diskBudgetEvictsOldFilesAndUntrustedHostsNeverFetch() = runBlocking {
        val bytes = image()
        val calls = AtomicInteger()
        val store = ArtworkStore(directory, bytes.size.toLong()) { calls.incrementAndGet(); bytes }
        assertNull(store.load("https://example.com/images/test.jpg"))
        assertEquals(0, calls.get())
        assertNotNull(store.load(url))
        assertNotNull(store.load(url + "?second"))
        assertTrue(directory.listFiles()!!.sumOf { it.length() } <= bytes.size.toLong())
        assertNotNull(ArtworkStore(directory) { error("Cached newest image expected") }.load(url + "?second"))
    }

    @Test fun widgetCacheReadsNeverTriggerNetworkAndSurviveANewStore() = runBlocking {
        val bytes = image()
        val calls = AtomicInteger()
        val store = ArtworkStore(directory) { calls.incrementAndGet(); bytes }
        assertNull(store.loadCached(url))
        assertEquals(0,calls.get())
        assertNotNull(store.load(url))
        val reopened = ArtworkStore(directory) { error("Widget must never fetch network artwork") }
        assertEquals(32,reopened.loadCached(url)!!.width)
        assertNull(reopened.loadCached(url+"?missing"))
        assertEquals(1,calls.get())
    }
}
