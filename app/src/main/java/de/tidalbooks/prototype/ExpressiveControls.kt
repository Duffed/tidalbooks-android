@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.tidalbooks.prototype

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity

@Composable
internal fun ExpressiveProgress(value: Float, modifier: Modifier = Modifier, playing: Boolean = false) {
    val amplitude by animateFloatAsState(if (playing) .6f else 0f,
        MaterialTheme.motionScheme.fastEffectsSpec(), label = "Wellenhöhe")
    val stroke = with(LocalDensity.current) { Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round) }
    LinearWavyProgressIndicator(progress = { value }, modifier = modifier,
        amplitude = { amplitude }, waveSpeed = if (playing) 16.dp else 0.dp,
        wavelength = 24.dp, stroke = stroke, trackStroke = stroke, stopSize = 0.dp,
        trackColor = MaterialTheme.colorScheme.outlineVariant)
}

/** Keep the native slider's drag, keyboard and accessibility behavior; customize only its track. */
@Composable
internal fun ExpressiveSeekBar(value: Float, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier, playing: Boolean = false) {
    Slider(value = value, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished,
        modifier = modifier, thumb = {
            Box(Modifier.size(width = 4.dp, height = 22.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape))
        }, track = { state ->
            ExpressiveProgress(value = state.value, playing = playing,
                modifier = Modifier.fillMaxWidth().height(14.dp).clearAndSetSemantics {})
        })
}

@Composable
internal fun ExpressiveNavigation(selectedTab: Int, onSelect: (Int) -> Unit) {
    Surface(modifier = Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp) {
        ShortNavigationBar(containerColor = Color.Transparent, windowInsets = WindowInsets(0, 0, 0, 0)) {
            listOf(Icons.AutoMirrored.Rounded.LibraryBooks, Icons.Rounded.Bookmark, Icons.Rounded.Settings).forEachIndexed { index, icon ->
                val scale by animateFloatAsState(if (selectedTab == index) 1.15f else 1f,
                    MaterialTheme.motionScheme.fastSpatialSpec(), label = "Navigationssymbol")
                ShortNavigationBarItem(selected = selectedTab == index, onClick = { onSelect(index) },
                    icon = { Icon(icon, null, Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) },
                    label = { Text(listOf("Bibliothek", "Lesezeichen", "Einstellungen")[index], fontSize = 11.sp) })
            }
        }
    }
}

/** Official Expressive components, with shape morphing on press and selection. */
@Composable
internal fun ExpressiveButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled,
        shapes = ButtonDefaults.shapes(), content = content)
}

@Composable
internal fun ExpressiveTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp), content: @Composable RowScope.() -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp),
        shapes = ButtonDefaults.shapes(), contentPadding = contentPadding, content = content)
}

@Composable
internal fun ExpressiveLoadingIndicator(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary,
    @Suppress("UNUSED_PARAMETER") strokeWidth: Dp = 3.dp) {
    LoadingIndicator(modifier = modifier, color = color)
}

@Composable
internal fun ExpressivePlayButton(playing: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
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
    ToggleButton(checked = selected, onCheckedChange = { onClick() },
        modifier = Modifier.heightIn(min = 48.dp).semantics { this.selected = selected }) {
        leadingIcon?.let { it(); Spacer(Modifier.width(6.dp)) }
        label()
    }
}
