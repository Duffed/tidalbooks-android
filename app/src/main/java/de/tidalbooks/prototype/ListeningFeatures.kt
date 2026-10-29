package de.tidalbooks.prototype

import java.text.Collator
import java.util.Locale

enum class BookSort(private val germanLabel: String) {
    LAST_PLAYED("Zuletzt gehört"), ADDED("Zuletzt hinzugefügt"), TITLE("Titel A–Z"), AUTHOR("Autor A–Z"), REMAINING("Kürzeste Restzeit");
    val label get() = translated(germanLabel)
}
internal fun filterBooks(books: List<LibraryBook>, query: String, status: String, sort: BookSort): List<LibraryBook> {
    val terms = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    val filtered = books.filter { b -> (status == "Alle" || b.status == status) &&
        terms.all { b.title.contains(it, true) || b.author.contains(it, true) } }
    val collator = Collator.getInstance(AppLanguage.locale).apply { strength = Collator.PRIMARY }
    val alphabetical = Comparator<LibraryBook> { a, b -> collator.compare(a.title, b.title) }.thenBy { it.id }
    return when (sort) {
        BookSort.LAST_PLAYED -> filtered.sortedWith(compareByDescending<LibraryBook> { it.lastPlayedAt }.thenByDescending { it.addedAt }.then(alphabetical))
        BookSort.ADDED -> filtered.sortedWith(compareByDescending<LibraryBook> { it.addedAt }.then(alphabetical))
        BookSort.TITLE -> filtered.sortedWith(alphabetical)
        BookSort.AUTHOR -> filtered.sortedWith(Comparator<LibraryBook> { a, b -> collator.compare(a.author, b.author) }.then(alphabetical))
        BookSort.REMAINING -> filtered.sortedWith(compareBy<LibraryBook> { (it.durationMs-it.elapsedMs).coerceAtLeast(0) }.then(alphabetical))
    }
}
internal fun resumeOffsetMs(seconds: Int, pausedElapsed: Long, pausedBoot: Int, nowElapsed: Long, boot: Int): Long =
    if (seconds in listOf(5, 10, 15, 30) && boot >= 0 && pausedBoot == boot && pausedElapsed >= 0 && nowElapsed-pausedElapsed >= 300_000)
        seconds * 1000L else 0L

class CatalogFailure(val status: Int) : Exception()
internal fun friendlyFailure(error: Exception): String = when (error) {
    is CatalogFailure -> when (error.status) {
        401 -> translated("Deine TIDAL-Anmeldung ist abgelaufen. Bitte unter Einstellungen erneut verbinden.")
        403 -> translated("TIDAL erlaubt den Zugriff auf dieses Hörbuch für dein Konto derzeit nicht.")
        404 -> translated("Dieses Hörbuch ist bei TIDAL derzeit nicht verfügbar. Deine Lesezeichen bleiben erhalten.")
        429 -> translated("TIDAL erhält gerade zu viele Anfragen. Bitte warte kurz und versuche es erneut.")
        else -> translated("TIDAL ist gerade nicht erreichbar. Bitte später erneut versuchen.")
    }
    is java.io.IOException -> translated("Die Verbindung zu TIDAL wurde unterbrochen. Prüfe WLAN oder mobile Daten und versuche es erneut.")
    else -> translated("Das hat nicht geklappt. Bitte erneut versuchen. Deine gespeicherten Hörstände bleiben erhalten.")
}
