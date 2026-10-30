package de.tidalbooks.prototype

import android.os.Handler
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tidal.sdk.player.playbackengine.PlaybackEngine
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class TidalSleepVolumeTest {
    @Test fun pinnedSdkSupportsAppOnlyFadeAndRestoresOriginalVolume() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<ProbeApp>()
        val player = withContext(Dispatchers.Main) { app.gateway.createPlayer() }
        val engine = player.playbackEngine
        val failed = AtomicBoolean(false)
        val adapter = TidalSleepVolume(engine) { failed.set(true) }
        try {
            assertTrue("Pinned SDK no longer matches the volume adapter", adapter.isSupported())
            val before = volume(engine)
            adapter.apply(.5f)
            assertEquals(before * .5f, volume(engine), .001f)
            adapter.apply(0f)
            assertEquals(0f, volume(engine), .001f)
            adapter.restore()
            assertEquals(before, volume(engine), .001f)
            assertFalse(failed.get())
        } finally { engine.release() }
    }
    private suspend fun volume(engine: PlaybackEngine): Float {
        val type = engine.javaClass
        val handler = type.getDeclaredField("handler").apply { isAccessible = true }.get(engine) as Handler
        val delegate = requireNotNull(type.getDeclaredField("delegate").apply { isAccessible = true }.get(engine))
        val result = CompletableDeferred<Float>()
        handler.post {
            try { result.complete(delegate.javaClass.getMethod("getPlayerVolume").invoke(delegate) as Float) }
            catch (e: Exception) { result.completeExceptionally(e) }
        }
        return withTimeout(5000) { result.await() }
    }
}
