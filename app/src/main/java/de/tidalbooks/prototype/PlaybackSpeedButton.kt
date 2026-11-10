package de.tidalbooks.prototype

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
internal fun PlaybackSpeedButton(speed: Float, enabled: Boolean, onSpeed: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val description = localized("Wiedergabegeschwindigkeit", "Playback speed")
    Box {
        FilterChip(
            selected = false,
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.semantics {
                contentDescription = "$description: ${playbackSpeedLabel(speed)}"
            },
            label = { Text(playbackSpeedLabel(speed)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            playbackSpeeds.forEach { value ->
                DropdownMenuItem(
                    text = { Text(playbackSpeedLabel(value)) },
                    leadingIcon = { RadioButton(selected = value == speed, onClick = null) },
                    onClick = { expanded = false; onSpeed(value) },
                )
            }
        }
    }
}
