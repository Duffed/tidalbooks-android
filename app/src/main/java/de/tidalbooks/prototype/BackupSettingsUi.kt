@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package de.tidalbooks.prototype

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun BackupSettingsCard(s: ProbeState, playback: PlaybackProbe, showTitle: Boolean = true) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reading by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<LibraryBackup?>(null) }
    val create =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            uri?.let { playback.exportBackup(it) }
        }
    val open =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                scope.launch {
                    reading = true

                    try {
                        preview =
                            withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.use(BackupCodec::read)
                                    ?: error(translated("Datei nicht lesbar"))
                            }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        playback.notify(
                            translated(
                                "Die Datei ist keine gültige, unterstützte Hörbuch-Sicherung (maximal 10 MB). Es wurde nichts verändert."
                            )
                        )
                    } finally {
                        reading = false
                    }
                }
        }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showTitle)
                Text(
                    translated("Backup & Wiederherstellung"),
                    style = MaterialTheme.typography.titleMedium,
                )

            Text(
                translated(
                    "Sichere Bücher, Hörstände, Lesezeichen und deine Stelle vor dem Einschlafen als Datei. Ohne Audiodateien oder TIDAL-Anmeldung. Bewahre die Datei an einem sicheren Ort auf."
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = {
                    create.launch(
                        "Hoerbuch-Backup-${SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.ROOT).format(Date())}.json"
                    )
                },
                enabled = s.libraryReady && !s.busy && !reading,
            ) {
                Text(translated("Backup speichern"))
            }
            OutlinedButton(
                onClick = {
                    open.launch(
                        arrayOf("application/json", "text/plain", "application/octet-stream")
                    )
                },
                enabled = s.libraryReady && !s.busy && !reading,
            ) {
                Text(
                    if (reading) translated("Datei wird geprüft …")
                    else translated("Backup wiederherstellen")
                )
            }
            Text(
                translated(
                    "Auf einem neuen Gerät meldest du dich anschließend erneut bei TIDAL an. Aktive Timer werden nicht übernommen."
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    preview?.let { backup ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(translated("Backup wiederherstellen?")) },
            text = {
                Text(
                    localized(
                        "${backup.books.size} Hörbücher und ${backup.bookmarks.size} Lesezeichen.\n\nBei enthaltenen Büchern ersetzt die Sicherung die Hörstände – auch durch ältere Stände. Andere Bücher und zusätzliche Lesezeichen bleiben erhalten. Die Wiedergabe wird pausiert und ein aktiver Timer ausgeschaltet.",
                        "${backup.books.size} audiobooks and ${backup.bookmarks.size} bookmarks.\n\nFor books included in the backup, saved positions will be replaced, even by older positions. Other books and additional bookmarks will be kept. Playback will be paused and any active timer turned off.",
                    )
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !s.busy,
                    onClick = {
                        preview = null
                        playback.restoreBackup(backup)
                    },
                ) {
                    Text(translated("Wiederherstellen"))
                }
            },
            dismissButton = {
                TextButton(onClick = { preview = null }) { Text(translated("Abbrechen")) }
            },
        )
    }
}
