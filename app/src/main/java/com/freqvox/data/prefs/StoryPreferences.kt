package com.freqvox.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistence for the header "Stories". Only one drop is active at a time, so the ring's
 * seen/unseen state is a single [lastSeenDropId] compared against the scheduler's current drop id.
 * Milestones are one-shot, tracked in [celebratedMilestones]. Weekly recap uses [lastRecapWeek].
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

    companion object {
        private const val KEY_LAST_SEEN = "last_seen_drop"
        private const val KEY_MILESTONES = "celebrated_milestones"
        private const val KEY_RECAP_WEEK = "last_recap_week"
    }
}
