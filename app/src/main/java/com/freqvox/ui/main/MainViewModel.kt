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

    val allPlaylists: StateFlow<List<PlaylistEntity>> = playlistRepository.allPlaylists
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<TrackEntity>> = combine(activeTracks, _searchQuery, taggedTrackIds) { tracks, query, tags ->
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
        val track = currentTrack.value ?: return
        viewModelScope.launch {
            // Find 'liked' tag case-insensitively
            val likedTag = allTags.value.find { it.name.equals("liked", ignoreCase = true) }
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

    fun createManualPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createManualPlaylist(name)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            playlistRepository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    fun deleteTrack(trackId: Long) {
        viewModelScope.launch {
            trackRepository.deleteTrack(trackId)
        }
    }

    fun deletePlaylist(playlist: com.freqvox.data.local.entities.PlaylistEntity) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlist)
        }
    }

    suspend fun getTracksForPlaylist(playlist: com.freqvox.data.local.entities.PlaylistEntity): List<TrackEntity> =
        playlistRepository.getTracksForPlaylist(playlist)

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
