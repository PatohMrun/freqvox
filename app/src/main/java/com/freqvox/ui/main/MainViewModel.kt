package com.freqvox.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.freqvox.data.artwork.ArtworkPreferences
import com.freqvox.data.artwork.VidmateThumbnailSource
import com.freqvox.data.backup.BackupRepository
import com.freqvox.data.repository.AddTrackResult
import com.freqvox.data.repository.TrackRepository
import com.freqvox.data.repository.PlaylistRepository
import com.freqvox.playback.PlaybackController
import com.freqvox.data.local.entities.PlayHistoryEntity
import com.freqvox.data.local.entities.PlaylistEntity
import com.freqvox.data.local.entities.TagEntity
import com.freqvox.data.local.entities.TrackEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val playlistRepository: PlaylistRepository,
    private val backupRepository: BackupRepository,
    private val playbackController: PlaybackController,
    private val artworkPrefs: ArtworkPreferences,
    private val vidmateSource: VidmateThumbnailSource
) : ViewModel() {

    // Threshold for skip tracker: 30 days
    private val skipThresholdMs = 30L * 24 * 60 * 60 * 1000

    // True while a library scan is running. The UI uses it (together with an empty
    // library) to show a "Scanning…" state, so a fresh install doesn't look like the
    // app failed to detect any music.
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Hidden folders — declared first so the track flows below can filter on it.
    private val _excludedFolders = MutableStateFlow(trackRepository.getExcludedFolders())
    val excludedFolders: StateFlow<Set<String>> = _excludedFolders.asStateFlow()

    // All tracks, minus any in hidden folders. Excluded tracks stay in the DB with
    // their playlist/tag/stat relations intact — they're only hidden from view here.
    val allTracks: StateFlow<List<TrackEntity>> = combine(
        trackRepository.allTracks, _excludedFolders
    ) { tracks, excluded ->
        if (excluded.isEmpty()) tracks else tracks.filter { it.folderPath !in excluded }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTags: StateFlow<List<TagEntity>> = trackRepository.allTags
        .map { tags -> tags.distinctBy { it.name.lowercase() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val playHistory: StateFlow<List<PlayHistoryEntity>> = trackRepository.getRecentPlayHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val taggedTrackIds: StateFlow<Map<String, Set<Long>>> = combine(
        trackRepository.allTags,
        trackRepository.allTrackTagCrossRefs
    ) { tags, crossRefs ->
        val tagIdToName = tags.associate { it.id to it.name.lowercase() }
        val result = mutableMapOf<String, MutableSet<Long>>()
        crossRefs.forEach { ref ->
            val name = tagIdToName[ref.tagId] ?: return@forEach
            result.getOrPut(name) { mutableSetOf() }.add(ref.trackId)
        }
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // Active tracks: skip-tracker hidden + disliked excluded from home/search/shuffle pools
    val activeTracks: StateFlow<List<TrackEntity>> = combine(
        trackRepository.getActiveTracks(System.currentTimeMillis() - skipThresholdMs),
        taggedTrackIds,
        _excludedFolders
    ) { tracks, tags, excluded ->
        val disliked = tags["disliked"] ?: emptySet()
        tracks.filter { it.id !in disliked && it.folderPath !in excluded }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPlaylists: StateFlow<List<PlaylistEntity>> = playlistRepository.allPlaylists
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Per-playlist track count for every playlist in the table. Lets the
    // Library render real counts on collapsed rows instead of "Tap to load".
    val playlistTrackCounts: StateFlow<Map<Long, Int>> = playlistRepository.playlistTrackCounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _currentTrackId = MutableStateFlow<Long?>(null)

    val currentTrack: StateFlow<TrackEntity?> = combine(_currentTrackId, allTracks) { id, tracks ->
        id?.let { tracks.find { t -> t.id == it } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTrackTags: StateFlow<List<TagEntity>> = _currentTrackId
        .flatMapLatest { id ->
            if (id != null) {
                trackRepository.getTagsForTrack(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isCurrentTrackLiked: StateFlow<Boolean> = currentTrackTags
        .map { tags -> tags.any { it.name.lowercase() == "liked" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isCurrentTrackDisliked: StateFlow<Boolean> = currentTrackTags
        .map { tags -> tags.any { it.name.lowercase() == "disliked" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Flat set of every track id that currently carries the "disliked" tag.
     *  UI rows use it to flip their 3-dot menu copy between "Dislike track"
     *  and "Remove dislike" without each row having to observe its own tags. */
    val dislikedTrackIds: StateFlow<Set<Long>> = taggedTrackIds
        .map { it["disliked"] ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** One-shot event emitted whenever a dislike is toggled. MainScreen
     *  collects it to show the snackbar (and skip the currently-playing
     *  track when its dislike was *just* added). */
    data class DislikeToggled(
        val trackId: Long,
        val isNowDisliked: Boolean,
        val isCurrentTrack: Boolean
    )

    private val _dislikeEvents = MutableSharedFlow<DislikeToggled>(extraBufferCapacity = 4)
    val dislikeEvents: SharedFlow<DislikeToggled> = _dislikeEvents.asSharedFlow()

    /** Generic one-shot snackbar channel. Any composable in the app can post a
     *  human-readable message that the root SnackbarHost (in MainScreen) will
     *  render. Used by search surfaces — which don't carry their own host — to
     *  surface "Added to <playlist>" / "Song deleted from device" feedback the
     *  same way Library does. */
    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    fun postSnackbar(message: String) {
        viewModelScope.launch { _snackbarMessages.emit(message) }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Set to a tag name when the user taps a tag chip in Search, null otherwise.
    // Lets `searchResults` distinguish "user tapped the Disliked chip" (= strict
    // tag-only view of what they hid) from "user happened to type the word
    // disliked" (= normal keyword pipeline, so a track legitimately named
    // "Disliked Mix" can still be found). Cleared on any direct text edit
    // because the user is now expressing a free-form search, not a chip click.
    private val _searchTagFilter = MutableStateFlow<String?>(null)
    val searchTagFilter: StateFlow<String?> = _searchTagFilter.asStateFlow()

    // Bumped on every chip tap so re-tapping the same tag (e.g. Disliked) still
    // re-runs searchResults even when query/tagFilter values are unchanged.
    private val _tagFilterGeneration = MutableStateFlow(0L)

    // Pair filter + generation — kotlinx combine only has typed overloads for ≤5 flows.
    private val searchTagSelection: Flow<Pair<String?, Long>> = combine(
        _searchTagFilter,
        _tagFilterGeneration
    ) { filter, generation -> filter to generation }

    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<List<TrackEntity>> = combine(
        allTracks,
        activeTracks,
        _searchQuery.debounce(180),
        taggedTrackIds,
        searchTagSelection
    ) { all, active, query, tags, tagSelection ->
        val tagFilter = tagSelection.first
        if (query.isBlank()) {
            emptyList()
        } else {
            val normalizedQuery = query.replace(Regex("[^A-Za-z0-9]"), "").lowercase()

            // ── Strict "Disliked" chip path ──────────────────────────────────
            // Triggered ONLY when the user tapped the Disliked chip (tagFilter
            // is set), not when they typed the word "disliked". Two reasons we
            // need a dedicated branch here:
            //   1. activeTracks excludes disliked tracks by design, so the
            //      normal pipeline would always return zero results for the
            //      Disliked chip — making it look broken. Source from
            //      allTracks here so the user can actually see what they hid.
            //   2. The generic query path ORs in title/artist/album keyword
            //      matches, which would let a song *titled* "Disliked Mix"
            //      pollute this view. The chip should mean "show me what I
            //      hid", nothing else.
            //
            // Typing "disliked" still goes through the normal pipeline below,
            // so a track legitimately named "Disliked …" remains findable by
            // typing — it just won't surface from the chip tap.
            if (tagFilter == "disliked") {
                val dislikedIds = tags["disliked"] ?: emptySet()
                return@combine all.filter { it.id in dislikedIds }
            }

            // Allow exact matches on tag names first
            val matchingIds = tags.filterKeys { 
                it.replace(Regex("[^A-Za-z0-9]"), "").lowercase().contains(normalizedQuery)
            }.values.flatten().toSet()
            
            active.filter { track ->
                val normTitle = track.title.replace(Regex("[^A-Za-z0-9]"), "").lowercase()
                val normArtist = track.artist.replace(Regex("[^A-Za-z0-9]"), "").lowercase()
                val normAlbum = track.album.replace(Regex("[^A-Za-z0-9]"), "").lowercase()
                
                normTitle.contains(normalizedQuery) ||
                normArtist.contains(normalizedQuery) ||
                normAlbum.contains(normalizedQuery) ||
                matchingIds.contains(track.id)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentlyAdded: StateFlow<List<TrackEntity>> = activeTracks
        .map { tracks -> tracks.take(10) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalSongs: StateFlow<Int> = allTracks
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalDuration: StateFlow<Long> = allTracks
        .map { tracks -> tracks.sumOf { it.duration } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // ── Listening stats ──────────────────────────────────────────────────────
    // Surfaces the listening data the app already records (playCount / skipCount /
    // totalPlayTime on each track + the recent play_history rows) as user-facing
    // insight. Recomputed reactively from allTracks + playHistory.
    data class ListeningStats(
        val totalMinutes: Long = 0,
        val totalPlays: Int = 0,
        val uniqueTracks: Int = 0,
        val addedThisMonth: Int = 0,
        val peakHour: Int? = null,
        val hourBuckets: List<Int> = List(24) { 0 },
        val topTracks: List<Pair<TrackEntity, Int>> = emptyList(),
        val mostSkipped: List<Pair<TrackEntity, Int>> = emptyList()
    )

    val listeningStats: StateFlow<ListeningStats> = combine(allTracks, playHistory) { tracks, history ->
        // MediaStore DATE_ADDED is in seconds, so compare against a seconds threshold.
        val monthAgoSec = System.currentTimeMillis() / 1000 - 30L * 86_400

        val hourBuckets = IntArray(24)
        history.forEach { ph ->
            val hour = Calendar.getInstance().apply { timeInMillis = ph.playedAt }.get(Calendar.HOUR_OF_DAY)
            if (hour in 0..23) hourBuckets[hour]++
        }
        val peakHour = if (history.isEmpty()) null
            else hourBuckets.indices.maxByOrNull { hourBuckets[it] }

        ListeningStats(
            totalMinutes = tracks.sumOf { it.totalPlayTime } / 60_000,
            totalPlays = tracks.sumOf { it.playCount },
            uniqueTracks = tracks.count { it.playCount > 0 },
            addedThisMonth = tracks.count { it.dateAdded > monthAgoSec },
            peakHour = peakHour,
            hourBuckets = hourBuckets.toList(),
            topTracks = tracks.filter { it.playCount > 0 }
                .sortedByDescending { it.playCount }
                .take(5)
                .map { it to it.playCount },
            mostSkipped = tracks.filter { it.skipCount > 0 }
                .sortedByDescending { it.skipCount }
                .take(5)
                .map { it to it.skipCount }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListeningStats())

    // ── Hidden folders ─────────────────────────────────────────────────────────
    // Draws from the UNFILTERED library so hidden folders remain listed (and can be
    // re-enabled). Toggling just updates the flow; the filtered allTracks/activeTracks
    // recompute reactively — no destructive rescan, so nothing is ever deleted.
    val availableFolders: StateFlow<List<String>> = combine(
        trackRepository.allTracks, _excludedFolders
    ) { tracks, excluded ->
        (tracks.map { it.folderPath }.toSet() + excluded).sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFolderExclusion(folder: String) {
        val updated = _excludedFolders.value.toMutableSet().apply {
            if (!add(folder)) remove(folder)
        }
        _excludedFolders.value = updated
        trackRepository.setExcludedFolders(updated)
    }

    // ── Online album-art: one-time consent + per-transport settings ────────────
    // consentShown is the only default-false flag: online fetching stays blocked
    // until the user has seen the disclosure, so a fresh install never fires network
    // traffic before they've read anything. The toggles default on ("works out of
    // the box") and are surfaced for granular control.
    private val _showArtworkConsent = MutableStateFlow(!artworkPrefs.consentShown)
    val showArtworkConsent: StateFlow<Boolean> = _showArtworkConsent.asStateFlow()

    private val _artworkOnlineEnabled = MutableStateFlow(artworkPrefs.onlineEnabled)
    val artworkOnlineEnabled: StateFlow<Boolean> = _artworkOnlineEnabled.asStateFlow()

    private val _artworkAllowCellular = MutableStateFlow(artworkPrefs.allowCellular)
    val artworkAllowCellular: StateFlow<Boolean> = _artworkAllowCellular.asStateFlow()

    private val _artworkAllowMeteredWifi = MutableStateFlow(artworkPrefs.allowMeteredWifi)
    val artworkAllowMeteredWifi: StateFlow<Boolean> = _artworkAllowMeteredWifi.asStateFlow()

    private var artworkBackfillJob: Job? = null

    /** Called when the user dismisses the one-time disclosure. Unblocks online fetching. */
    fun onArtworkConsentAcknowledged() {
        artworkPrefs.consentShown = true
        _showArtworkConsent.value = false
        launchArtworkBackfill()
    }

    fun setArtworkOnlineEnabled(enabled: Boolean) {
        artworkPrefs.onlineEnabled = enabled
        _artworkOnlineEnabled.value = enabled
        if (enabled) launchArtworkBackfill()
    }

    fun setArtworkAllowCellular(allow: Boolean) {
        artworkPrefs.allowCellular = allow
        _artworkAllowCellular.value = allow
        if (allow) launchArtworkBackfill()
    }

    fun setArtworkAllowMeteredWifi(allow: Boolean) {
        artworkPrefs.allowMeteredWifi = allow
        _artworkAllowMeteredWifi.value = allow
        if (allow) launchArtworkBackfill()
    }

    /** Clears the negative cache and re-attempts art for everything still missing it. */
    fun retryFailedArtwork() {
        artworkPrefs.clearFailedLookups()
        launchArtworkBackfill()
    }

    // Number of songs linked from a VidMate folder (0 = not linked). Drives the settings row.
    private val _vidmateLinkedCount = MutableStateFlow(vidmateSource.linkedCount())
    val vidmateLinkedCount: StateFlow<Int> = _vidmateLinkedCount.asStateFlow()

    /** Persists the SAF-granted VidMate folder, parses its DB, and re-runs the backfill so
     *  previously-missed tracks get their exact thumbnail. (Caller must already have taken
     *  persistable read permission on [uri].) */
    fun linkVidmateFolder(uri: Uri) {
        viewModelScope.launch {
            artworkPrefs.vidmateTreeUri = uri.toString()
            val count = vidmateSource.rebuildFromTree(uri)
            _vidmateLinkedCount.value = count
            artworkPrefs.clearFailedLookups()
            launchArtworkBackfill()
            postSnackbar(
                if (count > 0) "VidMate linked · $count songs"
                else "Couldn't read that VidMate folder"
            )
        }
    }

    // Single in-flight backfill; cancel any previous so settings changes don't stack.
    private fun launchArtworkBackfill() {
        if (!artworkPrefs.consentShown) return
        artworkBackfillJob?.cancel()
        artworkBackfillJob = viewModelScope.launch {
            trackRepository.backfillOnlineArtwork()
        }
    }

    // Demand-driven priority: whenever the playing track lacks art, fetch it first
    // (post-consent) so the user sees a cover for what they're actually listening to.
    private fun observeNowPlayingArtwork() {
        viewModelScope.launch {
            currentTrack.collect { track ->
                if (track != null && track.artworkPath == null && artworkPrefs.consentShown) {
                    launch { trackRepository.fetchOnlineArtwork(track) }
                }
            }
        }
    }

    init {
        scanLibrary()
        observeNowPlayingArtwork()
    }

    fun scanLibrary() {
        viewModelScope.launch {
            // isScanning drives the "Scanning…" UI on an empty library. It clears once
            // the scan returns; tracks appear via the allTracks flow as soon as they're
            // upserted, so the list replaces the indicator without waiting for artwork.
            _isScanning.value = true
            try {
                trackRepository.scanLibrary()
            } finally {
                _isScanning.value = false
            }
            // Local art is resolved inside scanLibrary(); kick off the online backfill
            // afterward so cheap offline sources win first. No-op until consent is given.
            launchArtworkBackfill()
        }
    }

    fun updateCurrentTrack(path: String?) {
        // Obsolete but kept for backwards compatibility if needed, 
        // ideally find ID from path and call updateCurrentTrackById
        allTracks.value.find { it.path == path }?.let {
            updateCurrentTrackById(it.id)
        }
    }

    fun updateCurrentTrackById(id: Long?) {
        _currentTrackId.value = id
    }

    fun onSearchQueryChange(query: String) {
        // Leave chip-tap mode only when the text no longer matches the active
        // tag name (e.g. user edits "disliked" → "dislike"). Re-tapping the
        // chip always restores strict mode via applyTagFilter.
        val activeTag = _searchTagFilter.value
        if (activeTag != null && normalizeSearchToken(query) != normalizeSearchToken(activeTag)) {
            _searchTagFilter.value = null
        }
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchTagFilter.value = null
        _searchQuery.value = ""
    }

    /** Called from the Search tab's tag chips. Sets both the visible query
     *  text and the strict tag-filter marker so `searchResults` can
     *  distinguish this from someone typing the tag's name by hand. */
    fun applyTagFilter(tagName: String) {
        _searchTagFilter.value = tagName.lowercase()
        _searchQuery.value = tagName
        _tagFilterGeneration.value += 1
    }

    private fun normalizeSearchToken(text: String): String =
        text.replace(Regex("[^A-Za-z0-9]"), "").lowercase()

    fun toggleTagForCurrentTrack(tagId: Long) {
        val track = currentTrack.value ?: return
        viewModelScope.launch {
            trackRepository.toggleTagForTrack(track.id, tagId)
        }
    }

    fun toggleLike() {
        val trackId = _currentTrackId.value ?: return
        viewModelScope.launch {
            trackRepository.toggleLikeForTrack(trackId)
        }
    }

    fun toggleDislike() {
        val trackId = _currentTrackId.value ?: return
        toggleDislikeForTrack(trackId)
    }

    /** Toggle the dislike tag for any track (not just the playing one).
     *  Emits a [DislikeToggled] event so the UI can react with a snackbar
     *  and — when the currently-playing track was the one disliked — skip
     *  it so the user doesn't keep hearing the song they just rejected. */
    fun toggleDislikeForTrack(trackId: Long) {
        viewModelScope.launch {
            val isNowDisliked = trackRepository.toggleDislikeForTrack(trackId)
            _dislikeEvents.emit(
                DislikeToggled(
                    trackId = trackId,
                    isNowDisliked = isNowDisliked,
                    isCurrentTrack = trackId == _currentTrackId.value
                )
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val playMoreLikeThis: Flow<List<TrackEntity>> = currentTrack
        .flatMapLatest { track ->
            if (track != null) {
                flow { emit(trackRepository.getSimilarTracks(track.id)) }
            } else {
                flowOf(emptyList())
            }
        }

    fun createDynamicPlaylist(name: String, includeTags: List<Long>) {
        viewModelScope.launch {
            playlistRepository.createDynamicPlaylist(name, includeTags)
        }
    }

    fun createManualPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createManualPlaylist(name)
        }
    }

    /** Saves the current playback queue as a new manual playlist. */
    fun saveQueueAsPlaylist(name: String) {
        val trackIds = playbackController.queue.value.mapNotNull { it.trackId.toLongOrNull() }
        if (name.isBlank() || trackIds.isEmpty()) {
            postSnackbar("Queue is empty")
            return
        }
        viewModelScope.launch {
            playlistRepository.createManualPlaylistWithTracks(name.trim(), trackIds)
            postSnackbar("Saved \"${name.trim()}\" • ${trackIds.size} tracks")
        }
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long): AddTrackResult =
        playlistRepository.addTrackToPlaylist(playlistId, trackId)

    /** Called after the system delete confirmation (or legacy resolver delete) succeeds. */
    fun completeTrackDeletion(trackId: Long) {
        viewModelScope.launch {
            trackRepository.purgeTrackFromLibrary(trackId)
            playbackController.removeTrackFromLibrary(trackId)
        }
    }

    fun deletePlaylist(playlist: com.freqvox.data.local.entities.PlaylistEntity) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlist)
        }
    }

    suspend fun getTracksForPlaylist(playlist: com.freqvox.data.local.entities.PlaylistEntity): List<TrackEntity> =
        playlistRepository.getTracksForPlaylist(playlist)

    // ── Reactive expanded-playlist tracks ────────────────────────────────────
    // The Library UI used to one-shot snapshot a playlist's tracks via .first()
    // and stash them in a Compose state. That meant adding/removing a song or
    // toggling a tag (for smart playlists) did NOT update the open list — the
    // "playlists fail to update" bug. Now we observe the underlying flow so any
    // mutation propagates straight to the UI.
    private val _expandedPlaylistId = MutableStateFlow<Long?>(null)
    val expandedPlaylistId: StateFlow<Long?> = _expandedPlaylistId.asStateFlow()

    // Internal flow that carries the playlist id alongside the tracks, so a
    // consumer can ignore an emission whose id doesn't match what's currently
    // expanded. This is the race-safe pattern: even if `stateIn` still holds
    // the previously expanded playlist's tracks for a frame, the UI can see
    // they belong to a stale id and render nothing instead of leaking them
    // under the new row.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val expandedPlaylistTracksWithId: StateFlow<Pair<Long, List<TrackEntity>>?> =
        _expandedPlaylistId
            .flatMapLatest { id ->
                if (id == null || id <= 0L) flowOf(null)
                else allPlaylists
                    .mapNotNull { lists -> lists.find { it.id == id } }
                    .distinctUntilChanged()
                    .flatMapLatest { playlist ->
                        playlistRepository.observeTracksForPlaylist(playlist)
                            .map { tracks -> playlist.id to tracks }
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val expandedPlaylistTracks: StateFlow<List<TrackEntity>> = combine(
        _expandedPlaylistId, expandedPlaylistTracksWithId
    ) { currentId, tagged ->
        if (tagged != null && currentId != null && tagged.first == currentId) tagged.second
        else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setExpandedPlaylist(id: Long?) {
        _expandedPlaylistId.value = id
    }

    // ── Home (and elsewhere): tag a specific track without opening the player ──
    private val _taggingTrackId = MutableStateFlow<Long?>(null)
    val taggingTrackId: StateFlow<Long?> = _taggingTrackId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val taggingTrackTags: StateFlow<List<TagEntity>> = _taggingTrackId
        .flatMapLatest { id ->
            if (id != null) trackRepository.getTagsForTrack(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openTagPicker(trackId: Long) {
        _taggingTrackId.value = trackId
    }

    fun closeTagPicker() {
        _taggingTrackId.value = null
    }

    fun toggleTagForTaggingTrack(tagId: Long) {
        val trackId = _taggingTrackId.value ?: return
        viewModelScope.launch {
            trackRepository.toggleTagForTrack(trackId, tagId)
        }
    }

    // ── Backup / Restore ──────────────────────────────────────────────────────
    // Both run off a user-picked SAF Uri, so no storage permission is involved.
    // Feedback is routed through the global snackbar so it shows from any screen.
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupRepository.exportToUri(uri)
            postSnackbar(
                when (result) {
                    is BackupRepository.Result.Success -> result.message
                    is BackupRepository.Result.Error -> "Backup failed: ${result.message}"
                }
            )
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupRepository.importFromUri(uri)
            postSnackbar(
                when (result) {
                    is BackupRepository.Result.Success -> result.message
                    is BackupRepository.Result.Error -> "Restore failed: ${result.message}"
                }
            )
        }
    }

    fun createCustomTag(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            // Generate a random vibrant color for the new tag
            val colors = listOf(0xFF7C4DFF, 0xFF4DFFB4, 0xFFFF4D7C, 0xFF42A5F5, 0xFFFF6D00, 0xFFFFA726, 0xFFE040FB)
            val randomColor = colors.random().toInt()
            trackRepository.createCustomTag(name.trim(), randomColor)
        }
    }

    // ── Tag management ──────────────────────────────────────────────────────────
    fun updateTag(tag: TagEntity, newName: String, newColor: Int) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            trackRepository.updateTag(tag.copy(name = newName.trim().lowercase(), color = newColor))
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            trackRepository.deleteTag(tag)
            // Also strip the tag from any smart-playlist queries so they don't
            // silently break with a dangling tag id.
            playlistRepository.removeTagFromQueries(tag.id)
        }
    }
}
