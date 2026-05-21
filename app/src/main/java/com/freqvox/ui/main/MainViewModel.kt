package com.freqvox.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freqvox.data.repository.TrackRepository
import com.freqvox.data.repository.PlaylistRepository
import com.freqvox.data.local.entities.PlayHistoryEntity
import com.freqvox.data.local.entities.PlaylistEntity
import com.freqvox.data.local.entities.TagEntity
import com.freqvox.data.local.entities.TrackEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val playlistRepository: PlaylistRepository
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

    // Only "Active" tracks (not hidden by skip tracker)
    val activeTracks: StateFlow<List<TrackEntity>> = trackRepository.getActiveTracks(
        System.currentTimeMillis() - skipThresholdMs
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTags: StateFlow<List<TagEntity>> = trackRepository.allTags
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

    val allPlaylists: StateFlow<List<PlaylistEntity>> = playlistRepository.allPlaylists
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentTrackPath = MutableStateFlow<String?>(null)
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrackPath
        .map { path -> allTracks.value.find { it.path == path } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTrackTags: StateFlow<List<TagEntity>> = _currentTrackPath
        .flatMapLatest { path ->
            val track = allTracks.value.find { it.path == path }
            if (track != null) {
                trackRepository.getTagsForTrack(track.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isCurrentTrackLiked: StateFlow<Boolean> = currentTrackTags
        .map { tags -> tags.any { it.name.lowercase() == "liked" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<TrackEntity>> = combine(activeTracks, _searchQuery) { tracks, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            tracks.filter { track ->
                track.title.contains(query, ignoreCase = true) ||
                track.artist.contains(query, ignoreCase = true) ||
                track.album.contains(query, ignoreCase = true)
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
        _currentTrackPath.value = path
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
        val track = currentTrack.value ?: return
        viewModelScope.launch {
            var likedTag = allTags.value.find { it.name.lowercase() == "liked" }
            if (likedTag == null) {
                // Should not happen as per ensureDefaultTags, but safety first
                trackRepository.scanLibrary() // will trigger ensureDefaultTags
                likedTag = allTags.value.find { it.name.lowercase() == "liked" }
            }
            
            if (likedTag != null) {
                trackRepository.toggleTagForTrack(track.id, likedTag.id)
            }
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
}
