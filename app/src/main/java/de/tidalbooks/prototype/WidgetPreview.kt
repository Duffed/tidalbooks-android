package de.tidalbooks.prototype

import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction

/** A working preview uses the exact RemoteViews and pending intents shown by the launcher. */
@Composable
internal fun WidgetPreview(snapshot: WidgetSnapshot, modifier: Modifier = Modifier) {
    val previous = remember { arrayOfNulls<WidgetSnapshot>(1) }
    val nativeView = remember { arrayOfNulls<View>(1) }
    AndroidView(modifier = modifier.semantics {
        contentDescription = "Widget-Vorschau: ${snapshot.title}"
        customActions = listOf(
            (if (snapshot.pauseAction) "Pause" else "Fortsetzen") to R.id.widget_play,
            "30 Sekunden zurück" to R.id.widget_back,
            "Lesezeichen setzen" to R.id.widget_mark,
            (if (snapshot.timerActive) "Einschlaftimer ausschalten" else "Einschlaftimer starten") to R.id.widget_timer,
            "Nachtmodus öffnen" to R.id.widget_night,
        ).map { (label,id) -> CustomAccessibilityAction(label) { nativeView[0]?.findViewById<View>(id)?.performClick() ?: false } }
    }, factory = { context ->
        AudiobookWidget.views(context,snapshot).apply(context,null).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            nativeView[0] = this
        }
    }, update = { view ->
        if (previous[0] != snapshot) {
            AudiobookWidget.views(view.context,snapshot).reapply(view.context,view)
            previous[0] = snapshot
        }
    })
}
