package com.freqvox.ui.story

import com.freqvox.data.prefs.StoryPreferences
import com.freqvox.ui.main.MainViewModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides the single active story drop and its seen/unseen state. Priority (added incrementally):
 * milestones (event-driven) > weekly recap (Mondays) > a throttled tip/explainer that changes
 * every [TIP_INTERVAL_DAYS] days, keeping the ring to a curated ~2–3×/week cadence.
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
        // Milestones — highest priority: the first achievement not yet celebrated.
        val celebrated = prefs.celebratedMilestones()
        StoryContent.metMilestones(stats).firstOrNull { it.key !in celebrated }?.let {
            return StoryContent.milestoneDrop(it, epochDay)
        }

        // Weekly recap (Monday-anchored): the top-priority drop each week until it's been seen.
        val dow = java.time.LocalDate.ofEpochDay(epochDay).dayOfWeek.value  // 1=Mon … 7=Sun
        val mondayKey = epochDay - (dow - 1)
        val recapId = "recap-$mondayKey"
        if (stats.totalPlays > 0 && prefs.lastSeenDropId != recapId) {
            return StoryContent.weeklyRecap(stats, mondayKey, epochDay)
        }

        // Otherwise a tip: the evergreen pool PLUS any contextual (data-aware) tips that currently
        // apply. Chosen at RANDOM per period, avoiding recently-shown ones, then persisted so it's
        // stable for the whole period and different per install.
        val period = Math.floorDiv(epochDay, TIP_INTERVAL_DAYS)
        val pool = StoryContent.educationalPool + contextualTips
        val byId = pool.associateBy { it.id }
        fun drop(seg: StorySegment) = StoryDrop("tip-$period", epochDay, seg.kind, listOf(seg))

        // Reassign when the period rolls over, on first run, or when the persisted pick is a
        // contextual tip that no longer applies.
        val needNew = prefs.currentTipPeriod != period || prefs.currentTipId == null || prefs.currentTipId !in byId
        if (needNew) {
            val recent = prefs.recentTips().toSet()
            val pick = pool.map { it.id }.filter { it !in recent }.ifEmpty { pool.map { it.id } }.random()
            prefs.currentTipId = pick
            prefs.currentTipPeriod = period
            // Don't repeat until roughly half the evergreen pool has been seen.
            prefs.pushRecentTip(pick, cap = (StoryContent.educationalPool.size / 2).coerceAtLeast(1))
            return drop(byId.getValue(pick))
        }
        return drop(byId.getValue(prefs.currentTipId!!))
    }

    fun isUnseen(dropId: String): Boolean = prefs.lastSeenDropId != dropId

    fun markSeen(dropId: String) {
        prefs.lastSeenDropId = dropId
        // A viewed milestone is celebrated once and never re-surfaces, even while still true.
        if (dropId.startsWith("milestone-")) {
            prefs.markMilestoneCelebrated(dropId.removePrefix("milestone-"))
        }
    }

    companion object {
        private const val TIP_INTERVAL_DAYS = 3L
    }
}
