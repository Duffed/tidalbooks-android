package de.tidalbooks.prototype

import android.os.Handler
import com.tidal.sdk.player.playbackengine.PlaybackEngine

/**
 * Adapter for pinned TIDAL 0.0.73: its implementation exposes playerVolume, but its
 * SingleHandlerPlaybackEngine interface does not. Resolve only these known members
 * and execute all access on the SDK handler. Never change Android's media volume.
 * Recheck this adapter on every SDK upgrade; failure leaves the timer stop intact.
 */
internal class TidalSleepVolume(private val engine: PlaybackEngine, private val onFailure: () -> Unit) {
    private data class Access(val handler: Handler, val delegate: Any,
        val get: java.lang.reflect.Method, val set: java.lang.reflect.Method)
    private val access: Access? = runCatching {
        val wrapper = engine.javaClass
        require(wrapper.name == "com.tidal.sdk.player.playbackengine.SingleHandlerPlaybackEngine")
        val handler = wrapper.getDeclaredField("handler").apply { isAccessible = true }.get(engine) as Handler
        val delegate = requireNotNull(wrapper.getDeclaredField("delegate").apply { isAccessible = true }.get(engine))
        require(delegate.javaClass.name == "com.tidal.sdk.player.playbackengine.ExoPlayerPlaybackEngine")
        Access(handler, delegate, delegate.javaClass.getMethod("getPlayerVolume"),
            delegate.javaClass.getMethod("setPlayerVolume", Float::class.javaPrimitiveType))
    }.getOrNull()
    private var warned = false
    // These two values are confined to the SDK's handler.
    private var base: Float? = null
    private var track: String? = null
    private fun failed() { if (!warned) { warned = true; onFailure() } }
    fun apply(gain: Float) {
        val target = access ?: return failed()
        if (!target.handler.post {
            runCatching {
                val current = (target.delegate as PlaybackEngine).mediaProduct?.productId
                if (base == null || track != current) {
                    base = (target.get.invoke(target.delegate) as Float).coerceIn(0f, 1f)
                    track = current
                }
                target.set.invoke(target.delegate, requireNotNull(base) * gain.coerceIn(0f, 1f))
            }.onFailure { failed() }
        }) failed()
    }
    fun restore() {
        access?.let { target ->
            target.handler.post {
                runCatching {
                    // Recompute the SDK's normal gain for the currently loaded track.
                    if (base != null) {
                        val delegate = target.delegate as PlaybackEngine
                        delegate.loudnessNormalizationPreAmp = delegate.loudnessNormalizationPreAmp
                    }
                    base = null; track = null
                }.onFailure { failed() }
            }
        }
    }
    internal fun isSupported() = access != null
}
