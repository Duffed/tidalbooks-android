package de.tidalbooks.prototype

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/** Neutral dark surfaces inspired by the official Android app's store screenshots. */
internal fun tidalColorScheme() = darkColorScheme(
    primary = Color.White, onPrimary = Color.Black,
    primaryContainer = Color(0xFF292929), onPrimaryContainer = Color.White,
    secondary = Color.White, onSecondary = Color.Black,
    secondaryContainer = Color(0xFF202020), onSecondaryContainer = Color.White,
    tertiary = Color(0xFF78DDCD), onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF262626), onTertiaryContainer = Color.White,
    background = Color.Black, onBackground = Color.White,
    surface = Color.Black, onSurface = Color.White,
    surfaceVariant = Color(0xFF252525), onSurfaceVariant = Color(0xFFADADAD),
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF121212),
    surfaceContainer = Color(0xFF191919), surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF303030), surfaceDim = Color.Black,
    surfaceBright = Color(0xFF333333),
    outline = Color(0xFF777777), outlineVariant = Color(0xFF383838),
    inverseSurface = Color(0xFFF1F1F1), inverseOnSurface = Color.Black,
    inversePrimary = Color.Black,
)
