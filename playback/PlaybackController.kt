package com.freqvox.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.ContextCompat

class PlaybackController(private val context: Context) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow("No track selected")
    val currentTrackTitle: StateFlow<String> = _currentTrackTitle.asStateFlow()

    private val _currentArtist = MutableStateFlow("Unknown Artist")
    val currentArtist: StateFlow<String> = _currentArtist.asStateFlow()

    private var pendingMediaItem: MediaItem? = null

    fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, FreqvoxPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                mediaController = controllerFuture?.get()
                setupControllerListener()
                pendingMediaItem?.let { playMediaItem(it) }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentMediaItem.value = mediaItem
                _currentTrackTitle.value = mediaItem?.mediaMetadata?.title?.toString() ?: "Unknown Track"
                _currentArtist.value = mediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown Artist"
            }
        })
    }

    fun playMediaUri(uri: String, title: String, artist: String) {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(Uri.parse(uri)) // Using path as artwork URI for extraction later or setting properly
            .build()
        
        val mediaItem = MediaItem.Builder()
            .setMediaId(uri)
            .setUri(uri)
            .setMediaMetadata(mediaMetadata)
            .build()
            
        playMediaItem(mediaItem)
    }

    private fun playMediaItem(mediaItem: MediaItem) {
        val controller = mediaController
        if (controller != null) {
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
            pendingMediaItem = null
        } else {
            pendingMediaItem = mediaItem
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    fun playNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun playPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }
}
