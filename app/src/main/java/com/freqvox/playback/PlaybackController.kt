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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.ContextCompat
import com.freqvox.data.local.dao.TrackDao
import com.freqvox.data.local.entities.SkipHistoryEntity
import javax.inject.Inject
import javax.inject.Singleton

class PlaybackController(
    private val context: Context,
    private val trackDao: TrackDao,
    private val sleepTimerManager: SleepTimerManager
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow("No track selected")
    val currentTrackTitle: StateFlow<String> = _currentTrackTitle.asStateFlow()

    private val _currentArtist = MutableStateFlow("")
    val currentArtist: StateFlow<String> = _currentArtist.asStateFlow()
    
    private val _currentArtworkUri = MutableStateFlow<Uri?>(null)
    val currentArtworkUri: StateFlow<Uri?> = _currentArtworkUri.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var pendingMediaItem: MediaItem? = null
    
    private var lastTrackId: Long? = null
    private var playStartTime: Long = 0

    init {
        startPositionPolling()
    }

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
                if (isPlaying) {
                    playStartTime = System.currentTimeMillis()
                } else {
                    logPlayTime()
                }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || 
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                    logPlayTime()
                }
                
                updateMetadata(mediaItem)
            }
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                _currentTrackTitle.value = mediaMetadata.title?.toString() ?: "Unknown Track"
                _currentArtist.value = mediaMetadata.artist?.toString() ?: "Unknown Artist"
                _currentArtworkUri.value = mediaMetadata.artworkUri
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val dur = mediaController?.duration ?: 0L
                    if (dur != androidx.media3.common.C.TIME_UNSET) {
                        _duration.value = dur
                    }
                }
            }
        })
    }

    private fun updateMetadata(mediaItem: MediaItem?) {
        val metadata = mediaItem?.mediaMetadata
        _currentTrackTitle.value = metadata?.title?.toString() ?: "Unknown Track"
        _currentArtist.value = metadata?.artist?.toString() ?: "Unknown Artist"
        _currentArtworkUri.value = metadata?.artworkUri
        val dur = mediaController?.duration ?: 0L
        if (dur != androidx.media3.common.C.TIME_UNSET) {
            _duration.value = dur
        }
        
        // Update track ID for stats
        val newId = mediaItem?.mediaId?.toLongOrNull()
        if (newId != lastTrackId) {
            lastTrackId = newId
            playStartTime = System.currentTimeMillis()
        }
    }

    private fun logPlayTime() {
        val id = lastTrackId ?: return
        val durationPlayed = System.currentTimeMillis() - playStartTime
        if (durationPlayed > 5000) { // Log if played for more than 5s
            scope.launch(Dispatchers.IO) {
                trackDao.incrementPlayCount(id, System.currentTimeMillis(), durationPlayed)
            }
        }
    }

    private fun startPositionPolling() {
        scope.launch {
            while (isActive) {
                mediaController?.let {
                    if (it.playbackState != Player.STATE_IDLE && it.playbackState != Player.STATE_ENDED) {
                        _currentPosition.value = it.currentPosition
                    }
                }
                delay(1000)
            }
        }
    }

    fun playMediaUri(uri: String, title: String, artist: String, trackId: String, artworkPath: String? = null) {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            
        if (artworkPath != null) {
            metadataBuilder.setArtworkUri(Uri.parse("file://$artworkPath"))
        }

        val mediaMetadata = metadataBuilder.build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(trackId)
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

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
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

    fun skipNext() {
        val id = lastTrackId
        if (id != null) {
            scope.launch(Dispatchers.IO) {
                trackDao.incrementTrackSkipCount(id)
                trackDao.insertSkipEvent(SkipHistoryEntity(trackId = id))
            }
        }
        playNext()
    }

    fun playPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerManager.startTimer(minutes) {
            mediaController?.pause()
        }
    }

    fun stopSleepTimer() {
        sleepTimerManager.stopTimer()
    }

    fun release() {
        logPlayTime()
        scope.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }
}
