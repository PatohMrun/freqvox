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
import com.freqvox.data.local.dao.TagDao
import com.freqvox.data.local.dao.TrackDao
import com.freqvox.data.local.entities.PlayHistoryEntity
import com.freqvox.data.local.entities.SkipHistoryEntity
import com.freqvox.data.local.entities.TrackEntity

data class QueueItem(
    val trackId: String,
    val title: String,
    val artist: String,
    // Stable, unique key for the queue UI. The SAME song can legitimately appear twice
    // (e.g. "Play next" on a track already queued); keying the list by trackId then
    // produced duplicate Compose keys and crashed the queue sheet. uid = queue position
    // at build time, which is unique and travels with the item across reorders.
    val uid: Int = 0
)

class PlaybackController(
    private val context: Context,
    private val trackDao: TrackDao,
    private val tagDao: TagDao,
    private val playHistoryDao: PlayHistoryDao,
    private val sleepTimerManager: SleepTimerManager,
    val equalizerManager: FreqvoxEqualizerManager,
    private val crossfadePrefs: CrossfadePreferences
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val prefs = context.getSharedPreferences("freqvox_playback", Context.MODE_PRIVATE)

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

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    val isTimerRunning: StateFlow<Boolean> = sleepTimerManager.isTimerRunning
    val timerTimeLeft: StateFlow<Long> = sleepTimerManager.timeLeft

    // "Stop after the current track finishes" — a count-down-less sleep mode.
    private val _stopAfterCurrentTrack = MutableStateFlow(false)
    val stopAfterCurrentTrack: StateFlow<Boolean> = _stopAfterCurrentTrack.asStateFlow()

    // "Stop after this album finishes" — pauses at the first track of a different album.
    private val _stopAfterAlbum = MutableStateFlow(false)
    val stopAfterAlbum: StateFlow<Boolean> = _stopAfterAlbum.asStateFlow()
    private var currentAlbumTitle: String? = null

    // Fades playback to silence over the final stretch of a minute-timer, then pauses.
    private var sleepFadeJob: Job? = null
    private val sleepFadeMs = 20_000L

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private var pendingMediaItem: MediaItem? = null

    // incrementPlayCount tracking
    private var lastTrackId: Long? = null
    private var playStartTime: Long = 0L
    // Whether the current track has already been counted as one play. Prevents a
    // single listen (with pauses/resumes) from incrementing playCount multiple times.
    private var playCounted: Boolean = false

    // Debounce job: suppresses brief isPlaying=false blips during seek buffering
    private var seekDebounceJob: kotlinx.coroutines.Job? = null

    // Play history accumulation — tracks actual listen time excluding pauses
    private var historyTrackId: Long? = null
    private var historyAccumulated: Long = 0L   // ms of confirmed listen time
    private var historySegmentStart: Long = 0L  // start of current un-paused segment

    private var isFetchingAutoplay = false
    // Tracks the pool of songs to append from (the group/playlist the user is currently playing)
    private var sourcePool: List<TrackEntity> = emptyList()

    // Pressing Next before this many ms into a track counts as a skip; after it,
    // it's treated as ordinary navigation.
    private val SKIP_THRESHOLD_MS = 15_000L

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
                    restorePlaybackState()
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
                    // Clear the segment start so a later call (e.g. a track change while still
                    // paused) can't re-log the same span — previously that double-counted the
                    // segment AND silently added the entire paused duration to totalPlayTime.
                    playStartTime = 0L
                    // Delay the UI state update so seek-buffering blips (< ~200ms) never reach the UI
                    seekDebounceJob?.cancel()
                    seekDebounceJob = scope.launch {
                        delay(200)
                        _isPlaying.value = false
                        savePlaybackState()
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Flush stats for the track we are leaving (before metadata updates)
                logPlayTime()
                insertHistoryRecord()
                updateMetadata(mediaItem)
                
                // "Stop after current track": when the previous track ends and the
                // player auto-advances, pause at the start of the next item.
                if (_stopAfterCurrentTrack.value &&
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                ) {
                    mediaController?.let { c ->
                        c.pause()
                        c.seekTo(c.currentMediaItemIndex.coerceAtLeast(0), 0L)
                    }
                    _stopAfterCurrentTrack.value = false
                }

                // "Stop after this album": when we auto-advance into a track whose album differs
                // from the one that was playing, the album is over — pause at its start.
                val newAlbum = mediaItem?.mediaMetadata?.albumTitle?.toString()
                if (_stopAfterAlbum.value &&
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                    currentAlbumTitle != null &&
                    !newAlbum.equals(currentAlbumTitle, ignoreCase = true)
                ) {
                    mediaController?.let { c ->
                        c.pause()
                        c.seekTo(c.currentMediaItemIndex.coerceAtLeast(0), 0L)
                    }
                    _stopAfterAlbum.value = false
                }
                currentAlbumTitle = newAlbum

                val controller = mediaController
                if (controller != null) {
                    _currentQueueIndex.value = controller.currentMediaItemIndex.coerceAtLeast(0)
                    checkAndAppendTracksIfNeeded(controller)
                } else {
                    _currentQueueIndex.value = 0
                }
                savePlaybackState()
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

            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                _playbackSpeed.value = playbackParameters.speed
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
        // Never append when any loop mode is active — the user has chosen to loop
        // their original selection, so injecting new tracks would break that intent.
        if (controller.repeatMode != Player.REPEAT_MODE_OFF) return

        val currentIndex = controller.currentMediaItemIndex
        val totalItems = controller.mediaItemCount

        if (totalItems - currentIndex <= 3 && !isFetchingAutoplay) {
            isFetchingAutoplay = true
            // MediaController must only be touched on the main thread — snapshot queue ids here.
            val alreadyQueuedIds = (0 until controller.mediaItemCount)
                .map { i -> controller.getMediaItemAt(i).mediaId }
                .toSet()

            scope.launch(Dispatchers.IO) {
                try {
                    // Two-stage autoplay:
                    //   1) Drain whatever is left of the original sourcePool (the playlist/
                    //      carousel/library group the user actually picked).
                    //   2) Once that pool is exhausted (or was never set), fall back to a
                    //      random sweep of the wider library — excluding tracks already in
                    //      the queue — so short Home carousels and small Library playlists
                    //      keep flowing instead of going silent at the last track.
                    val dislikedIds = tagDao.getDislikedTrackIds().toSet()
                    val unplayedPool = sourcePool
                        .filter { it.id.toString() !in alreadyQueuedIds && it.id !in dislikedIds }
                    val nextBatch: List<TrackEntity> = if (unplayedPool.isNotEmpty()) {
                        unplayedPool.take(15)
                    } else {
                        val excludeIds = alreadyQueuedIds
                            .mapNotNull { it.toLongOrNull() }
                            .take(900) // SQLite parameter cap safety
                        if (excludeIds.isEmpty()) {
                            trackDao.getRandomTracks(15)
                        } else {
                            trackDao.getRandomTracksExcluding(excludeIds, 15)
                        }
                    }

                    if (nextBatch.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            val ctrl = mediaController ?: return@withContext
                            val newItems = nextBatch.map { track ->
                                val extras = android.os.Bundle().apply {
                                    putBoolean("isAutoAppended", true)
                                }
                                val meta = MediaMetadata.Builder()
                                    .setTitle(track.title)
                                    .setArtist(track.artist)
                                    .setAlbumTitle(track.album)
                                    .setExtras(extras)
                                track.artworkPath?.let { meta.setArtworkUri(Uri.parse("file://$it")) }
                                MediaItem.Builder()
                                    .setMediaId(track.id.toString())
                                    .setUri(track.path)
                                    .setMediaMetadata(meta.build())
                                    .build()
                            }
                            ctrl.addMediaItems(newItems)

                            if (ctrl.playbackState == Player.STATE_ENDED) {
                                ctrl.seekToDefaultPosition(totalItems)
                                ctrl.prepare()
                                ctrl.play()
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
            // Open a segment only if we're actually playing; a track change while paused must
            // not start a phantom segment (onIsPlayingChanged(true) opens it on resume).
            playStartTime = if (mediaController?.isPlaying == true) System.currentTimeMillis() else 0L
            playCounted = false             // new track → eligible to be counted once
            // Reset history accumulation for the new track
            historyTrackId = newId
            historyAccumulated = 0L
            historySegmentStart = System.currentTimeMillis()
        }
    }

    private fun logPlayTime() {
        val id = lastTrackId ?: return
        // 0 = no open segment (paused / already logged) — nothing to record.
        if (playStartTime <= 0L) return
        val durationPlayed = System.currentTimeMillis() - playStartTime
        if (durationPlayed <= 0L) return
        // Count the play exactly once (first segment past the 5s threshold); every
        // other segment only accrues listen time so playCount isn't inflated.
        val countThisSegment = !playCounted && durationPlayed > 5000L
        if (countThisSegment) playCounted = true
        scope.launch(Dispatchers.IO) {
            if (countThisSegment) {
                trackDao.incrementPlayCount(id, System.currentTimeMillis(), durationPlayed)
            } else {
                trackDao.addPlayTime(id, System.currentTimeMillis(), durationPlayed)
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

    /** Always records a skip then advances — used when the user explicitly rejects
     *  a track (e.g. the dislike auto-skip). */
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

    /** User-facing "Next". Counts as a skip ONLY if they bailed early (before
     *  [SKIP_THRESHOLD_MS]); advancing near the end of a track is just navigation,
     *  not a dislike signal. Used by both the full player and the mini-player. */
    fun nextTrack() {
        val controller = mediaController ?: return
        val id = lastTrackId
        if (id != null && controller.currentPosition in 0 until SKIP_THRESHOLD_MS) {
            scope.launch(Dispatchers.IO) {
                trackDao.incrementTrackSkipCount(id)
                trackDao.insertSkipEvent(SkipHistoryEntity(trackId = id))
            }
        }
        controller.seekToNextMediaItem()
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

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
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
                artist  = item.mediaMetadata.artist?.toString() ?: "Unknown",
                uid     = i
            )
        }
        _currentQueueIndex.value = controller.currentMediaItemIndex.coerceAtLeast(0)
        savePlaybackState()
    }

    /** Drops a deleted track from the queue and advances playback if it was current. */
    fun removeTrackFromLibrary(trackId: Long) {
        val controller = mediaController ?: return
        val mediaId = trackId.toString()
        val indicesToRemove = (0 until controller.mediaItemCount)
            .filter { controller.getMediaItemAt(it).mediaId == mediaId }
            .sortedDescending()
        indicesToRemove.forEach { controller.removeMediaItem(it) }
        if (_currentTrackId.value == trackId) {
            if (controller.mediaItemCount == 0) {
                controller.stop()
                _currentTrackTitle.value = "No track selected"
                _currentArtist.value = ""
                _currentArtworkUri.value = null
                _currentTrackId.value = null
                _isPlaying.value = false
            }
        }
        refreshQueue()
    }

    fun playQueue(tracks: List<TrackEntity>, startIndex: Int = 0, sourcePool: List<TrackEntity> = tracks) {
        // Store the full pool so autoplay appends from the same context
        this.sourcePool = sourcePool
        val mediaItems = tracks.map { track ->
            val meta = MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist)
                .setAlbumTitle(track.album)
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

    private fun buildMediaItem(track: TrackEntity): MediaItem {
        val meta = MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist)
            .setAlbumTitle(track.album)
        track.artworkPath?.let { meta.setArtworkUri(Uri.parse("file://$it")) }
        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.path)
            .setMediaMetadata(meta.build())
            .build()
    }

    fun addToQueue(track: TrackEntity) {
        mediaController?.addMediaItem(buildMediaItem(track))
    }

    /** Inserts the track immediately after the one currently playing. */
    fun playTrackNext(track: TrackEntity) {
        val controller = mediaController ?: return
        val insertIndex = (controller.currentMediaItemIndex + 1)
            .coerceIn(0, controller.mediaItemCount)
        controller.addMediaItem(insertIndex, buildMediaItem(track))
    }

    fun removeFromQueue(index: Int) {
        mediaController?.removeMediaItem(index)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        controller.moveMediaItem(fromIndex, toIndex)
        refreshQueue()
    }

    fun jumpToQueueItem(index: Int) {
        val controller = mediaController ?: return
        controller.seekTo(index, 0L)
        if (!controller.isPlaying) controller.play()
    }

    fun startSleepTimer(minutes: Int) {
        // Minute-timer and the end-of-* modes are mutually exclusive sleep modes.
        _stopAfterCurrentTrack.value = false
        _stopAfterAlbum.value = false
        sleepTimerManager.startTimer(minutes) {
            mediaController?.pause()
            crossfadePrefs.sleepGain = 1f
            mediaController?.volume = 1f   // restore for the next play session
        }
        // Fade the last sleepFadeMs down to silence so sleep isn't a hard cut. The gain is
        // published via crossfadePrefs.sleepGain so the CrossfadeController multiplies it into
        // its own volume writes (ramp × duck × norm × sleepGain) instead of clobbering the fade;
        // the direct controller write below covers the common no-crossfade case at 100ms grain.
        sleepFadeJob?.cancel()
        sleepFadeJob = scope.launch {
            val totalMs = minutes * 60_000L
            val preFade = (totalMs - sleepFadeMs).coerceAtLeast(0L)
            delay(preFade)
            val rampMs = totalMs - preFade
            val steps = (rampMs / 100L).coerceAtLeast(1)
            for (i in 1..steps) {
                if (!isActive) break
                val f = (1f - i.toFloat() / steps).coerceIn(0f, 1f)
                crossfadePrefs.sleepGain = f
                mediaController?.volume = f
                delay(100)
            }
        }
    }

    fun setStopAfterCurrentTrack(enabled: Boolean) {
        if (enabled) { sleepTimerManager.stopTimer(); cancelSleepFade(); _stopAfterAlbum.value = false }
        _stopAfterCurrentTrack.value = enabled
    }

    fun setStopAfterAlbum(enabled: Boolean) {
        if (enabled) {
            sleepTimerManager.stopTimer()
            cancelSleepFade()
            _stopAfterCurrentTrack.value = false
            currentAlbumTitle = mediaController?.currentMediaItem?.mediaMetadata?.albumTitle?.toString()
        }
        _stopAfterAlbum.value = enabled
    }

    fun stopSleepTimer() {
        sleepTimerManager.stopTimer()
        cancelSleepFade()
        _stopAfterCurrentTrack.value = false
        _stopAfterAlbum.value = false
    }

    /** Cancel any in-flight sleep fade and restore full volume. */
    private fun cancelSleepFade() {
        sleepFadeJob?.cancel()
        sleepFadeJob = null
        crossfadePrefs.sleepGain = 1f
        mediaController?.volume = 1f
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

    private fun savePlaybackState() {
        val controller = mediaController ?: return
        if (controller.mediaItemCount == 0) return
        val trackIds = (0 until controller.mediaItemCount).mapNotNull { i ->
            val idStr = controller.getMediaItemAt(i).mediaId
            if (idStr.isNotEmpty()) idStr else null
        }
        val currentIndex = controller.currentMediaItemIndex
        val currentPosition = controller.currentPosition

        prefs.edit()
            .putString("saved_queue", trackIds.joinToString(","))
            .putInt("saved_index", currentIndex)
            .putLong("saved_position", currentPosition)
            .apply()
    }

    private fun restorePlaybackState() {
        val controller = mediaController ?: return
        if (controller.mediaItemCount > 0) return // Already populated
        
        val savedQueueStr = prefs.getString("saved_queue", "") ?: ""
        if (savedQueueStr.isEmpty()) return
        
        val savedIndex = prefs.getInt("saved_index", 0)
        val savedPosition = prefs.getLong("saved_position", 0L)
        
        val trackIds = savedQueueStr.split(",").mapNotNull { it.toLongOrNull() }
        if (trackIds.isEmpty()) return
        
        scope.launch(Dispatchers.IO) {
            val tracks = trackDao.getTracksByIds(trackIds)
            // Restore exact saved order, handling any deleted tracks
            val trackMap = tracks.associateBy { it.id }
            val orderedTracks = trackIds.mapNotNull { trackMap[it] }
            
            if (orderedTracks.isNotEmpty()) {
                val mediaItems = orderedTracks.map { track ->
                    val extras = android.os.Bundle().apply {
                        putBoolean("isAutoAppended", false)
                    }
                    val meta = MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setExtras(extras)
                    track.artworkPath?.let { meta.setArtworkUri(Uri.parse("file://$it")) }
                    MediaItem.Builder()
                        .setMediaId(track.id.toString())
                        .setUri(track.path)
                        .setMediaMetadata(meta.build())
                        .build()
                }
                
                withContext(Dispatchers.Main) {
                    controller.setMediaItems(mediaItems, savedIndex.coerceIn(0, mediaItems.lastIndex), savedPosition)
                    controller.prepare()
                }
            }
        }
    }
}
