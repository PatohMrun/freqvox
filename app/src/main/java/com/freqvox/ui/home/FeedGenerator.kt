package com.freqvox.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.freqvox.data.local.entities.PlayHistoryEntity
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.ui.tag.formatTagDisplayName
import com.freqvox.ui.tag.iconForTag
import java.util.Calendar
import kotlin.random.Random

/** Tags that already have curated feed sections — skip in the dynamic tag sweep. */
private val CURATED_TAG_NAMES = setOf(
    "liked", "workout", "chill", "focus", "hype", "party", "melancholy", "sad",
    "late night", "drive", "throwback", "underrated", "morning", "vibes", "sleep", "energetic"
)

data class FeedContext(
    val playHistory: List<PlayHistoryEntity> = emptyList(),
    val taggedTrackIds: Map<String, Set<Long>> = emptyMap()
)

sealed class FeedItem {
    data class HorizontalCarousel(val title: String, val tracks: List<TrackEntity>, val icon: ImageVector? = null) : FeedItem()
    data class DoubleHorizontalCarousel(val title: String, val tracks: List<TrackEntity>, val icon: ImageVector? = null) : FeedItem()
    data class VerticalList(val title: String?, val tracks: List<TrackEntity>, val icon: ImageVector? = null) : FeedItem()
    data class Grid(val title: String, val tracks: List<TrackEntity>, val icon: ImageVector? = null) : FeedItem()
    data class Spotlight(val track: TrackEntity) : FeedItem()
    data class FeaturedBanner(val track: TrackEntity) : FeedItem()
}

fun FeedItem.stableId(): String = when (this) {
    is FeedItem.HorizontalCarousel       -> "hc:$title:${tracks.firstOrNull()?.id ?: 0}:${tracks.size}"
    is FeedItem.DoubleHorizontalCarousel -> "dhc:$title:${tracks.firstOrNull()?.id ?: 0}:${tracks.size}"
    is FeedItem.VerticalList             -> "vl:${title ?: ""}:${tracks.firstOrNull()?.id ?: 0}:${tracks.size}"
    is FeedItem.Grid                     -> "grid:$title:${tracks.firstOrNull()?.id ?: 0}:${tracks.size}"
    is FeedItem.Spotlight                -> "spot:${track.id}"
    is FeedItem.FeaturedBanner           -> "feat:${track.id}"
}

/**
 * Accumulates "already used" picks across pages within one session, so the
 * "Diving Into <Artist>" / "Album: <X>" sections don't keep choosing the same
 * artist/album. Reset when the user pull-to-refreshes.
 */
data class FeedSessionState(
    val usedArtists: Set<String> = emptySet(),
    val usedAlbums:  Set<String> = emptySet()
)

/** Result of generating a single page: the items plus the updated session memory. */
data class FeedPage(val items: List<FeedItem>, val session: FeedSessionState)

object FeedGenerator {

    /**
     * Generate a feed "page". Pass distinct [random] instances per page (with different seeds)
     * to get a different remix of the same library, supporting endless scroll on HomeScreen.
     *
     * Pass the accumulated [session] from prior pages to keep "Diving Into <Artist>" /
     * "Album: <X>" sections from re-picking the same artist or album across pages.
     */
    fun generateFeed(
        tracks: List<TrackEntity>,
        context: FeedContext = FeedContext(),
        random: Random = Random.Default,
        session: FeedSessionState = FeedSessionState()
    ): FeedPage {
        if (tracks.isEmpty()) return FeedPage(emptyList(), session)

        val now = System.currentTimeMillis()
        // These are in MILLISECONDS — compared against TrackEntity.lastPlayed (millis).
        val sevenDaysAgo   = now - 7L  * 86_400_000
        val fourteenDays   = now - 14L * 86_400_000
        val thirtyDaysAgo  = now - 30L * 86_400_000
        // TrackEntity.dateAdded comes straight from MediaStore.DATE_ADDED, which is in
        // SECONDS. Date-added thresholds must use this, not the millisecond values above.
        val sevenDaysAgoSec = now / 1000 - 7L * 86_400

        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
        val isMorning = currentHour in 5..10
        val isLateNight = currentHour >= 22 || currentHour <= 4
        val isEvening = currentHour in 17..21
        val isCommute = currentHour in 7..9 || currentHour in 17..19

        // ── Play history analysis ─────────────────────────────────────────────
        val historyByTrack = context.playHistory.groupBy { it.trackId }

        val completionRate: Map<Long, Float> = historyByTrack.mapValues { (_, h) ->
            if (h.isEmpty()) 0f else h.count { it.completed }.toFloat() / h.size
        }

        val morningCount    = mutableMapOf<Long, Int>()
        val lateNightCount  = mutableMapOf<Long, Int>()
        val eveningCount    = mutableMapOf<Long, Int>()
        val weekendCount    = mutableMapOf<Long, Int>()
        val commuteCount    = mutableMapOf<Long, Int>()

        context.playHistory.forEach { ph ->
            val c = Calendar.getInstance().apply { timeInMillis = ph.playedAt }
            val h = c.get(Calendar.HOUR_OF_DAY)
            val d = c.get(Calendar.DAY_OF_WEEK)
            if (h in 5..10)  morningCount[ph.trackId]   = (morningCount[ph.trackId]   ?: 0) + 1
            if (h >= 22 || h <= 4) lateNightCount[ph.trackId] = (lateNightCount[ph.trackId] ?: 0) + 1
            if (h in 17..21) eveningCount[ph.trackId]   = (eveningCount[ph.trackId]   ?: 0) + 1
            if (h in 7..9 || h in 17..19) commuteCount[ph.trackId] = (commuteCount[ph.trackId] ?: 0) + 1
            if (d == Calendar.SATURDAY || d == Calendar.SUNDAY)
                weekendCount[ph.trackId] = (weekendCount[ph.trackId] ?: 0) + 1
        }

        val velocity7d = context.playHistory
            .filter { it.playedAt > sevenDaysAgo }
            .groupBy { it.trackId }
            .mapValues { it.value.size }

        // ── TrackEntity-based buckets ─────────────────────────────────────────
        val trackById = tracks.associateBy { it.id }

        val heavyRotation = tracks
            .filter { it.playCount > 2 }
            .sortedByDescending { it.playCount }
            .take(15)

        val recentlyPlayed = tracks
            .filter { it.lastPlayed != null && it.lastPlayed > sevenDaysAgo }
            .sortedByDescending { it.lastPlayed }
            .take(15)

        val recentlyAdded = tracks
            .sortedByDescending { it.dateAdded }
            .take(15)

        val forgottenGems = tracks
            .filter { it.playCount > 0 && (it.lastPlayed == null || it.lastPlayed < thirtyDaysAgo) }
            .sortedByDescending { it.playCount }
            .take(10)

        val neverHeard = tracks
            .filter { it.playCount == 0 }
            .shuffled(random)
            .take(12)

        val freshInbox = tracks
            .filter { it.playCount == 0 }
            .sortedByDescending { it.dateAdded }
            .take(10)

        val deepCuts = tracks
            .filter { it.duration > 6 * 60_000L }
            .shuffled(random)
            .take(8)

        val epicTracks = tracks
            .filter { it.duration > 10 * 60_000L }
            .shuffled(random)
            .take(8)

        val quickHits = tracks
            .filter { it.duration in 60_000L..(3 * 60_000L) }
            .shuffled(random)
            .take(12)

        val interludes = tracks
            .filter { it.duration in 10_000L..<60_000L }
            .shuffled(random)
            .take(6)

        val discovery = tracks
            .sortedBy { it.playCount }
            .take((tracks.size * 0.6).toInt().coerceAtLeast(20))
            .shuffled(random)
            .take(20)

        val highCompletion = tracks
            .filter { (completionRate[it.id] ?: 0f) > 0.8f && it.playCount > 1 }
            .sortedByDescending { completionRate[it.id] }
            .take(15)

        val risingFast = tracks
            .filter { (velocity7d[it.id] ?: 0) >= 2 }
            .sortedByDescending { (velocity7d[it.id] ?: 0).toFloat() / it.playCount.coerceAtLeast(1) }
            .take(10)

        val comeback = tracks
            .filter { it.playCount > 5 && it.lastPlayed != null && it.lastPlayed < fourteenDays }
            .sortedByDescending { it.playCount }
            .take(8)

        val buriedTreasures = tracks
            .filter { it.playCount in 1..3 && it.skipCount == 0 }
            .shuffled(random)
            .take(10)

        val guiltyPleasures = tracks
            .filter { it.skipCount > 2 && it.playCount > it.skipCount }
            .sortedByDescending { it.playCount }
            .take(10)

        val morningTracks   = tracks.filter { (morningCount[it.id]   ?: 0) >= 2 }.sortedByDescending { morningCount[it.id] }.take(15)
        val lateNightTracks = tracks.filter { (lateNightCount[it.id] ?: 0) >= 2 }.sortedByDescending { lateNightCount[it.id] }.take(15)
        val eveningTracks   = tracks.filter { (eveningCount[it.id]   ?: 0) >= 2 }.sortedByDescending { eveningCount[it.id] }.take(15)
        val weekendTracks   = tracks.filter { (weekendCount[it.id]   ?: 0) >= 2 }.sortedByDescending { weekendCount[it.id] }.take(15)
        val commuteTracks   = tracks.filter { (commuteCount[it.id]   ?: 0) >= 2 }.sortedByDescending { commuteCount[it.id] }.take(12)

        // ── Extra buckets for endless variety ────────────────────────────────
        // These are cheap derived views of the same in-memory `tracks` list.
        val pureRandom    = tracks.shuffled(random).take(15)
        val alphaShuffle  = tracks.shuffled(random).sortedBy { it.title.lowercase() }.take(15)
        // Pick an artist we haven't featured yet this session. If every artist
        // has already been used, drop the section this page rather than repeat
        // — keeps the feed feeling fresh; the next pull-to-refresh resets it.
        val byArtistPick  = tracks.groupBy { it.artist.lowercase() }
            .filter { it.value.size >= 2 && it.key !in session.usedArtists }
            .entries.shuffled(random)
            .firstOrNull()
            ?.let { (artist, list) -> artist.replaceFirstChar { c -> c.uppercase() } to list.shuffled(random).take(12) }
        val byAlbumPick   = tracks.groupBy { it.album.lowercase() }
            .filter { it.value.size >= 3 && it.key.isNotBlank() && it.key !in session.usedAlbums }
            .entries.shuffled(random)
            .firstOrNull()
            ?.let { (album, list) -> album.replaceFirstChar { c -> c.uppercase() } to list.take(12) }
        val freshThisWeek = tracks.filter { it.dateAdded > sevenDaysAgoSec }.sortedByDescending { it.dateAdded }.take(12)
        val oldestPicks   = tracks.sortedBy { it.dateAdded }.take(12).shuffled(random)
        val mediumLength  = tracks.filter { it.duration in (3 * 60_000L)..(6 * 60_000L) }.shuffled(random).take(12)
        val highSkipPlay  = tracks.filter { it.skipCount > 0 && it.playCount > 0 && it.playCount >= it.skipCount }
            .sortedByDescending { it.playCount }.take(10)
        val zeroSkip      = tracks.filter { it.playCount >= 2 && it.skipCount == 0 }.shuffled(random).take(12)
        val longestEver   = tracks.sortedByDescending { it.duration }.take(8)

        // ── Tag-based sections ────────────────────────────────────────────────
        fun tagTracks(vararg names: String): List<TrackEntity> {
            val ids = names.flatMapTo(mutableSetOf()) { context.taggedTrackIds[it.lowercase()] ?: emptySet() }
            return ids.mapNotNull { trackById[it] }.shuffled(random).take(15) // Advised cap applied
        }

        val likedTracks      = tagTracks("liked")
        val workoutTracks    = tagTracks("workout")
        val chillTracks      = tagTracks("chill")
        val focusTracks      = tagTracks("focus")
        val hypeTracks       = tagTracks("hype", "party")
        val melancholyTracks = tagTracks("melancholy", "sad")
        val lateNightTagged  = tagTracks("late night")
        val driveTracks      = tagTracks("drive")
        val throwbackTracks  = tagTracks("throwback")
        val underratedTracks = tagTracks("underrated")
        val morningTagged    = tagTracks("morning")
        val vibeTracks       = tagTracks("vibes")
        val sleepTracks      = tagTracks("sleep")
        val energeticTracks  = tagTracks("energetic")

        // ── Assemble feed ─────────────────────────────────────────────────────
        val feedOptions = mutableListOf<FeedItem>()

        fun createDynamicSection(title: String, tracksList: List<TrackEntity>, icon: ImageVector? = null): FeedItem {
            when {
                tracksList.size == 1 ->
                    return FeedItem.VerticalList(title, tracksList, icon)
                tracksList.size == 2 ->
                    return FeedItem.HorizontalCarousel(title, tracksList, icon)
            }
            val options = mutableListOf<FeedItem>()
            if (tracksList.size >= 2) options.add(FeedItem.HorizontalCarousel(title, tracksList, icon))
            if (tracksList.size >= 4) {
                options.add(FeedItem.Grid(title, tracksList.take(4), icon))
                options.add(FeedItem.VerticalList(title, tracksList.take(5), icon))
            }
            if (tracksList.size >= 6) options.add(FeedItem.DoubleHorizontalCarousel(title, tracksList, icon))

            return options.randomOrNull(random) ?: FeedItem.HorizontalCarousel(title, tracksList, icon)
        }

        // Spotlight
        tracks.filter { it.playCount > 0 }.maxByOrNull { it.playCount }?.let {
            feedOptions.add(FeedItem.Spotlight(it))
        }

        // Featured discovery banner
        (discovery.firstOrNull() ?: neverHeard.firstOrNull())?.let {
            feedOptions.add(FeedItem.FeaturedBanner(it))
        }

        // Time-contextual slot
        when {
            isMorning && morningTracks.size >= 4 ->
                feedOptions.add(FeedItem.HorizontalCarousel("Morning Mix", morningTracks, Icons.Default.LightMode))
            isMorning && morningTagged.size >= 3 ->
                feedOptions.add(FeedItem.HorizontalCarousel("Morning Mix", morningTagged, Icons.Default.LightMode))
            isLateNight && lateNightTracks.size >= 4 ->
                feedOptions.add(FeedItem.HorizontalCarousel("Late Night", lateNightTracks, Icons.Default.Bedtime))
            isLateNight && lateNightTagged.size >= 3 ->
                feedOptions.add(FeedItem.HorizontalCarousel("Late Night", lateNightTagged, Icons.Default.Bedtime))
            isEvening && eveningTracks.size >= 4 ->
                feedOptions.add(FeedItem.HorizontalCarousel("Evening Wind Down", eveningTracks, Icons.Default.NightsStay))
        }

        // Liked
        if (likedTracks.isNotEmpty())
            feedOptions.add(FeedItem.HorizontalCarousel("Your Favorites", likedTracks, Icons.Default.Favorite))

        // Heavy rotation
        if (heavyRotation.size >= 4)
            feedOptions.add(FeedItem.HorizontalCarousel("Heavy Rotation", heavyRotation, Icons.Default.Autorenew))

        // Recently played
        if (recentlyPlayed.size >= 3)
            feedOptions.add(FeedItem.VerticalList("Recently Played", recentlyPlayed.take(5), Icons.Default.History))

        // Recently added
        if (recentlyAdded.size >= 4)
            feedOptions.add(FeedItem.HorizontalCarousel("Recently Added", recentlyAdded, Icons.Default.NewReleases))

        // Always finish these
        if (highCompletion.size >= 4)
            feedOptions.add(FeedItem.HorizontalCarousel("Always Finish These", highCompletion, Icons.Default.DoneAll))

        // Rising fast
        if (risingFast.size >= 3)
            feedOptions.add(createDynamicSection("Rising Fast", risingFast, Icons.Default.TrendingUp))


        // Workout / Energy
        if (workoutTracks.isNotEmpty())
            feedOptions.add(createDynamicSection("Workout Mode", workoutTracks, Icons.Default.FitnessCenter))
        else if (energeticTracks.isNotEmpty())
            feedOptions.add(createDynamicSection("High Energy", energeticTracks, Icons.Default.Bolt))

        // Chill
        if (chillTracks.isNotEmpty())
            feedOptions.add(createDynamicSection("Chill Session", chillTracks, Icons.Default.SelfImprovement))

        // Inbox
        if (freshInbox.size >= 3 && freshInbox != neverHeard)
            feedOptions.add(createDynamicSection("Inbox", freshInbox, Icons.Default.MoveToInbox))

        // Comeback
        if (comeback.size >= 2)
            feedOptions.add(FeedItem.Grid("Comeback", comeback.take(4), Icons.Default.Celebration))

        // Forgotten gems
        if (forgottenGems.size >= 2)
            feedOptions.add(FeedItem.Grid("Forgotten Gems", forgottenGems.take(4), Icons.Default.Diamond))

        // Deep cuts
        if (deepCuts.size >= 2)
            feedOptions.add(createDynamicSection("Deep Cuts", deepCuts, Icons.Default.AllInclusive))

        // Quick hits
        if (quickHits.size >= 3)
            feedOptions.add(createDynamicSection("Quick Hits", quickHits, Icons.Default.Timer))

        // Focus
        if (focusTracks.isNotEmpty())
            feedOptions.add(createDynamicSection("Focus Mode", focusTracks, Icons.Default.MenuBook))

        // Drive
        if (driveTracks.isNotEmpty())
            feedOptions.add(createDynamicSection("Drive Mode", driveTracks, Icons.Default.DirectionsCar))

        // Sleep
        if (sleepTracks.isNotEmpty())
            feedOptions.add(createDynamicSection("Sleep", sleepTracks, Icons.Default.Nightlight))

        // Weekend
        if (isWeekend) {
            val weekendMix = (weekendTracks + tagTracks("party", "hype")).distinctBy { it.id }
            if (weekendMix.size >= 4)
                feedOptions.add(createDynamicSection("Weekend Mood", weekendMix.take(15), Icons.Default.Nightlife))
        }

        // Commute
        if (isCommute && commuteTracks.size >= 4)
            feedOptions.add(createDynamicSection("Commute Vibes", commuteTracks, Icons.Default.DirectionsBus))

        // Moods / Tags (any tag with ≥1 song qualifies)
        if (hypeTracks.isNotEmpty()) feedOptions.add(createDynamicSection("Hype", hypeTracks, Icons.Default.Whatshot))
        if (melancholyTracks.isNotEmpty()) feedOptions.add(createDynamicSection("Melancholy", melancholyTracks, Icons.Default.Cloud))
        if (throwbackTracks.isNotEmpty()) feedOptions.add(createDynamicSection("Throwback", throwbackTracks, Icons.Default.Update))
        if (underratedTracks.isNotEmpty()) feedOptions.add(createDynamicSection("Underrated", underratedTracks, Icons.Default.Grade))
        if (vibeTracks.isNotEmpty()) feedOptions.add(createDynamicSection("The Vibe", vibeTracks, Icons.Default.AutoAwesome))
        if (guiltyPleasures.size >= 3) feedOptions.add(createDynamicSection("Guilty Pleasures", guiltyPleasures, Icons.Default.Psychology))

        // User-created and other tags: one feed category per tag with ≥1 song (not already curated above)
        context.taggedTrackIds
            .filter { (name, ids) -> ids.isNotEmpty() && name.lowercase() !in CURATED_TAG_NAMES }
            .keys
            .shuffled(random)
            .take(12)
            .forEach { tagName ->
                val tracks = tagTracks(tagName)
                if (tracks.isNotEmpty()) {
                    feedOptions.add(
                        createDynamicSection(
                            formatTagDisplayName(tagName),
                            tracks,
                            iconForTag(tagName)
                        )
                    )
                }
            }

        // Buried treasures
        if (buriedTreasures.size >= 2)
            feedOptions.add(FeedItem.Grid("Buried Treasures", buriedTreasures.take(4), Icons.Default.AutoGraph))

        // Epic journeys
        if (epicTracks.size >= 2)
            feedOptions.add(createDynamicSection("Epic Journeys", epicTracks, Icons.Default.Explore))

        // Interludes
        if (interludes.size >= 3)
            feedOptions.add(createDynamicSection("Interludes & Skits", interludes, Icons.Default.ChatBubbleOutline))

        // Never heard
        if (neverHeard.size >= 2)
            feedOptions.add(FeedItem.Grid("Never Heard", neverHeard.take(4), Icons.Default.QuestionMark))

        // ── Endless-variety sections (gated by per-page coin flips so different
        // pages surface different subsets; tuned to keep ~5-7 of these per page) ──
        fun coin(p: Double): Boolean = random.nextDouble() < p

        if (coin(0.85) && pureRandom.size >= 3)
            feedOptions.add(createDynamicSection("Wildcards", pureRandom, Icons.Default.Shuffle))

        if (coin(0.55) && alphaShuffle.size >= 3)
            feedOptions.add(createDynamicSection("A to Z Crawl", alphaShuffle, Icons.Default.SortByAlpha))

        // Track which artist / album we actually committed to this page so we
        // can pass it back to the caller and avoid re-picking on later pages.
        var pickedArtist: String? = null
        var pickedAlbum:  String? = null

        byArtistPick?.let { (name, list) ->
            if (coin(0.7) && list.size >= 2) {
                feedOptions.add(createDynamicSection("Diving Into $name", list, Icons.Default.Person))
                pickedArtist = name.lowercase()
            }
        }

        byAlbumPick?.let { (name, list) ->
            if (coin(0.6) && list.size >= 3) {
                feedOptions.add(createDynamicSection("Album: $name", list, Icons.Default.Album))
                pickedAlbum = name.lowercase()
            }
        }

        if (coin(0.6) && freshThisWeek.size >= 3)
            feedOptions.add(createDynamicSection("Fresh This Week", freshThisWeek, Icons.Default.FiberNew))

        if (coin(0.45) && oldestPicks.size >= 3)
            feedOptions.add(createDynamicSection("From the Vault", oldestPicks, Icons.Default.Inventory2))

        if (coin(0.55) && mediumLength.size >= 3)
            feedOptions.add(createDynamicSection("The Sweet Spot", mediumLength, Icons.Default.Tune))

        if (coin(0.4) && highSkipPlay.size >= 3)
            feedOptions.add(createDynamicSection("Skip-then-Stay", highSkipPlay, Icons.Default.SkipNext))

        if (coin(0.5) && zeroSkip.size >= 3)
            feedOptions.add(createDynamicSection("Never Skipped", zeroSkip, Icons.Default.ThumbUp))

        if (coin(0.4) && longestEver.size >= 2)
            feedOptions.add(FeedItem.Grid("The Long Way Round", longestEver.take(4), Icons.Default.Timelapse))

        // Assemble final feed (fully shuffled with the per-page seed)
        val feed = feedOptions.shuffled(random).toMutableList()

        // Fill remaining tracks not yet shown
        val usedIds = feed.flatMap { item ->
            when (item) {
                is FeedItem.HorizontalCarousel       -> item.tracks.map { it.id }
                is FeedItem.DoubleHorizontalCarousel -> item.tracks.map { it.id }
                is FeedItem.VerticalList             -> item.tracks.map { it.id }
                is FeedItem.Grid                     -> item.tracks.map { it.id }
                is FeedItem.Spotlight                -> listOf(item.track.id)
                is FeedItem.FeaturedBanner           -> listOf(item.track.id)
            }
        }.toSet()

        // Filler: any track not yet rendered gets surfaced in a chunked tail so
        // small libraries still produce visually long pages. Chunk size and tail
        // length both increased for the endless feed.
        tracks.filter { it.id !in usedIds }.shuffled(random).chunked(4).take(18)
            .forEachIndexed { i, chunk ->
                if (chunk.isNotEmpty())
                    feed.add(FeedItem.VerticalList(if (i == 0) "Discover More" else null, chunk, if (i == 0) Icons.Default.Explore else null))
            }

        // If even the filler is exhausted (very tiny library) sprinkle a few
        // shuffled re-views of the whole library so the page never feels short.
        if (tracks.size >= 4) {
            repeat(3) { i ->
                val sample = tracks.shuffled(random).take(4)
                if (sample.isNotEmpty()) {
                    feed.add(
                        FeedItem.Grid(
                            title = if (i == 0) "Pick a Direction" else "Another Angle",
                            tracks = sample,
                            icon = if (i == 0) Icons.Default.Casino else null
                        )
                    )
                }
            }
        }

        // Drop sections that don't have enough content to look good
        val finalItems = feed.filter { item ->
            when (item) {
                is FeedItem.VerticalList             -> item.tracks.isNotEmpty()
                is FeedItem.HorizontalCarousel       -> item.tracks.size >= 2
                is FeedItem.DoubleHorizontalCarousel -> item.tracks.size >= 6
                is FeedItem.Grid                     -> item.tracks.size >= 2
                else -> true
            }
        }

        val newSession = session.copy(
            usedArtists = session.usedArtists + listOfNotNull(pickedArtist),
            usedAlbums  = session.usedAlbums  + listOfNotNull(pickedAlbum)
        )
        return FeedPage(finalItems, newSession)
    }
}
