@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.tidalbooks.prototype

import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun AudiobookTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors = if (Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) darkColorScheme(primary = Color(0xFFA1D1C7),surface = Color(0xFF101513))
    else lightColorScheme(primary = Color(0xFF28665C),surface = Color(0xFFF7FBF7))
    MaterialTheme(colorScheme = colors, shapes = Shapes(
        small = RoundedCornerShape(12.dp),medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),extraLarge = RoundedCornerShape(32.dp)),content = content)
}

fun clock(ms: Long): String {
    val seconds = ms.coerceAtLeast(0)/1000
    return if (seconds >= 3600) "%d:%02d:%02d".format(Locale.ROOT,seconds/3600,seconds/60%60,seconds%60)
    else "%d:%02d".format(Locale.ROOT,seconds/60,seconds%60)
}
private fun durationLabel(ms: Long): String {
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
    if (playerOpen && s.album != null) {
        PlayerScreen(s,onBack = { playerOpen = false },onPlay = toggle,onSkip = playback::skip,
            onSeek = playback::seekAbsolute,onBookmark = { playback.addBookmark(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
            onTimer = { sheet = "timer" },onChapters = { sheet = "chapters" },onMarks = { sheet = "marks" },
            onFinished = playback::markCompleted,onRestart = { restart = true },snackbar = snackbar)
    } else {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { LargeTopAppBar(title = { Text(listOf("Bibliothek","Lesezeichen","Einstellungen")[tab],fontWeight = FontWeight.Medium) },
                actions = { if (tab == 0) IconButton(onClick = { sheet = "add" }) { Icon(Icons.Rounded.Add,"Hörbuch hinzufügen") } },scrollBehavior = scrollBehavior) },
            bottomBar = {
                Column {
                    s.album?.let { MiniPlayer(s,{ playerOpen = true },toggle) }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        listOf(Icons.AutoMirrored.Rounded.LibraryBooks,Icons.Rounded.Bookmark,Icons.Rounded.Settings).forEachIndexed { index,icon ->
                            NavigationBarItem(selected = tab == index,onClick = { tab = index },icon = { Icon(icon,null) },
                                label = { Text(listOf("Bibliothek","Lesezeichen","Einstellungen")[index]) })
                        }
                    }
                }
            }, snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = { if (tab == 0 && s.books.isNotEmpty()) ExtendedFloatingActionButton(
                onClick = { sheet = "add" },icon = { Icon(Icons.Rounded.Add,null) },text = { Text("Hinzufügen") }) },
        ) { padding ->
            when(tab) {
                0 -> LibraryScreen(s,Modifier.padding(padding),openBook,{ sheet = "add" },{ remove = it })
                1 -> BookmarkScreen(s.bookmarks,Modifier.padding(padding),onOpen = { playback.openBookmark(it); playerOpen = true },
                    onRename = { rename = it },onDelete = playback::deleteBookmark)
                else -> SettingsScreen(s,Modifier.padding(padding),playback)
            }
        }
    }
    when(sheet) {
        "add" -> ModalBottomSheet(onDismissRequest = { sheet = null }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp).imePadding(),verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    Button(onClick = { sheet = null; tab = 2 },modifier = Modifier.fillMaxWidth()) { Text("Zuerst mit TIDAL verbinden") }
                } else Button(onClick = {
                    val parsed = runCatching { albumId(link) }
                    if (parsed.isFailure) playback.notify("Bitte einen gültigen TIDAL-Albumlink eingeben.")
                    else { playback.dismissMessage(); importingId = parsed.getOrNull(); playback.loadAlbum(link) }
                },enabled = !s.busy && s.libraryReady && link.isNotBlank(),modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    if (s.busy) { CircularProgressIndicator(Modifier.size(20.dp),strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)) }
                    Text(if (s.busy) "Hörbuch wird geladen" else "Zur Bibliothek hinzufügen")
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
private fun LibraryScreen(s: ProbeState, modifier: Modifier, onOpen: (LibraryBook,Boolean) -> Unit, onAdd: () -> Unit, onRemove: (LibraryBook) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("Alle") }
    val books = s.books.filter { filter == "Alle" || it.status == filter }
    val continueBook = s.books.firstOrNull { !it.completed && it.elapsedMs > 0 }
    LazyVerticalGrid(columns = GridCells.Adaptive(156.dp),modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp,end = 20.dp,top = 8.dp,bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (!s.libraryReady) item(span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (continueBook != null && filter == "Alle") item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Weiterhören",style = MaterialTheme.typography.titleMedium)
                ContinueCard(continueBook,{ onOpen(continueBook,false) },{ onOpen(continueBook,true) })
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Alle","Angefangen","Nicht begonnen","Beendet").forEach { label ->
                    FilterChip(selected = filter == label,onClick = { filter = label },label = { Text(label) },
                        leadingIcon = if (filter == label) ({ Icon(Icons.Rounded.Check,null,Modifier.size(16.dp)) }) else null)
                }
            }
        }
        if (books.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            EmptyState(Icons.Rounded.Headphones,if (s.books.isEmpty()) "Deine Bibliothek wartet auf dich" else "Hier ist noch Platz",
                if (s.books.isEmpty()) "Füge dein erstes Hörbuch mit einem TIDAL-Albumlink hinzu." else "In dieser Ansicht gibt es noch keine Hörbücher.") {
                Button(onClick = onAdd) { Icon(Icons.Rounded.Add,null); Spacer(Modifier.width(8.dp)); Text("Hörbuch hinzufügen") }
            }
        }
        items(books,key = { it.id }) { book ->
            var menu by remember { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth().clickable { onOpen(book,false) }) {
                Box {
                    BookArtwork(book.coverUrl,Modifier.fillMaxWidth().aspectRatio(1f))
                    if (book.completed) Surface(color = MaterialTheme.colorScheme.primaryContainer,shape = CircleShape,modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                        Icon(Icons.Rounded.Check,null,Modifier.padding(6.dp).size(18.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp),verticalAlignment = Alignment.Top) {
                    Text(book.title,Modifier.weight(1f),style = MaterialTheme.typography.titleSmall,maxLines = 3,overflow = TextOverflow.Ellipsis)
                    Box {
                        IconButton(onClick = { menu = true },modifier = Modifier.size(28.dp)) { Icon(Icons.Rounded.MoreVert,"Optionen für ${book.title}",Modifier.size(20.dp)) }
                        DropdownMenu(expanded = menu,onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Aus Bibliothek entfernen") },onClick = { menu = false; onRemove(book) },leadingIcon = { Icon(Icons.Rounded.DeleteOutline,null) })
                        }
                    }
                }
                Text(if (book.completed) "Beendet" else if (book.elapsedMs > 0) "${(book.fraction*100).toInt()} % · ${durationLabel(book.durationMs-book.elapsedMs)} übrig" else durationLabel(book.durationMs),
                    style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant,modifier = Modifier.padding(top = 5.dp))
                if (book.elapsedMs > 0) LinearProgressIndicator(progress = { book.fraction },modifier = Modifier.fillMaxWidth().padding(top = 9.dp).height(4.dp))
            }
        }
    }
}

@Composable
private fun ContinueCard(book: LibraryBook, onOpen: () -> Unit, onPlay: () -> Unit) {
    ElevatedCard(onClick = onOpen,shape = RoundedCornerShape(28.dp),colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),elevation = CardDefaults.elevatedCardElevation(0.dp)) {
        Row(Modifier.padding(16.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BookArtwork(book.coverUrl,Modifier.size(82.dp))
            Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(book.title,style = MaterialTheme.typography.titleMedium,maxLines = 2,overflow = TextOverflow.Ellipsis)
                Text("${durationLabel(book.durationMs-book.elapsedMs)} verbleibend",style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { book.fraction },modifier = Modifier.fillMaxWidth().height(4.dp))
            }
            FilledIconButton(onClick = onPlay,modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.PlayArrow,"Fortsetzen") }
        }
    }
}

@Composable
private fun MiniPlayer(s: ProbeState, onOpen: () -> Unit, onPlay: () -> Unit) {
    val album = s.album ?: return
    val total = album.tracks.sumOf { it.durationMs }
    val fraction = if (total > 0) elapsedBookMs(album.tracks,s.index,s.positionMs).toFloat()/total else 0f
    Surface(onClick = onOpen,color = MaterialTheme.colorScheme.surfaceContainerHigh,shape = RoundedCornerShape(24.dp),modifier = Modifier.padding(horizontal = 12.dp,vertical = 6.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().padding(start = 10.dp,top = 10.dp,end = 6.dp,bottom = 8.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BookArtwork(album.coverUrl,Modifier.size(46.dp))
                Column(Modifier.weight(1f)) {
                    Text(album.title,maxLines = 1,overflow = TextOverflow.Ellipsis,style = MaterialTheme.typography.titleSmall)
                    Text(if (s.completed) "Beendet" else "Abschnitt ${s.index+1} · ${clock(s.positionMs)}",style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onPlay) { Icon(if (s.requestedPlay) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,if (s.requestedPlay) "Pause" else "Fortsetzen",Modifier.size(30.dp)) }
            }
            LinearProgressIndicator(progress = { fraction.coerceIn(0f,1f) },modifier = Modifier.fillMaxWidth().height(3.dp))
        }
    }
}

@Composable
private fun PlayerScreen(s: ProbeState,onBack: () -> Unit,onPlay: () -> Unit,onSkip: (Long) -> Unit,onSeek: (Long) -> Unit,
    onBookmark: () -> Unit,onTimer: () -> Unit,onChapters: () -> Unit,onMarks: () -> Unit,onFinished: () -> Unit,onRestart: () -> Unit,snackbar: SnackbarHostState) {
    val album = s.album ?: return
    val total = album.tracks.sumOf { it.durationMs }
    val elapsed = elapsedBookMs(album.tracks,s.index,s.positionMs)
    val fraction = if (total > 0) (elapsed.toFloat()/total).coerceIn(0f,1f) else 0f
    var dragging by remember(album.id) { mutableStateOf<Float?>(null) }
    var menu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Jetzt hören",style = MaterialTheme.typography.titleMedium) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack,"Zur Bibliothek") } },actions = {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert,"Hörbuchoptionen") }
                DropdownMenu(expanded = menu,onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Von vorne hören") },onClick = { menu = false; onRestart() },leadingIcon = { Icon(Icons.Rounded.Replay,null) })
                    DropdownMenuItem(text = { Text("Als beendet markieren") },onClick = { menu = false; onFinished() },leadingIcon = { Icon(Icons.Rounded.CheckCircle,null) })
                    DropdownMenuItem(text = { Text("Bei TIDAL öffnen") },onClick = { menu = false; context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://tidal.com/browse/album/${album.id}"))) },leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew,null) })
                }
            }
        }) },snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding = PaddingValues(start = 28.dp,end = 28.dp,top = 14.dp,bottom = 28.dp),horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { BookArtwork(album.coverUrl,Modifier.size(236.dp),large = true) }
            item {
                Column(Modifier.fillMaxWidth(),verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(album.title,style = MaterialTheme.typography.headlineSmall,fontWeight = FontWeight.Medium,maxLines = 4,overflow = TextOverflow.Ellipsis)
                    if (album.author.isNotBlank()) Text(album.author,style = MaterialTheme.typography.bodyMedium,color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("TIDAL · ${if(s.completed) "Beendet" else "Abschnitt ${s.index+1} von ${album.tracks.size}"}",style = MaterialTheme.typography.labelMedium,color = MaterialTheme.colorScheme.primary)
                }
            }
            item {
                Column(Modifier.fillMaxWidth()) {
                    Slider(value = dragging ?: fraction,onValueChange = { dragging = it },onValueChangeFinished = {
                        dragging?.let { onSeek((it*total).toLong()) }; dragging = null
                    },modifier = Modifier.semantics { contentDescription = "Position im Hörbuch" })
                    Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(clock(if (dragging == null) elapsed else ((dragging ?: 0f)*total).toLong()),style = MaterialTheme.typography.labelMedium)
                        Text("−${clock((total-elapsed).coerceAtLeast(0))}",style = MaterialTheme.typography.labelMedium,color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceEvenly,verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onSkip(-30_000) },modifier = Modifier.size(60.dp)) { Icon(Icons.Rounded.Replay30,"30 Sekunden zurück",Modifier.size(34.dp)) }
                    FilledIconButton(onClick = onPlay,shape = RoundedCornerShape(28.dp),modifier = Modifier.size(width = 88.dp,height = 76.dp)) {
                        if (s.loading) CircularProgressIndicator(Modifier.size(30.dp),color = MaterialTheme.colorScheme.onPrimary,strokeWidth = 3.dp)
                        else Icon(if (s.requestedPlay) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,if (s.requestedPlay) "Pause" else "Abspielen",Modifier.size(42.dp))
                    }
                    IconButton(onClick = { onSkip(30_000) },modifier = Modifier.size(60.dp)) { Icon(Icons.Rounded.Forward30,"30 Sekunden vor",Modifier.size(34.dp)) }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceEvenly) {
                    PlayerAction(Icons.Rounded.BookmarkAdd,"Merken",onBookmark)
                    PlayerAction(Icons.Rounded.Bedtime,if(s.sleepSession?.active == true) s.timerRemainingMs?.let { clock(it) } ?: "Trackende" else "Timer",onTimer,s.sleepSession?.active == true)
                    PlayerAction(Icons.AutoMirrored.Rounded.FormatListBulleted,"Abschnitte",onChapters)
                    PlayerAction(Icons.Rounded.Bookmarks,"Stellen",onMarks)
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

@Composable
private fun PlayerAction(icon: ImageVector,label: String,onClick: () -> Unit,selected: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FilledTonalIconButton(onClick = onClick,modifier = Modifier.size(52.dp),colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Icon(icon,label)
        }
        Text(label,style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun BookmarkScreen(marks: List<BookmarkItem>,modifier: Modifier,onOpen: (StoredBookmark) -> Unit,onRename: (StoredBookmark) -> Unit,onDelete: (String) -> Unit) {
    LazyColumn(modifier.fillMaxWidth(),contentPadding = PaddingValues(20.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (marks.isEmpty()) item { EmptyState(Icons.Rounded.Bookmarks,"Noch keine Lesezeichen","Merke dir eine Stelle mit einem Tipp auf „Merken“ im Player.") {} }
        items(marks,key = { it.bookmark.id }) { item ->
            val mark = item.bookmark
            var menu by remember { mutableStateOf(false) }
            Surface(onClick = { onOpen(mark) },shape = RoundedCornerShape(24.dp),color = MaterialTheme.colorScheme.surfaceContainerLow) {
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
    var custom by rememberSaveable { mutableStateOf("20") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp).imePadding(),verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Bedtime,null,tint = MaterialTheme.colorScheme.primary,modifier = Modifier.size(30.dp))
                Text("Einschlaftimer",style = MaterialTheme.typography.headlineSmall)
            }
            Text("Nach Ablauf wird pausiert. Die Stelle vom Timerstart bleibt für morgen gespeichert.",color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (s.sleepSession?.active == true) Surface(color = MaterialTheme.colorScheme.secondaryContainer,shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(s.timerRemainingMs?.let { clock(it) } ?: "Am Trackende",style = MaterialTheme.typography.displaySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (s.timerRemainingMs != null) FilledTonalButton(onClick = playback::extendTimer) { Text("+10 Minuten") }
                        TextButton(onClick = playback::cancelTimer) { Text("Timer ausschalten") }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15,30,45,60).forEach { minutes ->
                    FilledTonalButton(onClick = { playback.startTimer(minutes); onDismiss() },modifier = Modifier.weight(1f),contentPadding = PaddingValues(horizontal = 4.dp,vertical = 14.dp)) { Text("$minutes Min.") }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(custom,{ custom = it.filter(Char::isDigit).take(3) },modifier = Modifier.weight(1f),singleLine = true,
                    label = { Text("Eigene Dauer") },suffix = { Text("Min.") },keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),shape = RoundedCornerShape(18.dp))
                Button(onClick = { playback.startTimer(custom.toIntOrNull() ?: 0); if ((custom.toIntOrNull() ?: 0) in 1..180) onDismiss() },modifier = Modifier.height(56.dp)) { Text("Starten") }
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
        item {
            Surface(shape = RoundedCornerShape(28.dp),color = MaterialTheme.colorScheme.secondaryContainer) {
                Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(if (s.loggedIn) Icons.Rounded.CheckCircle else Icons.Rounded.Headphones,null,Modifier.size(32.dp))
                    Text(if (s.loggedIn) "Mit TIDAL verbunden" else "Deine Hörbücher auf TIDAL",style = MaterialTheme.typography.headlineSmall)
                    Text(if (s.loggedIn) "Dein Konto ist bereit. Fortschritt und Lesezeichen bleiben auf diesem Gerät." else "Verbinde dein Konto und höre deine Alben als Hörbücher weiter.")
                    Button(onClick = { playback.beginAlternativeLogin() },enabled = !s.busy) { Text(if (s.loggedIn) "TIDAL neu verbinden" else "Mit TIDAL verbinden") }
                    s.alternativeCode?.let { Text(it,style = MaterialTheme.typography.displaySmall,fontWeight = FontWeight.Medium) }
                    s.alternativeUrl?.let { url ->
                        Text("Gerätecode bei TIDAL bestätigen und danach per Zurück-Geste hierher wechseln.")
                        Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) }) { Text("Bei TIDAL bestätigen") }
                        TextButton(onClick = playback::cancelAlternativeLogin) { Text("Abbrechen") }
                    }
                }
            }
        }
        item { SettingsCard(Icons.Rounded.Notifications,"Medienbenachrichtigung","Steuere die Wiedergabe über die Benachrichtigungen, den Sperrbildschirm oder deine Kopfhörer.") {
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)) }) { Text("Benachrichtigungen öffnen") }
        } }
        item { SettingsCard(Icons.Rounded.Palette,"Dein Pixel, deine Farben","Die App übernimmt die dynamischen Farben und das helle oder dunkle Design deines Geräts.") {} }
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
