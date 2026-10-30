package de.tidalbooks.prototype

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
internal fun WidgetOpacitySlider(value: Float, onChange: (Float) -> Unit, onFinished: () -> Unit = {}) {
    Column {
        Text(localized("Hintergrund-Deckkraft: ${value.toInt()} %", "Background opacity: ${value.toInt()} %"), style = MaterialTheme.typography.titleMedium)
        Slider(value = value, onValueChange = onChange, onValueChangeFinished = onFinished,
            valueRange = 0f..100f, modifier = Modifier.semantics { contentDescription = translated("Hintergrund-Deckkraft in Prozent") })
        Text(translated("0 % transparent · 100 % deckend. Schrift und Bedienelemente bleiben sichtbar."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
