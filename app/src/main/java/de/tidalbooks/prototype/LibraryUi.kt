@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package de.tidalbooks.prototype

import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.appwidget.AppWidgetManager
import android.content.ComponentName

fun clock(ms: Long): String {
    val seconds = ms.coerceAtLeast(0)/1000
    return if (seconds >= 3600) "%d:%02d:%02d".format(Locale.ROOT,seconds/3600,seconds/60%60,seconds%60)
    else "%d:%02d".format(Locale.ROOT,seconds/60,seconds%60)
}
internal fun durationLabel(ms: Long): String {
    val minutes = (ms.coerceAtLeast(0)+59_999)/60_000
    return if (minutes >= 60) "${minutes/60} Std. ${minutes%60} Min." else "$minutes Min."
}

@Composable
fun LibraryApp(s: ProbeState, playback: PlaybackProbe, sharedInput: String, shareNonce: Int, openPlayerNonce: Int) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var link by rememberSaveable { mutableStateOf("") }
    var importingId by remember { mutableStateOf<String?>(null) }
    var rename by remember { mutableStateOf<StoredBookmark?>(null) }
    var remove by remember { mutableStateOf<LibraryBook?>(null) }
    var restart by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(shareNonce) { if (shareNonce > 0 && sharedInput.isNotBlank()) { link = sharedInput; sheet = "add" } }
    LaunchedEffect(openPlayerNonce) { if (openPlayerNonce > 0) playerOpen = true }
    LaunchedEffect(s.books,s.busy,importingId) {
        if (importingId != null && !s.busy && s.books.any { it.id == importingId }) {
            importingId = null; sheet = null; playerOpen = true
        }
    }
    LaunchedEffect(s.message,sheet) {
        val message = s.message
        if (message.isNotBlank() && sheet != "add") {
            snackbar.showSnackbar(message,duration = SnackbarDuration.Short)
            if (playback.state.value.message == message) playback.dismissMessage()
        }
    }
    BackHandler(enabled = playerOpen) { playerOpen = false }
    BackHandler(enabled = !playerOpen && tab != 0) { tab = 0 }
    val openBook: (LibraryBook,Boolean) -> Unit = { book,play -> playback.openBook(book.id,play); playerOpen = true }
    val toggle: () -> Unit = {
        if (s.requestedPlay) playback.pause() else if (s.completed) restart = true else playback.play()
    }
    var miniBounds by remember { mutableStateOf(Rect.Zero) }
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxSize().onGloballyPositioned { rootPosition = it.positionInRoot() }) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        Scaffold(modifier = if (tab == 0) Modifier else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                if (tab == 0) TopAppBar(
                    title = { Text("Bibliothek",style = MaterialTheme.typography.headlineMedium,fontWeight = FontWeight.SemiBold) })
                else LargeTopAppBar(
                    title = { Text(listOf("Bibliothek","Lesezeichen","Einstellungen")[tab],fontWeight = FontWeight.SemiBold) },
                    scrollBehavior = scrollBehavior)
            },
            bottomBar = {
                Column {
                    if (s.album != null) Spacer(Modifier.fillMaxWidth().height(89.dp)
                        .onGloballyPositioned { miniBounds = it.boundsInRoot() })
                    ExpressiveNavigation(tab) { tab = it }
                }
            }, snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = { if (tab == 0 && s.books.isNotEmpty()) ExtendedFloatingActionButton(
                onClick = { sheet = "add" },icon = { Icon(Icons.Rounded.Add,null) },text = { Text("Hinzufügen") }) },
        ) { padding ->
            val tabState = rememberSaveableStateHolder()
            Crossfade(targetState = tab, animationSpec = tween(180), label = "Tabwechsel") { visibleTab ->
                tabState.SaveableStateProvider(visibleTab) {
                    when(visibleTab) {
                        0 -> LibraryScreen(s,Modifier.padding(padding),openBook,{ sheet = "add" },{ remove = it },{ tab = 2 })
                        1 -> BookmarkScreen(s.bookmarks,Modifier.padding(padding),onOpen = { playback.openBookmark(it); playerOpen = true },
                            onRename = { rename = it },onDelete = playback::deleteBookmark)
                        else -> SettingsScreen(s,Modifier.padding(padding),playback)
                    }
                }
            }
        }
        if (s.album != null && miniBounds.height > 0f) {
            ExpandablePlayer(
                expanded = playerOpen,
                onExpandedChange = { playerOpen = it },
                miniBounds = miniBounds.translate(-rootPosition),
                miniContent = { MiniPlayer(s,{ playerOpen = true },toggle,embedded = true) },
            ) {
                PlayerScreen(s,onBack = { playerOpen = false },onPlay = toggle,onSkip = playback::skip,
                    onSeek = playback::seekAbsolute,onBookmark = { playback.addBookmark(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                    onTimer = { sheet = "timer" },onChapters = { sheet = "chapters" },onMarks = { sheet = "marks" },
                    onFinished = playback::markCompleted,onRestart = { restart = true },onHistory = { sheet = "history" },onUndo = { s.history.firstOrNull { it.bookId == s.album?.id }?.let(playback::restoreHistory) },onRetry = playback::retryPlayback,snackbar = snackbar)
            }
        }
    }
    when(sheet) {
        "add" -> ModalBottomSheet(onDismissRequest = { sheet = null }) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp).imePadding(),verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Ein Hörbuch hinzufügen",style = MaterialTheme.typography.headlineSmall)
                Text("Füge den Albumlink aus TIDAL ein. Jedes Hörbuch behält seinen eigenen Fortschritt.",color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = link,onValueChange = { link = it },label = { Text("TIDAL-Albumlink") },
                    leadingIcon = { Icon(Icons.Rounded.Link,null) },modifier = Modifier.fillMaxWidth(),shape = RoundedCornerShape(20.dp),singleLine = true)
                TextButton(onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    link = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                }) { Icon(Icons.Rounded.ContentPaste,null,Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Link einfügen") }
                if (s.message.isNotBlank()) Text(s.message,color = MaterialTheme.colorScheme.error,style = MaterialTheme.typography.bodyMedium)
                if (!s.loggedIn) {
                    ExpressiveButton(onClick = { sheet = null; tab = 2 },modifier = Modifier.fillMaxWidth()) { Text("Zuerst mit TIDAL verbinden") }
                } else ExpressiveButton(onClick = {
                    val parsed = runCatching { albumId(link) }
                    if (parsed.isFailure) playback.notify("Bitte einen gültigen TIDAL-Albumlink eingeben.")
                    else { playback.dismissMessage(); importingId = parsed.getOrNull(); playback.loadAlbum(link) }
                },enabled = !s.busy && s.libraryReady && link.isNotBlank(),modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    if (s.busy) { ExpressiveLoadingIndicator(Modifier.size(20.dp),strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)) }
                    Text(if (s.busy) "Hörbuch wird geladen" else if (s.message.isNotBlank()) "Erneut versuchen" else "Zur Bibliothek hinzufügen")
                }
            }
        }
        "timer" -> TimerSheet(s,playback) { sheet = null }
        "chapters" -> ModalBottomSheet(onDismissRequest = { sheet = null }) {
            Text("Abschnitte",Modifier.padding(horizontal = 24.dp,vertical = 12.dp),style = MaterialTheme.typography.headlineSmall)
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp).navigationBarsPadding()) {
                items(s.album?.tracks.orEmpty().withIndex().toList(),key = { it.index }) { (index,track) ->
                    ListItem(headlineContent = { Text(track.title,maxLines = 2,overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("${track.disc}.${track.number} · ${durationLabel(track.durationMs)}") },
                        leadingContent = { if (index == s.index) Icon(Icons.Rounded.GraphicEq,null) else Text("${index+1}",style = MaterialTheme.typography.titleMedium) },
                        colors = ListItemDefaults.colors(containerColor = if (index == s.index) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                        modifier = Modifier.clickable { playback.seek(index,0); sheet = null }.padding(horizontal = 8.dp))
                }
            }
        }
        "history" -> PositionHistorySheet(s, playback) { sheet = null }
        "marks" -> ModalBottomSheet(onDismissRequest = { sheet = null }) {
            Text("Deine Stellen",Modifier.padding(24.dp),style = MaterialTheme.typography.headlineSmall)
            BookmarkScreen(s.bookmarks.filter { it.bookmark.bookId == s.album?.id },Modifier.heightIn(max = 520.dp),
                onOpen = { playback.openBookmark(it); sheet = null },onRename = { rename = it },onDelete = playback::deleteBookmark)
        }
    }
    rename?.let { mark ->
        var name by remember(mark.id) { mutableStateOf(mark.label.orEmpty()) }
        AlertDialog(onDismissRequest = { rename = null },title = { Text("Lesezeichen benennen") },
            text = { OutlinedTextField(name,{ name = it.take(120) },label = { Text("Name") },singleLine = true) },
            confirmButton = { TextButton(onClick = { playback.renameBookmark(mark.id,name); rename = null }) { Text("Speichern") } },
            dismissButton = { TextButton(onClick = { rename = null }) { Text("Abbrechen") } })
    }
    remove?.let { book -> AlertDialog(onDismissRequest = { remove = null },title = { Text("Hörbuch entfernen?") },
        text = { Text("Fortschritt und Lesezeichen für dieses Hörbuch werden auf diesem Gerät entfernt.") },
        confirmButton = { TextButton(onClick = { playback.removeBook(book.id); remove = null }) { Text("Entfernen") } },
        dismissButton = { TextButton(onClick = { remove = null }) { Text("Behalten") } }) }
    if (restart) AlertDialog(onDismissRequest = { restart = false },title = { Text("Von vorne hören?") },
        text = { Text("Der Hörfortschritt beginnt wieder am Anfang. Deine Lesezeichen bleiben erhalten.") },
        confirmButton = { TextButton(onClick = { playback.restartBook(play = true); restart = false }) { Text("Von vorne starten") } },
        dismissButton = { TextButton(onClick = { restart = false }) { Text("Abbrechen") } })

}

@Composable
internal fun LibraryScreen(s: ProbeState, modifier: Modifier, onOpen: (LibraryBook,Boolean) -> Unit, onAdd: () -> Unit, onRemove: (LibraryBook) -> Unit, onConnect: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("Alle") }
    var query by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    val prefs = LocalContext.current.getSharedPreferences("library-view", android.content.Context.MODE_PRIVATE)
    var listView by rememberSaveable { mutableStateOf(prefs.getBoolean("listView", false)) }
    var sortName by rememberSaveable { mutableStateOf(prefs.getString("sort", BookSort.LAST_PLAYED.name)!!) }
    val sort = BookSort.entries.firstOrNull { it.name == sortName } ?: BookSort.LAST_PLAYED
    var sortMenu by remember { mutableStateOf(false) }
    val books = remember(s.books, query, filter, sort) { filterBooks(s.books, query, filter, sort) }
    val continueBook = s.books.filter { !it.completed && it.elapsedMs > 0 }.maxByOrNull { it.lastPlayedAt }
    val fontScale = LocalDensity.current.fontScale
    val gridState = rememberLazyGridState()
    val hasOverflow by remember { derivedStateOf { gridState.canScrollForward || gridState.canScrollBackward } }
    LazyVerticalGrid(columns = if (listView) GridCells.Fixed(1) else GridCells.Adaptive((156 * fontScale.coerceAtLeast(1f)).dp),
        modifier = modifier.fillMaxSize().testTag("library-scroll"),state = gridState,userScrollEnabled = hasOverflow,
        contentPadding = PaddingValues(start = 20.dp,end = 20.dp,top = 8.dp,bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),verticalArrangement = Arrangement.spacedBy(if (listView) 8.dp else 18.dp)) {
        if (!s.libraryReady) item(key = "loading",span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (s.libraryReady && s.books.isEmpty()) item(key = "welcome",span = { GridItemSpan(maxLineSpan) }) {
            GettingStartedCard(s.loggedIn, onConnect, onAdd)
        }
        if (continueBook != null && filter == "Alle" && query.isBlank()) item(key = "continue",span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Weiterhören",style = MaterialTheme.typography.titleMedium)
                ContinueCard(continueBook,{ onOpen(continueBook,false) },{ onOpen(continueBook,true) },s.showPlaybackWave && s.album?.id == continueBook.id)
            }
        }
        item(key = "filters",span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Alle","Angefangen","Nicht begonnen","Beendet").forEach { label ->
                    ExpressiveChoice(selected = filter == label,onClick = { filter = label },label = { Text(label) },
                        leadingIcon = if (filter == label) ({ Icon(Icons.Rounded.Check,null,Modifier.size(16.dp)) }) else null)
                }
            }
        }
        if (s.books.isNotEmpty()) item(key = "search",span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconToggleButton(checked = listView, onCheckedChange = {
                        listView = it
                        prefs.edit().putBoolean("listView", it).apply()
                    }, modifier = Modifier.semantics { stateDescription = if (listView) "Listenansicht" else "Kachelansicht" }) {
                        Icon(if (listView) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                            if (listView) "Kachelansicht anzeigen" else "Listenansicht anzeigen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        searchExpanded = !searchExpanded
                        if (!searchExpanded) query = ""
                    }) { Icon(if (searchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                        if (searchExpanded) "Suche schließen" else "Bibliothek durchsuchen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, "Sortieren: ${sort.label}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(sortMenu, { sortMenu = false }) {
                            BookSort.entries.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = {
                                sortName = option.name; prefs.edit().putString("sort", sortName).apply(); sortMenu = false
                            }, leadingIcon = { if (sort == option) Icon(Icons.Rounded.Check, null) }) }
                        }
                    }
                }
                if (searchExpanded) {
                    OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Titel oder Autor suchen") }, leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Suche löschen") } })
                }
            }
        }
        if (books.isEmpty() && s.books.isNotEmpty()) item(key = "empty",span = { GridItemSpan(maxLineSpan) }) {
            EmptyState(Icons.Rounded.Headphones,if (query.isNotBlank()) "Keine passenden Hörbücher" else "Hier ist noch Platz",
                "Ändere den Suchtext oder wähle einen anderen Statusfilter.") {
                TextButton(onClick = { query = ""; filter = "Alle" }) { Text("Suche und Filter zurücksetzen") }
            }
        }
        items(books,key = { "book:${it.id}" }) { book ->
            if (listView) {
                BookListRow(book,
                    Modifier.animateItem().testTag("library-list-${book.id}"),
                    onOpen = { onOpen(book, false) }, onRemove = { onRemove(book) })
            } else {
                var menu by remember { mutableStateOf(false) }
                val interactions = remember { MutableInteractionSource() }
                Column(Modifier.animateItem().testTag("library-grid-${book.id}").fillMaxWidth().pressScale(interactions)
                    .shadow(2.dp,RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(interactionSource = interactions,indication = ripple()) { onOpen(book,false) }.padding(12.dp)) {
                    Box {
                        BookArtwork(book.coverUrl,Modifier.fillMaxWidth().aspectRatio(1f))
                        if (book.completed) Surface(color = MaterialTheme.colorScheme.primaryContainer,shape = CircleShape,modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                            Icon(Icons.Rounded.Check,null,Modifier.padding(6.dp).size(18.dp))
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp),verticalAlignment = Alignment.Top) {
                        Text(book.title,Modifier.weight(1f),style = MaterialTheme.typography.titleSmall,maxLines = 3,overflow = TextOverflow.Ellipsis)
                        Box {
                            IconButton(onClick = { menu = true },modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.MoreVert,"Optionen für ${book.title}",Modifier.size(20.dp)) }
                            DropdownMenu(expanded = menu,onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Aus Bibliothek entfernen") },onClick = { menu = false; onRemove(book) },leadingIcon = { Icon(Icons.Rounded.DeleteOutline,null) })
                            }
                        }
                    }
                    Text(if (book.completed) "Beendet" else if (book.elapsedMs > 0) "${(book.fraction*100).toInt()} % · ${durationLabel(book.durationMs-book.elapsedMs)} übrig" else durationLabel(book.durationMs),
                        style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant,modifier = Modifier.padding(top = 8.dp))
                    if (book.elapsedMs > 0) SoftProgress(value = book.fraction,playing = s.showPlaybackWave && s.album?.id == book.id,modifier = Modifier.fillMaxWidth().padding(top = 12.dp,bottom = 4.dp).height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun BookListRow(book: LibraryBook, modifier: Modifier,
    onOpen: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Surface(onClick = onOpen, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(start = 12.dp, top = 10.dp, end = 0.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BookArtwork(book.coverUrl, Modifier.size(90.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (book.completed) "Beendet" else if (book.elapsedMs > 0)
                    "${durationLabel(book.durationMs-book.elapsedMs)} verbleibend"
                    else durationLabel(book.durationMs),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Optionen für ${book.title}") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Aus Bibliothek entfernen") },
                        onClick = { menu = false; onRemove() }, leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) })
                }
            }
        }
    }
}

@Composable
private fun ContinueCard(book: LibraryBook, onOpen: () -> Unit, onPlay: () -> Unit, playing: Boolean) {
    val interactions = remember { MutableInteractionSource() }
    ElevatedCard(onClick = onOpen,modifier = Modifier.pressScale(interactions),interactionSource = interactions,shape = RoundedCornerShape(28.dp),colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),elevation = CardDefaults.elevatedCardElevation(2.dp)) {
        Row(Modifier.padding(16.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BookArtwork(book.coverUrl,Modifier.size(82.dp))
            Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(book.title,style = MaterialTheme.typography.titleMedium,maxLines = 2,overflow = TextOverflow.Ellipsis)
                Text("${durationLabel(book.durationMs-book.elapsedMs)} verbleibend",style = MaterialTheme.typography.bodySmall)
                SoftProgress(value = book.fraction,playing = playing,modifier = Modifier.fillMaxWidth().height(12.dp))
            }
            MotionIconButton(onClick = onPlay,modifier = Modifier.size(48.dp),prominent = true) { Icon(Icons.Rounded.PlayArrow,"Fortsetzen") }
        }
    }
}

@Composable
private fun MiniPlayer(s: ProbeState, onOpen: () -> Unit, onPlay: () -> Unit, embedded: Boolean = false) {
    val album = s.album ?: return
    val total = album.tracks.sumOf { it.durationMs }
    val fraction = if (total > 0) elapsedBookMs(album.tracks,s.index,s.positionMs).toFloat()/total else 0f
    Surface(onClick = onOpen,color = MaterialTheme.colorScheme.surfaceContainerHigh,shape = RoundedCornerShape(24.dp),modifier = if (embedded) Modifier else Modifier.padding(horizontal = 12.dp,vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BookArtwork(album.coverUrl, Modifier.size(48.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall)
                Text(if (s.completed) "Beendet" else "Abschnitt ${s.index+1} · ${clock(s.positionMs)}",
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                SoftProgress(value = fraction, playing = s.showPlaybackWave,
                    modifier = Modifier.fillMaxWidth().height(8.dp))
            }
            MotionIconButton(onClick = onPlay, modifier = Modifier.size(48.dp)) {
                Crossfade(s.requestedPlay, animationSpec = tween(180), label = "Mini-Player Wiedergabe") { playing ->
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (playing) "Pause" else "Fortsetzen", Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
internal fun PlayerScreen(s: ProbeState,onBack: () -> Unit,onPlay: () -> Unit,onSkip: (Long) -> Unit,onSeek: (Long) -> Unit,
    onBookmark: () -> Unit,onTimer: () -> Unit,onChapters: () -> Unit,onMarks: () -> Unit,onFinished: () -> Unit,onRestart: () -> Unit,onHistory: () -> Unit,onUndo: () -> Unit,onRetry: () -> Unit,snackbar: SnackbarHostState) {
    val album = s.album ?: return
    val total = album.tracks.sumOf { it.durationMs }
    val elapsed = elapsedBookMs(album.tracks,s.index,s.positionMs)
    var chapterMode by rememberSaveable { mutableStateOf(false) }
    val track = album.tracks.getOrNull(s.index)
    val seekDuration = if (chapterMode) track?.durationMs ?: total else total
    val seekElapsed = if (chapterMode) s.positionMs else elapsed
    val seekBase = if (chapterMode) elapsedBookMs(album.tracks, s.index, 0) else 0L
    var dragging by remember(album.id, s.index, chapterMode) { mutableStateOf<Float?>(null) }
    var menu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var bookmarkFeedback by remember { mutableIntStateOf(0) }
    var bookmarkConfirmed by remember { mutableStateOf(false) }
    var bookmarkBaseline by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(s.bookmarks,bookmarkBaseline) {
        bookmarkBaseline?.let { baseline ->
            if (s.bookmarks.any { it.bookmark.bookId == album.id && it.bookmark.id !in baseline }) {
                bookmarkBaseline = null
                bookmarkFeedback++
            }
        }
    }
    LaunchedEffect(bookmarkFeedback) {
        if (bookmarkFeedback > 0) { bookmarkConfirmed = true; delay(1100); bookmarkConfirmed = false }
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(.72f)) {
            BookArtwork(album.coverUrl, Modifier.fillMaxSize(), backdrop = true)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to MaterialTheme.colorScheme.surface.copy(alpha = .75f),
                .45f to MaterialTheme.colorScheme.surface.copy(alpha = .86f),
                1f to MaterialTheme.colorScheme.surface)))
        }
        Scaffold(containerColor = Color.Transparent,
            topBar = { CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            title = { Column(horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(width = 32.dp,height = 4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                Text("JETZT HÖREN",style = MaterialTheme.typography.labelMedium,color = MaterialTheme.colorScheme.onSurfaceVariant)
            } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.KeyboardArrowDown,"Player minimieren") } },actions = {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert,"Hörbuchoptionen") }
                    DropdownMenu(expanded = menu,onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Positionsverlauf") },onClick = { menu = false; onHistory() },leadingIcon = { Icon(Icons.Rounded.History,null) })
                        DropdownMenuItem(text = { Text("Von vorne hören") },onClick = { menu = false; onRestart() },leadingIcon = { Icon(Icons.Rounded.Replay,null) })
                        DropdownMenuItem(text = { Text("Als beendet markieren") },onClick = { menu = false; onFinished() },leadingIcon = { Icon(Icons.Rounded.CheckCircle,null) })
                        DropdownMenuItem(text = { Text("Bei TIDAL öffnen") },onClick = { menu = false; context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://tidal.com/browse/album/${album.id}"))) },leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew,null) })
                    }
                }
            }) },
            bottomBar = {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(topStart = 28.dp,topEnd = 28.dp),
                    shadowElevation = 8.dp,tonalElevation = 3.dp) {
                    FlowRow(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp,vertical = 12.dp),
                        maxItemsInEachRow = if (LocalDensity.current.fontScale > 1.3f) 2 else 4,
                        horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlayerAction(if (bookmarkConfirmed) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkAdd,if (bookmarkConfirmed) "Gemerkt" else "Merken",{
                            bookmarkBaseline = s.bookmarks.map { it.bookmark.id }.toSet()
                            onBookmark()
                        },bookmarkConfirmed)
                        PlayerAction(Icons.Rounded.Bedtime,if(s.sleepSession?.active == true) s.timerRemainingMs?.let { clock(it) } ?: "Trackende" else "Timer",onTimer,s.sleepSession?.active == true)
                        PlayerAction(Icons.AutoMirrored.Rounded.FormatListBulleted,"Abschnitte",onChapters)
                        PlayerAction(Icons.Rounded.Bookmarks,"Stellen",onMarks)
                    }
                }
            },snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding = PaddingValues(start = 24.dp,end = 24.dp,top = 6.dp,bottom = 24.dp),horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { PlayerCover(album.coverUrl,s.requestedPlay && !s.loading) }
                item {
                    Column(Modifier.fillMaxWidth(),verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(album.title,style = MaterialTheme.typography.headlineSmall,fontWeight = FontWeight.Medium,maxLines = 4,overflow = TextOverflow.Ellipsis)
                        if (album.author.isNotBlank()) Text(album.author,style = MaterialTheme.typography.bodyMedium,color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AnimatedContent(targetState = if (s.completed) "Beendet" else "Abschnitt ${s.index+1} von ${album.tracks.size}",
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },label = "Abschnittwechsel") { label ->
                            Text(label,style = MaterialTheme.typography.labelMedium,color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !chapterMode, onClick = { chapterMode = false }, label = { Text("Ganzes Buch") })
                            FilterChip(selected = chapterMode, onClick = { chapterMode = true }, label = { Text("Abschnitt") })
                        }
                        ExpressiveSeekBar(playing = s.showPlaybackWave,value = dragging ?: (seekElapsed.toFloat()/seekDuration.coerceAtLeast(1)).coerceIn(0f,1f),onValueChange = { dragging = it },onValueChangeFinished = {
                            dragging?.let { onSeek(seekBase + (it*seekDuration).toLong().coerceAtMost((seekDuration-1).coerceAtLeast(0))) }; dragging = null
                        },modifier = Modifier.semantics { contentDescription = if (chapterMode) "Position im Abschnitt" else "Position im Hörbuch"
                            stateDescription = "${clock(seekElapsed)} von ${clock(seekDuration)}" })
                        Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(clock(if (dragging == null) seekElapsed else ((dragging ?: 0f)*seekDuration).toLong()),style = MaterialTheme.typography.labelMedium)
                            Text("−${clock((seekDuration-(dragging?.let { (it*seekDuration).toLong() } ?: seekElapsed)).coerceAtLeast(0))}",style = MaterialTheme.typography.labelMedium,color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceEvenly,verticalAlignment = Alignment.CenterVertically) {
                        MotionIconButton(onClick = { onSkip(-s.skipSeconds * 1000L) },modifier = Modifier.size(60.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.Replay,"${s.skipSeconds} Sekunden zurück",Modifier.size(26.dp)); Text("${s.skipSeconds} s",style = MaterialTheme.typography.labelSmall) } }
                        ExpressivePlayButton(playing = s.requestedPlay,onClick = onPlay) {
                            Crossfade(targetState = if (s.loading) "loading" else if (s.requestedPlay) "pause" else "play",
                                animationSpec = tween(150),label = "Wiedergabetaste") { state ->
                                Box(Modifier.size(42.dp),contentAlignment = Alignment.Center) {
                                    if (state == "loading") ExpressiveLoadingIndicator(Modifier.size(30.dp),color = MaterialTheme.colorScheme.onPrimary,strokeWidth = 3.dp)
                                    else Icon(if (state == "pause") Icons.Rounded.Pause else Icons.Rounded.PlayArrow,if (state == "pause") "Pause" else "Abspielen",Modifier.size(42.dp))
                                }
                            }
                        }
                        MotionIconButton(onClick = { onSkip(s.skipSeconds * 1000L) },modifier = Modifier.size(60.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.Replay,"${s.skipSeconds} Sekunden vor",Modifier.size(26.dp).scale(scaleX = -1f,scaleY = 1f)); Text("${s.skipSeconds} s",style = MaterialTheme.typography.labelSmall) } }
                    }
                }
                s.playbackProblem?.let { problem -> item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(problem, color = MaterialTheme.colorScheme.onErrorContainer)
                            TextButton(onClick = onRetry, enabled = !s.busy) { Text("Wiedergabe erneut versuchen") }
                        }
                    }
                } }
                if (s.history.any { it.bookId == album.id }) item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onUndo, enabled = !s.busy) { Text("Letzten Sprung rückgängig") }
                        TextButton(onClick = onHistory) { Text("Positionsverlauf") }
                    }
                }
                s.sleepSession?.let { item {
                    Surface(onClick = { (context.applicationContext as ProbeApp).probe.returnToSleepStart() },shape = RoundedCornerShape(22.dp),color = MaterialTheme.colorScheme.surfaceContainer) {
                        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.History,null,tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text("Zur Stelle vor dem Einschlafen",style = MaterialTheme.typography.titleSmall)
                                Text("Beginn deiner letzten Einschlafsitzung",style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Rounded.ChevronRight,null)
                        }
                    }
                } }
            }
        }
    }
}

@Composable
private fun PlayerAction(icon: ImageVector,label: String,onClick: () -> Unit,selected: Boolean = false) {
    val containerColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(180),label = "Timerstatus")
    val interactions = remember { MutableInteractionSource() }
    Column(horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FilledTonalIconButton(onClick = onClick,shapes = IconButtonDefaults.shapes(),interactionSource = interactions,modifier = Modifier.size(52.dp).pressScale(interactions),colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor)) {
            Crossfade(icon,animationSpec = tween(180),label = "Aktionssymbol") { value -> Icon(value,label) }
        }
        Text(label,style = MaterialTheme.typography.labelSmall, modifier = Modifier.widthIn(max = 120.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun BookmarkScreen(marks: List<BookmarkItem>,modifier: Modifier,onOpen: (StoredBookmark) -> Unit,onRename: (StoredBookmark) -> Unit,onDelete: (String) -> Unit) {
    LazyColumn(modifier.fillMaxWidth(),contentPadding = PaddingValues(20.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (marks.isEmpty()) item { EmptyState(Icons.Rounded.Bookmarks,"Noch keine Lesezeichen","Merke dir eine Stelle mit einem Tipp auf „Merken“ im Player.") {} }
        items(marks,key = { it.bookmark.id }) { item ->
            val mark = item.bookmark
            var menu by remember { mutableStateOf(false) }
            Surface(onClick = { onOpen(mark) },modifier = Modifier.animateItem(),shape = RoundedCornerShape(24.dp),color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BookArtwork(item.bookCover,Modifier.size(56.dp))
                    Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(mark.label ?: "Stelle bei ${clock(mark.elapsedMs)}",style = MaterialTheme.typography.titleMedium,maxLines = 2,overflow = TextOverflow.Ellipsis)
                        Text(item.bookTitle,style = MaterialTheme.typography.bodySmall,maxLines = 1,overflow = TextOverflow.Ellipsis,color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${clock(mark.elapsedMs)} · ${SimpleDateFormat("dd.MM. · HH:mm",Locale.GERMAN).format(Date(mark.createdAt))}",style = MaterialTheme.typography.labelSmall,color = MaterialTheme.colorScheme.primary)
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert,"Lesezeichenoptionen") }
                        DropdownMenu(expanded = menu,onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Benennen") },onClick = { menu = false; onRename(mark) },leadingIcon = { Icon(Icons.Rounded.Edit,null) })
                            DropdownMenuItem(text = { Text("Löschen") },onClick = { menu = false; onDelete(mark.id) },leadingIcon = { Icon(Icons.Rounded.DeleteOutline,null) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerSheet(s: ProbeState,playback: PlaybackProbe,onDismiss: () -> Unit) {
    var custom by rememberSaveable { mutableStateOf(s.lastTimerMinutes.toString()) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp).imePadding(),verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Bedtime,null,tint = MaterialTheme.colorScheme.primary,modifier = Modifier.size(30.dp))
                Text("Einschlaftimer",style = MaterialTheme.typography.headlineSmall)
            }
            Text("In den letzten 5 Sekunden wird es langsam leiser. Danach wird pausiert und mindestens 5 Sekunden zurückgesprungen (höchstens bis zum Buchanfang). Die Stelle vom Timerstart bleibt zusätzlich gespeichert.",color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (s.sleepSession?.active == true) Surface(color = MaterialTheme.colorScheme.secondaryContainer,shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(s.timerRemainingMs?.let { clock(it) } ?: "Am Trackende",style = MaterialTheme.typography.displaySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (s.timerRemainingMs != null) ExpressiveTonalButton(onClick = playback::extendTimer) { Text("+10 Minuten") }
                        TextButton(onClick = playback::cancelTimer) { Text("Timer ausschalten") }
                    }
                }
            }
            ExpressiveButton(onClick = { playback.startTimer(s.lastTimerMinutes); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                Text("Wie zuletzt: ${s.lastTimerMinutes} Minuten")
            }
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(8.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15,30,45,60).forEach { minutes ->
                    ExpressiveTonalButton(onClick = { playback.startTimer(minutes); onDismiss() },modifier = Modifier,contentPadding = PaddingValues(horizontal = 16.dp,vertical = 14.dp)) { Text("$minutes Min.") }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(custom,{ custom = it.filter(Char::isDigit).take(3) },modifier = Modifier.weight(1f),singleLine = true,
                    label = { Text("Eigene Dauer") },suffix = { Text("Min.") },keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),shape = RoundedCornerShape(18.dp))
                ExpressiveButton(onClick = { playback.startTimer(custom.toIntOrNull() ?: 0); if ((custom.toIntOrNull() ?: 0) in 1..180) onDismiss() },modifier = Modifier.height(56.dp)) { Text("Starten") }
            }
            OutlinedButton(onClick = { playback.trackEndTimer(); onDismiss() },modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Icon(Icons.AutoMirrored.Rounded.LastPage,null); Spacer(Modifier.width(8.dp)); Text("Am Ende dieses Tracks")
            }
        }
    }
}

@Composable
private fun SettingsScreen(s: ProbeState,modifier: Modifier,playback: PlaybackProbe) {
    val context = LocalContext.current
    LazyColumn(modifier.fillMaxSize(),contentPadding = PaddingValues(20.dp),verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item(key = "listening") { ListeningSettingsCard(s.resumeRewindSeconds, playback::setResumeRewind) }
        item(key = "appearance") { ThemeSettingsCard() }
        item(key = "skip") { SettingsCard(Icons.Rounded.Replay,"Sprungweite","Gilt für Vor- und Zurückspringen im Player, im Widget und in der Mediensteuerung.") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 15, 30, 60).forEach { seconds ->
                    FilterChip(selected = s.skipSeconds == seconds, onClick = { playback.setSkipSeconds(seconds) },
                        label = { Text("$seconds Sekunden") })
                }
            }
        } }
        item(key = "widget") { SettingsCard(Icons.Rounded.Widgets,"Homescreen-Widget","Start/Pause, ${s.skipSeconds} Sekunden zurück, Lesezeichen und dein letzter Einschlaftimer direkt auf dem Startbildschirm.") {
            WidgetPreview(WidgetSnapshot.from(s),Modifier.fillMaxWidth().height(160.dp))
            val manager = AppWidgetManager.getInstance(context)
            if (manager.isRequestPinAppWidgetSupported) TextButton(onClick = {
                manager.requestPinAppWidget(ComponentName(context,AudiobookWidget::class.java),null,null)
            }) { Text("Widget hinzufügen") }
            else Text("Startbildschirm lange drücken → Widgets → Hörbuch.",style = MaterialTheme.typography.bodySmall)
        } }
        item(key = "backup") { BackupSettingsCard(s, playback) }
        item {
            Surface(shape = RoundedCornerShape(28.dp),color = MaterialTheme.colorScheme.secondaryContainer) {
                Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(if (s.loggedIn) Icons.Rounded.CheckCircle else Icons.Rounded.Headphones,null,Modifier.size(32.dp))
                    Text(if (s.loggedIn) "Mit TIDAL verbunden" else "Deine Hörbücher auf TIDAL",style = MaterialTheme.typography.headlineSmall)
                    Text(if (s.loggedIn) "Dein Konto ist bereit. Fortschritt und Lesezeichen bleiben auf diesem Gerät." else "Verbinde dein Konto und höre deine Alben als Hörbücher weiter.")
                    ExpressiveButton(onClick = { playback.beginAlternativeLogin() },enabled = !s.busy) { Text(if (s.loggedIn) "TIDAL neu verbinden" else "Mit TIDAL verbinden") }
                    s.alternativeCode?.let { Text(it,style = MaterialTheme.typography.displaySmall,fontWeight = FontWeight.Medium) }
                    s.alternativeUrl?.let { url ->
                        Text("Gerätecode bei TIDAL bestätigen und danach per Zurück-Geste hierher wechseln.")
                        ExpressiveButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) }) { Text("Bei TIDAL bestätigen") }
                        TextButton(onClick = playback::cancelAlternativeLogin) { Text("Abbrechen") }
                    }
                }
            }
        }
        item { SettingsCard(Icons.Rounded.Notifications,"Medienbenachrichtigung","Steuere die Wiedergabe über die Benachrichtigungen, den Sperrbildschirm oder deine Kopfhörer.") {
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)) }) { Text("Benachrichtigungen öffnen") }
        } }
        item { SettingsCard(Icons.Rounded.PhoneAndroid,"Auf diesem Gerät gespeichert","${s.books.size} Hörbücher · ${s.bookmarks.size} Lesezeichen. Deine Bibliothek benötigt keinen zusätzlichen Account und keinen eigenen Server.") {} }
        item { Text("Hörbuch ${BuildConfig.VERSION_NAME}\nWiedergabe und Katalog von TIDAL",style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant,modifier = Modifier.padding(8.dp)) }
    }
}

@Composable
private fun SettingsCard(icon: ImageVector,title: String,text: String,actions: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp),color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp),verticalAlignment = Alignment.CenterVertically) {
                Icon(icon,null,tint = MaterialTheme.colorScheme.primary)
                Text(title,style = MaterialTheme.typography.titleMedium)
            }
            Text(text,style = MaterialTheme.typography.bodyMedium,color = MaterialTheme.colorScheme.onSurfaceVariant)
            actions()
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector,title: String,text: String,action: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp,vertical = 36.dp),horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = RoundedCornerShape(32.dp),color = MaterialTheme.colorScheme.secondaryContainer) { Icon(icon,null,Modifier.padding(24.dp).size(48.dp),tint = MaterialTheme.colorScheme.onSecondaryContainer) }
        Text(title,style = MaterialTheme.typography.titleLarge)
        Text(text,style = MaterialTheme.typography.bodyMedium,color = MaterialTheme.colorScheme.onSurfaceVariant)
        action()
    }
}
