package de.tidalbooks.prototype

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.*
import org.junit.Test

class ThemeSettingsTest {
    @Test
    fun unknownPreferencesFallBackWithoutChangingDefaults() {
        assertEquals(ThemeSettings(), ThemeSettings.decode(null, null))
        assertEquals(ThemeSettings(), ThemeSettings.decode("old-color", "obsolete"))
    }

    @Test
    fun explicitBrightnessOverridesSystem() {
        assertFalse(ThemeSettings(mode = ThemeMode.LIGHT).isDark(true))
        assertTrue(ThemeSettings(mode = ThemeMode.DARK).isDark(false))
        assertTrue(ThemeSettings().isDark(true))
        assertFalse(ThemeSettings().isDark(false))
    }

    @Test
    fun curatedTextAndControlColorsHaveReadableContrast() {
        for (palette in AppPalette.entries) for (dark in listOf(false, true)) {
            val scheme = curatedColorScheme(palette, dark)
            val pairs =
                listOf(
                    scheme.onPrimary to scheme.primary,
                    scheme.onPrimaryContainer to scheme.primaryContainer,
                    scheme.onSecondary to scheme.secondary,
                    scheme.onSecondaryContainer to scheme.secondaryContainer,
                    scheme.onSurface to scheme.surface,
                    scheme.onSurface to scheme.surfaceContainerHigh,
                    scheme.onSurfaceVariant to scheme.surfaceContainerHigh,
                )

            pairs.forEach { (text, background) ->
                assertTrue(
                    "$palette dark=$dark: contrast ${contrast(text, background)}",
                    contrast(text, background) >= 4.5f,
                )
            }
        }
    }

    private fun contrast(a: Color, b: Color): Float =
        (max(a.luminance(), b.luminance()) + .05f) / (min(a.luminance(), b.luminance()) + .05f)
}
