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
        val feed = mutableListOf<FeedItem>()

        // 1. Liked
        if (likedTracks.isNotEmpty())
            feed.add(FeedItem.HorizontalCarousel("Your Favorites", likedTracks, Icons.Default.Favorite))

        // 2. Heavy rotation
        if (heavyRotation.size >= 4)
            feed.add(FeedItem.HorizontalCarousel("Heavy Rotation", heavyRotation, Icons.Default.Autorenew))

        // 3. Recently played
        if (recentlyPlayed.size >= 3)
            feed.add(FeedItem.VerticalList("Recently Played", recentlyPlayed.take(5), Icons.Default.History))

        // 4. Rising fast
        if (risingFast.size >= 3)
            feed.add(FeedItem.HorizontalCarousel("Rising Fast", risingFast, Icons.Default.TrendingUp))

        // 5. Time-contextual slot
        when {
            isMorning && morningTracks.size >= 4 ->
                feed.add(FeedItem.HorizontalCarousel("Morning Mix", morningTracks, Icons.Default.LightMode))
            isMorning && morningTagged.size >= 3 ->
                feed.add(FeedItem.HorizontalCarousel("Morning Mix", morningTagged, Icons.Default.LightMode))
            isLateNight && lateNightTracks.size >= 4 ->
                feed.add(FeedItem.HorizontalCarousel("Late Night", lateNightTracks, Icons.Default.Bedtime))
            isLateNight && lateNightTagged.size >= 3 ->
                feed.add(FeedItem.HorizontalCarousel("Late Night", lateNightTagged, Icons.Default.Bedtime))
            isEvening && eveningTracks.size >= 4 ->
                feed.add(FeedItem.HorizontalCarousel("Evening Wind Down", eveningTracks, Icons.Default.NightsStay))
        }

        // 6. Spotlight
        tracks.filter { it.playCount > 0 }.maxByOrNull { it.playCount }?.let {
            feed.add(FeedItem.Spotlight(it))
        }

        // 7. Workout
        if (workoutTracks.size >= 3)
            feed.add(FeedItem.HorizontalCarousel("Workout Mode", workoutTracks, Icons.Default.FitnessCenter))
        else if (energeticTracks.size >= 3)
            feed.add(FeedItem.HorizontalCarousel("High Energy", energeticTracks, Icons.Default.Bolt))

        // 8. Recently added
        if (recentlyAdded.size >= 4)
            feed.add(FeedItem.HorizontalCarousel("Recently Added", recentlyAdded, Icons.Default.NewReleases))

        // 9. Chill
        if (chillTracks.size >= 3)
            feed.add(FeedItem.HorizontalCarousel("Chill Session", chillTracks, Icons.Default.SelfImprovement))

        // 10. Fresh inbox
        if (freshInbox.size >= 3 && freshInbox != neverHeard)
            feed.add(FeedItem.HorizontalCarousel("Inbox", freshInbox, Icons.Default.MoveToInbox))

        // 11. Comeback
        if (comeback.size >= 2)
            feed.add(FeedItem.Grid("Comeback", comeback.take(4), Icons.Default.Celebration))

        // 12. Forgotten gems
        if (forgottenGems.size >= 2)
            feed.add(FeedItem.Grid("Forgotten Gems", forgottenGems.take(4), Icons.Default.Diamond))

        // 13. Always finish these
        if (highCompletion.size >= 4)
            feed.add(FeedItem.HorizontalCarousel("Always Finish These", highCompletion, Icons.Default.DoneAll))

        // 14. Deep cuts
        if (deepCuts.size >= 2)
            feed.add(FeedItem.VerticalList("Deep Cuts", deepCuts.take(5), Icons.Default.AllInclusive))

        // 15. Quick hits
        when {
            quickHits.size >= 6 -> feed.add(FeedItem.DoubleHorizontalCarousel("Quick Hits", quickHits, Icons.Default.Timer))
            quickHits.size >= 3 -> feed.add(FeedItem.HorizontalCarousel("Quick Hits", quickHits, Icons.Default.Timer))
        }

        // 16. Focus / study
        if (focusTracks.size >= 3)
            feed.add(FeedItem.HorizontalCarousel("Focus Mode", focusTracks, Icons.Default.MenuBook))

        // 17. Drive
        if (driveTracks.size >= 3)
            feed.add(FeedItem.HorizontalCarousel("Drive Mode", driveTracks, Icons.Default.DirectionsCar))

        // 18. Sleep
        if (sleepTracks.size >= 3)
            feed.add(FeedItem.HorizontalCarousel("Sleep", sleepTracks, Icons.Default.Nightlight))

        // 19. Weekend
        if (isWeekend) {
            val weekendMix = (weekendTracks + tagTracks("party", "hype")).distinctBy { it.id }
            if (weekendMix.size >= 4)
                feed.add(FeedItem.HorizontalCarousel("Weekend Mood", weekendMix.take(15), Icons.Default.Nightlife))
        }

        // 20. Commute
        if (isCommute && commuteTracks.size >= 4)
            feed.add(FeedItem.HorizontalCarousel("Commute Vibes", commuteTracks, Icons.Default.DirectionsBus))

        // 21. Hype
        if (hypeTracks.size >= 3)
            feed.add(FeedItem.HorizontalCarousel("Hype", hypeTracks, Icons.Default.Whatshot))

        // 22. Melancholy
        if (melancholyTracks.size >= 3)
            feed.add(FeedItem.HorizontalCarousel("Melancholy", melancholyTracks, Icons.Default.Cloud))

        // 23. Throwback
        if (throwbackTracks.size >= 3)
            feed.add(FeedItem.HorizontalCarousel("Throwback", throwbackTracks, Icons.Default.Update))

        // 24. Underrated
        if (underratedTracks.size >= 3)
            feed.add(FeedItem.HorizontalCarousel("Underrated", underratedTracks, Icons.Default.Grade))

        // 25. The vibe
        if (vibeTracks.size >= 3)
            feed.add(FeedItem.HorizontalCarousel("The Vibe", vibeTracks, Icons.Default.AutoAwesome))

        // 26. Guilty pleasures
        if (guiltyPleasures.size >= 3)
            feed.add(FeedItem.HorizontalCarousel("Guilty Pleasures", guiltyPleasures, Icons.Default.Psychology))

        // 27. Buried treasures
        if (buriedTreasures.size >= 2)
            feed.add(FeedItem.Grid("Buried Treasures", buriedTreasures.take(4), Icons.Default.AutoGraph))

        // 28. Epic journeys
        if (epicTracks.size >= 2)
            feed.add(FeedItem.VerticalList("Epic Journeys", epicTracks.take(5), Icons.Default.Explore))

        // 29. Interludes & skits
        if (interludes.size >= 3)
            feed.add(FeedItem.HorizontalCarousel("Interludes & Skits", interludes, Icons.Default.ChatBubbleOutline))

        // 30. Featured discovery banner
        (discovery.firstOrNull() ?: neverHeard.firstOrNull())?.let {
            feed.add(FeedItem.FeaturedBanner(it))
        }

        // 31. Never heard
        if (neverHeard.size >= 2)
            feed.add(FeedItem.Grid("Never Heard", neverHeard.take(4), Icons.Default.QuestionMark))

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
