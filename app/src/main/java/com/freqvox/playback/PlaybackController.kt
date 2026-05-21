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
import com.freqvox.data.local.dao.PlayHistoryDao
import com.freqvox.data.local.dao.TrackDao
import com.freqvox.data.local.entities.PlayHistoryEntity
import com.freqvox.data.local.entities.SkipHistoryEntity
import com.freqvox.data.local.entities.TrackEntity

data class QueueItem(
    val trackId: String,
    val title: String,
    val artist: String
)

class PlaybackController(
    private val context: Context,
    private val trackDao: TrackDao,
    private val playHistoryDao: PlayHistoryDao,
    private val sleepTimerManager: SleepTimerManager,
    val equalizerManager: FreqvoxEqualizerManager
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _currentTrackId = MutableStateFlow<Long?>(null)
    val currentTrackId: StateFlow<Long?> = _currentTrackId.asStateFlow()

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

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    // 0 = off, 1 = repeat one, 2 = repeat all
    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    val isTimerRunning: StateFlow<Boolean> = sleepTimerManager.isTimerRunning
    val timerTimeLeft: StateFlow<Long> = sleepTimerManager.timeLeft

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private var pendingMediaItem: MediaItem? = null

    // incrementPlayCount tracking (existing behaviour, untouched)
    private var lastTrackId: Long? = null
    private var playStartTime: Long = 0L

    // Debounce job: suppresses brief isPlaying=false blips during seek buffering
    private var seekDebounceJob: kotlinx.coroutines.Job? = null

    // Play history accumulation — tracks actual listen time excluding pauses
    private var historyTrackId: Long? = null
    private var historyAccumulated: Long = 0L   // ms of confirmed listen time
    private var historySegmentStart: Long = 0L  // start of current un-paused segment

    private var isFetchingAutoplay = false

    init {
        startPositionPolling()
    }

    fun initializeController() {
        if (mediaController?.isConnected == true) return
        // Don't start a second connection if one is already in progress
        if (controllerFuture?.isDone == false) return
        val sessionToken = SessionToken(
            context,
            ComponentName(context, FreqvoxPlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                // Ignore stale callbacks from a superseded future
                if (controllerFuture !== future) return@addListener
                try {
                    // Capture 'future' (already resolved here), NOT controllerFuture
                    // — reading controllerFuture.get() when it points to a different future blocks the main thread
                    mediaController = future.get()
                    setupControllerListener()
                    pendingMediaItem?.let { playMediaItem(it) }
                } catch (e: Exception) {
                    android.util.Log.e("PlaybackController", "Service connection failed", e)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun reconnectIfNeeded() {
        if (mediaController?.isConnected == true) return
        // Connection still in progress — let it complete
        if (controllerFuture?.isDone == false) return
        // Service died after a previous completed connection — clean up and reconnect
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
        initializeController()
    }

    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    seekDebounceJob?.cancel()
                    _isPlaying.value = true
                    playStartTime = System.currentTimeMillis()
                    historySegmentStart = System.currentTimeMillis()
                } else {
                    // Seal history segment immediately — accurate listen time
                    if (historySegmentStart > 0L) {
                        historyAccumulated += System.currentTimeMillis() - historySegmentStart
                        historySegmentStart = 0L
                    }
                    logPlayTime()
                    // Delay the UI state update so seek-buffering blips (< ~200ms) never reach the UI
                    seekDebounceJob?.cancel()
                    seekDebounceJob = scope.launch {
                        delay(200)
                        _isPlaying.value = false
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Flush stats for the track we are leaving (before metadata updates)
                logPlayTime()
                insertHistoryRecord()
                updateMetadata(mediaItem)
                
                val controller = mediaController
                if (controller != null) {
                    _currentQueueIndex.value = controller.currentMediaItemIndex.coerceAtLeast(0)
                    checkAndAppendTracksIfNeeded(controller)
                } else {
                    _currentQueueIndex.value = 0
                }
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                refreshQueue()
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
                } else if (playbackState == Player.STATE_ENDED) {
                    val controller = mediaController ?: return
                    // As a fallback, try to append if we hit the end
                    checkAndAppendTracksIfNeeded(controller)
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e(
                    "PlaybackController",
                    "Playback error: ${error.errorCodeName} — ${error.cause?.message ?: error.message}",
                    error
                )
            }
        })
    }

    private fun checkAndAppendTracksIfNeeded(controller: MediaController) {
        if (controller.repeatMode != Player.REPEAT_MODE_OFF) return
        
        val currentIndex = controller.currentMediaItemIndex
        val totalItems = controller.mediaItemCount
        
        if (totalItems - currentIndex <= 3 && !isFetchingAutoplay) {
            isFetchingAutoplay = true
            scope.launch(Dispatchers.IO) {
                try {
                    val nextBatch = trackDao.getRandomTracks(15)
                    if (nextBatch.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            val newItems = nextBatch.map { track ->
                                val extras = android.os.Bundle().apply {
                                    putBoolean("isAutoAppended", true)
                                }
                                val meta = MediaMetadata.Builder()
                                    .setTitle(track.title)
                                    .setArtist(track.artist)
                                    .setExtras(extras)
                                track.artworkPath?.let { meta.setArtworkUri(Uri.parse("file://$it")) }
                                MediaItem.Builder()
                                    .setMediaId(track.id.toString())
                                    .setUri(track.path)
                                    .setMediaMetadata(meta.build())
                                    .build()
                            }
                            controller.addMediaItems(newItems)
                            
                            if (controller.playbackState == Player.STATE_ENDED) {
                                controller.seekToDefaultPosition(totalItems)
                                controller.prepare()
                                controller.play()
                            }
                        }
                    }
                } finally {
                    isFetchingAutoplay = false
                }
            }
        }
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

        val newId = mediaItem?.mediaId?.toLongOrNull()
        if (newId != lastTrackId) {
            lastTrackId = newId
            _currentTrackId.value = newId   // drives like/tag state in ViewModel
            playStartTime = System.currentTimeMillis()
            // Reset history accumulation for the new track
            historyTrackId = newId
            historyAccumulated = 0L
            historySegmentStart = System.currentTimeMillis()
        }
    }

    private fun logPlayTime() {
        val id = lastTrackId ?: return
        val durationPlayed = System.currentTimeMillis() - playStartTime
        if (durationPlayed > 5000L) {
            scope.launch(Dispatchers.IO) {
                trackDao.incrementPlayCount(id, System.currentTimeMillis(), durationPlayed)
            }
        }
    }

    private fun insertHistoryRecord() {
        val id = historyTrackId ?: return
        // Include the current active segment if playback is running during transition
        val currentSegment = if (_isPlaying.value && historySegmentStart > 0L)
            System.currentTimeMillis() - historySegmentStart
        else 0L
        val total = historyAccumulated + currentSegment
        if (total < 5000L) return

        val dur = _duration.value
        val completed = dur > 0L && total >= (dur * 0.75f).toLong()
        scope.launch(Dispatchers.IO) {
            playHistoryDao.insert(
                PlayHistoryEntity(
                    trackId = id,
                    playedAt = System.currentTimeMillis(),
                    durationPlayed = total,
                    completed = completed
                )
            )
        }
        // Reset so updateMetadata can cleanly initialise the next track
        historyTrackId = null
        historyAccumulated = 0L
        historySegmentStart = 0L
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

        val mediaItem = MediaItem.Builder()
            .setMediaId(trackId)
            .setUri(uri)
            .setMediaMetadata(metadataBuilder.build())
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
            if (controller.isPlaying) controller.pause() else controller.play()
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
        val controller = mediaController ?: return
        // Industry standard: If playing for more than 3 seconds, restart. Otherwise skip back.
        if (controller.currentPosition > 3000) {
            controller.seekTo(0)
        } else {
            controller.seekToPreviousMediaItem()
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val new = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = new
        _shuffleEnabled.value = new
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val new = when (controller.repeatMode) {
            0 -> 2   // OFF → ALL
            2 -> 1   // ALL → ONE
            else -> 0 // ONE → OFF
        }

        // If turning ON repeat, strip out auto-appended tracks so the user loops their intended original queue.
        // We only do this if they haven't actually reached the auto-appended section yet.
        if (new != 0) {
            val currentItem = controller.currentMediaItem
            val isPlayingAutoAppended = currentItem?.mediaMetadata?.extras?.getBoolean("isAutoAppended") == true
            
            if (!isPlayingAutoAppended) {
                val itemsToRemove = mutableListOf<Int>()
                for (i in controller.mediaItemCount - 1 downTo 0) {
                    val item = controller.getMediaItemAt(i)
                    if (item.mediaMetadata.extras?.getBoolean("isAutoAppended") == true) {
                        itemsToRemove.add(i)
                    }
                }
                itemsToRemove.forEach { index ->
                    controller.removeMediaItem(index)
                }
            }
        }

        controller.repeatMode = new
        _repeatMode.value = new
    }

    private fun refreshQueue() {
        val controller = mediaController ?: return
        _queue.value = (0 until controller.mediaItemCount).map { i ->
            val item = controller.getMediaItemAt(i)
            QueueItem(
                trackId = item.mediaId,
                title   = item.mediaMetadata.title?.toString() ?: "Unknown",
                artist  = item.mediaMetadata.artist?.toString() ?: "Unknown"
            )
        }
        _currentQueueIndex.value = controller.currentMediaItemIndex.coerceAtLeast(0)
    }

    fun playQueue(tracks: List<TrackEntity>, startIndex: Int = 0) {
        val mediaItems = tracks.map { track ->
            val meta = MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist)
            track.artworkPath?.let { meta.setArtworkUri(Uri.parse("file://$it")) }
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.path)
                .setMediaMetadata(meta.build())
                .build()
        }
        val controller = mediaController
        if (controller != null) {
            controller.setMediaItems(mediaItems, startIndex, 0L)
            controller.prepare()
            controller.play()
            pendingMediaItem = null
        } else {
            pendingMediaItem = mediaItems.getOrNull(startIndex)
        }
    }

    fun addToQueue(track: TrackEntity) {
        val meta = MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist)
        track.artworkPath?.let { meta.setArtworkUri(Uri.parse("file://$it")) }
        val item = MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.path)
            .setMediaMetadata(meta.build())
            .build()
        mediaController?.addMediaItem(item)
    }

    fun removeFromQueue(index: Int) {
        mediaController?.removeMediaItem(index)
    }

    fun jumpToQueueItem(index: Int) {
        val controller = mediaController ?: return
        controller.seekTo(index, 0L)
        if (!controller.isPlaying) controller.play()
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
        if (_isPlaying.value && historySegmentStart > 0L) {
            historyAccumulated += System.currentTimeMillis() - historySegmentStart
            historySegmentStart = 0L
        }
        logPlayTime()
        insertHistoryRecord()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
        scope.cancel()
    }
}
