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
    fun activeDrop(stats: MainViewModel.ListeningStats, epochDay: Long): StoryDrop {
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

        // Otherwise a tip/explainer: chosen at RANDOM per period, avoiding recently-shown ones,
        // then persisted so it's stable for the whole period (and different per install).
        val period = Math.floorDiv(epochDay, TIP_INTERVAL_DAYS)
        if (prefs.currentTipPeriod != period || prefs.currentTipId == null) {
            val ids = StoryContent.tipIds
            val recent = prefs.recentTips().toSet()
            val pick = ids.filter { it !in recent }.ifEmpty { ids }.random()
            prefs.currentTipId = pick
            prefs.currentTipPeriod = period
            // Don't repeat until roughly half the pool has been seen.
            prefs.pushRecentTip(pick, cap = (ids.size / 2).coerceAtLeast(1))
            return StoryContent.tipDropById(pick, period, epochDay)
        }
        return StoryContent.tipDropById(prefs.currentTipId!!, period, epochDay)
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
