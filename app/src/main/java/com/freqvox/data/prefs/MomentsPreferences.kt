package com.freqvox.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks whether the day's "moment" (the story shown behind the header DP ring) has been viewed,
 * by remembering the epoch-day it was last opened. A fresh day means a fresh, unseen moment.
 */
@Singleton
class MomentsPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("freqvox_moments", Context.MODE_PRIVATE)

    var lastSeenDay: Long
        get() = prefs.getLong(KEY, -1L)
        set(value) = prefs.edit().putLong(KEY, value).apply()

    companion object {
        private const val KEY = "last_seen_day"
    }
}
