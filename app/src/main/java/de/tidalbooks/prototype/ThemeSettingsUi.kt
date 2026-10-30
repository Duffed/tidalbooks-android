@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.tidalbooks.prototype

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal fun ThemeSettingsCard() {
    val store = LocalThemeStore.current
    val settings = store.settings

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    translated("Farben & Darstellung"),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Text(
                translated("Änderungen werden sofort übernommen und gespeichert."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(translated("Design"), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppDesign.entries.forEach { design ->
                    FilterChip(
                        selected = settings.design == design,
                        onClick = { store.update(settings.copy(design = design)) },
                        label = { Text(design.label) },
                    )
                }
            }
            if (settings.design == AppDesign.TIDAL) {
                Text(
                    translated(
                        "Schwarze Flächen, klare Cover und weiße Bedienelemente – angelehnt an TIDAL. Deine Material-Farben und Helligkeitswahl bleiben für den Wechsel zurück gespeichert."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(translated("Farben"), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppPalette.entries.forEach { palette ->
                        FilterChip(
                            selected = settings.palette == palette,
                            onClick = { store.update(settings.copy(palette = palette)) },
                            label = { Text(palette.label) },
                            leadingIcon = {
                                Box(
                                    Modifier.size(18.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (palette == AppPalette.SYSTEM)
                                                MaterialTheme.colorScheme.primary
                                            else androidx.compose.ui.graphics.Color(palette.light)
                                        )
                                )
                            },
                        )
                    }
                }
                if (settings.palette == AppPalette.SYSTEM)
                    Text(
                        if (Build.VERSION.SDK_INT >= 31)
                            translated("Material You übernimmt die Farben deines Geräts.")
                        else
                            translated(
                                "Material You ist ab Android 12 verfügbar. Hier wird die Wald-Palette verwendet."
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                Text(translated("Darstellung"), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.mode == mode,
                            onClick = { store.update(settings.copy(mode = mode)) },
                            label = { Text(mode.label) },
                            shape = RoundedCornerShape(16.dp),
                        )
                    }
                }
            }
        }
    }
}
