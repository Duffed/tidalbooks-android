package de.tidalbooks.prototype

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WidgetPreviewTest {
    @get:Rule val compose = createComposeRule()
    @Test fun previewExposesAllFourControlsToAccessibilityServices() {
        val s = ProbeState(album = Album("book","Hörbuch",listOf(Track("a","A",60000,1,1))))
        compose.setContent { WidgetPreview(WidgetSnapshot.from(s),Modifier.width(300.dp).height(160.dp)) }
        val node = compose.onNodeWithContentDescription("Widget-Vorschau: Hörbuch").fetchSemanticsNode()
        val labels = node.config[SemanticsActions.CustomActions].map { it.label }
        assertEquals(listOf("Fortsetzen","30 Sekunden zurück","Lesezeichen setzen","Einschlaftimer starten"),labels)
    }
}
