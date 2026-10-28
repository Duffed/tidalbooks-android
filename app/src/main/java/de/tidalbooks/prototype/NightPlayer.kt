@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.tidalbooks.prototype

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

private fun Context.nightActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.nightActivity()
    else -> null
}

/** Brightness belongs to this window only; closing the night UI restores its previous value. */
@Composable
private fun NightBrightness(dim: Boolean, level: Float) {
    val view = LocalView.current
    val activity = LocalContext.current.nightActivity()
    val window = (view.parent as? DialogWindowProvider)?.window ?: activity?.window
    val originalBrightness = remember(window) { window?.attributes?.screenBrightness ?: -1f }
    DisposableEffect(window) {
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val lightStatus = controller?.isAppearanceLightStatusBars ?: false
        val lightNavigation = controller?.isAppearanceLightNavigationBars ?: false
        onDispose {
            window?.let { it.attributes = it.attributes.apply { screenBrightness = originalBrightness } }
            controller?.isAppearanceLightStatusBars = lightStatus
            controller?.isAppearanceLightNavigationBars = lightNavigation
        }
    }
    SideEffect {
        window?.let {
            it.attributes = it.attributes.apply { screenBrightness = if (dim) level.coerceIn(.03f,.3f) else originalBrightness }
            WindowCompat.getInsetsController(it,it.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
}

@Composable
internal fun NightPlayer(s: ProbeState, onClose: () -> Unit, onPlay: () -> Unit, onSkip: (Long) -> Unit,
    onBookmark: () -> Unit, onTimer: (Int) -> Unit, onTrackEnd: () -> Unit,
    onExtendTimer: () -> Unit, onCancelTimer: () -> Unit, onSleepStart: () -> Unit) {
    var dim by rememberSaveable { mutableStateOf(true) }
    var brightness by rememberSaveable { mutableFloatStateOf(.08f) }
    NightBrightness(dim,brightness)
    val colors = darkColorScheme(primary = Color(0xFFE6C28C), onPrimary = Color(0xFF241B10),
        background = Color.Black, surface = Color.Black, onSurface = Color(0xFFDED6C9),
        onSurfaceVariant = Color(0xFFB8AEA0), secondaryContainer = Color(0xFF28231D),
        onSecondaryContainer = Color(0xFFDED6C9), surfaceContainer = Color(0xFF191714))
    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bedtime,null,tint = colors.primary)
                    Text("Nachtmodus",Modifier.weight(1f).padding(start = 12.dp),style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onClose) { Icon(Icons.Rounded.Close,"Nachtmodus schließen") }
                }
                Text(s.album?.title ?: "Wähle zuerst ein Hörbuch in deiner Bibliothek.",
                    style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                s.album?.let { album ->
                    Text("Abschnitt ${s.index+1} von ${album.tracks.size} · ${clock(s.positionMs)}",
                        color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
                if (s.loading) Text("Abschnitt wird geladen …",color = colors.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { onSkip(-30_000) },enabled = s.album != null && !s.busy, modifier = Modifier.size(68.dp)) {
                        Icon(Icons.Rounded.Replay30,"30 Sekunden zurück",Modifier.size(34.dp))
                    }
                    FilledIconButton(onClick = onPlay,enabled = s.album != null && s.libraryReady && !s.busy && !s.completed,
                        modifier = Modifier.size(96.dp),shape = RoundedCornerShape(32.dp)) {
                        Icon(if (s.requestedPlay) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (s.requestedPlay) "Pause" else "Abspielen",Modifier.size(52.dp))
                    }
                    FilledTonalIconButton(onClick = { onSkip(30_000) },enabled = s.album != null && !s.busy, modifier = Modifier.size(68.dp)) {
                        Icon(Icons.Rounded.Forward30,"30 Sekunden vor",Modifier.size(34.dp))
                    }
                }
                s.playbackProblem?.let { Text(it,color = colors.error) }
                s.message.takeIf { it.isNotBlank() && it != s.playbackProblem }?.let {
                    Text(it, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(onClick = onBookmark,enabled = s.album != null && !s.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
                    Icon(Icons.Rounded.BookmarkAdd,null); Spacer(Modifier.width(12.dp)); Text("Stelle merken")
                }
                Text("Einschlaftimer",Modifier.align(Alignment.Start),style = MaterialTheme.typography.titleMedium)
                if (s.sleepSession?.active == true) {
                    Text(s.timerRemainingMs?.let(::clock) ?: "Am Trackende",style = MaterialTheme.typography.displaySmall,color = colors.primary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (s.sleepSession.deadlineElapsedMs != null) FilledTonalButton(onClick = onExtendTimer) { Text("+10 Minuten") }
                        TextButton(onClick = onCancelTimer) { Text("Timer ausschalten") }
                    }
                }
                FlowRow(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(8.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15,30,45,60).forEach { minutes ->
                        FilledTonalButton(onClick = { onTimer(minutes) },enabled = s.album != null && !s.busy,
                            modifier = Modifier.heightIn(min = 52.dp)) { Text("$minutes Min.") }
                    }
                }
                TextButton(onClick = onTrackEnd, enabled = s.album != null && !s.busy) { Text("Am Ende dieses Tracks") }
                if (s.sleepSession != null) TextButton(onClick = onSleepStart) { Text("Zur Stelle vor dem Einschlafen") }
                HorizontalDivider(color = colors.outlineVariant)
                Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                    Text("Bildschirm dimmen",Modifier.weight(1f))
                    Switch(checked = dim,onCheckedChange = { dim = it },modifier = Modifier.semantics { contentDescription = "Bildschirm dimmen" })
                }
                if (dim) Slider(value = brightness,onValueChange = { brightness = it },valueRange = .03f.. .3f,
                    modifier = Modifier.semantics { contentDescription = "Helligkeit im Nachtmodus" })
                Text("Die Helligkeit gilt nur hier. Beim Schließen wird sie wiederhergestellt. Der Bildschirm kann sich wie gewohnt ausschalten.",
                    style = MaterialTheme.typography.bodySmall,color = colors.onSurfaceVariant)
            }
        }
    }
}
