package de.tidalbooks.prototype

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

import androidx.compose.ui.platform.LocalContext

@Composable
fun BookArtwork(url: String?, modifier: Modifier = Modifier, large: Boolean = false, backdrop: Boolean = false) {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { ArtworkStore.get(context) }
    // A new URL must not retain the previous book's bitmap while loading.
    key(url) {
        val bitmap by produceState<Bitmap?>(url?.let(store::peek), url) {
            value = url?.let { store.load(it) }
        }
        Box(modifier.clip(RoundedCornerShape(if (LocalTidalDesign.current) 3.dp else if (large) 28.dp else 16.dp)).background(Brush.linearGradient(listOf(
            MaterialTheme.colorScheme.secondaryContainer,MaterialTheme.colorScheme.tertiaryContainer))),contentAlignment = Alignment.Center) {
            Crossfade(targetState = bitmap,animationSpec = tween(220),label = "Buchcover") { artwork ->
                // Filtered low-resolution artwork stays soft on older Android versions too.
                val image = remember(artwork, backdrop) {
                    if (backdrop && artwork != null) Bitmap.createScaledBitmap(artwork, 12, 12, true) else artwork
                }
                image?.let { Image(it.asImageBitmap(),null,
                    Modifier.fillMaxSize().then(if (backdrop) Modifier.blur(32.dp) else Modifier),
                    contentScale = if (backdrop) ContentScale.Crop else ContentScale.Fit) }
                    ?: Icon(Icons.Rounded.Headphones,null,Modifier.fillMaxSize().padding(if (large) 66.dp else 12.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .75f))
            }
        }
    }
}
