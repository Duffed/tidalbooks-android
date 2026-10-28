package de.tidalbooks.prototype

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.Assert.*

class LibraryViewTest {
    @get:Rule val compose = createComposeRule()
    private val prefs = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("library-view", Context.MODE_PRIVATE)
    private var hadChoice = false
    private var oldChoice = false
    private val books = listOf(
        LibraryBook("1", "Berg", "Anna", null, 60000, 0, false, 1),
        LibraryBook("2", "Meer", "Berta", null, 120000, 0, false, 2))

    @Before fun prepare() {
        hadChoice = prefs.contains("listView"); oldChoice = prefs.getBoolean("listView", false)
        prefs.edit().remove("listView").commit()
    }
    @After fun restoreUserChoice() {
        prefs.edit().apply { if (hadChoice) putBoolean("listView", oldChoice) else remove("listView") }.commit()
    }

    @Test fun switchBothWaysAndOpenFromListWithoutAutoplay() {
        var opened: String? = null
        var autoPlay: Boolean? = null
        compose.setContent { MaterialTheme {
            LibraryScreen(ProbeState(books = books, libraryReady = true), Modifier,
                { book, play -> opened = book.id; autoPlay = play }, {}, {}, {})
        } }
        compose.onNodeWithTag("library-grid-1").assertExists()
        compose.onNodeWithContentDescription("Listenansicht anzeigen").performScrollTo().performClick()
        compose.onNodeWithTag("library-list-1").assertExists()
        compose.onNodeWithText("Anna").assertExists()
        compose.onNodeWithTag("library-list-1").performScrollTo().performClick()
        assertEquals("1", opened)
        assertEquals(false, autoPlay)
        assertTrue(prefs.getBoolean("listView", false))
        compose.onNodeWithContentDescription("Kachelansicht anzeigen").performScrollTo().performClick()
        compose.onNodeWithTag("library-grid-1").assertExists()
        compose.onNodeWithTag("library-list-1").assertDoesNotExist()
        assertFalse(prefs.getBoolean("listView", true))
    }

    @Test fun savedListChoiceSurvivesRecreationAndSearchStillFiltersByAuthor() {
        val visible = mutableStateOf(true)
        compose.setContent { MaterialTheme {
            if (visible.value) LibraryScreen(ProbeState(books = books, libraryReady = true), Modifier, { _, _ -> }, {}, {}, {})
            else Text("Neu laden")
        } }
        compose.onNodeWithContentDescription("Listenansicht anzeigen").performScrollTo().performClick()
        compose.runOnIdle { visible.value = false }
        compose.onNodeWithText("Neu laden").assertExists()
        compose.runOnIdle { visible.value = true }
        compose.onNodeWithTag("library-list-1").assertExists()
        compose.onNodeWithContentDescription("Bibliothek durchsuchen").performScrollTo().performClick()
        compose.onNodeWithText("Titel oder Autor suchen").performTextInput("Berta")
        compose.onNodeWithTag("library-list-2").assertExists()
        compose.onNodeWithTag("library-list-1").assertDoesNotExist()
        compose.onNodeWithContentDescription("Kachelansicht anzeigen").performScrollTo().performClick()
        compose.onNodeWithTag("library-grid-2").assertExists()
        compose.onNodeWithTag("library-grid-1").assertDoesNotExist()
    }
}
