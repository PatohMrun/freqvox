package com.freqvox.data.artwork

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User-facing settings for online album-art lookup, plus the negative cache.
 *
 * Defaults are "works out of the box": lookup is enabled on every connection. The
 * one thing that is NOT default-true is [consentShown] — online fetching stays
 * blocked until the user has seen the one-time disclosure, so a fresh install can
 * never fire network traffic before the user has read anything.
 */
@Singleton
class ArtworkPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("freqvox_artwork", Context.MODE_PRIVATE)

    /** Master switch for fetching art online at all. */
    var onlineEnabled: Boolean
        get() = prefs.getBoolean(KEY_ONLINE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ONLINE_ENABLED, value).apply()

    /** Allow fetching over a cellular/mobile-data connection. */
    var allowCellular: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_CELLULAR, true)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_CELLULAR, value).apply()

    /** Allow fetching over a Wi-Fi/other link that the system marks as metered. */
    var allowMeteredWifi: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_METERED_WIFI, true)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_METERED_WIFI, value).apply()

    /** Whether the one-time info/consent dialog has been acknowledged. Gate A. */
    var consentShown: Boolean
        get() = prefs.getBoolean(KEY_CONSENT_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_CONSENT_SHOWN, value).apply()

    /** SAF tree URI of the user-linked VidMate folder (null = not linked). */
    var vidmateTreeUri: String?
        get() = prefs.getString(KEY_VIDMATE_TREE, null)
        set(value) = prefs.edit().putString(KEY_VIDMATE_TREE, value).apply()

    /**
     * Negative cache: track ids we've already looked up online and found nothing for.
     * Prevents re-hitting the API for them on every scan. Cleared by [clearFailedLookups]
     * (the "retry failed artwork" action) so improved metadata gets another chance.
     */
    fun isLookupFailed(trackId: Long): Boolean =
        prefs.getStringSet(KEY_FAILED, emptySet())?.contains(trackId.toString()) == true

    fun markLookupFailed(trackId: Long) {
        // getStringSet's returned set must not be mutated in place — copy first.
        val updated = (prefs.getStringSet(KEY_FAILED, emptySet()) ?: emptySet()).toMutableSet()
        updated.add(trackId.toString())
        prefs.edit().putStringSet(KEY_FAILED, updated).apply()
    }

    fun clearFailedLookups() {
        prefs.edit().remove(KEY_FAILED).apply()
    }

    companion object {
        private const val KEY_ONLINE_ENABLED = "online_enabled"
        private const val KEY_ALLOW_CELLULAR = "allow_cellular"
        private const val KEY_ALLOW_METERED_WIFI = "allow_metered_wifi"
        private const val KEY_CONSENT_SHOWN = "consent_shown"
        private const val KEY_FAILED = "art_lookup_failed"
        private const val KEY_VIDMATE_TREE = "vidmate_tree_uri"
    }
}
