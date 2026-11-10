package de.tidalbooks.prototype

import android.os.Handler
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TidalPlaybackSpeedTest {
    @Test fun pinnedSdkChangesSpeedWithoutStartingPlaybackAndSurvivesReset() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<ProbeApp>()
        val player = withContext(Dispatchers.Main) { app.gateway.createPlayer() }
        val engine = player.playbackEngine
        val failed = AtomicBoolean(false)
        val adapter = TidalPlaybackSpeed(engine) { failed.set(true) }
        val type = engine.javaClass
        val handler = type.getDeclaredField("handler").apply { isAccessible = true }.get(engine) as Handler
        val delegate = requireNotNull(type.getDeclaredField("delegate").apply { isAccessible = true }.get(engine))
        val getPlayer = delegate.javaClass.getDeclaredMethod("getExtendedExoPlayer").apply { isAccessible = true }

        suspend fun checkSpeed(speed: Float) {
            adapter.apply(speed)
            val checked = CompletableDeferred<Unit>()
            handler.post {
                try {
                    val actual = getPlayer.invoke(delegate) as Player
                    assertEquals(speed, actual.playbackParameters.speed, .001f)
                    assertEquals(1f, actual.playbackParameters.pitch, .001f)
                    assertFalse(actual.playWhenReady)
                    checked.complete(Unit)
                } catch (e: Throwable) { checked.completeExceptionally(e) }
            }
            withTimeout(5000) { checked.await() }
        }

        try {
            assertTrue(adapter.isSupported())
            checkSpeed(1.5f)
            checkSpeed(.75f)
            engine.reset()
            checkSpeed(2f)
            checkSpeed(1f)
            assertFalse(failed.get())
        } finally { engine.release() }
    }
}
