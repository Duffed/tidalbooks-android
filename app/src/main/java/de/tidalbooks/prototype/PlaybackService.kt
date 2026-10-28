package de.tidalbooks.prototype

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.*
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.*

@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private val scope = CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private var session: MediaSession? = null
    private val playback get() = (application as ProbeApp).probe
    override fun onCreate() {
        super.onCreate()
        playback.serviceStarted()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("playback",getString(R.string.playback_channel),NotificationManager.IMPORTANCE_LOW))
        setMediaNotificationProvider(object : DefaultMediaNotificationProvider(this,{ 1001 },"playback",R.string.playback_channel) {
            override fun getMediaButtons(session: MediaSession, playerCommands: Player.Commands,
                mediaButtonPreferences: ImmutableList<CommandButton>, showPauseButton: Boolean): ImmutableList<CommandButton> {
                val buttons = super.getMediaButtons(session,playerCommands,mediaButtonPreferences,showPauseButton)
                val play = buttons.firstOrNull { it.playerCommand == Player.COMMAND_PLAY_PAUSE }?.let {
                    CommandButton.Builder(it.icon).setPlayerCommand(Player.COMMAND_PLAY_PAUSE).setDisplayName(it.displayName)
                        .setExtras(Bundle().apply { putInt(COMMAND_KEY_COMPACT_VIEW_INDEX,1) }).build()
                }
                // Older Android notifications use this order; Android 13+ uses the session slots.
                return ImmutableList.copyOf(listOfNotNull(
                    buttons.firstOrNull { it.sessionCommand?.customAction == "de.tidalbooks.SKIP_BACK" },play,
                    buttons.firstOrNull { it.sessionCommand?.customAction == "de.tidalbooks.SKIP_FORWARD" }))
            }
        }.apply { setSmallIcon(R.drawable.ic_notification) })
        val intent = PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java).putExtra("openPlayer",true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val adapter = BookMediaPlayer(playback)
        val back = SessionCommand("de.tidalbooks.SKIP_BACK",Bundle.EMPTY)
        val forward = SessionCommand("de.tidalbooks.SKIP_FORWARD",Bundle.EMPTY)
        fun buttons(seconds: Int): List<CommandButton> = listOf(
            CommandButton.Builder(when(seconds) {
                5 -> CommandButton.ICON_SKIP_BACK_5
                15 -> CommandButton.ICON_SKIP_BACK_15
                30 -> CommandButton.ICON_SKIP_BACK_30
                else -> CommandButton.ICON_SKIP_BACK
            }).setSessionCommand(back).setSlots(CommandButton.SLOT_BACK)
                .setExtras(Bundle().apply { putInt(DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX,0) })
                .setDisplayName("$seconds Sekunden zurück").build(),
            CommandButton.Builder(when(seconds) {
                5 -> CommandButton.ICON_SKIP_FORWARD_5
                15 -> CommandButton.ICON_SKIP_FORWARD_15
                30 -> CommandButton.ICON_SKIP_FORWARD_30
                else -> CommandButton.ICON_SKIP_FORWARD
            }).setSessionCommand(forward).setSlots(CommandButton.SLOT_FORWARD)
                .setExtras(Bundle().apply { putInt(DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX,2) })
                .setDisplayName("$seconds Sekunden vor").build(),
        )
        val initialButtons = buttons(playback.state.value.skipSeconds)
        session = MediaSession.Builder(this,adapter).setId("audiobooks").setSessionActivity(intent)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult =
                    MediaSession.ConnectionResult.AcceptedResultBuilder(session).setAvailableSessionCommands(
                        MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().add(back).add(forward).build()).build()
                override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
                    when(customCommand.customAction) {
                        back.customAction -> playback.skip(-playback.state.value.skipSeconds * 1000L)
                        forward.customAction -> playback.skip(playback.state.value.skipSeconds * 1000L)
                        else -> return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            })
            .setMediaButtonPreferences(initialButtons).setCustomLayout(initialButtons).build()
        // UI starts this service directly, so no controller would register the session for us.
        // Registration enables the actual MediaStyle notification and system transport controls.
        addSession(requireNotNull(session))
        scope.launch {
            var currentSeconds = playback.state.value.skipSeconds
            playback.state.collect { state ->
                adapter.refresh()
                if (currentSeconds != state.skipSeconds) {
                    currentSeconds = state.skipSeconds
                    val updated = buttons(currentSeconds)
                    session?.setMediaButtonPreferences(updated)
                    session?.setCustomLayout(updated)
                }
            }
        }
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent,flags,startId)
        return START_NOT_STICKY
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        playback.saveCurrentPosition()
        super.onTaskRemoved(rootIntent)
    }
    override fun onDestroy() {
        playback.serviceDestroyed()
        scope.cancel()
        session?.let { removeSession(it); it.player.release(); it.release() }
        session = null
        super.onDestroy()
    }
}

@UnstableApi
private class BookMediaPlayer(private val playback: PlaybackProbe) : SimpleBasePlayer(Looper.getMainLooper()) {
    private var cachedAlbum: Album? = null
    private var playlist: List<MediaItemData> = emptyList()
    fun refresh() = invalidateState()
    override fun getState(): State {
        val s = playback.state.value
        val album = s.album
        if (cachedAlbum !== album) {
            cachedAlbum = album
            playlist = album?.tracks?.mapIndexed { index, track ->
                val metadata = MediaMetadata.Builder().setTitle(album.title).setAlbumTitle(album.title)
                    .setArtist("Track ${index+1} von ${album.tracks.size}" + if (album.author.isNotBlank()) " · ${album.author}" else " · TIDAL")
                    .setSubtitle(track.title).setIsPlayable(true).setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
                    .apply { album.coverUrl?.let { setArtworkUri(Uri.parse(it)) } }.build()
                MediaItemData.Builder("${album.id}:${track.id}:$index")
                    .setMediaItem(MediaItem.Builder().setMediaId(track.id).setMediaMetadata(metadata).build())
                    .setDurationUs(track.durationMs*1000).build()
            }.orEmpty()
        }
        return State.Builder().setAvailableCommands(Player.Commands.Builder().addAll(
            Player.COMMAND_PLAY_PAUSE,Player.COMMAND_GET_CURRENT_MEDIA_ITEM,Player.COMMAND_GET_TIMELINE,
            Player.COMMAND_GET_METADATA,Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,Player.COMMAND_SEEK_TO_MEDIA_ITEM,
            Player.COMMAND_SEEK_BACK,Player.COMMAND_SEEK_FORWARD).build())
            .setPlaylist(playlist).setCurrentMediaItemIndex(if (playlist.isEmpty()) C.INDEX_UNSET else s.index)
            .setPlaybackState(if (playlist.isEmpty()) Player.STATE_IDLE else if (s.loading) Player.STATE_BUFFERING else Player.STATE_READY)
            .setPlayWhenReady(s.requestedPlay,Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setSeekBackIncrementMs(s.skipSeconds * 1000L).setSeekForwardIncrementMs(s.skipSeconds * 1000L)
            .setContentPositionMs(s.positionMs).build()
    }
    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) playback.play() else playback.pause()
        return Futures.immediateVoidFuture()
    }
    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        when (seekCommand) {
            Player.COMMAND_SEEK_BACK -> playback.skip(-playback.state.value.skipSeconds * 1000L)
            Player.COMMAND_SEEK_FORWARD -> playback.skip(playback.state.value.skipSeconds * 1000L)
            else -> playback.seek(mediaItemIndex,if (positionMs == C.TIME_UNSET) 0 else positionMs)
        }
        return Futures.immediateVoidFuture()
    }
}
