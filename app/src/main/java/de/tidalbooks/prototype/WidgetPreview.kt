package de.tidalbooks.prototype

import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun WidgetPreview(
    snapshot: WidgetSnapshot,
    modifier: Modifier = Modifier,
    tidal: Boolean? = null,
    opacity: Int? = null,
) {
    val english = AppLanguage.english
    val previous = remember(english) { arrayOfNulls<Triple<WidgetSnapshot, Boolean, Int>>(1) }
    val nativeView = remember { arrayOfNulls<View>(1) }

    AndroidView(
        modifier =
            modifier.semantics {
                contentDescription =
                    localized(
                        "Widget-Vorschau: ${snapshot.title}",
                        "Widget preview: ${snapshot.title}",
                    )
                customActions =
                    listOf(
                            (if (snapshot.pauseAction) translated("Pause")
                            else translated("Fortsetzen")) to R.id.widget_play,
                            localized(
                                "${snapshot.skipSeconds} Sekunden zurück",
                                "Back ${snapshot.skipSeconds} seconds",
                            ) to R.id.widget_back,
                            translated("Lesezeichen setzen") to R.id.widget_mark,
                            (if (snapshot.timerActive) translated("Einschlaftimer ausschalten")
                            else translated("Einschlaftimer starten")) to R.id.widget_timer,
                        )
                        .map { (label, id) ->
                            CustomAccessibilityAction(label) {
                                nativeView[0]?.findViewById<View>(id)?.performClick() ?: false
                            }
                        }
            },
        factory = { context ->
            AudiobookWidget.views(
                    context,
                    snapshot,
                    tidal ?: AudiobookWidget.usesTidalDesign(context),
                    opacity ?: AudiobookWidget.backgroundOpacity(context),
                )
                .apply(context, null)
                .apply {
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    nativeView[0] = this
                }
        },
        update = { view ->
            val appearance =
                Triple(
                    snapshot,
                    tidal ?: AudiobookWidget.usesTidalDesign(view.context),
                    opacity ?: AudiobookWidget.backgroundOpacity(view.context),
                )

            if (previous[0] != appearance) {
                AudiobookWidget.views(view.context, snapshot, appearance.second, appearance.third)
                    .reapply(view.context, view)
                previous[0] = appearance
            }
        },
    )
}
