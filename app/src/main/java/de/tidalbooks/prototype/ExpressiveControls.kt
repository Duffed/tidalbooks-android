@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.tidalbooks.prototype

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment

@Composable
internal fun ExpressiveProgress(value: Float, modifier: Modifier = Modifier, playing: Boolean = false) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        entrance.snapTo(0f)
        if (playing) entrance.animateTo(.6f, tween(220, easing = FastOutSlowInEasing))
    }
    // Read in composition so Material's cached path receives each updated amplitude.
    val amplitude = entrance.value
    val stroke = with(LocalDensity.current) { Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round) }
    // One shared container keeps the baseline and layout stable across state changes.
    Box(modifier, contentAlignment = Alignment.Center) {
        if (!playing || LocalTidalDesign.current) {
            // Pause/buffering/error must not wait for an exit animation.
            LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth().height(2.dp),
                trackColor = MaterialTheme.colorScheme.outlineVariant, gapSize = 0.dp, drawStopIndicator = {})
        } else {
            LinearWavyProgressIndicator(progress = { value }, modifier = Modifier.matchParentSize(),
                amplitude = { amplitude }, waveSpeed = 16.dp,
                wavelength = 24.dp, stroke = stroke, trackStroke = stroke, stopSize = 0.dp, gapSize = 0.dp,
                trackColor = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** Keep the native slider's drag, keyboard and accessibility behavior; customize only its track. */
@Composable
internal fun ExpressiveSeekBar(value: Float, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier, playing: Boolean = false) {
    val tidal = LocalTidalDesign.current
    val activeColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Slider(value = value, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished,
        modifier = modifier, thumb = {
            Box(Modifier.size(width = if (tidal) 10.dp else 4.dp, height = 22.dp), contentAlignment = Alignment.Center) {
                Box((if (tidal) Modifier.size(10.dp) else Modifier.fillMaxSize())
                    .background(activeColor, CircleShape))
            }
        }, track = { state ->
            if (tidal) Canvas(Modifier.fillMaxWidth().height(22.dp).clearAndSetSemantics {}) {
                val y = size.height / 2f
                val start = if (rtl) size.width else 0f
                val end = if (rtl) 0f else size.width
                val position = start + (end - start) * state.value.coerceIn(0f, 1f)
                drawLine(trackColor, Offset(start, y), Offset(end, y), 2.dp.toPx(), StrokeCap.Round)
                drawLine(activeColor, Offset(start, y), Offset(position, y), 2.dp.toPx(), StrokeCap.Round)
            } else ExpressiveProgress(value = state.value, playing = playing,
                modifier = Modifier.fillMaxWidth().height(14.dp).clearAndSetSemantics {})
        })
}

@Composable
internal fun ExpressiveNavigation(selectedTab: Int, onSelect: (Int) -> Unit) {
    if (LocalTidalDesign.current) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            listOf(Icons.AutoMirrored.Rounded.LibraryBooks, Icons.Rounded.Bookmark, Icons.Rounded.Settings).forEachIndexed { index, icon ->
                NavigationBarItem(selected = selectedTab == index, onClick = { onSelect(index) },
                    icon = { Icon(icon, null) },
                    label = { Text(listOf(translated("Bibliothek"), translated("Lesezeichen"), translated("Einstellungen"))[index], fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent,
                        selectedIconColor = Color.White, selectedTextColor = Color.White,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant))
            }
        }
        return
    }
    Surface(modifier = Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp) {
        ShortNavigationBar(containerColor = Color.Transparent, windowInsets = WindowInsets(0, 0, 0, 0)) {
            listOf(Icons.AutoMirrored.Rounded.LibraryBooks, Icons.Rounded.Bookmark, Icons.Rounded.Settings).forEachIndexed { index, icon ->
                val scale by animateFloatAsState(if (selectedTab == index) 1.15f else 1f,
                    MaterialTheme.motionScheme.fastSpatialSpec(), label = "Navigationssymbol")
                ShortNavigationBarItem(selected = selectedTab == index, onClick = { onSelect(index) },
                    icon = { Icon(icon, null, Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) },
                    label = { Text(listOf(translated("Bibliothek"), translated("Lesezeichen"), translated("Einstellungen"))[index], fontSize = 11.sp) })
            }
        }
    }
}

/** Official Expressive components, with shape morphing on press and selection. */
@Composable
internal fun ExpressiveButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    if (LocalTidalDesign.current) {
        Button(onClick = onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled,
            shape = RoundedCornerShape(6.dp), content = content)
        return
    }
    Button(onClick = onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled,
        shapes = ButtonDefaults.shapes(), content = content)
}

@Composable
internal fun ExpressiveTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp), content: @Composable RowScope.() -> Unit) {
    if (LocalTidalDesign.current) {
        FilledTonalButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(6.dp), contentPadding = contentPadding, content = content)
        return
    }
    FilledTonalButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp),
        shapes = ButtonDefaults.shapes(), contentPadding = contentPadding, content = content)
}

@Composable
internal fun ExpressiveLoadingIndicator(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary,
    @Suppress("UNUSED_PARAMETER") strokeWidth: Dp = 3.dp) {
    if (LocalTidalDesign.current) CircularProgressIndicator(modifier = modifier, color = color, strokeWidth = strokeWidth)
    else LoadingIndicator(modifier = modifier, color = color)
}

@Composable
internal fun ExpressivePlayButton(playing: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    if (LocalTidalDesign.current) {
        FilledIconButton(onClick = onClick, modifier = Modifier.size(80.dp), shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black),
            content = content)
        return
    }
    FilledIconToggleButton(checked = playing, onCheckedChange = { onClick() },
        modifier = Modifier.size(width = 104.dp, height = 88.dp),
        shapes = IconButtonDefaults.toggleableShapes(shape = CircleShape, pressedShape = RoundedCornerShape(16.dp), checkedShape = RoundedCornerShape(28.dp)),
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
            checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer, checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer),
        content = content)
}

@Composable
internal fun ExpressiveChoice(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null) {
    if (LocalTidalDesign.current) {
        FilterChip(selected = selected, onClick = onClick, label = label,
            modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(6.dp),
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.White,
                selectedLabelColor = Color.Black, selectedLeadingIconColor = Color.Black), leadingIcon = leadingIcon)
        return
    }
    ToggleButton(checked = selected, onCheckedChange = { onClick() },
        modifier = Modifier.heightIn(min = 48.dp).semantics { this.selected = selected }) {
        leadingIcon?.let { it(); Spacer(Modifier.width(6.dp)) }
        label()
    }
}
