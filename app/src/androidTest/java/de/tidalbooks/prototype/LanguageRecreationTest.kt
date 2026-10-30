package de.tidalbooks.prototype

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Runs only in the isolated firststart build, using the real Activity and platform locale change. */
class LanguageRecreationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun languageSwitchKeepsExpandedSettingsAcrossActivityRecreation() {
        assumeTrue("Run with -PfreshInstallCheck=true", BuildConfig.APPLICATION_ID.endsWith(".firststart"))
        val original = AppLanguage.choice
        try {
            compose.onNodeWithText(translated("Einstellungen")).performClick()
            compose.onNodeWithText(translated("Darstellung")).performScrollTo().performClick()
            for (language in listOf(LanguageChoice.ENGLISH, LanguageChoice.GERMAN, LanguageChoice.ENGLISH)) {
                compose.onNodeWithText(language.label).performScrollTo().performClick()
                compose.waitForIdle()
                compose.waitUntil(10_000) { AppLanguage.choice == language }
                compose.onNodeWithText(localized("Sprache", "Language")).assertExists()
                compose.onNodeWithText(language.label).assertIsSelected()
                compose.onAllNodesWithText(translated("Einstellungen")).assertCountEquals(2)
            }
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText("Language").assertExists()
            compose.onNodeWithText("English").assertIsSelected()
            assertEquals(LanguageChoice.ENGLISH, AppLanguage.choice)
        } finally {
            compose.activityRule.scenario.onActivity { AppLanguage.select(it, original) }
            compose.waitForIdle()
        }
    }
}
