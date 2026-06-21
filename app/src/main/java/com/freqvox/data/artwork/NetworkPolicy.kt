package com.freqvox.data.artwork

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether an online art fetch is permitted on the *current* connection,
 * honoring the user's per-transport toggles. Re-evaluated on every call because the
 * active network (and its metered-ness) can change at any moment during a backfill.
 */
@Singleton
class NetworkPolicy @Inject constructor(
    @ApplicationContext context: Context,
    private val prefs: ArtworkPreferences
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun canFetch(): Boolean {
        if (!prefs.onlineEnabled) return false

        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false

        // Cellular is governed by its own switch regardless of metered-ness.
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return prefs.allowCellular
        }

        // Wi-Fi / Ethernet / other: only restricted when the link is metered.
        val isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        return if (isMetered) prefs.allowMeteredWifi else true
    }
}
