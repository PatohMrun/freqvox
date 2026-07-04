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
        // Milestone and weekly-recap branches slot in above this line in later phases.
        val period = Math.floorDiv(epochDay, TIP_INTERVAL_DAYS)
        return StoryContent.tipDrop(stats, period, epochDay)
    }

    fun isUnseen(dropId: String): Boolean = prefs.lastSeenDropId != dropId

    fun markSeen(dropId: String) {
        prefs.lastSeenDropId = dropId
    }

    companion object {
        private const val TIP_INTERVAL_DAYS = 3L
    }
}
