package de.tidalbooks.prototype

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.Assert.*

class ThemePreferencesTest {
    @get:Rule val compose = createComposeRule()
    private val preferences =
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("theme_preferences_test_only", Context.MODE_PRIVATE)

    @Before
    fun before() {
        preferences.edit().clear().commit()
    }

    @After
    fun after() {
        preferences.edit().clear().commit()
    }

    @Test
    fun selectionUpdatesImmediatelyAndSurvivesStoreRecreation() {
        preferences
            .edit()
            .putString("style", "STUDIO")
            .putString("palette", "PLUM")
            .putString("mode", "DARK")
            .commit()
        val store = ThemeStore(preferences)

        assertEquals(ThemeSettings(AppPalette.PLUM, ThemeMode.DARK), store.settings)
        compose.setContent {
            AudiobookTheme(store) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ThemeSettingsCard()
                }
            }
        }
        compose.onNodeWithText("Studio").assertDoesNotExist()
        compose.onNodeWithText("Ozean").performScrollTo().performClick()
        compose.onNodeWithText("Hell").performScrollTo().performClick()
        compose.onNodeWithText("Hell").assertIsSelected()
        compose.runOnIdle {
            assertEquals(ThemeSettings(AppPalette.OCEAN, ThemeMode.LIGHT), store.settings)
            assertEquals(store.settings, ThemeStore(preferences).settings)
            assertFalse(preferences.contains("style"))
        }
        compose.onNodeWithText("Material You").performScrollTo().performClick()
        compose.onNodeWithText("Dunkel").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(AppPalette.SYSTEM, store.settings.palette)
            assertEquals(ThemeMode.DARK, ThemeStore(preferences).settings.mode)
        }
    }

    @Test
    fun expressivePlaybackControlOnlyTogglesOnUserAction() {
        var calls = 0

        compose.setContent {
            AudiobookTheme(ThemeStore(preferences)) {
                var playing by remember { mutableStateOf(false) }

                ExpressivePlayButton(
                    playing,
                    {
                        calls++
                        playing = !playing
                    },
                ) {
                    Text(if (playing) "Pause" else "Abspielen")
                }
            }
        }
        compose.runOnIdle { assertEquals(0, calls) }
        compose.onNodeWithText("Abspielen").performClick()
        compose.onNodeWithText("Pause").assertIsOn()
        compose.onNodeWithText("Pause").performClick()
        compose.onNodeWithText("Abspielen").assertIsOff()
        compose.runOnIdle { assertEquals(2, calls) }
    }

    @Test
    fun expressiveNavigationKeepsLabelsAndSelection() {
        compose.setContent {
            AudiobookTheme(ThemeStore(preferences)) {
                var tab by remember { mutableIntStateOf(0) }

                ExpressiveNavigation(tab) { tab = it }
            }
        }
        compose.onNodeWithText("Bibliothek").assertIsSelected()
        compose.onNodeWithText("Lesezeichen").performClick().assertIsSelected()
        compose.onNodeWithText("Bibliothek").assertIsNotSelected()
        compose.onNodeWithText("Einstellungen").performClick().assertIsSelected()
    }
}
