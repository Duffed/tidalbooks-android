package de.tidalbooks.prototype

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Launcher reconfiguration changes appearance only; never starts playback. */
class WidgetSettingsActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) { super.attachBaseContext(AppLanguage.wrap(newBase)) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val manager = AppWidgetManager.getInstance(this)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID ||
            manager.getAppWidgetInfo(id)?.provider != ComponentName(this, AudiobookWidget::class.java)) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            val state by (application as ProbeApp).probe.state.collectAsStateWithLifecycle()
            var tidal by rememberSaveable { mutableStateOf(AudiobookWidget.usesTidalDesign(this)) }
            var opacity by rememberSaveable { mutableFloatStateOf(AudiobookWidget.backgroundOpacity(this).toFloat()) }
            AudiobookTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(translated("Widget-Optionen"), style = MaterialTheme.typography.headlineMedium)
                        Text(translated("Farben & Design"), style = MaterialTheme.typography.titleLarge)
                        Text(translated("Material You nutzt deine Systemfarben. TIDAL verwendet eine dunkle Fläche mit weißen Bedienelementen."))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(selected = !tidal, onClick = { tidal = false }, label = { Text(translated("Material You")) })
                            FilterChip(selected = tidal, onClick = { tidal = true }, label = { Text("TIDAL") })
                        }
                        WidgetOpacitySlider(opacity, { opacity = it })
                        Box {
                            key(tidal) { WidgetPreview(WidgetSnapshot.from(state), Modifier.fillMaxWidth().height(160.dp), tidal, opacity.toInt()) }
                        }
                        Text(translated("Die Auswahl gilt für alle TIDAL-Books-Widgets. Das App-Design bleibt unabhängig davon."),
                            style = MaterialTheme.typography.bodySmall)
                        Button(onClick = {
                            getSharedPreferences("widget-appearance", Context.MODE_PRIVATE)
                                .edit().putBoolean("tidal", tidal).putInt("opacity", opacity.toInt()).apply()
                            AudiobookWidget.update(this@WidgetSettingsActivity, WidgetSnapshot.from(state))
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                            finish()
                        }, enabled = state.libraryReady, modifier = Modifier.fillMaxWidth()) { Text(translated("Übernehmen")) }
                        TextButton(onClick = { finish() }, modifier = Modifier.fillMaxWidth()) { Text(translated("Abbrechen")) }
                    }
                }
            }
        }
    }
}
