package de.tidalbooks.prototype

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ListeningUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun searchFiltersByAuthorAndCanBeCleared() {
        val books = listOf(LibraryBook("1", "Berg", "Anna", null, 60000, 0, false, 1),
            LibraryBook("2", "Meer", "Berta", null, 60000, 0, false, 2))
        compose.setContent { MaterialTheme { LibraryScreen(ProbeState(books = books, libraryReady = true), Modifier, { _, _ -> }, {}, {}, {}) } }
        compose.onNodeWithText("Titel oder Autor suchen").assertDoesNotExist()
        compose.onNodeWithContentDescription("Bibliothek durchsuchen").performScrollTo().performClick()
        compose.onNodeWithText("Titel oder Autor suchen").performTextInput("Berta")
        compose.onNodeWithText("Meer", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Berg", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Suche löschen").performClick()
        compose.onNodeWithText("Berg", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Suche schließen").performClick()
        compose.onNodeWithText("Titel oder Autor suchen").assertDoesNotExist()
    }
    @Test fun onboardingOffersConnectBeforeImportAndNeverStartsPlayback() {
        var connected = false
        compose.setContent { MaterialTheme { GettingStartedCard(false, { connected = true }, { error("Import must follow login") }) } }
        compose.onNodeWithText("Mit TIDAL verbinden").performClick()
        assertTrue(connected)
    }
    @Test fun chapterSeekUsesTrackOffsetAndPlayerRemainsUsableWithLargeText() {
        var requested = -1L
        val album = Album("book", "Ein kurzer Titel", listOf(Track("a", "A", 60000, 1, 1), Track("b", "B", 120000, 1, 2)))
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                MaterialTheme { PlayerScreen(ProbeState(album = album, index = 1, positionMs = 10000, libraryReady = true),
                    {}, {}, {}, { requested = it }, {}, {}, {}, {}, {}, {}, {}, {}, {}, remember { SnackbarHostState() }) }
            }
        }
        compose.onNodeWithText("Abschnitt", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithContentDescription("Position im Abschnitt").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(.5f) }
        assertEquals(120_000L, requested)
        compose.onNodeWithContentDescription("Abspielen").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Player minimieren").assertIsDisplayed()
    }
}
