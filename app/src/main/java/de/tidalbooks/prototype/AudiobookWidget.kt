package de.tidalbooks.prototype

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AudiobookWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // System/periodic updates only redraw; they never start audio or a foreground service.
        val pending = goAsync()
        val app = context.applicationContext as ProbeApp

        app.scope.launch {
            try {
                val state = withTimeout(8_000) { app.probe.state.first { it.libraryReady } }

                state.album?.coverUrl?.let { ArtworkStore.get(context).loadCached(it) }
                update(context, WidgetSnapshot.from(state))
            } catch (_: Exception) {
                runCatching { update(context, WidgetSnapshot.from(app.probe.state.value)) }
            } finally {
                pending?.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val prefix = context.packageName + ".widget."
        val command = intent.action?.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)

        if (command !in setOf("PLAY", "PAUSE", "BACK", "MARK", "TIMER")) return

        val pending = goAsync()
        val app = context.applicationContext as ProbeApp

        app.scope.launch {
            try {
                withTimeout(8_000) {
                    val p = app.probe

                    p.state.first { it.libraryReady }
                    val s = p.state.value

                    s.album?.coverUrl?.let { ArtworkStore.get(context).loadCached(it) }
                    if (s.album == null || s.busy) {
                        toast(
                            context,
                            if (s.album == null) translated("Wähle zuerst ein Hörbuch in der App.")
                            else translated("Bitte kurz warten und erneut tippen."),
                        )

                        return@withTimeout
                    }

                    when (command) {
                        "PLAY" -> {
                            p.refreshLogin().join()
                            if (p.state.value.loggedIn && !s.completed) p.play()
                            else
                                toast(
                                    context,
                                    if (s.completed)
                                        translated(
                                            "Zum erneuten Hören in der App „Von vorne“ wählen."
                                        )
                                    else translated("Bitte in der App mit TIDAL verbinden."),
                                )
                        }
                        "PAUSE" -> p.pause() // Explicit pause can never resume a restored process.
                        "BACK" -> p.skip(-s.skipSeconds * 1000L)
                        "MARK" -> {
                            p.addBookmark()
                            p.library.flush()
                            toast(context, translated("Lesezeichen gesetzt."))
                        }
                        "TIMER" -> {
                            if (s.sleepSession?.active == true) {
                                p.cancelTimer()
                                toast(context, translated("Einschlaftimer ausgeschaltet."))
                            } else {
                                p.startTimer(s.lastTimerMinutes)
                                toast(
                                    context,
                                    localized(
                                        "Einschlaftimer: ${s.lastTimerMinutes} Min.",
                                        "Sleep timer: ${s.lastTimerMinutes} min",
                                    ),
                                )
                            }
                        }
                    }
                    p.library.flush()
                }
            } catch (_: TimeoutCancellationException) {
                toast(context, translated("Bitte die Hörbuch-App öffnen und erneut versuchen."))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                toast(
                    context,
                    translated("Die Aktion konnte nicht ausgeführt werden. Bitte die App öffnen."),
                )
            } finally {
                runCatching { update(context, WidgetSnapshot.from(app.probe.state.value)) }
                pending?.finish()
            }
        }
    }

    companion object {
        private fun toast(context: Context, text: String) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }

        fun hasInstances(context: Context): Boolean =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, AudiobookWidget::class.java))
                .isNotEmpty()

        internal fun usesTidalDesign(context: Context): Boolean =
            context
                .getSharedPreferences("widget-appearance", Context.MODE_PRIVATE)
                .getBoolean("tidal", false)

        internal fun backgroundOpacity(context: Context): Int =
            context
                .getSharedPreferences("widget-appearance", Context.MODE_PRIVATE)
                .getInt("opacity", 100)
                .coerceIn(0, 100)

        internal fun views(
            context: Context,
            s: WidgetSnapshot,
            tidal: Boolean = usesTidalDesign(context),
            opacity: Int = backgroundOpacity(context),
        ): RemoteViews {
            return RemoteViews(
                    context.packageName,
                    if (tidal) R.layout.audiobook_widget_tidal else R.layout.audiobook_widget,
                )
                .apply {
                    setInt(
                        R.id.widget_background_image,
                        "setImageAlpha",
                        (opacity.coerceIn(0, 100) * 255 + 50) / 100,
                    )
                    setTextViewText(
                        R.id.widget_title,
                        if (s.hasBook) s.title else translated("Hörbuch auswählen"),
                    )
                    setTextViewText(R.id.widget_mark_label, translated("Merken"))
                    setContentDescription(R.id.widget_mark, translated("Lesezeichen setzen"))
                    setTextViewText(
                        R.id.widget_subtitle,
                        if (s.hasBook) {
                            if (s.completed) translated("Beendet · In der App von vorne hören")
                            else
                                localized(
                                    "${durationLabel((s.durationMs-s.elapsedMs).coerceAtLeast(0))} verbleibend",
                                    "${durationLabel((s.durationMs-s.elapsedMs).coerceAtLeast(0))} left",
                                )
                        } else translated("Tippen und Bibliothek öffnen"),
                    )
                    setProgressBar(
                        R.id.widget_progress,
                        1000,
                        if (s.durationMs > 0)
                            (s.elapsedMs * 1000 / s.durationMs).toInt().coerceIn(0, 1000)
                        else 0,
                        false,
                    )
                    val cover = s.coverUrl?.let { ArtworkStore.get(context).peek(it) }

                    if (cover != null)
                        setImageViewBitmap(
                            R.id.widget_cover,
                            Bitmap.createScaledBitmap(cover, 192, 192, true),
                        )
                    else
                        setImageViewResource(
                            R.id.widget_cover,
                            if (tidal) R.drawable.ic_widget_headphones_tidal
                            else R.drawable.ic_widget_headphones,
                        )

                    setImageViewResource(
                        R.id.widget_play_icon,
                        if (tidal) {
                            if (s.pauseAction) R.drawable.ic_widget_pause_tidal
                            else R.drawable.ic_widget_play_tidal
                        } else if (s.pauseAction) R.drawable.ic_widget_pause
                        else R.drawable.ic_widget_play,
                    )
                    setTextViewText(
                        R.id.widget_play_label,
                        if (s.pauseAction) translated("Pause") else translated("Start"),
                    )
                    setContentDescription(
                        R.id.widget_play,
                        if (s.pauseAction) translated("Pause") else translated("Fortsetzen"),
                    )
                    setContentDescription(
                        R.id.widget_timer,
                        if (s.timerActive) translated("Einschlaftimer ausschalten")
                        else
                            localized(
                                "Einschlaftimer starten: ${s.lastTimerMinutes} Minuten",
                                "Start sleep timer: ${s.lastTimerMinutes} minutes",
                            ),
                    )
                    setViewVisibility(
                        R.id.widget_countdown,
                        if (s.timerActive && s.deadlineMs != null) View.VISIBLE else View.GONE,
                    )
                    setViewVisibility(
                        R.id.widget_timer_label,
                        if (s.timerActive && s.deadlineMs != null) View.GONE else View.VISIBLE,
                    )
                    setTextViewText(
                        R.id.widget_timer_label,
                        if (s.timerActive) translated("Trackende")
                        else localized("${s.lastTimerMinutes} Min.", "${s.lastTimerMinutes} min"),
                    )
                    setChronometerCountDown(R.id.widget_countdown, true)
                    setChronometer(
                        R.id.widget_countdown,
                        s.deadlineMs ?: 0,
                        null,
                        s.timerActive && s.deadlineMs != null,
                    )

                    fun command(name: String): PendingIntent =
                        PendingIntent.getBroadcast(
                            context,
                            name.hashCode(),
                            Intent(context, AudiobookWidget::class.java)
                                .setAction(context.packageName + ".widget." + name)
                                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    val open =
                        PendingIntent.getActivity(
                            context,
                            701,
                            Intent(context, MainActivity::class.java).putExtra("openPlayer", true),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )

                    setOnClickPendingIntent(R.id.widget_header, open)
                    setTextViewText(R.id.widget_back_label, "−${s.skipSeconds} s")
                    setContentDescription(
                        R.id.widget_back,
                        localized(
                            "${s.skipSeconds} Sekunden zurück",
                            "Back ${s.skipSeconds} seconds",
                        ),
                    )
                    setOnClickPendingIntent(
                        R.id.widget_play,
                        command(if (s.pauseAction) "PAUSE" else "PLAY"),
                    )
                    setOnClickPendingIntent(R.id.widget_back, command("BACK"))
                    setOnClickPendingIntent(R.id.widget_mark, command("MARK"))
                    setOnClickPendingIntent(R.id.widget_timer, command("TIMER"))
                    for (id in
                        listOf(
                            R.id.widget_play,
                            R.id.widget_back,
                            R.id.widget_mark,
                            R.id.widget_timer,
                        )) {
                        setBoolean(
                            id,
                            "setEnabled",
                            s.hasBook && (id != R.id.widget_play || !s.completed),
                        )
                        setFloat(
                            id,
                            "setAlpha",
                            if (s.hasBook && (id != R.id.widget_play || !s.completed)) 1f else .4f,
                        )
                    }
                }
        }

        internal fun update(context: Context, s: WidgetSnapshot) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AudiobookWidget::class.java))

            if (ids.isNotEmpty()) manager.updateAppWidget(ids, views(context, s))
        }
    }
}
