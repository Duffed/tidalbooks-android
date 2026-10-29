@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package de.tidalbooks.prototype

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.content.edit

enum class ThemeMode(private val germanLabel: String) { SYSTEM("System"), LIGHT("Hell"), DARK("Dunkel"); val label get() = translated(germanLabel) }
enum class AppDesign(private val germanLabel: String) { EXPRESSIVE("Material Expressive"), TIDAL("TIDAL"); val label get() = translated(germanLabel) }
enum class AppPalette(private val germanLabel: String, val light: Long, val dark: Long) {
    SYSTEM("Material You", 0xFF28665C, 0xFFA1D1C7),
    OCEAN("Ozean", 0xFF285EA8, 0xFFA9C7FF),
    FOREST("Wald", 0xFF28665C, 0xFFA1D1C7),
    PLUM("Pflaume", 0xFF74529A, 0xFFD6BAFF),
    CLAY("Terrakotta", 0xFF934B35, 0xFFFFB59C),
    GRAPHITE("Graphit", 0xFF515B66, 0xFFC0CAD7);
    val label get() = translated(germanLabel)
}

data class ThemeSettings(
    val palette: AppPalette = AppPalette.SYSTEM,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val design: AppDesign = AppDesign.EXPRESSIVE,
) {
    fun isDark(systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    companion object {
        fun decode(palette: String?, mode: String?, design: String? = null) = ThemeSettings(
            AppPalette.entries.firstOrNull { it.name == palette } ?: AppPalette.SYSTEM,
            ThemeMode.entries.firstOrNull { it.name == mode } ?: ThemeMode.SYSTEM,
            AppDesign.entries.firstOrNull { it.name == design } ?: AppDesign.EXPRESSIVE,
        )
    }
}

/** Ausschließlich UI-Vorlieben; kein Zugriff auf Bibliothek oder Anmeldedaten. */
@Stable
class ThemeStore(private val preferences: SharedPreferences) {
    var settings by mutableStateOf(ThemeSettings.decode(
        preferences.getString("palette", null), preferences.getString("mode", null), preferences.getString("design", null)))
        private set

    fun update(value: ThemeSettings) {
        settings = value
        preferences.edit {
            remove("style")
            putString("palette", value.palette.name)
            putString("mode", value.mode.name)
            putString("design", value.design.name)
        }
    }
}

internal val LocalThemeStore = staticCompositionLocalOf<ThemeStore> { error("AudiobookTheme fehlt") }
internal val LocalTidalDesign = staticCompositionLocalOf { false }

@Composable
internal fun appCorner(radius: Dp) = RoundedCornerShape(if (LocalTidalDesign.current) 6.dp else radius)

/** Vollständige Farbrollen für jede Palette. */
internal fun curatedColorScheme(palette: AppPalette, dark: Boolean): ColorScheme {
    val accent = Color(if (dark) palette.dark else palette.light)
    val ink = if (dark) Color(0xFFF1F0F4) else Color(0xFF1A1B20)
    val base = if (dark) Color(0xFF111216) else Color(0xFFFCFCFF)
    val tint = accent
    fun surface(amount: Float) = lerp(base, tint, amount)
    val container = if (dark) lerp(base, accent, .24f) else lerp(Color.White, accent, .15f)
    val secondary = if (dark) lerp(accent, Color.White, .15f) else lerp(accent, Color(0xFF30313A), .25f)
    val defaults = if (dark) darkColorScheme() else lightColorScheme()
    return defaults.copy(
        primary = accent, onPrimary = if (dark) Color(0xFF15212A) else Color.White,
        primaryContainer = container, onPrimaryContainer = ink, inversePrimary = Color(if (dark) palette.light else palette.dark),
        secondary = secondary, onSecondary = if (dark) Color(0xFF15212A) else Color.White,
        secondaryContainer = surface(if (dark) .22f else .12f), onSecondaryContainer = ink,
        tertiary = secondary, onTertiary = if (dark) Color(0xFF15212A) else Color.White,
        tertiaryContainer = container, onTertiaryContainer = ink,
        background = base, onBackground = ink, surface = base, onSurface = ink,
        surfaceVariant = surface(if (dark) .26f else .13f),
        onSurfaceVariant = if (dark) Color(0xFFCCC8D0) else Color(0xFF49474F),
        surfaceTint = accent, inverseSurface = if (dark) Color(0xFFE7E3EA) else Color(0xFF303036),
        inverseOnSurface = if (dark) Color(0xFF29272F) else Color(0xFFF5F0F8),
        outline = if (dark) Color(0xFF96939C) else Color(0xFF75727B),
        outlineVariant = if (dark) Color(0xFF49464F) else Color(0xFFC9C5CE),
        surfaceDim = surface(if (dark) 0f else .16f), surfaceBright = surface(if (dark) .22f else 0f),
        surfaceContainerLowest = if (dark) Color(0xFF0B0C10) else Color.White,
        surfaceContainerLow = surface(.04f), surfaceContainer = surface(.08f),
        surfaceContainerHigh = surface(.13f), surfaceContainerHighest = surface(.18f),
    )
}

@Composable
fun AudiobookTheme(store: ThemeStore? = null, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val themeStore = store ?: remember(context.applicationContext) {
        ThemeStore(context.applicationContext.getSharedPreferences("appearance", Context.MODE_PRIVATE))
    }
    val settings = themeStore.settings
    val tidal = settings.design == AppDesign.TIDAL
    val dark = tidal || settings.isDark(isSystemInDarkTheme())
    val colors = if (tidal) tidalColorScheme() else if (settings.palette == AppPalette.SYSTEM && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else curatedColorScheme(settings.palette, dark)
    val typography = Typography().let { type ->
        type.copy(
            headlineSmall = type.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = type.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
    val view = LocalView.current
    SideEffect {
        if (Build.VERSION.SDK_INT >= 29) view.isForceDarkAllowed = false
        (context as? Activity)?.let { activity ->
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalThemeStore provides themeStore, LocalTidalDesign provides tidal) {
        MaterialExpressiveTheme(colorScheme = colors, typography = typography, motionScheme = if (tidal) MotionScheme.standard() else MotionScheme.expressive(), shapes = Shapes(
            small = appCorner(12.dp), medium = appCorner(20.dp),
            large = appCorner(28.dp), extraLarge = appCorner(32.dp)),
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.onSurface, content = content)
        }
    }
}
