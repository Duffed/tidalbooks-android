package de.tidalbooks.prototype

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow

enum class LanguageChoice(val tag: String, val label: String) {
    SYSTEM("", "System"),
    GERMAN("de", "Deutsch"),
    ENGLISH("en", "English"),
}

internal object AppLanguage {
    var choice by mutableStateOf(LanguageChoice.SYSTEM)
        private set

    private var systemLanguage by mutableStateOf("de")
    val changes = MutableStateFlow(0)
    val english: Boolean
        get() =
            when (choice) {
                LanguageChoice.ENGLISH -> true
                LanguageChoice.GERMAN -> false
                LanguageChoice.SYSTEM -> systemLanguage == "en"
            }

    val locale: Locale
        get() = if (english) Locale.ENGLISH else Locale.GERMAN

    internal fun configure(value: LanguageChoice, system: String = systemLanguage) {
        val previousEnglish = english

        systemLanguage = system
        choice = value
        if (previousEnglish != english) changes.value++
    }

    fun initialize(context: Context) {
        val selected =
            if (Build.VERSION.SDK_INT >= 33) {
                val locales = context.getSystemService(LocaleManager::class.java).applicationLocales

                when (if (locales.isEmpty) "" else locales[0].language) {
                    "de" -> LanguageChoice.GERMAN
                    "en" -> LanguageChoice.ENGLISH
                    else -> LanguageChoice.SYSTEM
                }
            } else
                LanguageChoice.entries.firstOrNull {
                    it.name ==
                        context
                            .getSharedPreferences("language", Context.MODE_PRIVATE)
                            .getString("choice", null)
                } ?: LanguageChoice.SYSTEM
        val system =
            if (Build.VERSION.SDK_INT >= 33)
                context.getSystemService(LocaleManager::class.java).systemLocales
            else android.content.res.Resources.getSystem().configuration.locales
        val supportedSystemLanguage =
            (0 until system.size())
                .map { system[it].language }
                .firstOrNull { it == "de" || it == "en" } ?: "de"

        configure(selected, supportedSystemLanguage)
    }

    fun select(context: Context, value: LanguageChoice) {
        context.getSharedPreferences("language", Context.MODE_PRIVATE).edit {
            putString("choice", value.name)
        }
        configure(value)
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(value.tag)
        } else (context as? Activity)?.recreate()
    }

    fun wrap(context: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return context

        val selected =
            context.getSharedPreferences("language", Context.MODE_PRIVATE).getString("choice", null)
        val tag =
            when (selected) {
                "ENGLISH" -> "en"
                "GERMAN" -> "de"
                else -> return context
            }

        return context.createConfigurationContext(
            Configuration(context.resources.configuration).apply {
                setLocales(LocaleList.forLanguageTags(tag))
            }
        )
    }
}

internal fun localized(german: String, english: String): String =
    if (AppLanguage.english) english else german

internal fun translated(german: String): String =
    if (AppLanguage.english) englishLabels[german] ?: german else german

internal fun displayAppMessage(message: String): String =
    if (AppLanguage.english) englishLabels[message] ?: message
    else englishLabels.entries.firstOrNull { it.value == message }?.key ?: message

internal fun formatSavedDate(timestamp: Long): String =
    java.text
        .SimpleDateFormat(
            localized("dd.MM. · HH:mm", "d MMM · HH:mm"),
            AppLanguage.locale,
        )
        .format(java.util.Date(timestamp))

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LanguageSettingsCard() {
    val context = LocalContext.current

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(localized("Sprache", "Language"), style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageChoice.entries.forEach { language ->
                    FilterChip(
                        selected = AppLanguage.choice == language,
                        onClick = { AppLanguage.select(context, language) },
                        label = { Text(language.label) },
                    )
                }
            }
            Text(
                localized(
                    "Gilt auch für Widgets und Mediensteuerung. Buchtitel und eigene Lesezeichennamen bleiben unverändert.",
                    "Also applies to widgets and media controls. Book titles and your own bookmark names stay unchanged.",
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
