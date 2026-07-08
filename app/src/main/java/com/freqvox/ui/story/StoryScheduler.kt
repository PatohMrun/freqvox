package com.freqvox.ui.story

import com.freqvox.data.prefs.StoryPreferences
import com.freqvox.ui.main.MainViewModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the active story drop by COMPOSING everything currently due into one session —
 * every uncelebrated milestone, the weekly recap when its Monday arrives, and the current
 * tip — as segments of a single multi-card story (the way a real status works).
 *
 * The old design surfaced one candidate at a time behind a strict priority queue, which had
 * two complaints: content felt distanced (each item waited out its own cooldown) and a drop
 * only ever carried one thing. Composing solves both: an eventful day yields one RICH drop,
 * not a chain of relights, so the ring can afford to be responsive:
 *  - drops containing a fresh milestone light the ring IMMEDIATELY (events are worth it);
 *  - routine content (tips/recap) still respects a short post-view cooldown so the ring
 *    isn't chatty — steady state is a drop every 1–2 days, never several per day.
 */
@Singleton
class StoryScheduler @Inject constructor(
    private val prefs: StoryPreferences
) {
    fun activeDrop(
        stats: MainViewModel.ListeningStats,
        contextualTips: List<StorySegment>,
        epochDay: Long
    ): StoryDrop {
        val segments = mutableListOf<StorySegment>()
        // The drop id is the joined component ids — new content changes the id, which is what
        // relights the ring; markSeen parses the same components to retire each piece.
        val parts = mutableListOf<String>()

        // Milestones — ALL uncelebrated ones ride along, each as its own card.
        val celebrated = prefs.celebratedMilestones()
        StoryContent.metMilestones(stats).filter { it.key !in celebrated }.forEach { def ->
            segments += StoryContent.milestoneSegment(def)
            parts += "m:${def.key}"
        }

        // Weekly recap (Monday-anchored), until it's been seen for this week. Gated on
        // lastRecapWeek — NOT lastSeenDropId — so a later tip view can't resurrect it.
        val dow = java.time.LocalDate.ofEpochDay(epochDay).dayOfWeek.value  // 1=Mon … 7=Sun
        val mondayKey = epochDay - (dow - 1)
        var hasRecap = false
        if (stats.totalPlays > 0 && prefs.lastRecapWeek != mondayKey) {
            segments += StoryContent.weeklyRecap(stats, mondayKey, epochDay).segments
            parts += "r:$mondayKey"
            hasRecap = true
        }

        // A tip always closes the session: the evergreen pool PLUS any contextual (data-aware)
        // tips that currently apply. Chosen at RANDOM per period, avoiding recently-shown ones,
        // then persisted so it's stable for the whole period and different per install.
        val period = Math.floorDiv(epochDay, TIP_INTERVAL_DAYS)
        val pool = StoryContent.educationalPool + contextualTips
        val byId = pool.associateBy { it.id }
        val needNew = prefs.currentTipPeriod != period || prefs.currentTipId == null || prefs.currentTipId !in byId
        val tipId = if (needNew) {
            val recent = prefs.recentTips().toSet()
            val pick = pool.map { it.id }.filter { it !in recent }.ifEmpty { pool.map { it.id } }.random()
            prefs.currentTipId = pick
            prefs.currentTipPeriod = period
            // Don't repeat until roughly half the evergreen pool has been seen.
            prefs.pushRecentTip(pick, cap = (StoryContent.educationalPool.size / 2).coerceAtLeast(1))
            pick
        } else prefs.currentTipId!!
        segments += byId.getValue(tipId)
        parts += "t:$period:$tipId"

        val kind = when {
            parts.any { it.startsWith("m:") } -> SegmentKind.MILESTONE
            hasRecap -> SegmentKind.RECAP
            else -> SegmentKind.TIP
        }
        return StoryDrop(parts.joinToString("|"), epochDay, kind, segments)
    }

    fun isUnseen(drop: StoryDrop): Boolean {
        if (prefs.lastSeenDropId == drop.id) return false
        // Event-driven content bypasses the cooldown — an achievement just earned should
        // light the ring now, and composition guarantees it arrives bundled (no chaining).
        if (drop.segments.any { it.kind == SegmentKind.MILESTONE }) return true
        return java.time.LocalDate.now().toEpochDay() >= prefs.nextDropEarliestDay
    }

    fun markSeen(drop: StoryDrop) {
        // Only consuming NEW content starts a cooldown — re-viewing the current drop
        // (tapping the faded ring) must not keep pushing the next one further out.
        if (prefs.lastSeenDropId != drop.id) {
            prefs.nextDropEarliestDay =
                java.time.LocalDate.now().toEpochDay() + (1..MAX_GAP_DAYS).random()
        }
        prefs.lastSeenDropId = drop.id
        drop.id.split('|').forEach { part ->
            when {
                // Viewed milestones are celebrated once and never re-surface.
                part.startsWith("m:") -> prefs.markMilestoneCelebrated(part.removePrefix("m:"))
                // A viewed recap retires for its whole week.
                part.startsWith("r:") -> part.removePrefix("r:").toLongOrNull()?.let { prefs.lastRecapWeek = it }
            }
        }
    }

    companion object {
        /** The rotating tip refreshes every 2 days (was 3 — "felt distanced"). */
        private const val TIP_INTERVAL_DAYS = 2L
        /** Post-view cooldown for ROUTINE drops: 1–2 days (milestone drops bypass it). */
        private const val MAX_GAP_DAYS = 2
    }
}
