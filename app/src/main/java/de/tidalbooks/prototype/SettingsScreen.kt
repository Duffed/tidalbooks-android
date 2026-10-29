@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package de.tidalbooks.prototype

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

internal enum class SettingsSection {
    ACCOUNT, PLAYBACK, APPEARANCE, WIDGET, BACKUP, ABOUT;
    val title: String get() = when (this) {
        ACCOUNT -> localized("TIDAL-Konto", "TIDAL account")
        PLAYBACK -> localized("Wiedergabe", "Playback")
        APPEARANCE -> translated("Darstellung")
        WIDGET -> translated("Homescreen-Widget")
        BACKUP -> translated("Backup & Wiederherstellung")
        ABOUT -> localized("Über die App", "About")
    }
}

@Composable
internal fun SettingsScreen(s: ProbeState, modifier: Modifier, playback: PlaybackProbe) {
    val context = LocalContext.current
    val theme = LocalThemeStore.current.settings
    var tidalWidget by remember { mutableStateOf(AudiobookWidget.usesTidalDesign(context)) }
    var widgetOpacity by remember { mutableFloatStateOf(AudiobookWidget.backgroundOpacity(context).toFloat()) }
    var expanded by rememberSaveable { mutableStateOf<SettingsSection?>(if (s.loggedIn) null else SettingsSection.ACCOUNT) }
    val toggle: (SettingsSection) -> Unit = { expanded = if (expanded == it) null else it }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "account") {
            SettingsSectionCard(Icons.Rounded.AccountCircle, SettingsSection.ACCOUNT.title,
                if (s.loggedIn) localized("Verbunden", "Connected") else translated("Mit TIDAL verbinden"),
                expanded == SettingsSection.ACCOUNT, { toggle(SettingsSection.ACCOUNT) }) {
                Surface(shape = appCorner(28.dp),color = MaterialTheme.colorScheme.secondaryContainer) {
                                Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Icon(if (s.loggedIn) Icons.Rounded.CheckCircle else Icons.Rounded.Headphones,null,Modifier.size(32.dp))
                                    Text(if (s.loggedIn) translated("Mit TIDAL verbunden") else translated("Deine Hörbücher auf TIDAL"),style = MaterialTheme.typography.headlineSmall)
                                    Text(if (s.loggedIn) translated("Dein Konto ist bereit. Fortschritt und Lesezeichen bleiben auf diesem Gerät.") else translated("Verbinde dein Konto und höre deine Alben als Hörbücher weiter."))
                                    ExpressiveButton(onClick = { playback.beginAlternativeLogin() },enabled = !s.busy) { Text(if (s.loggedIn) translated("TIDAL neu verbinden") else translated("Mit TIDAL verbinden")) }
                                    s.alternativeCode?.let { Text(it,style = MaterialTheme.typography.displaySmall,fontWeight = FontWeight.Medium) }
                                    s.alternativeUrl?.let { url ->
                                        Text(translated("Gerätecode bei TIDAL bestätigen und danach per Zurück-Geste hierher wechseln."))
                                        ExpressiveButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) }) { Text(translated("Bei TIDAL bestätigen")) }
                                        TextButton(onClick = playback::cancelAlternativeLogin) { Text(translated("Abbrechen")) }
                                    }
                                }
                            }
            }
        }
        item(key = "playback") {
            val rewind = if (s.resumeRewindSeconds == 0) translated("Aus") else "${s.resumeRewindSeconds} s"
            SettingsSectionCard(Icons.Rounded.Headphones, SettingsSection.PLAYBACK.title,
                localized("Sprungweite ${s.skipSeconds} s · Rücksprung: $rewind", "Skip ${s.skipSeconds} s · Rewind: $rewind"),
                expanded == SettingsSection.PLAYBACK, { toggle(SettingsSection.PLAYBACK) }) {
                SettingsCard(Icons.Rounded.Replay,translated("Sprungweite"),translated("Gilt für Vor- und Zurückspringen im Player, im Widget und in der Mediensteuerung.")) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(5, 15, 30, 60).forEach { seconds ->
                                    FilterChip(selected = s.skipSeconds == seconds, onClick = { playback.setSkipSeconds(seconds) },
                                        label = { Text(localized("$seconds Sekunden", "$seconds seconds")) })
                                }
                            }
                        }
                ListeningSettingsCard(s.resumeRewindSeconds, playback::setResumeRewind)
                SettingsCard(Icons.Rounded.Notifications,translated("Medienbenachrichtigung"),translated("Steuere die Wiedergabe über die Benachrichtigungen, den Sperrbildschirm oder deine Kopfhörer.")) {
                            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)) }) { Text(translated("Benachrichtigungen öffnen")) }
                        }
            }
        }
        item(key = "appearance") {
            SettingsSectionCard(Icons.Rounded.Palette, SettingsSection.APPEARANCE.title,
                "${theme.design.label} · ${AppLanguage.choice.label}",
                expanded == SettingsSection.APPEARANCE, { toggle(SettingsSection.APPEARANCE) }) {
                LanguageSettingsCard()
                ThemeSettingsCard()
            }
        }
        item(key = "widget") {
            val open = expanded == SettingsSection.WIDGET
            val design = if (tidalWidget) "TIDAL" else "Material You"
            val opacity = widgetOpacity.toInt()
            SettingsSectionCard(Icons.Rounded.Widgets, SettingsSection.WIDGET.title,
                localized("$design · $opacity % Deckkraft", "$design · $opacity % opacity"),
                open, { toggle(SettingsSection.WIDGET) }) {
                SettingsCard(showTitle = false, icon = Icons.Rounded.Widgets, title = translated("Homescreen-Widget"), text = localized("Start/Pause, ${s.skipSeconds} Sekunden zurück, Lesezeichen und dein letzter Einschlaftimer direkt auf dem Startbildschirm.", "Play/pause, skip back ${s.skipSeconds} seconds, bookmarks and your last sleep timer, right on your home screen.")) {
                            Text(translated("Widget-Design"), style = MaterialTheme.typography.titleMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(false to translated("Material You"), true to "TIDAL").forEach { (tidal, label) ->
                                    FilterChip(selected = tidalWidget == tidal, onClick = {
                                        context.getSharedPreferences("widget-appearance", android.content.Context.MODE_PRIVATE)
                                            .edit().putBoolean("tidal", tidal).apply()
                                        tidalWidget = tidal
                                        AudiobookWidget.update(context, WidgetSnapshot.from(s))
                                    }, label = { Text(label) })
                                }
                            }
                            Text(translated("Unabhängig vom App-Design. Gilt für alle Widgets."), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            WidgetOpacitySlider(widgetOpacity, { widgetOpacity = it }, {
                                context.getSharedPreferences("widget-appearance", android.content.Context.MODE_PRIVATE)
                                    .edit().putInt("opacity", widgetOpacity.toInt()).apply()
                                AudiobookWidget.update(context, WidgetSnapshot.from(s))
                            })
                            key(tidalWidget) { WidgetPreview(WidgetSnapshot.from(s),Modifier.fillMaxWidth().height(160.dp), opacity = widgetOpacity.toInt()) }
                            val manager = AppWidgetManager.getInstance(context)
                            if (manager.isRequestPinAppWidgetSupported) TextButton(onClick = {
                                manager.requestPinAppWidget(ComponentName(context,AudiobookWidget::class.java),null,null)
                            }) { Text(translated("Widget hinzufügen")) }
                            else Text(translated("Startbildschirm lange drücken → Widgets → Hörbuch."),style = MaterialTheme.typography.bodySmall)
                        }
            }
        }
        item(key = "backup") {
            SettingsSectionCard(Icons.Rounded.Backup, SettingsSection.BACKUP.title,
                localized("Bibliothek sichern oder wiederherstellen", "Save or restore your library"),
                expanded == SettingsSection.BACKUP, { toggle(SettingsSection.BACKUP) }) {
                BackupSettingsCard(s, playback, showTitle = false)
            }
        }
        item(key = "about") {
            SettingsSectionCard(Icons.Rounded.Info, SettingsSection.ABOUT.title, "TIDAL Books ${BuildConfig.VERSION_NAME}",
                expanded == SettingsSection.ABOUT, { toggle(SettingsSection.ABOUT) }) {
                SettingsCard(Icons.Rounded.PhoneAndroid,translated("Auf diesem Gerät gespeichert"),localized("${s.books.size} Hörbücher · ${s.bookmarks.size} Lesezeichen. Deine Bibliothek benötigt keinen zusätzlichen Account und keinen eigenen Server.", "${s.books.size} audiobooks · ${s.bookmarks.size} bookmarks. Your library needs no additional account or server.")) {}
                Text(localized("TIDAL Books ${BuildConfig.VERSION_NAME}\nWiedergabe und Katalog von TIDAL", "TIDAL Books ${BuildConfig.VERSION_NAME}\nPlayback and catalogue by TIDAL"),style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant,modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(icon: ImageVector, title: String, value: String, expanded: Boolean,
    onClick: () -> Unit, content: @Composable () -> Unit) {
    // Resolve observable language state during composition, not during semantics traversal.
    val expansionLabel = if (expanded) localized("Aufgeklappt", "Expanded") else localized("Zugeklappt", "Collapsed")
    Surface(shape = appCorner(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.semantics {
                    stateDescription = expansionLabel
                }) {
                // The pinned Material3 ListItem traverses detached parent semantics during
                // locale recreation. A normal row keeps one accessible clickable header.
                Row(Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(value, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
            }
        }
    }
}

@Composable
private fun SettingsCard(icon: ImageVector,title: String,text: String,showTitle: Boolean = true,actions: @Composable () -> Unit) {
    Surface(shape = appCorner(24.dp),color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (showTitle) Row(horizontalArrangement = Arrangement.spacedBy(14.dp),verticalAlignment = Alignment.CenterVertically) {
                Icon(icon,null,tint = MaterialTheme.colorScheme.primary)
                Text(title,style = MaterialTheme.typography.titleMedium)
            }
            Text(text,style = MaterialTheme.typography.bodyMedium,color = MaterialTheme.colorScheme.onSurfaceVariant)
            actions()
        }
    }
}
