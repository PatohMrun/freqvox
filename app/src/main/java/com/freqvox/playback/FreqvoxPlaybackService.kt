package com.freqvox.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.freqvox.R
import com.freqvox.data.local.dao.TagDao
import com.freqvox.data.local.dao.TrackDao
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.data.repository.PlaylistRepository
import com.freqvox.data.repository.TrackRepository
import com.freqvox.ui.tag.formatTagDisplayName
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Playback service. A [MediaLibraryService] (not just a [MediaSessionService]) so
 * Android Auto / Wear can browse the library: the [MediaLibrarySession.Callback]
 * below exposes a tree of All Songs / Playlists / Tags. In-app playback is
 * unaffected — [PlaybackController] still connects with a plain MediaController.
 */
@AndroidEntryPoint
class FreqvoxPlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private var audioFocusHelper: PlaybackAudioFocusHelper? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Inject lateinit var equalizerManager: FreqvoxEqualizerManager
    @Inject lateinit var trackRepository: TrackRepository
    @Inject lateinit var trackDao: TrackDao
    @Inject lateinit var tagDao: TagDao
    @Inject lateinit var playlistRepository: PlaylistRepository

    override fun onCreate() {
        super.onCreate()

        val okHttpClient = OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Freqvox/1.0 (Android)")

        val defaultDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this, dataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(defaultDataSourceFactory))
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ false)
            .setHandleAudioBecomingNoisy(false)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        audioFocusHelper = PlaybackAudioFocusHelper(this, player).also { it.start() }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) audioFocusHelper?.onPlaybackStarted()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                refreshLikeNotificationButton()
            }
        })

        val intent = Intent(this, Class.forName("com.freqvox.MainActivity"))
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setSessionActivity(pendingIntent)
            .build()

        mediaSession?.setCustomLayout(buildLikeCommandButtons(isLiked = false))

        equalizerManager.init(player.audioSessionId)
        refreshLikeNotificationButton()
    }

    // ── MediaLibrarySession callback (browse tree + custom like command) ────────
    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            // Grant the custom "like" command alongside the standard media + library
            // commands so the notification/Auto button works.
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(SessionCommand(MediaSessionCommands.TOGGLE_LIKE, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == MediaSessionCommands.TOGGLE_LIKE) {
                val trackId = session.player.currentMediaItem?.mediaId?.toLongOrNull()
                if (trackId != null) {
                    serviceScope.launch {
                        trackRepository.toggleLikeForTrack(trackId)
                        refreshLikeNotificationButton()
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(
                LibraryResult.ofItem(browsableItem(ROOT_ID, "Freqvox"), params)
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                val children = buildChildren(parentId)
                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(children), params))
            }
            return future
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val future = SettableFuture.create<LibraryResult<MediaItem>>()
            serviceScope.launch {
                val id = mediaId.toLongOrNull()
                val track = if (id != null) trackDao.getTracksByIds(listOf(id)).firstOrNull() else null
                if (track != null) {
                    future.set(LibraryResult.ofItem(track.toBrowsableItem(), null))
                } else {
                    future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                }
            }
            return future
        }

        // Android Auto hands back items that carry only a mediaId; fill in the real
        // playback URI here before they reach the player.
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val future = SettableFuture.create<MutableList<MediaItem>>()
            serviceScope.launch {
                // Only resolve items that arrive WITHOUT a URI (browse items from
                // Android Auto). In-app items already carry their URI (and extras
                // like isAutoAppended that cycleRepeatMode depends on) — pass those
                // through untouched so we don't strip that metadata.
                val ids = mediaItems
                    .filter { it.localConfiguration == null }
                    .mapNotNull { it.mediaId.toLongOrNull() }
                val byId = if (ids.isEmpty()) emptyMap() else trackDao.getTracksByIds(ids).associateBy { it.id }
                val resolved = mediaItems.map { item ->
                    if (item.localConfiguration != null) item
                    else item.mediaId.toLongOrNull()?.let { byId[it] }?.toPlayableItem() ?: item
                }.toMutableList()
                future.set(resolved)
            }
            return future
        }
    }

    private suspend fun buildChildren(parentId: String): List<MediaItem> = when {
        parentId == ROOT_ID -> listOf(
            browsableItem(ID_ALL, "All Songs"),
            browsableItem(ID_PLAYLISTS, "Playlists"),
            browsableItem(ID_TAGS, "Tags")
        )
        parentId == ID_ALL ->
            trackRepository.allTracks.first().map { it.toBrowsableItem() }
        parentId == ID_PLAYLISTS ->
            playlistRepository.allPlaylists.first().map { browsableItem("$PREFIX_PLAYLIST${it.id}", it.name) }
        parentId == ID_TAGS ->
            tagDao.getAllTags().first().map { browsableItem("$PREFIX_TAG${it.id}", formatTagDisplayName(it.name)) }
        parentId.startsWith(PREFIX_PLAYLIST) -> {
            val id = parentId.removePrefix(PREFIX_PLAYLIST).toLongOrNull()
            val playlist = id?.let { playlistRepository.getPlaylistById(it) }
            if (playlist != null) playlistRepository.getTracksForPlaylist(playlist).map { it.toBrowsableItem() }
            else emptyList()
        }
        parentId.startsWith(PREFIX_TAG) -> {
            val id = parentId.removePrefix(PREFIX_TAG).toLongOrNull()
            if (id != null) tagDao.getTracksForTag(id).first().map { it.toBrowsableItem() } else emptyList()
        }
        else -> emptyList()
    }

    private fun browsableItem(id: String, title: String): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
            .build()
        return MediaItem.Builder().setMediaId(id).setMediaMetadata(meta).build()
    }

    /** Browse-list representation: metadata + mediaId, no URI (resolved in onAddMediaItems). */
    private fun TrackEntity.toBrowsableItem(): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .apply { artworkPath?.let { setArtworkUri(android.net.Uri.parse("file://$it")) } }
            .build()
        return MediaItem.Builder().setMediaId(id.toString()).setMediaMetadata(meta).build()
    }

    /** Full playable item including the playback URI. */
    private fun TrackEntity.toPlayableItem(): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setIsPlayable(true)
            .apply { artworkPath?.let { setArtworkUri(android.net.Uri.parse("file://$it")) } }
            .build()
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(path)
            .setMediaMetadata(meta)
            .build()
    }

    private fun buildLikeCommandButtons(isLiked: Boolean): ImmutableList<CommandButton> {
        val button = CommandButton.Builder()
            .setDisplayName(if (isLiked) "Unlike" else "Like")
            .setIconResId(
                if (isLiked) R.drawable.ic_notif_favorite else R.drawable.ic_notif_favorite_border
            )
            .setSessionCommand(
                SessionCommand(MediaSessionCommands.TOGGLE_LIKE, Bundle.EMPTY)
            )
            .build()
        return ImmutableList.of(button)
    }

    private fun refreshLikeNotificationButton() {
        val session = mediaSession ?: return
        val trackId = session.player.currentMediaItem?.mediaId?.toLongOrNull()
        if (trackId == null) {
            session.setCustomLayout(buildLikeCommandButtons(isLiked = false))
            return
        }
        serviceScope.launch {
            val liked = trackRepository.isTrackLiked(trackId)
            session.setCustomLayout(buildLikeCommandButtons(isLiked = liked))
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onDestroy() {
        audioFocusHelper?.stop()
        audioFocusHelper = null
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        equalizerManager.release()
        super.onDestroy()
    }

    companion object {
        private const val ROOT_ID = "root"
        private const val ID_ALL = "all_songs"
        private const val ID_PLAYLISTS = "playlists"
        private const val ID_TAGS = "tags"
        private const val PREFIX_PLAYLIST = "pl_"
        private const val PREFIX_TAG = "tag_"
    }
}
