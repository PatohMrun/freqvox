package com.freqvox.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers the Library screen's small UI state across navigation and app restarts:
 * which pill (tab) was last open, and the user's custom pill order. Both used to be
 * `remember {}` in the composable, so they reset every time the user left and came
 * back — making a reorder pointless and always snapping back to Songs.
 *
 * Values are stored as plain tab names; unknown/removed names are ignored on read so
 * adding or renaming a [com.freqvox.ui.library.LibraryTab] can never crash a restore.
 */
@Singleton
class LibraryUiPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("freqvox_library_ui", Context.MODE_PRIVATE)

    /** Name of the last-opened tab, or null if the user hasn't picked one yet. */
    var lastTab: String?
        get() = prefs.getString(KEY_LAST_TAB, null)
        set(value) = prefs.edit().putString(KEY_LAST_TAB, value).apply()

    /** Persisted tab order as a list of tab names (empty = use the default order). */
    var tabOrder: List<String>
        get() = prefs.getString(KEY_TAB_ORDER, null)
            ?.split(',')
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        set(value) = prefs.edit().putString(KEY_TAB_ORDER, value.joinToString(",")).apply()

    private companion object {
        const val KEY_LAST_TAB = "last_tab"
        const val KEY_TAB_ORDER = "tab_order"
    }
}
