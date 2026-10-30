package de.tidalbooks.prototype

import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class AppLanguageTest {
    @After
    fun restoreGerman() {
        AppLanguage.configure(LanguageChoice.SYSTEM, "de")
    }

    @Test
    fun systemUsesEnglishAndFallsBackToGermanForUnsupportedLanguages() {
        AppLanguage.configure(LanguageChoice.SYSTEM, "en")
        assertEquals("Library", translated("Bibliothek"))
        AppLanguage.configure(LanguageChoice.SYSTEM, "fr")
        assertEquals("Bibliothek", translated("Bibliothek"))
    }

    @Test
    fun explicitChoiceOverridesSystemAndEnumLabelsUpdateWithoutRestart() {
        AppLanguage.configure(LanguageChoice.GERMAN, "en")
        assertEquals("Hell", ThemeMode.LIGHT.label)
        AppLanguage.configure(LanguageChoice.ENGLISH, "de")
        assertEquals("Light", ThemeMode.LIGHT.label)
        assertEquals("Ocean", AppPalette.OCEAN.label)
        assertEquals("Recently played", BookSort.LAST_PLAYED.label)
        assertEquals("1 hr 30 min", durationLabel(5_400_000))
    }

    @Test
    fun statusIdentifiersAndUserMetadataStayUnchangedInEnglish() {
        AppLanguage.configure(LanguageChoice.ENGLISH)
        val book = LibraryBook("1", "Angefangen", "Autor", null, 60000, 1000, false, 1)

        assertEquals("Angefangen", book.status)
        assertEquals(listOf(book), filterBooks(listOf(book), "Autor", "Angefangen", BookSort.TITLE))
        assertEquals("Angefangen", book.title)
        assertEquals("In progress", translated(book.status))
        assertEquals("Before seeking", translated("Vor dem Spulen"))
    }

    @Test
    fun errorsAndEmptyWidgetsUseEnglish() {
        AppLanguage.configure(LanguageChoice.ENGLISH)
        assertEquals(
            "Your TIDAL sign-in has expired. Please reconnect in Settings.",
            friendlyFailure(CatalogFailure(401)),
        )
        assertEquals("Choose an audiobook", WidgetSnapshot.from(ProbeState()).title)
        assertEquals("Invalid listening position.", displayAppMessage("Ungültiger Hörstand."))
        AppLanguage.configure(LanguageChoice.GERMAN)
        assertEquals("Ungültiger Hörstand.", displayAppMessage("Invalid listening position."))
    }
}
