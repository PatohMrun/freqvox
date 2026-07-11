package com.freqvox.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers the expand/collapse state of the side-panel accordion sections across drawer
 * open/close and app restarts. `rememberSaveable` alone doesn't cut it here: the drawer content
 * is disposed when the drawer closes, so its saved state is dropped and every section springs
 * back to its default on the next open. Persisting per-section (keyed by the section title) in
 * SharedPreferences makes the choice stick.
 */
@Singleton
class SidePanelPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("freqvox_side_panel", Context.MODE_PRIVATE)

    fun isSectionExpanded(title: String, default: Boolean): Boolean =
        prefs.getBoolean(keyFor(title), default)

    fun setSectionExpanded(title: String, expanded: Boolean) {
        prefs.edit().putBoolean(keyFor(title), expanded).apply()
    }

    /** Raw key→expanded map, for backup export. Keys are already section-scoped. */
    fun exportAll(): Map<String, Boolean> =
        prefs.all.mapNotNull { (k, v) -> (v as? Boolean)?.let { k to it } }.toMap()

    /** Backup restore: re-applies exported raw keys as-is. */
    fun importAll(sections: Map<String, Boolean>) {
        val e = prefs.edit()
        sections.forEach { (k, v) -> e.putBoolean(k, v) }
        e.apply()
    }

    private fun keyFor(title: String) = "section_${title.lowercase()}"
}
