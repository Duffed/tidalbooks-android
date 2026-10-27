package de.tidalbooks.prototype

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private val artworkCache = LruCache<String,Bitmap>(24)
private val artworkHttp = OkHttpClient.Builder().followRedirects(false).build()

@Composable
fun BookArtwork(url: String?, modifier: Modifier = Modifier, large: Boolean = false) {
    val bitmap by produceState<Bitmap?>(url?.let { artworkCache.get(it) },url) {
        value = url?.let { address -> withContext(Dispatchers.IO) {
            artworkCache.get(address) ?: runCatching {
                val parsed = address.toHttpUrlOrNull() ?: return@runCatching null
                if (!parsed.isHttps || parsed.host != "resources.tidal.com" || !parsed.encodedPath.startsWith("/images/")) return@runCatching null
                artworkHttp.newCall(Request.Builder().url(parsed).build()).execute().use { response ->
                    if (!response.isSuccessful) null else response.body?.byteStream()?.use { BitmapFactory.decodeStream(it) }
                }?.also { artworkCache.put(address,it) }
            }.getOrNull()
        } }
    }
    Box(modifier.clip(RoundedCornerShape(if (large) 28.dp else 16.dp)).background(Brush.linearGradient(listOf(
        MaterialTheme.colorScheme.secondaryContainer,MaterialTheme.colorScheme.tertiaryContainer))),contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale = ContentScale.Fit) }
            ?: Icon(Icons.Rounded.Headphones,null,Modifier.fillMaxSize().padding(if (large) 66.dp else 12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .75f))
    }
}
