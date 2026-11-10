package de.tidalbooks.prototype

import android.os.Handler
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.tidal.sdk.player.playbackengine.PlaybackEngine
import java.util.concurrent.atomic.AtomicBoolean

/** Version-bound adapter for TIDAL 0.0.73, like TidalSleepVolume. Recheck on SDK updates.
 * Resolve the current player each time: resets and chapter transitions may replace it.
 * All player access stays on the SDK handler; play/pause, position and focus are untouched.
 */
internal class TidalPlaybackSpeed(engine: PlaybackEngine, private val onFailure: () -> Unit) {
    private data class Access(val handler: Handler, val delegate: Any, val getPlayer: java.lang.reflect.Method)
    private val access = runCatching {
        val wrapper = engine.javaClass
        require(wrapper.name == "com.tidal.sdk.player.playbackengine.SingleHandlerPlaybackEngine")
        val handler = wrapper.getDeclaredField("handler").apply { isAccessible = true }.get(engine) as Handler
        val delegate = requireNotNull(wrapper.getDeclaredField("delegate").apply { isAccessible = true }.get(engine))
        require(delegate.javaClass.name == "com.tidal.sdk.player.playbackengine.ExoPlayerPlaybackEngine")
        Access(handler, delegate, delegate.javaClass.getDeclaredMethod("getExtendedExoPlayer").apply { isAccessible = true })
    }.getOrNull()
    private val queued = AtomicBoolean(false)
    private val failed = AtomicBoolean(false)
    @Volatile private var desired = 1f

    internal fun isSupported() = access != null

    fun apply(speed: Float) {
        desired = validPlaybackSpeed(speed)
        if (failed.get()) return
        val target = access ?: return fail()
        if (!queued.compareAndSet(false, true)) return
        if (!target.handler.post {
            try {
                val player = target.getPlayer.invoke(target.delegate) as? Player
                val parameters = PlaybackParameters(desired, 1f)
                if (player != null && player.playbackParameters != parameters) {
                    player.playbackParameters = parameters
                }
            } catch (_: Exception) {
                fail()
            } finally {
                queued.set(false)
            }
        }) {
            queued.set(false)
            fail()
        }
    }

    private fun fail() { if (failed.compareAndSet(false, true)) onFailure() }
}
