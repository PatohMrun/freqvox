package com.freqvox.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.freqvox.data.artwork.ArtworkPreferences
import com.freqvox.data.artwork.VidmateThumbnailSource
import com.freqvox.data.prefs.LibraryUiPreferences
import com.freqvox.playback.CrossfadePreferences
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/** Shared compiled normalizer — the search pipeline used to construct this same Regex
 *  inline ~4x per track per keystroke emission (plus the duplicate grouping sweeps). */
private val NON_ALNUM_REGEX = Regex("[^A-Za-z0-9]")

@HiltViewModel
class MainViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val playlistRepository: PlaylistRepository,
    private val backupRepository: BackupRepository,
    private val playbackController: PlaybackController,
    private val artworkPrefs: ArtworkPreferences,
    private val vidmateSource: VidmateThumbnailSource,
    private val libraryUiPrefs: LibraryUiPreferences,
    private val crossfadePrefs: CrossfadePreferences,
    private val themePrefs: com.freqvox.data.prefs.ThemePreferences,
    private val sidePanelPrefs: com.freqvox.data.prefs.SidePanelPreferences,
    private val accentPrefs: com.freqvox.data.prefs.AccentPreferences,
    private val tagProfilePrefs: com.freqvox.data.prefs.TagProfilePreferences,
    private val storyScheduler: com.freqvox.ui.story.StoryScheduler
) : ViewModel() {

    // ── Per-tag playback profiles (tag → EQ preset) ────────────────────────────
    private val _tagProfiles = MutableStateFlow(tagProfilePrefs.all())
    val tagProfiles: StateFlow<Map<String, Int>> = _tagProfiles.asStateFlow()
    val eqPresets: List<String> get() = playbackController.equalizerManager.getPresets()

    fun setTagProfile(tag: String, presetIndex: Int) {
        if (presetIndex < 0) tagProfilePrefs.removeProfile(tag) else tagProfilePrefs.setProfile(tag, presetIndex)
        _tagProfiles.value = tagProfilePrefs.all()
    }

    // Apply state: remember the preset that was active before a profile took over, to restore
    // it when the next track has no profiled tag.
    private var tagProfileActive = false
    private var tagProfileSavedPreset = -1

    /** Started from an init block below currentTrackTags (init blocks run in declaration order). */
    private fun observeTagProfiles() {
        viewModelScope.launch {
            combine(currentTrackTags, _tagProfiles) { tags, profiles -> tags to profiles }
                .collect { (tags, profiles) -> applyTagProfile(tags, profiles) }
        }
    }

    private fun applyTagProfile(tags: List<TagEntity>, profiles: Map<String, Int>) {
        val eq = playbackController.equalizerManager
        // Only touch the EQ once it's initialised — calling usePreset on an uninitialised
        // equalizer would persist zeroed band levels.
        if (eq.numBands <= 0) return
        val preset = tags.firstNotNullOfOrNull { profiles[it.name.lowercase()] }
        if (preset != null) {
            if (!tagProfileActive) {
                tagProfileSavedPreset = eq.currentPreset.value.toInt()
                tagProfileActive = true
            }
            eq.usePreset(preset.toShort())
            if (!eq.isEnabled.value) eq.setEnabled(true)
        } else if (tagProfileActive) {
            tagProfileActive = false
            if (tagProfileSavedPreset >= 0) eq.usePreset(tagProfileSavedPreset.toShort())
        }
    }

    // Side-panel accordion expand/collapse, persisted so it survives drawer close + app restart.
    fun isPanelSectionExpanded(title: String, default: Boolean) =
        sidePanelPrefs.isSectionExpanded(title, default)
    fun setPanelSectionExpanded(title: String, expanded: Boolean) =
        sidePanelPrefs.setSectionExpanded(title, expanded)

    // Fixed-accent (pin the gradient palette to one colour instead of album-derived).
    private val _fixedAccentEnabled = MutableStateFlow(accentPrefs.enabled)
    val fixedAccentEnabled: StateFlow<Boolean> = _fixedAccentEnabled.asStateFlow()
    private val _fixedAccentColor = MutableStateFlow(accentPrefs.color)
    val fixedAccentColor: StateFlow<Int> = _fixedAccentColor.asStateFlow()
    fun setFixedAccentEnabled(enabled: Boolean) {
        accentPrefs.enabled = enabled
        _fixedAccentEnabled.value = enabled
    }
    fun setFixedAccentColor(color: Int) {
        accentPrefs.color = color
        _fixedAccentColor.value = color
    }
    // Whether the chosen accent ALSO pins the background gradient (off = gradient stays
    // album-dynamic while the UI controls follow the swatch).
    private val _accentPinGradient = MutableStateFlow(accentPrefs.pinGradient)
    val accentPinGradient: StateFlow<Boolean> = _accentPinGradient.asStateFlow()
    fun setAccentPinGradient(pin: Boolean) {
        accentPrefs.pinGradient = pin
        _accentPinGradient.value = pin
    }

    // Theme mode for the in-header toggle. The actual palette selection happens at the
    // composition root (MainActivity), which observes the same singleton StateFlow.
    val themeMode: StateFlow<com.freqvox.data.prefs.ThemeMode> = themePrefs.mode
    fun cycleTheme() = themePrefs.cycle()

    // ── Crossfade (0s = off / gapless) ─────────────────────────────────────────
    private val _crossfadeSeconds = MutableStateFlow(crossfadePrefs.durationSeconds)
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()
    fun setCrossfadeSeconds(seconds: Int) {
        crossfadePrefs.durationSeconds = seconds
        _crossfadeSeconds.value = crossfadePrefs.durationSeconds
    }

    // Album mode: skip crossfade between consecutive same-album tracks (only meaningful when
    // crossfade is on). Persisted; read live by the CrossfadeController on the next transition.
    private val _albumMode = MutableStateFlow(crossfadePrefs.albumMode)
    val albumMode: StateFlow<Boolean> = _albumMode.asStateFlow()
    fun setAlbumMode(enabled: Boolean) {
        crossfadePrefs.albumMode = enabled
        _albumMode.value = crossfadePrefs.albumMode
    }

    // Volume normalization (ReplayGain, attenuate-only). Persisted; read live by CrossfadeController.
    private val _volumeNormalization = MutableStateFlow(crossfadePrefs.volumeNormalization)
    val volumeNormalization: StateFlow<Boolean> = _volumeNormalization.asStateFlow()
    fun setVolumeNormalization(enabled: Boolean) {
        crossfadePrefs.volumeNormalization = enabled
        _volumeNormalization.value = crossfadePrefs.volumeNormalization
    }

    // ── Library pill state (persisted so it survives navigation + restarts) ────
    /** Last-opened Library tab name, or null if never set. */
    val libraryLastTab: String? get() = libraryUiPrefs.lastTab
    fun setLibraryLastTab(name: String) { libraryUiPrefs.lastTab = name }

    /** Persisted Library pill order as tab names (empty = default order). */
    val libraryTabOrder: List<String> get() = libraryUiPrefs.tabOrder
    fun setLibraryTabOrder(order: List<String>) { libraryUiPrefs.tabOrder = order }

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
    }.flowOn(Dispatchers.Default).stateIn(
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
    }.flowOn(Dispatchers.Default).stateIn(
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

    // Now that currentTrackTags exists, start applying per-tag playback profiles.
    init { observeTagProfiles() }

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

    // Keyword pipeline only. Tag-driven views (including "show me what I disliked")
    // live entirely in the boolean tag filter below — one tag model, not two. The old
    // strict "chip mode" (_searchTagFilter + generation counter) is gone with the
    // results-mode chip row that drove it.
    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<List<TrackEntity>> = combine(
        activeTracks,
        _searchQuery.debounce(180),
        taggedTrackIds
    ) { active, query, tags ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val normalizedQuery = query.replace(NON_ALNUM_REGEX, "").lowercase()

            // Allow exact matches on tag names first
            val matchingIds = tags.filterKeys {
                it.replace(NON_ALNUM_REGEX, "").lowercase().contains(normalizedQuery)
            }.values.flatten().toSet()

            active.filter { track ->
                val normTitle = track.title.replace(NON_ALNUM_REGEX, "").lowercase()
                val normArtist = track.artist.replace(NON_ALNUM_REGEX, "").lowercase()
                val normAlbum = track.album.replace(NON_ALNUM_REGEX, "").lowercase()

                normTitle.contains(normalizedQuery) ||
                normArtist.contains(normalizedQuery) ||
                normAlbum.contains(normalizedQuery) ||
                matchingIds.contains(track.id)
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ── Boolean tag filter (Search → Browse) ──────────────────────────────────
    // Tri-state per tag: absent = neutral, in include = REQUIRED (AND), in exclude
    // = FORBIDDEN (NOT). A track matches when it carries ALL include tags and NONE
    // of the exclude tags — e.g. "chill AND workout", "hype NOT throwback". Keyed by
    // lowercase tag name to line up with taggedTrackIds. Independent of the text query
    // (only surfaced in the blank-query browse view), so the two don't interfere.
    private val _includeTagNames = MutableStateFlow<Set<String>>(emptySet())
    val includeTagNames: StateFlow<Set<String>> = _includeTagNames.asStateFlow()
    private val _excludeTagNames = MutableStateFlow<Set<String>>(emptySet())
    val excludeTagNames: StateFlow<Set<String>> = _excludeTagNames.asStateFlow()

    /** Cycle a tag: neutral → require → exclude → neutral. */
    fun cycleTagFilterState(tagName: String) {
        val name = tagName.lowercase()
        val inc = _includeTagNames.value
        val exc = _excludeTagNames.value
        when {
            name in inc -> { _includeTagNames.value = inc - name; _excludeTagNames.value = exc + name }
            name in exc -> { _excludeTagNames.value = exc - name }
            else -> { _includeTagNames.value = inc + name }
        }
    }

    fun clearTagFilter() {
        _includeTagNames.value = emptySet()
        _excludeTagNames.value = emptySet()
    }

    val tagFilterResults: StateFlow<List<TrackEntity>> = combine(
        allTracks, activeTracks, taggedTrackIds, _includeTagNames, _excludeTagNames
    ) { all, active, tagMap, include, exclude ->
        if (include.isEmpty() && exclude.isEmpty()) emptyList()
        else {
            // Requiring "disliked" means "show me what I hid" — it must search the FULL
            // library, because activeTracks excludes disliked tracks by design (which made
            // that chip a guaranteed zero-result dead end).
            val source = if ("disliked" in include) all else active
            source.filter { track ->
                include.all { tag -> tagMap[tag]?.contains(track.id) == true } &&
                    exclude.none { tag -> tagMap[tag]?.contains(track.id) == true }
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Likely-duplicate groups: tracks sharing a normalized title+artist (case/punctuation
    // insensitive). Each group is sorted most-played first so the user keeps the "primary".
    val duplicateGroups: StateFlow<List<List<TrackEntity>>> = allTracks
        .map { tracks ->
            tracks.groupBy {
                (it.title + "|" + it.artist).replace(NON_ALNUM_REGEX, "").lowercase()
            }.values
                .filter { it.size > 1 }
                .map { grp -> grp.sortedByDescending { it.playCount } }
                .sortedByDescending { it.size }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Home feed (built here, on Dispatchers.Default — not during composition) ─
    // The feed used to be generated inside a remember{} while HomeScreen composed, blocking
    // the main thread for every page on open, refresh and pagination ("the waiter cooks at
    // the table"). Semantics preserved exactly:
    //  - per-page determinism: seed = refreshTrigger * 1_000_003L + page * 31L, unchanged;
    //  - cross-page artist/album exclusion: FeedSessionState threads page→page; the session
    //    AFTER each page is cached so growing pageCount appends ONLY the new page (the old
    //    remember{} regenerated every page on each pagination step);
    //  - pull-to-refresh: a new refreshTrigger misses the cache → fresh pages + fresh session;
    //  - a pageCount reset (Home re-entered) truncates to the deterministic prefix.
    data class FeedParams(val refreshTrigger: Int, val pageCount: Int)
    private val _feedParams = MutableStateFlow(FeedParams(0, 1))
    fun setFeedParams(refreshTrigger: Int, pageCount: Int) {
        _feedParams.value = FeedParams(refreshTrigger, pageCount)
    }

    private data class FeedCache(
        val tracks: List<TrackEntity>,
        val history: List<PlayHistoryEntity>,
        val tags: Map<String, Set<Long>>,
        val refreshTrigger: Int,
        val pages: List<List<com.freqvox.ui.home.FeedItem>>,
        /** Session state AFTER each page, so any prefix can be extended without replaying. */
        val sessions: List<com.freqvox.ui.home.FeedSessionState>
    )

    // Only ever touched inside the combine transform below, which runs serially in the
    // stateIn coroutine — no concurrent access.
    private var feedCache: FeedCache? = null

    val homeFeed: StateFlow<List<com.freqvox.ui.home.PagedFeedItem>> = combine(
        activeTracks, playHistory, taggedTrackIds, _feedParams
    ) { tracks, history, tags, params ->
        val cached = feedCache?.takeIf {
            it.refreshTrigger == params.refreshTrigger &&
                it.tracks == tracks && it.history == history && it.tags == tags
        }
        val keep = minOf(cached?.pages?.size ?: 0, params.pageCount)
        val pages = ArrayList<List<com.freqvox.ui.home.FeedItem>>(params.pageCount)
        val sessions = ArrayList<com.freqvox.ui.home.FeedSessionState>(params.pageCount)
        if (cached != null && keep > 0) {
            pages += cached.pages.take(keep)
            sessions += cached.sessions.take(keep)
        }
        var session = sessions.lastOrNull() ?: com.freqvox.ui.home.FeedSessionState()
        val ctx = com.freqvox.ui.home.FeedContext(history, tags)
        while (pages.size < params.pageCount && tracks.isNotEmpty()) {
            val page = pages.size + 1
            val pageRandom = kotlin.random.Random(params.refreshTrigger * 1_000_003L + page * 31L)
            val result = com.freqvox.ui.home.FeedGenerator.generateFeed(tracks, ctx, pageRandom, session)
            session = result.session
            pages += result.items
            sessions += session
        }
        feedCache = FeedCache(tracks, history, tags, params.refreshTrigger, pages, sessions)
        pages.flatMapIndexed { i, items -> items.map { com.freqvox.ui.home.PagedFeedItem(i + 1, it) } }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Mixes — date-seeded so they're stable all day; same off-main-thread motivation.
    val dailyMixes: StateFlow<List<com.freqvox.ui.home.DailyMix>> = combine(
        activeTracks, taggedTrackIds
    ) { tracks, tags ->
        com.freqvox.ui.home.generateDailyMixes(tracks, tags, java.time.LocalDate.now().toEpochDay())
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        val mostSkipped: List<Pair<TrackEntity, Int>> = emptyList(),
        val currentStreakDays: Int = 0,
        val weekMinutes: Long = 0,
        val weeklyGoalMinutes: Int = 300,   // gentle default: ~5 hours/week
        val topArtists: List<Pair<String, Int>> = emptyList(),  // artist → total plays
        val topTags: List<Pair<String, Int>> = emptyList(),     // tag name → total plays
        val weekDailyMinutes: List<Float> = emptyList(),        // last 7 days, normalized 0..1
        val activeDays14: List<Boolean> = emptyList(),          // last 14 days, listened?
        val newThisWeek: List<TrackEntity> = emptyList(),       // added in the last 7 days
        val topArtistTrack: TrackEntity? = null                 // a cover for the #1 artist
    )

    val listeningStats: StateFlow<ListeningStats> = combine(allTracks, playHistory, taggedTrackIds) { tracks, history, tagMap ->
        // MediaStore DATE_ADDED is in seconds, so compare against a seconds threshold.
        val monthAgoSec = System.currentTimeMillis() / 1000 - 30L * 86_400

        val hourBuckets = IntArray(24)
        history.forEach { ph ->
            val hour = Calendar.getInstance().apply { timeInMillis = ph.playedAt }.get(Calendar.HOUR_OF_DAY)
            if (hour in 0..23) hourBuckets[hour]++
        }
        val peakHour = if (history.isEmpty()) null
            else hourBuckets.indices.maxByOrNull { hourBuckets[it] }

        // Daily streak: consecutive days (ending today, or yesterday if nothing yet today) that
        // have at least one play. Uses epoch-day so it's DST/timezone/year-boundary safe.
        val zone = java.time.ZoneId.systemDefault()
        val playDays = history.mapTo(HashSet()) {
            java.time.Instant.ofEpochMilli(it.playedAt).atZone(zone).toLocalDate().toEpochDay()
        }
        val today = java.time.LocalDate.now()
        var streak = 0
        var cursor = if (today.toEpochDay() in playDays) today.toEpochDay() else today.toEpochDay() - 1
        while (cursor in playDays) { streak++; cursor-- }

        // Minutes actually listened so far this calendar week (Mon-anchored), from play_history.
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekStartMs = monday.atStartOfDay(zone).toInstant().toEpochMilli()
        val weekMinutes = history.filter { it.playedAt >= weekStartMs }.sumOf { it.durationPlayed } / 60_000

        // Recap data: per-day minutes (last 7), active-day flags (last 14), new adds, top-artist cover.
        val minutesByDay = LongArray(7)
        history.forEach { ph ->
            val d = java.time.Instant.ofEpochMilli(ph.playedAt).atZone(zone).toLocalDate()
            val delta = today.toEpochDay() - d.toEpochDay()
            if (delta in 0..6) minutesByDay[(6 - delta).toInt()] += ph.durationPlayed
        }
        val maxDay = (minutesByDay.maxOrNull() ?: 0L).coerceAtLeast(1L)
        val weekDailyMinutes = minutesByDay.map { (it.toFloat() / maxDay).coerceIn(0f, 1f) }
        val activeDays14 = (13 downTo 0).map { back -> (today.toEpochDay() - back) in playDays }
        val weekAgoSec = System.currentTimeMillis() / 1000 - 7L * 86_400
        val newThisWeek = tracks.filter { it.dateAdded > weekAgoSec }.sortedByDescending { it.dateAdded }.take(6)
        val topArtistName = tracks.filter { it.playCount > 0 && it.artist.isNotBlank() }
            .groupBy { it.artist }.maxByOrNull { e -> e.value.sumOf { it.playCount } }?.key
        val topArtistTrack = topArtistName?.let { name -> tracks.filter { it.artist == name }.maxByOrNull { it.playCount } }

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
                .map { it to it.skipCount },
            currentStreakDays = streak,
            weekMinutes = weekMinutes,
            topArtists = tracks.filter { it.playCount > 0 && it.artist.isNotBlank() }
                .groupBy { it.artist }
                .mapValues { e -> e.value.sumOf { it.playCount } }
                .entries.sortedByDescending { it.value }
                .take(6).map { it.key to it.value },
            topTags = run {
                val playById = tracks.associate { it.id to it.playCount }
                tagMap.filterKeys { !it.equals("disliked", ignoreCase = true) }
                    .mapValues { (_, ids) -> ids.sumOf { playById[it] ?: 0 } }
                    .filter { it.value > 0 }
                    .entries.sortedByDescending { it.value }
                    .take(6).map { it.key to it.value }
            },
            weekDailyMinutes = weekDailyMinutes,
            activeDays14 = activeDays14,
            newThisWeek = newThisWeek,
            topArtistTrack = topArtistTrack
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListeningStats())

    // ── Header story-ring ("Stories") ──────────────────────────────────────────
    private val storyEpochDay: Long = java.time.LocalDate.now().toEpochDay()
    private val _storySeenBump = MutableStateFlow(0)
    val activeStoryDrop: StateFlow<com.freqvox.ui.story.StoryDrop> = listeningStats
        .map { stats ->
            val tagged = taggedTrackIds.value.values.flatten().toHashSet()
            // Computed inline: duplicateGroups is WhileSubscribed and only collected while the
            // Duplicates sheet is open, so its .value here would almost always be the stale
            // initial emptyList and the "N duplicates" tip would never fire.
            val dupCount = allTracks.value
                .groupBy { (it.title + "|" + it.artist).replace(NON_ALNUM_REGEX, "").lowercase() }
                .values.count { it.size > 1 }
            val ctx = com.freqvox.ui.story.StoryContent.StoryState(
                crossfadeOff = crossfadePrefs.durationSeconds == 0,
                eqUnused = !playbackController.equalizerManager.isEnabled.value,
                untaggedCount = allTracks.value.count { it.id !in tagged },
                hasCustomTags = allTags.value.any { it.name.lowercase() !in setOf("liked", "disliked") },
                duplicateGroups = dupCount,
                mostSkippedTrack = stats.mostSkipped.firstOrNull()?.first,
                mostSkippedCount = stats.mostSkipped.firstOrNull()?.second ?: 0,
                fixedAccentDefault = !accentPrefs.enabled,
                hasProfiles = tagProfilePrefs.all().isNotEmpty()
            )
            storyScheduler.activeDrop(stats, com.freqvox.ui.story.StoryContent.contextualTips(ctx), storyEpochDay)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            // Placeholder until real stats emit — NOT a scheduler call. activeDrop()
            // persists its tip choice, and at construction the contextual pool is still
            // empty, so a persisted contextual tip looked missing and got re-picked on
            // EVERY app start: new tip → new drop id → the ring relit with different
            // content each launch. The ring just idles on the empty drop instead.
            com.freqvox.ui.story.StoryDrop("", storyEpochDay, com.freqvox.ui.story.SegmentKind.TIP, emptyList())
        )
    val hasUnseenStory: StateFlow<Boolean> = combine(activeStoryDrop, _storySeenBump) { drop, _ ->
        storyScheduler.isUnseen(drop)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** One flag per card of the active drop (true = displayed) — drives the segmented
     *  WhatsApp-style ring: one arc per card, greyed only where actually watched. */
    val storyCardSeenFlags: StateFlow<List<Boolean>> =
        combine(activeStoryDrop, _storySeenBump) { drop, _ ->
            storyScheduler.seenFlags(drop)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Viewer resume point: the first card not yet displayed (0 = replay from start).
     *  Takes the drop the VIEWER holds — the caller freezes activeStoryDrop when opening,
     *  so a mid-view recomposition can't shift indices under the user. */
    fun firstUnseenStoryIndex(drop: com.freqvox.ui.story.StoryDrop): Int =
        storyScheduler.firstUnseenIndex(drop)

    /** Per-card seen reporting from the viewer; retirement/cooldown live in the scheduler.
     *  Marks against the frozen drop being viewed, not whatever activeStoryDrop recomposed
     *  to since — those can differ while music plays in the background. */
    fun markStoryCardSeen(drop: com.freqvox.ui.story.StoryDrop, segment: com.freqvox.ui.story.StorySegment) {
        storyScheduler.markCardSeen(drop, segment)
        _storySeenBump.value++
    }

    // ── Hidden folders ─────────────────────────────────────────────────────────
    // Draws from the UNFILTERED library so hidden folders remain listed (and can be
    // re-enabled). Toggling just updates the flow; the filtered allTracks/activeTracks
    // recompute reactively — no destructive rescan, so nothing is ever deleted.
    val availableFolders: StateFlow<List<String>> = combine(
        trackRepository.allTracks, _excludedFolders
    ) { tracks, excluded ->
        (tracks.map { it.folderPath }.toSet() + excluded).sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Removes hidden-folder entries whose folder no longer exists in the library (no
     *  tracks under it), keeping both the live filter and the persisted set in sync. */
    private suspend fun pruneDeadExcludedFolders() {
        val present = trackRepository.allTracks.first().mapTo(HashSet()) { it.folderPath }
        val current = _excludedFolders.value
        val pruned = current.filterTo(LinkedHashSet()) { it in present }
        if (pruned.size != current.size) {
            _excludedFolders.value = pruned
            trackRepository.setExcludedFolders(pruned)
        }
    }

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

    // True while a user-triggered "retry failed lookups" sweep is running, so the
    // button can show a spinner instead of being a silent tap.
    private val _artworkRetrying = MutableStateFlow(false)
    val artworkRetrying: StateFlow<Boolean> = _artworkRetrying.asStateFlow()

    /** Clears the negative cache and re-attempts art for everything still missing it,
     *  with a snackbar AND a button spinner (artworkRetrying) so the action isn't silent. */
    fun retryFailedArtwork() {
        viewModelScope.launch {
            val missing = allTracks.value.count { it.artworkPath == null }
            when {
                !artworkPrefs.onlineEnabled -> { postSnackbar("Turn on online lookup to fetch covers"); return@launch }
                missing == 0 -> { postSnackbar("Every track already has a cover"); return@launch }
            }
            artworkPrefs.clearFailedLookups()
            postSnackbar("Looking for covers · $missing track${if (missing == 1) "" else "s"}")
            if (!artworkPrefs.consentShown) return@launch
            // Run the backfill and keep the spinner up until the sweep finishes.
            artworkBackfillJob?.cancel()
            _artworkRetrying.value = true
            val job = viewModelScope.launch { trackRepository.backfillOnlineArtwork() }
            artworkBackfillJob = job
            try { job.join() } finally { _artworkRetrying.value = false }
        }
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
            // Re-queue ONLY the tracks this link can newly resolve — not the whole
            // library — so linking doesn't kick off a full-library online re-sweep.
            if (count > 0) trackRepository.requeueVidmateLinkable()
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

    fun scanLibrary(reportResult: Boolean = false) {
        viewModelScope.launch {
            // For a user-initiated rescan, diff the library by PATH across the scan so the
            // result can be reported. Without this the manual "Rescan library" looked dead:
            // the MediaStore observer already auto-scans on change, so a manual pass usually
            // finds nothing new — and said nothing.
            val before = if (reportResult)
                trackRepository.allTracks.first().mapTo(HashSet()) { it.path } else emptySet()
            // isScanning drives the "Scanning…" UI on an empty library. It clears once
            // the scan returns; tracks appear via the allTracks flow as soon as they're
            // upserted, so the list replaces the indicator without waiting for artwork.
            _isScanning.value = true
            try {
                trackRepository.scanLibrary()
            } finally {
                _isScanning.value = false
            }
            if (reportResult) {
                val after = trackRepository.allTracks.first().mapTo(HashSet()) { it.path }
                val added = (after - before).size
                val removed = (before - after).size
                postSnackbar(
                    when {
                        added == 0 && removed == 0 ->
                            "Scan complete — library is up to date (${after.size} songs)"
                        else -> buildString {
                            append("Scan complete — ")
                            if (added > 0) append("$added new song${if (added == 1) "" else "s"}")
                            if (added > 0 && removed > 0) append(", ")
                            if (removed > 0) append("$removed removed")
                        }
                    }
                )
            }
            // Drop hidden-folder entries whose folder no longer has any tracks (everything
            // in it was deleted/moved), so the hidden list doesn't accumulate dead folders.
            pruneDeadExcludedFolders()
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
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
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

    // ── Playlist editing ───────────────────────────────────────────────────────
    /** Fire-and-forget add (used by the in-playlist "Add songs" picker, which tracks
     *  its own added state). */
    fun addTrackToPlaylistAsync(playlistId: Long, trackId: Long) {
        viewModelScope.launch { playlistRepository.addTrackToPlaylist(playlistId, trackId) }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch { playlistRepository.renamePlaylist(id, name) }
    }

    /** Creates a new manual playlist and drops the given track into it — lets the
     *  "Add to playlist" sheet create a playlist inline instead of dead-ending on
     *  "no playlists". */
    fun createPlaylistAndAddTrack(name: String, trackId: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistRepository.createManualPlaylistWithTracks(name.trim(), listOf(trackId))
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch { playlistRepository.removeTrackFromPlaylist(playlistId, trackId) }
    }

    /** Called after the system delete confirmation (or legacy resolver delete) succeeds. */
    fun completeTrackDeletion(trackId: Long) {
        viewModelScope.launch {
            trackRepository.purgeTrackFromLibrary(trackId)
            playbackController.removeTrackFromLibrary(trackId)
        }
    }

    // ── Bulk (multi-select) actions ────────────────────────────────────────────
    fun bulkAddTag(tagId: Long, trackIds: Collection<Long>) {
        viewModelScope.launch { trackIds.forEach { trackRepository.addTagToTrack(it, tagId) } }
    }

    fun bulkAddToPlaylist(playlistId: Long, trackIds: Collection<Long>) {
        viewModelScope.launch {
            var added = 0
            trackIds.forEach {
                if (playlistRepository.addTrackToPlaylist(playlistId, it) == AddTrackResult.Added) added++
            }
            postSnackbar("Added $added track${if (added == 1) "" else "s"}")
        }
    }

    fun updateTrackMetadata(trackId: Long, title: String, artist: String, album: String) {
        viewModelScope.launch { trackRepository.updateTrackMetadata(trackId, title, artist, album) }
    }

    fun bulkCompleteDeletion(trackIds: Collection<Long>) {
        viewModelScope.launch {
            trackIds.forEach {
                trackRepository.purgeTrackFromLibrary(it)
                playbackController.removeTrackFromLibrary(it)
            }
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

    /** One-shot signal: switch the Library to its Playlists tab. Used by the side-panel
     *  "quick jump" so tapping a playlist OPENS it instead of immediately playing it.
     *  Catches the case where Library is already on screen (a fresh navigation reads the
     *  persisted tab instead). */
    private val _openPlaylistTab = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openPlaylistTab: SharedFlow<Unit> = _openPlaylistTab.asSharedFlow()

    /** Side-panel quick-jump: expand the playlist and ask the Library to show its
     *  Playlists tab. The caller navigates to the Library route. */
    fun openPlaylistFromSidebar(playlistId: Long) {
        setExpandedPlaylist(playlistId)
        viewModelScope.launch { _openPlaylistTab.emit(Unit) }
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
        val normalized = newName.trim().lowercase()
        // Renaming onto another tag's name would leave two same-named tags (allTags
        // distinctBy(name) then hides one, orphaning its track links).
        if (allTags.value.any { it.id != tag.id && it.name.lowercase() == normalized }) {
            postSnackbar("A tag named \"${newName.trim()}\" already exists")
            return
        }
        viewModelScope.launch {
            trackRepository.updateTag(tag.copy(name = normalized, color = newColor))
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
