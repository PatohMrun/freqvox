package com.freqvox.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** App theme preference. SYSTEM follows the device light/dark setting; LIGHT/DARK force it. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Persists the user's theme choice. A @Singleton so the same StateFlow is shared by the
 * composition root (which selects the palette) and the in-header toggle (which changes it).
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("freqvox_theme", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(loadMode())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    /** Cycle for the header toggle: System → Light → Dark → System. */
    fun cycle() {
        setMode(
            when (_mode.value) {
                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.SYSTEM
            }
        )
    }

    private fun loadMode(): ThemeMode = runCatching {
        ThemeMode.valueOf(prefs.getString(KEY_MODE, ThemeMode.SYSTEM.name)!!)
    }.getOrDefault(ThemeMode.SYSTEM)

    private companion object {
        const val KEY_MODE = "theme_mode"
    }
}
