@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package de.tidalbooks.prototype

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Dieselbe kleine Druckbewegung für Karten und Bedienelemente. */
@Composable
internal fun Modifier.pressScale(interactions: MutableInteractionSource): Modifier {

    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .95f else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(), label = "Druckbewegung")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
internal fun SoftProgress(value: Float, modifier: Modifier = Modifier, playing: Boolean = false) {
    val progress by animateFloatAsState(value.coerceIn(0f, 1f), tween(450), label = "Hörfortschritt")
    ExpressiveProgress(value = progress, modifier = modifier, playing = playing)
}

@Composable
internal fun PlayerCover(url: String?, playing: Boolean) {
    val scale by animateFloatAsState(if (playing || LocalTidalDesign.current) 1f else .96f,
        MaterialTheme.motionScheme.defaultSpatialSpec(), label = "Coverbewegung")
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    val coverLimit = (configuration.screenHeightDp * (if (fontScale > 1.2f) .24f else .40f)).coerceIn(140f, 380f).dp
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        BookArtwork(url, Modifier.widthIn(max = coverLimit).fillMaxWidth().aspectRatio(1f).graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (LocalTidalDesign.current) 0.dp else 18.dp, appCorner(28.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = .2f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = .16f)), large = true)
    }
}

@Composable
internal fun MotionIconButton(onClick: () -> Unit, modifier: Modifier = Modifier,
    prominent: Boolean = false, content: @Composable () -> Unit) {
    if (LocalTidalDesign.current) {
        if (prominent) FilledIconButton(onClick = onClick, modifier = modifier,
            shape = androidx.compose.foundation.shape.CircleShape, content = content)
        else IconButton(onClick = onClick, modifier = modifier, content = content)
        return
    }
    val interactions = remember { MutableInteractionSource() }
    if (prominent) FilledIconButton(onClick = onClick, modifier = modifier.pressScale(interactions),
        interactionSource = interactions, shapes = IconButtonDefaults.shapes(), content = content)
    else FilledTonalIconButton(onClick = onClick, modifier = modifier.pressScale(interactions),
        interactionSource = interactions, shapes = IconButtonDefaults.shapes(), content = content)
}
