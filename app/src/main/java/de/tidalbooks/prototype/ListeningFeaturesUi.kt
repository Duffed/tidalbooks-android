@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package de.tidalbooks.prototype

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun GettingStartedCard(loggedIn: Boolean, onConnect: () -> Unit, onAdd: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(translated("Dein erstes Hörbuch"), style = MaterialTheme.typography.headlineSmall)
            Text(translated("Du brauchst ein TIDAL-Konto mit passendem Abo und eine Internetverbindung. Die App speichert deine Hörstände auf diesem Gerät."))
            Text(if (loggedIn) translated("1 · Mit TIDAL verbunden ✓") else translated("1 · Verbinde dein TIDAL-Konto in den Einstellungen."))
            Text(translated("2 · Öffne das Hörbuchalbum in TIDAL. Teile es mit TIDAL Books oder kopiere den Albumlink und füge ihn hier ein."))
            Text(translated("3 · Öffne dein Buch und tippe auf Abspielen. Beim nächsten Start wartet es pausiert an deiner Stelle."))
            ExpressiveButton(onClick = if (loggedIn) onAdd else onConnect, modifier = Modifier.fillMaxWidth()) {
                Text(if (loggedIn) translated("Erstes Hörbuch hinzufügen") else translated("Mit TIDAL verbinden"))
            }
            Text(translated("Schon eine Sicherung? Du kannst sie unter Einstellungen → Backup & Wiederherstellung einlesen."), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun ListeningSettingsCard(seconds: Int, onChange: (Int) -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(translated("Wieder ins Hörbuch finden"), style = MaterialTheme.typography.titleMedium)
            Text(translated("Nach mindestens 5 Minuten Pause beim Weiterhören automatisch zurückspringen. Nach einem Geräteneustart bleibt die gespeicherte Stelle unverändert."))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(0, 5, 10, 15, 30).forEach { value ->
                    FilterChip(selected = value == seconds, onClick = { onChange(value) },
                        label = { Text(if (value == 0) translated("Aus") else localized("$value Sek.", "$value sec")) })
                }
            }
            Text(translated("Gezielt gewählte Stellen bleiben erhalten. Der Rücksprung des Einschlaftimers funktioniert unabhängig davon."), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun PositionHistorySheet(s: ProbeState, playback: PlaybackProbe, onDismiss: () -> Unit) {
    val history = s.history.filter { it.bookId == s.album?.id }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(translated("Positionsverlauf"), Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.headlineSmall)
        Text(translated("Die letzten zehn Stellen vor einem Sprung. Play/Pause bleibt beim Zurückspringen erhalten."), Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodyMedium)
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 500.dp).navigationBarsPadding(), contentPadding = PaddingValues(vertical = 16.dp)) {
            if (history.isEmpty()) item { Text(translated("Noch keine Positionssprünge in diesem Buch."), Modifier.padding(24.dp)) }
            items(history, key = { it.id }) { jump ->
                val tracks = s.album?.tracks.orEmpty()
                val index = tracks.indexOfFirst { it.id == jump.trackId }
                ListItem(headlineContent = { Text(localized("Kapitel ${index+1} · ${clock(jump.positionMs)}", "Chapter ${index+1} · ${clock(jump.positionMs)}")) },
                    supportingContent = { Text("${translated(jump.reason)}\n${formatSavedDate(jump.createdAt)}") },
                    modifier = Modifier.clickable(enabled = index >= 0 && !s.busy) { playback.restoreHistory(jump); onDismiss() })
            }
        }
    }
}
