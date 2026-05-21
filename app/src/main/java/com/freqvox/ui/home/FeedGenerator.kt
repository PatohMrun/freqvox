package com.freqvox.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.freqvox.data.local.entities.PlayHistoryEntity
import com.freqvox.data.local.entities.TrackEntity
import java.util.Calendar

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

object FeedGenerator {

    fun generateFeed(tracks: List<TrackEntity>, context: FeedContext = FeedContext()): List<FeedItem> {
        if (tracks.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        val sevenDaysAgo   = now - 7L  * 86_400_000
        val fourteenDays   = now - 14L * 86_400_000
        val thirtyDaysAgo  = now - 30L * 86_400_000

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
            .shuffled()
            .take(12)

        val freshInbox = tracks
            .filter { it.playCount == 0 }
            .sortedByDescending { it.dateAdded }
            .take(10)

        val deepCuts = tracks
            .filter { it.duration > 6 * 60_000L }
            .shuffled()
            .take(8)

        val epicTracks = tracks
            .filter { it.duration > 10 * 60_000L }
            .shuffled()
            .take(8)

        val quickHits = tracks
            .filter { it.duration in 60_000L..(3 * 60_000L) }
            .shuffled()
            .take(12)

        val interludes = tracks
            .filter { it.duration in 10_000L..<60_000L }
            .shuffled()
            .take(6)

        val discovery = tracks
            .sortedBy { it.playCount }
            .take((tracks.size * 0.6).toInt().coerceAtLeast(20))
            .shuffled()
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
            .shuffled()
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

        // ── Tag-based sections ────────────────────────────────────────────────
        fun tagTracks(vararg names: String): List<TrackEntity> {
            val ids = names.flatMapTo(mutableSetOf()) { context.taggedTrackIds[it.lowercase()] ?: emptySet() }
            return ids.mapNotNull { trackById[it] }.shuffled().take(15) // Advised cap applied
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
            val options = mutableListOf<FeedItem>()
            if (tracksList.size >= 2) options.add(FeedItem.HorizontalCarousel(title, tracksList, icon))
            if (tracksList.size >= 4) {
                options.add(FeedItem.Grid(title, tracksList.take(4), icon))
                options.add(FeedItem.VerticalList(title, tracksList.take(5), icon))
            }
            if (tracksList.size >= 6) options.add(FeedItem.DoubleHorizontalCarousel(title, tracksList, icon))
            
            return options.randomOrNull() ?: FeedItem.HorizontalCarousel(title, tracksList, icon)
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
        if (workoutTracks.size >= 3)
            feedOptions.add(createDynamicSection("Workout Mode", workoutTracks, Icons.Default.FitnessCenter))
        else if (energeticTracks.size >= 3)
            feedOptions.add(createDynamicSection("High Energy", energeticTracks, Icons.Default.Bolt))

        // Chill
        if (chillTracks.size >= 3)
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
        if (focusTracks.size >= 3)
            feedOptions.add(createDynamicSection("Focus Mode", focusTracks, Icons.Default.MenuBook))

        // Drive
        if (driveTracks.size >= 3)
            feedOptions.add(createDynamicSection("Drive Mode", driveTracks, Icons.Default.DirectionsCar))

        // Sleep
        if (sleepTracks.size >= 3)
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

        // Moods / Tags
        if (hypeTracks.size >= 3) feedOptions.add(createDynamicSection("Hype", hypeTracks, Icons.Default.Whatshot))
        if (melancholyTracks.size >= 3) feedOptions.add(createDynamicSection("Melancholy", melancholyTracks, Icons.Default.Cloud))
        if (throwbackTracks.size >= 3) feedOptions.add(createDynamicSection("Throwback", throwbackTracks, Icons.Default.Update))
        if (underratedTracks.size >= 3) feedOptions.add(createDynamicSection("Underrated", underratedTracks, Icons.Default.Grade))
        if (vibeTracks.size >= 3) feedOptions.add(createDynamicSection("The Vibe", vibeTracks, Icons.Default.AutoAwesome))
        if (guiltyPleasures.size >= 3) feedOptions.add(createDynamicSection("Guilty Pleasures", guiltyPleasures, Icons.Default.Psychology))

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

        // Assemble final feed (fully shuffled)
        val feed = feedOptions.shuffled().toMutableList()

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

        tracks.filter { it.id !in usedIds }.shuffled().chunked(3).take(10)
            .forEachIndexed { i, chunk ->
                if (chunk.isNotEmpty())
                    feed.add(FeedItem.VerticalList(if (i == 0) "Discover More" else null, chunk, if (i == 0) Icons.Default.Explore else null))
            }

        // Drop sections that don't have enough content to look good
        return feed.filter { item ->
            when (item) {
                is FeedItem.VerticalList             -> item.tracks.isNotEmpty()
                is FeedItem.HorizontalCarousel       -> item.tracks.size >= 2
                is FeedItem.DoubleHorizontalCarousel -> item.tracks.size >= 6
                is FeedItem.Grid                     -> item.tracks.size >= 2
                else -> true
            }
        }
    }
}
