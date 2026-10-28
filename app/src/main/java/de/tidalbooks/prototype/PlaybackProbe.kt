package de.tidalbooks.prototype

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.tidal.sdk.player.common.model.MediaProduct
import com.tidal.sdk.player.common.model.ProductType
import com.tidal.sdk.player.playbackengine.model.Event
import com.tidal.sdk.player.playbackengine.model.PlaybackState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.util.UUID

/** Application-owned playback coordinator; the UI and media session use the same state. */
class PlaybackProbe(private val app: ProbeApp, private val gateway: OfficialTidalGateway, private val scope: CoroutineScope) {
    private val mutable = MutableStateFlow(ProbeState())
    val state = mutable.asStateFlow()
    val library = LibraryStore(app, scope) { message -> mutable.update { it.copy(message = message) } }
    private val preferences = app.getSharedPreferences("library-selection", Context.MODE_PRIVATE)
    private val player by lazy { gateway.createPlayer() }
    private val engine get() = player.playbackEngine
    private val timer = SleepDeadline()
    private var fadeCheckpoint: ProbeState? = null
    private val sleepVolume by lazy { TidalSleepVolume(engine) {
        notify("Das sanfte Ausblenden ist nicht verfügbar. Der Timer pausiert trotzdem und springt zurück.")
    } }
    private val bootCount get() = runCatching { Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -1) }.getOrDefault(-1)
    private var restoringBackup = false
    private var initialized = false
    private var serviceActive = false
    private var alternativeLogin: Job? = null
    private val decoderRecovery = DecoderRecovery()
    private data class PendingSeek(val trackId: String, val positionMs: Long)
    private var pendingSeek: PendingSeek? = null
    private var seekTarget: Long? = null
    private var lastTickMs = SystemClock.elapsedRealtime()
    private var lastSaveMs = 0L
    private var stalledSince: Long? = null
    private var lastPlaying = false
    private var previousId: String? = null
    private val wake = (app.getSystemService(Context.POWER_SERVICE) as PowerManager)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tidalbooks:playback").apply { setReferenceCounted(false) }

    init {
        mutable.update { it.copy(resumeRewindSeconds = preferences.getInt("resumeSeconds", 0).takeIf { v -> v in listOf(0, 5, 10, 15, 30) } ?: 0,
            lastTimerMinutes = preferences.getInt("lastTimerMinutes", 30).coerceIn(1, 180)) }
        scope.launch { library.dao.observeHistory().catch { notify("Positionsverlauf konnte nicht geladen werden.") }
            .collect { history -> mutable.update { it.copy(history = history) } } }
        scope.launch { library.dao.observeBooks().catch { notify("Bibliothek konnte nicht geöffnet werden.") }
            .collect { books -> mutable.update { it.copy(books = books) } } }
        scope.launch { library.dao.observeBookmarks().catch { notify("Lesezeichen konnten nicht geladen werden.") }
            .collect { marks -> mutable.update { it.copy(bookmarks = marks) } } }
        scope.launch {
            try {
                val saved = library.dao.sleep()
                val restored = restoredTimer(saved, bootCount, SystemClock.elapsedRealtime())
                mutable.update { it.copy(sleepSession = restored) }
                if (restored != saved && restored != null) library.write { library.dao.sleep(restored) }.await()
                if (restored?.active == true) timer.restore(restored.deadlineElapsedMs, restored.endTrackId)
                migratePrototypeProgress()
                preferences.getString("lastBook", null)?.let { id ->
                    library.dao.book(id)?.let { restoreState(it) }
                }
                if (sleepExpiredWhileAway(saved, bootCount, SystemClock.elapsedRealtime())) {
                    val s = state.value
                    s.album?.takeIf { !s.completed }?.let { album ->
                        val target = sleepResumePosition(album.tracks, s.index, s.positionMs, null)
                        mutable.update { it.copy(index = target.index, positionMs = target.positionMs) }
                        saveCurrentPosition(readEngine = false)
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { notify("Die Bibliothek konnte noch nicht vollständig wiederhergestellt werden.") }
            finally { mutable.update { it.copy(libraryReady = true) } }
        }
        scope.launch { while (isActive) { tick(); delay(if (state.value.requestedPlay || state.value.playing || timer.deadlineMs != null || pendingSeek != null) 100L else 1_000L) } }
        scope.launch {
            state.map(WidgetSnapshot::from).distinctUntilChanged().collect { snapshot ->
                if (AudiobookWidget.hasInstances(app)) runCatching { AudiobookWidget.update(app,snapshot) }
            }
        }
    }

    fun notify(message: String) { mutable.update { it.copy(message = message) } }
    fun dismissMessage() { mutable.update { it.copy(message = "") } }
    fun refreshLogin() = scope.launch {
        mutable.update { it.copy(loggedIn = gateway.isLoggedIn(), alternativeActive = gateway.alternativeSelected) }
    }
    fun beginAlternativeLogin() {
        if (state.value.busy) return
        alternativeLogin = scope.launch {
            operation {
                pause()
                if (!gateway.resumeAlternativeLogin()) {
                    val login = gateway.beginAlternativeLogin()
                    mutable.update { it.copy(alternativeCode = login.userCode, alternativeUrl = login.verificationUrl,
                        message = "Auf der TIDAL-Seite bestätigen und anschließend zur App zurückkehren.") }
                    gateway.completeAlternativeLogin(login)
                }
                if (initialized) { engine.reset(); pendingSeek = null }
                mutable.update { it.copy(loggedIn = true, alternativeActive = true, message = "Mit TIDAL verbunden.") }
            }
            mutable.update { it.copy(alternativeCode = null, alternativeUrl = null) }
        }
    }
    fun cancelAlternativeLogin() {
        alternativeLogin?.cancel()
        mutable.update { it.copy(alternativeCode = null, alternativeUrl = null, message = "Anmeldung abgebrochen.") }
    }
    fun finishLogin(redirectUri: String) = scope.launch { operation {
        val ok = gateway.finishLogin(redirectUri)
        mutable.update { it.copy(loggedIn = ok, alternativeActive = false, message = if(ok) "Mit TIDAL verbunden." else "Die Anmeldung wurde nicht abgeschlossen.") }
    } }
    fun checkLegacyPlayback() = scope.launch { operation {
        val id = state.value.album?.tracks?.firstOrNull()?.id ?: return@operation
        val report = gateway.probeLegacyPlayback(id)
        mutable.update { it.copy(legacyReport = report) }
    } }

    fun loadAlbum(input: String, country: String = "DE") = scope.launch {
        if (state.value.busy || !state.value.libraryReady) return@launch
        operation {
            val id = albumId(input)
            saveCurrentPosition(); library.flush()
            var stored = library.dao.book(id)
            if (stored == null) {
                val album = gateway.album(id, country.trim().uppercase())
                check(album.tracks.isNotEmpty() && album.tracks.all { it.durationMs > 0 }) { "Das Album enthält nicht abspielbare Tracks." }
                library.add(album)
                stored = library.dao.book(id)
            }
            selectBook(requireNotNull(stored), play = false)
            notify("Hörbuch in deiner Bibliothek.")
        }
    }
    fun openBook(id: String, autoPlay: Boolean = false) = scope.launch {
        if (state.value.busy || !state.value.libraryReady) return@launch
        if (state.value.album?.id == id) { if (autoPlay) play(); return@launch }
        operation {
            saveCurrentPosition(); library.flush()
            selectBook(library.dao.book(id) ?: error("Dieses Hörbuch ist nicht mehr in der Bibliothek."), play = autoPlay)
        }
    }
    private fun restoreState(book: StoredBook, override: BookPosition? = null) {
        val album = book.album()
        val position = override ?: restoreBookPosition(album.tracks, book.progressTrackId, book.progressMs)
        mutable.update { it.copy(album = album, index = position.index, positionMs = position.positionMs,
            completed = book.completed && override == null, playing = false, requestedPlay = false,
            presentation = "Noch nicht geprüft", continuousPlayedMs = 0, fullObserved = false, playbackProblem = null) }
        preferences.edit().putString("lastBook", book.id).apply()
    }
    private fun selectBook(book: StoredBook, override: BookPosition? = null, play: Boolean) {
        val shouldPlay = play && state.value.loggedIn && (!book.completed || override != null)
        notePaused()
        if (initialized) { engine.pause(); engine.reset() }
        pendingSeek = null; seekTarget = null; previousId = null; lastPlaying = false
        resetSleepFade()
        restoreState(book, override)
        if (override != null) clearResumeReference()
        if (shouldPlay && override == null) prepareResume()
        ensureInitialized()
        loadTrack(state.value.index, state.value.positionMs, shouldPlay)
        saveCurrentPosition(readEngine = false)
    }
    fun removeBook(id: String) = scope.launch { operation {
        if (state.value.album?.id == id) {
            pause()
            resetSleepFade()
            if (initialized) engine.reset()
            pendingSeek = null; seekTarget = null
            mutable.update { it.copy(album = null, index = 0, positionMs = 0, playing = false, requestedPlay = false) }
            preferences.edit().remove("lastBook").apply()
        }
        library.flush()
        library.write { library.database.withTransaction { library.dao.deleteSleepForBook(id); library.dao.delete(id) } }.await()
        if (state.value.sleepSession?.startBookId == id) {
            timer.cancel(); mutable.update { it.copy(sleepSession = null, timerRemainingMs = null) }
        }
        notify("Hörbuch aus der Bibliothek entfernt.")
    } }

    private suspend fun operation(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true) }
        try { block() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            val location = e.stackTrace.firstOrNull()?.let { "${it.className}.${it.methodName}:${it.lineNumber}" }.orEmpty()
            android.util.Log.w("TidalBooks", "${e.javaClass.simpleName} at $location")
            val safe = if (e is IllegalArgumentException || e is IllegalStateException)
                e.message?.takeIf { !it.contains("token", true) && !it.contains("https://") } else null
            notify(if (e is CatalogFailure || e is java.io.IOException) friendlyFailure(e) else safe ?: friendlyFailure(e))
        } finally { mutable.update { it.copy(busy = false) } }
    }

    private fun ensureInitialized() {
        if (initialized) return
        engine.setRepeatOne(false)
        initialized = true
        scope.launch { engine.events.collect { event ->
            when(event) {
                is Event.MediaProductTransition -> {
                    val album = state.value.album ?: return@collect
                    val index = album.tracks.indexOfFirst { it.id == event.mediaProduct.productId }
                    if (index < 0) return@collect
                    val pending = pendingSeek?.takeIf { it.trackId == event.mediaProduct.productId }
                    val full = event.playbackContext.assetPresentation.name == "FULL"
                    mutable.update { it.copy(index = index, positionMs = pending?.positionMs ?: 0,
                        transitions = it.transitions + if (previousId != null && previousId != event.mediaProduct.productId) 1 else 0,
                        continuousPlayedMs = 0, presentation = event.playbackContext.assetPresentation.name,
                        fullObserved = it.fullObserved || full) }
                    previousId = event.mediaProduct.productId
                    if (!full) pause("TIDAL liefert für diesen Zugang nur eine Vorschau.", readEngine = false)
                    else {
                        if (pending != null) { engine.seek(pending.positionMs.toFloat()); seekTarget = pending.positionMs; pendingSeek = null }
                        if (!state.value.requestedPlay) engine.pause()
                        setNext()
                    }
                    saveCurrentPosition(readEngine = false)
                }
                is Event.MediaProductEnded -> {
                    if (timer.endTrackId == event.mediaProduct.productId) {
                        val duration = state.value.album?.tracks?.getOrNull(state.value.index)?.durationMs
                        if (duration != null) mutable.update { it.copy(positionMs = duration) }
                        expireTimer(readEngine = false)
                        return@collect
                    }
                    val album = state.value.album
                    if (album != null && event.mediaProduct.productId == album.tracks.last().id) {
                        if (fadeCheckpoint != null) {
                            mutable.update { it.copy(positionMs = album.tracks.last().durationMs) }
                            expireTimer(readEngine = false)
                            return@collect
                        }
                        mutable.update { it.copy(index = album.tracks.lastIndex, positionMs = album.tracks.last().durationMs, completed = true) }
                        pause("Hörbuch beendet.", readEngine = false)
                    }
                }
                is Event.Error -> {
                    when (decoderRecovery.onError(event.errorCode, state.value.requestedPlay)) {
                        DecoderRecovery.Action.INVALIDATE_PAUSED -> {
                            engine.reset()
                            pendingSeek = null; seekTarget = null
                            mutable.update { it.copy(playing = false, loading = false) }
                            saveCurrentPosition(readEngine = false)
                            return@collect
                        }
                        DecoderRecovery.Action.RETRY -> {
                            val s = state.value
                            engine.pause(); engine.reset()
                            pendingSeek = null; seekTarget = null
                            loadTrack(s.index, s.positionMs, play = true, recoveringDecoder = true)
                            return@collect
                        }
                        DecoderRecovery.Action.REPORT -> Unit
                    }
                    val code = event.errorCode.take(48).filter { it.isLetterOrDigit() || it in ":_-" }
                    val problem = if (event.errorCode == "-1:3:4006")
                        "Android konnte die Audiowiedergabe nicht wiederherstellen. Versuche es erneut ($code)."
                    else "TIDAL konnte den Abschnitt nicht laden ($code). Prüfe deine Verbindung. Falls der Fehler bleibt, verbinde TIDAL in den Einstellungen erneut."
                    pause(problem, readEngine = false)
                    mutable.update { it.copy(playbackProblem = problem) }
                }
                is Event.StreamingPrivilegesRevoked -> {
                    val problem = "TIDAL wird auf einem anderen Gerät wiedergegeben. Beende dort die Wiedergabe, bevor du hier erneut startest."
                    pause(problem)
                    mutable.update { it.copy(playbackProblem = problem) }
                }
                is Event.PlaybackStateChange -> {
                    if (event.playbackState == PlaybackState.NOT_PLAYING && state.value.playing && state.value.requestedPlay) {
                        notePaused()
                        mutable.update { it.copy(requestedPlay = false, playing = false) }; saveCurrentPosition(readEngine = false)
                    }
                }
                else -> Unit
            }
        } }
    }
    private fun tick() {
        val now = SystemClock.elapsedRealtime()
        if (restoringBackup) return
        var playing = false
        if (initialized && state.value.album != null) {
            playing = engine.playbackState == PlaybackState.PLAYING
            if (playing && !lastPlaying) noteHeard()
            val actual = (engine.assetPosition*1000).toLong().coerceAtLeast(0)
            val pending = pendingSeek != null
            val sameTrack = engine.mediaProduct?.productId == state.value.album?.tracks?.getOrNull(state.value.index)?.id
            seekTarget?.let { if (!pending && kotlin.math.abs(actual-it) <= 1000) {
                seekTarget = null; mutable.update { s -> s.copy(seekObserved = true) }
            } }
            val delta = if (playing && lastPlaying) (now-lastTickMs).coerceIn(0,1000) else 0
            mutable.update { it.copy(playing = playing, loading = engine.playbackState == PlaybackState.STALLED && it.playbackProblem == null,
                positionMs = if (!pending && seekTarget == null && !it.completed && it.playbackProblem == null && sameTrack) actual else it.positionMs,
                continuousPlayedMs = it.continuousPlayedMs+delta, timerRemainingMs = timer.remaining(now)) }
            if (playing && now-lastSaveMs >= 4000) { saveCurrentPosition(); lastSaveMs = now }
        } else mutable.update { it.copy(timerRemainingMs = timer.remaining(now)) }
        if (state.value.requestedPlay && state.value.loading) {
            if (stalledSince == null) stalledSince = now
            if (now-requireNotNull(stalledSince) >= 20_000) {
                val problem = "Der Abschnitt lädt ungewöhnlich lange. Prüfe deine Internetverbindung und versuche es erneut."
                pause(problem)
                mutable.update { it.copy(playbackProblem = problem, loading = false) }
                stalledSince = null
            }
        } else stalledSince = null
        updateSleepFade(now)
        if (timer.expired(now)) expireTimer()
        if (serviceActive && (state.value.requestedPlay || timer.deadlineMs != null)) {
            if (!wake.isHeld) wake.acquire(185*60_000L)
        } else if (wake.isHeld) wake.release()
        lastPlaying = playing; lastTickMs = now
    }
    private fun setNext() {
        if (!initialized || state.value.album == null) return
        val s = state.value
        val next = if (timer.endTrackId != null) null else s.album?.tracks?.getOrNull(s.index+1)
        engine.setNext(next?.let { MediaProduct(ProductType.TRACK,it.id) })
    }
    private fun loadTrack(index: Int, positionMs: Long, play: Boolean, recoveringDecoder: Boolean = false) {
        val track = state.value.album?.tracks?.getOrNull(index) ?: return
        if (!recoveringDecoder) decoderRecovery.newRequest()
        mutable.update { it.copy(index = index, positionMs = positionMs, requestedPlay = play, playing = false,
            continuousPlayedMs = 0, presentation = "Wird geladen", playbackProblem = null) }
        stalledSince = null
        pendingSeek = PendingSeek(track.id,positionMs)
        seekTarget = positionMs
        if (timer.endTrackId != null) {
            timer.endOfTrack(track.id)
            state.value.sleepSession?.let { persistSleep(it.copy(endTrackId = track.id)) }
        }
        engine.load(MediaProduct(ProductType.TRACK,track.id)); setNext()
        if (play) { startService(); engine.play() }
    }
    private fun startService() { ContextCompat.startForegroundService(app, Intent(app,PlaybackService::class.java)) }
    fun play() {
        if (state.value.playbackProblem != null) { retryPlayback(); return }
        var s = state.value
        if (s.album == null || !s.libraryReady || s.busy) return
        if (!s.loggedIn) { notify("Bitte zuerst mit TIDAL verbinden."); return }
        if (s.completed) { notify("Dieses Hörbuch ist beendet. Wähle „Von vorne hören“, um es erneut zu starten."); return }
        decoderRecovery.newRequest()
        if (timer.expired(SystemClock.elapsedRealtime())) { expireTimer(); return }
        prepareResume()
        s = state.value
        val album = s.album ?: return
        if (!initialized || engine.mediaProduct?.productId != album.tracks[s.index].id) {
            ensureInitialized(); loadTrack(s.index,s.positionMs,false)
        }
        if (state.value.presentation == "PREVIEW") { notify("Dieser TIDAL-Zugang liefert nur Vorschauen."); return }
        mutable.update { it.copy(requestedPlay = true) }
        startService(); setNext(); engine.play()
    }
    fun pause(message: String? = null, readEngine: Boolean = true) {
        capturePosition(readEngine)
        notePaused()
        mutable.update { it.copy(requestedPlay = false, playing = false, message = message ?: it.message) }
        if (initialized && engine.mediaProduct != null) engine.pause()
        saveCurrentPosition(readEngine = false)
    }
    private fun capturePosition(readEngine: Boolean) {
        val s = state.value
        val track = s.album?.tracks?.getOrNull(s.index) ?: return
        if (readEngine && s.playbackProblem == null && initialized && pendingSeek == null && seekTarget == null && !s.completed && engine.mediaProduct?.productId == track.id) {
            val position = (engine.assetPosition*1000).toLong().coerceIn(0,track.durationMs)
            mutable.update { it.copy(positionMs = position) }
        }
    }
    fun saveCurrentPosition(readEngine: Boolean = true) {
        if (restoringBackup) return
        capturePosition(readEngine)
        // Persist the start of the fade until expiry, so process death cannot skip quiet audio.
        val s = fadeCheckpoint?.takeIf { it.album?.id == state.value.album?.id } ?: state.value
        val album = s.album ?: return
        val track = album.tracks.getOrNull(s.index) ?: return
        val elapsed = elapsedBookMs(album.tracks,s.index,s.positionMs)
        val savedAt = System.currentTimeMillis()
        if (state.value.playing) notePaused()
        library.write { library.dao.progress(album.id,track.id,s.positionMs,elapsed,s.completed,savedAt) }
    }
    fun seek(index: Int, positionMs: Long, recordHistory: Boolean = true) {
        if (restoringBackup) return
        val s = state.value
        val track = s.album?.tracks?.getOrNull(index) ?: return
        val target = positionMs.coerceIn(0,(track.durationMs-1).coerceAtLeast(0))
        if (recordHistory && (index != s.index || kotlin.math.abs(target-s.positionMs) > 1000)) rememberPosition("Vor dem Spulen")
        if (recordHistory) clearResumeReference()
        resetSleepFade()
        ensureInitialized()
        mutable.update { it.copy(index = index, positionMs = target, completed = false, continuousPlayedMs = 0) }
        seekTarget = target
        if (engine.mediaProduct?.productId == track.id) { pendingSeek = null; engine.seek(target.toFloat()) }
        else loadTrack(index,target,s.requestedPlay)
        saveCurrentPosition(readEngine = false)
    }
    fun seekAbsolute(positionMs: Long) {
        val tracks = state.value.album?.tracks ?: return
        val target = bookPosition(tracks,positionMs); seek(target.index,target.positionMs)
    }
    fun skip(deltaMs: Long) {
        val s = state.value; val tracks = s.album?.tracks ?: return
        seekAbsolute(elapsedBookMs(tracks,s.index,s.positionMs)+deltaMs)
    }
    fun restartBook(play: Boolean = false) {
        if (restoringBackup || state.value.album == null) return
        mutable.update { it.copy(completed = false) }
        seek(0,0)
        if (play) this.play()
    }
    fun markCompleted() {
        if (restoringBackup) return
        cancelTimer()
        val album = state.value.album ?: return
        mutable.update { it.copy(completed = true, index = album.tracks.lastIndex, positionMs = album.tracks.last().durationMs) }
        pause("Als beendet markiert.",readEngine = false)
    }
    fun addBookmark(label: String? = null) {
        capturePosition(true)
        val s = state.value; val album = s.album ?: return; val track = album.tracks[s.index]
        val mark = StoredBookmark(UUID.randomUUID().toString(),album.id,track.id,s.positionMs,
            elapsedBookMs(album.tracks,s.index,s.positionMs),track.title,label?.trim()?.take(120)?.ifBlank { null },System.currentTimeMillis())
        scope.launch { operation { library.write { library.dao.bookmark(mark) }.await(); notify("Lesezeichen gesetzt.") } }
    }
    fun renameBookmark(id: String, label: String) = scope.launch { operation {
        library.write { library.dao.renameBookmark(id,label.trim().take(120).ifBlank { null }) }.await()
    } }
    fun deleteBookmark(id: String) = scope.launch { operation { library.write { library.dao.deleteBookmark(id) }.await() } }
    fun openBookmark(mark: StoredBookmark) { openSavedPosition(mark.bookId,mark.trackId,mark.positionMs) }
    private fun openSavedPosition(bookId: String, trackId: String, positionMs: Long, historyId: String? = null) = scope.launch {
        if (state.value.busy) return@launch
        val wasPlaying = state.value.requestedPlay
        operation {
            if (historyId == null) rememberPosition("Vor dem Stellensprung")
            clearResumeReference()
            saveCurrentPosition(); library.flush()
            val book = library.dao.book(bookId) ?: error("Dieses Hörbuch ist nicht mehr in der Bibliothek.")
            val target = restoreBookPosition(book.album().tracks,trackId,positionMs)
            selectBook(book,target,wasPlaying)
            if (historyId != null) library.write { library.dao.deleteHistory(historyId) }.await()
        }
    }
    private fun persistSleep(value: StoredSleep) {
        mutable.update { it.copy(sleepSession = value) }
        library.write { library.dao.sleep(value) }
    }
    private fun sleepAnchor(deadline: Long?, endTrackId: String?): StoredSleep? {
        capturePosition(true)
        val s = state.value; val album = s.album ?: return null; val track = album.tracks[s.index]
        return StoredSleep(startBookId = album.id,startTrackId = track.id,startPositionMs = s.positionMs,
            active = true,deadlineElapsedMs = deadline,endTrackId = endTrackId,bootCount = bootCount)
    }
    fun startTimer(minutes: Int) {
        if (restoringBackup || state.value.completed) return
        if (minutes !in 1..180) { notify("Bitte 1 bis 180 Minuten eingeben."); return }
        val record = sleepAnchor(SystemClock.elapsedRealtime()+minutes*60_000L,null) ?: return
        preferences.edit().putInt("lastTimerMinutes", minutes).apply()
        mutable.update { it.copy(lastTimerMinutes = minutes) }
        resetSleepFade()
        timer.start(minutes,SystemClock.elapsedRealtime()); persistSleep(record.copy(deadlineElapsedMs = timer.deadlineMs)); setNext()
        mutable.update { it.copy(timerRemainingMs = timer.remaining(SystemClock.elapsedRealtime())) }
        notify("Einschlaftimer: $minutes ${if (minutes == 1) "Minute" else "Minuten"}.")
    }
    fun trackEndTimer() {
        if (restoringBackup || state.value.completed) return
        resetSleepFade()
        val s = state.value; val track = s.album?.tracks?.getOrNull(s.index) ?: return
        val record = sleepAnchor(null,track.id) ?: return
        timer.endOfTrack(track.id); persistSleep(record); setNext()
        mutable.update { it.copy(timerRemainingMs = null) }; notify("Wiedergabe endet nach diesem Track.")
    }
    fun extendTimer() {
        if (restoringBackup) return
        resetSleepFade()
        timer.extend()
        state.value.sleepSession?.takeIf { it.active && it.deadlineElapsedMs != null }?.let {
            persistSleep(it.copy(deadlineElapsedMs = timer.deadlineMs))
        }
    }
    fun cancelTimer() {
        resetSleepFade()
        timer.cancel()
        state.value.sleepSession?.let { persistSleep(it.copy(active = false,deadlineElapsedMs = null,endTrackId = null)) }
        setNext(); mutable.update { it.copy(timerRemainingMs = null) }
    }
    private fun resetSleepFade() {
        if (fadeCheckpoint != null && initialized) sleepVolume.restore()
        fadeCheckpoint = null
    }
    private fun updateSleepFade(now: Long) {
        val s = state.value
        val track = s.album?.tracks?.getOrNull(s.index)
        val remaining = timer.remaining(now) ?: track?.takeIf { it.id == timer.endTrackId }
            ?.let { (it.durationMs - s.positionMs).coerceAtLeast(0) }
        if (remaining != null && remaining <= SLEEP_FADE_MS && s.album != null) {
            if (fadeCheckpoint == null) {
                fadeCheckpoint = s
                saveCurrentPosition(readEngine = false)
            }
            if (initialized) sleepVolume.apply(sleepFadeGain(remaining))
        }
    }
    private fun expireTimer(readEngine: Boolean = true) {
        capturePosition(readEngine)
        val s = state.value
        val tracks = s.album?.tracks
        val start = fadeCheckpoint?.takeIf { it.album?.id == s.album?.id }?.let {
            elapsedBookMs(requireNotNull(it.album).tracks, it.index, it.positionMs)
        }
        val target = tracks?.let { sleepResumePosition(it, s.index, s.positionMs, start) }
        // Pause before restoring gain; never briefly resume at full volume.
        pause("Einschlaftimer beendet. Zum Weiterhören mindestens 5 Sekunden zurückgesprungen.", readEngine = false)
        cancelTimer()
        if (initialized) engine.setNext(null)
        if (target != null) seek(target.index, target.positionMs, recordHistory = false)
    }
    fun returnToSleepStart() {
        state.value.sleepSession?.let { openSavedPosition(it.startBookId,it.startTrackId,it.startPositionMs) }
    }
    fun serviceStarted() { serviceActive = true }
    fun serviceDestroyed() {
        serviceActive = false; pause()
        if (wake.isHeld) wake.release()
    }
    fun setResumeRewind(seconds: Int) {
        if (seconds !in listOf(0, 5, 10, 15, 30)) return
        preferences.edit().putInt("resumeSeconds", seconds).apply()
        mutable.update { it.copy(resumeRewindSeconds = seconds) }
    }
    private fun notePaused() {
        val s = state.value
        if (!s.requestedPlay && !s.playing) return
        s.album?.id?.let { id -> preferences.edit()
            .putLong("paused:$id", SystemClock.elapsedRealtime()).putInt("boot:$id", bootCount).apply() }
    }
    private fun clearResumeReference() {
        state.value.album?.id?.let { preferences.edit().remove("paused:$it").remove("boot:$it").apply() }
    }
    private fun noteHeard() {
        val id = state.value.album?.id ?: return
        val now = System.currentTimeMillis()
        library.write { library.dao.heard(id, now) }
    }
    private fun prepareResume() {
        val s = state.value
        val album = s.album ?: return
        if (s.requestedPlay || s.completed) return
        val offset = resumeOffsetMs(s.resumeRewindSeconds, preferences.getLong("paused:${album.id}", -1),
            preferences.getInt("boot:${album.id}", -1), SystemClock.elapsedRealtime(), bootCount)
        clearResumeReference()
        if (offset == 0L) return
        rememberPosition("Vor dem automatischen Rücksprung")
        val target = bookPosition(album.tracks, elapsedBookMs(album.tracks, s.index, s.positionMs)-offset)
        mutable.update { it.copy(index = target.index, positionMs = target.positionMs) }
        if (initialized && engine.mediaProduct?.productId == album.tracks[target.index].id) {
            pendingSeek = null; seekTarget = target.positionMs; engine.seek(target.positionMs.toFloat())
        }
        saveCurrentPosition(readEngine = false)
    }
    private fun rememberPosition(reason: String) {
        capturePosition(true)
        val s = state.value; val album = s.album ?: return
        val track = album.tracks.getOrNull(s.index) ?: return
        val jump = StoredJump(UUID.randomUUID().toString(), album.id, track.id, s.positionMs, System.currentTimeMillis(), reason)
        library.write { library.database.withTransaction { library.dao.insertHistory(jump); library.dao.trimHistory(album.id) } }
    }
    fun restoreHistory(jump: StoredJump) { openSavedPosition(jump.bookId, jump.trackId, jump.positionMs, historyId = jump.id) }
    fun retryPlayback() {
        val s = state.value
        if (s.busy || !s.libraryReady || s.completed || s.album == null) return
        if (!s.loggedIn) { notify("Bitte unter Einstellungen erneut mit TIDAL verbinden."); return }
        ensureInitialized()
        engine.pause(); engine.reset()
        pendingSeek = null; seekTarget = null
        mutable.update { it.copy(playbackProblem = null) }
        loadTrack(s.index, s.positionMs, true)
    }

    fun exportBackup(uri: Uri) = scope.launch {
        if (!state.value.libraryReady || state.value.busy) return@launch
        operation {
            try {
                saveCurrentPosition()
                val backup = library.snapshot(state.value.album?.id)
                withContext(Dispatchers.IO) {
                    val json = BackupCodec.encode(backup)
                    // Validate our output too, before opening/truncating the chosen document.
                    BackupCodec.decode(json)
                    requireNotNull(app.contentResolver.openOutputStream(uri, "wt")).bufferedWriter(Charsets.UTF_8).use { it.write(json) }
                }
                notify("Backup gespeichert: ${backup.books.size} Hörbücher und ${backup.bookmarks.size} Lesezeichen.")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { notify("Das Backup konnte nicht vollständig gespeichert werden. Bitte Speicherort und freien Speicher prüfen und erneut sichern.") }
        }
    }
    fun restoreBackup(backup: LibraryBackup) = scope.launch {
        if (!state.value.libraryReady || state.value.busy) return@launch
        operation {
            pause()
            cancelTimer()
            restoringBackup = true
            try {
                library.restore(backup)
                if (initialized) engine.reset()
                pendingSeek = null; seekTarget = null; previousId = null; lastPlaying = false
                val selected = backup.lastBookId ?: state.value.album?.id ?: backup.books.firstOrNull()?.id
                selected?.let { library.dao.book(it) }?.let { restoreState(it) }
                preferences.edit().apply {
                    backup.books.forEach { remove("paused:${it.id}"); remove("boot:${it.id}") }
                }.apply()
                val restoredSleep = library.dao.sleep()
                mutable.update { it.copy(sleepSession = restoredSleep, loading = false, timerRemainingMs = null) }
                notify("Backup wiederhergestellt. Deine Hörbücher stehen pausiert bereit.")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { notify("Wiederherstellung fehlgeschlagen. Bitte die Sicherung prüfen. Die Bibliothek wurde nicht teilweise ersetzt.") }
            finally { restoringBackup = false }
        }
    }

    private suspend fun migratePrototypeProgress() {
        val file = java.io.File(app.filesDir,"prototype-progress.json")
        if (!file.exists()) return
        try {
            val data = withContext(Dispatchers.IO) { JSONObject(file.readText()) }
            val id = data.getString("albumId")
            if (library.dao.book(id) == null && gateway.isLoggedIn()) {
                val album = gateway.album(id,data.optString("country","DE"))
                library.add(album)
                val index = data.optInt("index").coerceIn(0,album.tracks.lastIndex)
                val position = data.optLong("positionMs").coerceIn(0,album.tracks[index].durationMs)
                library.write { library.dao.progress(id,album.tracks[index].id,position,
                    elapsedBookMs(album.tracks,index,position),false,System.currentTimeMillis()) }.await()
            }
            if (library.dao.book(id) != null) {
                preferences.edit().putString("lastBook",id).apply()
                withContext(Dispatchers.IO) { file.delete() }
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { /* Retain the one-time import for a later successful launch. */ }
    }
}
