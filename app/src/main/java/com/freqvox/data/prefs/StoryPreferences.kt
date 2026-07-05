package com.freqvox.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistence for the header "Stories". Only one drop is active at a time, so the ring's
 * seen/unseen state is a single [lastSeenDropId] compared against the scheduler's current drop id.
 * Milestones are one-shot, tracked in [celebratedMilestones]. The tip shown each period is chosen
 * randomly and remembered ([currentTipId]) so it stays stable, and [recentTips] spaces repeats out.
 */
@Singleton
class StoryPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("freqvox_stories", Context.MODE_PRIVATE)

    var lastSeenDropId: String?
        get() = prefs.getString(KEY_LAST_SEEN, null)
        set(value) = prefs.edit().putString(KEY_LAST_SEEN, value).apply()

    fun celebratedMilestones(): Set<String> =
        prefs.getStringSet(KEY_MILESTONES, emptySet()) ?: emptySet()

    fun markMilestoneCelebrated(key: String) {
        prefs.edit().putStringSet(KEY_MILESTONES, celebratedMilestones() + key).apply()
    }

    /** Epoch-week (epochDay / 7) the weekly recap was last generated for. */
    var lastRecapWeek: Long
        get() = prefs.getLong(KEY_RECAP_WEEK, -1L)
        set(value) = prefs.edit().putLong(KEY_RECAP_WEEK, value).apply()

    // Randomly-chosen tip, fixed for its drop period so the ring is stable until it rolls over.
    var currentTipId: String?
        get() = prefs.getString(KEY_TIP_ID, null)
        set(value) = prefs.edit().putString(KEY_TIP_ID, value).apply()

    var currentTipPeriod: Long
        get() = prefs.getLong(KEY_TIP_PERIOD, Long.MIN_VALUE)
        set(value) = prefs.edit().putLong(KEY_TIP_PERIOD, value).apply()

    /** Recently-shown tip ids (newest last), so a tip isn't repeated again soon. */
    fun recentTips(): List<String> =
        prefs.getString(KEY_RECENT, "")?.split('\n')?.filter { it.isNotEmpty() } ?: emptyList()

    fun pushRecentTip(id: String, cap: Int) {
        val next = (recentTips() + id).takeLast(cap.coerceAtLeast(1))
        prefs.edit().putString(KEY_RECENT, next.joinToString("\n")).apply()
    }

    companion object {
        private const val KEY_LAST_SEEN = "last_seen_drop"
        private const val KEY_MILESTONES = "celebrated_milestones"
        private const val KEY_RECAP_WEEK = "last_recap_week"
        private const val KEY_TIP_ID = "current_tip_id"
        private const val KEY_TIP_PERIOD = "current_tip_period"
        private const val KEY_RECENT = "recent_tips"
    }
}
