package com.freqvox.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val playlistRepository: PlaylistRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    // Threshold for skip tracker: 30 days
    private val skipThresholdMs = 30L * 24 * 60 * 60 * 1000

    // All tracks for searching/displaying everything
    val allTracks: StateFlow<List<TrackEntity>> = trackRepository.allTracks
        .stateIn(
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
        taggedTrackIds
    ) { tracks, tags ->
        val disliked = tags["disliked"] ?: emptySet()
        tracks.filter { it.id !in disliked }
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<List<TrackEntity>> = combine(activeTracks, _searchQuery.debounce(180), taggedTrackIds) { tracks, query, tags ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val normalizedQuery = query.replace(Regex("[^A-Za-z0-9]"), "").lowercase()
            
            // Allow exact matches on tag names first
            val matchingIds = tags.filterKeys { 
                it.replace(Regex("[^A-Za-z0-9]"), "").lowercase().contains(normalizedQuery)
            }.values.flatten().toSet()
            
            tracks.filter { track ->
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

    init {
        scanLibrary()
    }

    fun scanLibrary() {
        viewModelScope.launch {
            trackRepository.scanLibrary()
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
        _searchQuery.value = query
    }

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
        viewModelScope.launch {
            trackRepository.toggleDislikeForTrack(trackId)
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

    fun createCustomTag(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            // Generate a random vibrant color for the new tag
            val colors = listOf(0xFF7C4DFF, 0xFF4DFFB4, 0xFFFF4D7C, 0xFF42A5F5, 0xFFFF6D00, 0xFFFFA726, 0xFFE040FB)
            val randomColor = colors.random().toInt()
            trackRepository.createCustomTag(name.trim(), randomColor)
        }
    }
}
