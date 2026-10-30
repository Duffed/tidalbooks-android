package de.tidalbooks.prototype

import android.content.Context
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EnglishUiTest {
    @get:Rule val compose = createComposeRule()

    @After
    fun restoreLanguage() {
        compose.runOnIdle { AppLanguage.initialize(ApplicationProvider.getApplicationContext()) }
    }

    @Test
    fun languageSwitchRecomposesFiltersWithoutChangingTheirMeaningOrTitles() {
        val books =
            listOf(
                LibraryBook("1", "Angefangen", "Anna", null, 60000, 10000, false, 1),
                LibraryBook("2", "Finished", "Berta", null, 60000, 60000, true, 2),
            )

        compose.runOnIdle { AppLanguage.configure(LanguageChoice.GERMAN) }
        compose.setContent {
            MaterialTheme {
                LibraryScreen(
                    ProbeState(books = books, libraryReady = true),
                    Modifier,
                    { _, _ -> },
                    {},
                    {},
                    {},
                )
            }
        }
        compose.onNode(hasText("Beendet") and isSelectable()).performClick()
        compose.runOnIdle { AppLanguage.configure(LanguageChoice.ENGLISH) }
        compose.onNode(hasText("Finished") and isSelectable()).assertIsSelected()
        compose.onNode(hasText("All") and isSelectable()).performClick()
        // The unchanged title appears in both Continue listening and the library row.
        compose.onAllNodesWithText("Angefangen", useUnmergedTree = true).assertCountEquals(2)
        compose.onNodeWithContentDescription("Search library").assertExists()
        compose.runOnIdle { AppLanguage.configure(LanguageChoice.GERMAN) }
        compose.onNode(hasText("Alle") and isSelectable()).assertIsSelected()
    }

    @Test
    fun englishOnboardingAndNativeWidgetsUseTranslatedControls() {
        compose.runOnIdle { AppLanguage.configure(LanguageChoice.ENGLISH) }
        compose.setContent { MaterialTheme { GettingStartedCard(false, {}, {}) } }
        compose.onNodeWithText("Your first audiobook").assertExists()
        compose.onNodeWithText("Connect to TIDAL").assertExists()
        compose.runOnIdle {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val snapshot = WidgetSnapshot.from(ProbeState())

            for (tidal in listOf(false, true)) {
                val view =
                    AudiobookWidget.views(context, snapshot, tidal)
                        .apply(context, FrameLayout(context))

                assertEquals(
                    "Choose an audiobook",
                    view.findViewById<TextView>(R.id.widget_title).text.toString(),
                )
                assertEquals(
                    "Bookmark",
                    view.findViewById<TextView>(R.id.widget_mark_label).text.toString(),
                )
                assertEquals(
                    "Play",
                    view.findViewById<TextView>(R.id.widget_play_label).text.toString(),
                )
            }
        }
    }
}
