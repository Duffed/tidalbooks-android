package de.tidalbooks.prototype

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Process
import android.view.View
import android.widget.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class WidgetViewsTest {
    @Test
    fun bothDesignsApplyBackgroundOpacityWithoutFadingControls() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            for (tidal in listOf(false, true)) for (opacity in listOf(0, 40, 100)) {
                val view =
                    AudiobookWidget.views(
                            context,
                            WidgetSnapshot.from(ProbeState(album = album)),
                            tidal,
                            opacity,
                        )
                        .apply(context, FrameLayout(context))

                assertEquals(
                    (opacity * 255 + 50) / 100,
                    view.findViewById<ImageView>(R.id.widget_background_image).imageAlpha,
                )
                assertEquals(1f, view.findViewById<View>(R.id.widget_play).alpha)
                assertTrue(view.findViewById<View>(R.id.widget_play).isEnabled)
                assertTrue(view.findViewById<ImageView>(R.id.widget_cover).clipToOutline)
            }
        }
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val album = Album("b", "Ein Hörbuch", listOf(Track("a", "Anfang", 60_000, 1, 1)))

    @Test
    fun providerIsPrivateAndNativeWidgetShowsPlaybackControls() {
        val component = ComponentName(context, AudiobookWidget::class.java)

        assertFalse(context.packageManager.getReceiverInfo(component, 0).exported)
        val info =
            AppWidgetManager.getInstance(context)
                .getInstalledProvidersForPackage(context.packageName, Process.myUserHandle())
                .single { it.provider == component }

        assertEquals(0, info.updatePeriodMillis)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val views =
                AudiobookWidget.views(
                        context,
                        WidgetSnapshot.from(
                            ProbeState(album = album, requestedPlay = true, skipSeconds = 15)
                        ),
                    )
                    .apply(context, FrameLayout(context))

            assertEquals(
                "Ein Hörbuch",
                views.findViewById<TextView>(R.id.widget_title).text.toString(),
            )
            assertEquals(
                "−15 s",
                views.findViewById<TextView>(R.id.widget_back_label).text.toString(),
            )
            assertEquals(
                "15 Sekunden zurück",
                views.findViewById<View>(R.id.widget_back).contentDescription,
            )
            assertEquals(
                "Pause",
                views.findViewById<TextView>(R.id.widget_play_label).text.toString(),
            )
            listOf(R.id.widget_play, R.id.widget_back, R.id.widget_mark, R.id.widget_timer)
                .forEach {
                    assertTrue(views.findViewById<View>(it).hasOnClickListeners())
                }
        }
    }

    @Test
    fun emptyWidgetDisablesPlaybackAndCountdownUsesTheStoredDeadline() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val empty =
                AudiobookWidget.views(context, WidgetSnapshot.from(ProbeState()))
                    .apply(context, FrameLayout(context))

            assertFalse(empty.findViewById<View>(R.id.widget_play).isEnabled)
            assertFalse(empty.findViewById<View>(R.id.widget_mark).isEnabled)
            val deadline = android.os.SystemClock.elapsedRealtime() + 60_000
            val session =
                StoredSleep(
                    startBookId = "b",
                    startTrackId = "a",
                    startPositionMs = 0,
                    active = true,
                    deadlineElapsedMs = deadline,
                    endTrackId = null,
                    bootCount = 1,
                )
            val view =
                AudiobookWidget.views(
                        context,
                        WidgetSnapshot.from(ProbeState(album = album, sleepSession = session)),
                    )
                    .apply(context, FrameLayout(context))
            val clock = view.findViewById<Chronometer>(R.id.widget_countdown)

            assertTrue(clock.isCountDown)
            assertEquals(deadline, clock.base)
            assertEquals(View.VISIBLE, clock.visibility)
        }
    }
}
