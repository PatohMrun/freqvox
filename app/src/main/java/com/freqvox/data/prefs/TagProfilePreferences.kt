package com.freqvox.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps a tag (lowercase name) to an equalizer preset index — a "playback profile". When the
 * current track carries a mapped tag, that EQ preset is applied automatically. Stored one key
 * per tag so it's trivial to enumerate and edit.
 */
@Singleton
class TagProfilePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("freqvox_tag_profiles", Context.MODE_PRIVATE)

    fun all(): Map<String, Int> = prefs.all.mapNotNull { (k, v) ->
        if (k.startsWith(PREFIX) && v is Int) k.removePrefix(PREFIX) to v else null
    }.toMap()

    fun setProfile(tag: String, presetIndex: Int) {
        prefs.edit().putInt(PREFIX + tag.lowercase(), presetIndex).apply()
    }

    fun removeProfile(tag: String) {
        prefs.edit().remove(PREFIX + tag.lowercase()).apply()
    }

    companion object {
        private const val PREFIX = "profile_"
    }
}
