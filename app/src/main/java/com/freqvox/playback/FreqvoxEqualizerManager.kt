package com.freqvox.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqParams(
    val numBands: Int = 0,
    val centerFreqs: List<Int> = emptyList(),
    val minLevel: Int = -1500,
    val maxLevel: Int = 1500
)

class FreqvoxEqualizerManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("freqvox_eq", Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null

    private val _isEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _bandLevels = MutableStateFlow<Map<Int, Int>>(loadBandLevels())
    val bandLevels: StateFlow<Map<Int, Int>> = _bandLevels.asStateFlow()

    private val _currentPreset = MutableStateFlow(prefs.getInt(KEY_PRESET, -1).toShort())
    val currentPreset: StateFlow<Short> = _currentPreset.asStateFlow()

    private val _params = MutableStateFlow(EqParams())
    val params: StateFlow<EqParams> = _params.asStateFlow()

    var numBands: Int = 0
        private set

    var centerFreqs: List<Int> = emptyList()
        private set

    var minLevel: Int = -1500
        private set

    var maxLevel: Int = 1500
        private set

    fun init(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            equalizer?.release()
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            numBands = eq.numberOfBands.toInt()

            val freqs = mutableListOf<Int>()
            for (i in 0 until numBands) freqs.add(eq.getCenterFreq(i.toShort()))
            centerFreqs = freqs

            val range = eq.bandLevelRange
            if (range != null && range.size >= 2) {
                minLevel = range[0].toInt()
                maxLevel = range[1].toInt()
            }

            eq.enabled = _isEnabled.value

            if (_bandLevels.value.isNotEmpty()) {
                _bandLevels.value.forEach { (band, level) ->
                    try { eq.setBandLevel(band.toShort(), level.toShort()) }
                    catch (e: Exception) { Log.e(TAG, "Band $band failed", e) }
                }
            } else {
                val initialLevels = mutableMapOf<Int, Int>()
                for (i in 0 until numBands) initialLevels[i] = eq.getBandLevel(i.toShort()).toInt()
                _bandLevels.value = initialLevels
                saveBandLevels(initialLevels)
            }
            
            _params.value = EqParams(numBands, centerFreqs, minLevel, maxLevel)
        } catch (e: Exception) {
            Log.e(TAG, "Init failed", e)
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        try { equalizer?.enabled = enabled }
        catch (e: Exception) { Log.e(TAG, "Toggle failed", e) }
    }

    fun setBandLevel(band: Int, level: Int) {
        val coercedLevel = level.coerceIn(minLevel, maxLevel)
        val newLevels = _bandLevels.value.toMutableMap()
        newLevels[band] = coercedLevel
        _bandLevels.value = newLevels
        saveBandLevels(newLevels)
        try { equalizer?.setBandLevel(band.toShort(), coercedLevel.toShort()) }
        catch (e: Exception) { Log.e(TAG, "Level set failed", e) }
        _currentPreset.value = -1
        prefs.edit().putInt(KEY_PRESET, -1).apply()
    }

    fun getPresets(): List<String> {
        val presets = mutableListOf<String>()
        try {
            equalizer?.let { eq ->
                for (i in 0 until eq.numberOfPresets) presets.add(eq.getPresetName(i.toShort()))
            }
        } catch (e: Exception) { Log.e(TAG, "Preset fetch failed", e) }
        return presets
    }

    fun usePreset(presetIndex: Short) {
        if (presetIndex < 0) return
        try {
            equalizer?.usePreset(presetIndex)
            _currentPreset.value = presetIndex
            prefs.edit().putInt(KEY_PRESET, presetIndex.toInt()).apply()

            val newLevels = mutableMapOf<Int, Int>()
            for (i in 0 until numBands) newLevels[i] = equalizer?.getBandLevel(i.toShort())?.toInt() ?: 0
            _bandLevels.value = newLevels
            saveBandLevels(newLevels)
        } catch (e: Exception) { Log.e(TAG, "Preset apply failed", e) }
    }

    fun release() {
        try { equalizer?.release() } catch (e: Exception) { Log.e(TAG, "Release failed", e) }
        equalizer = null
    }

    private fun saveBandLevels(levels: Map<Int, Int>) {
        prefs.edit().apply {
            putInt(KEY_BAND_COUNT, levels.size)
            levels.forEach { (band, level) -> putInt("$KEY_BAND_PREFIX$band", level) }
            apply()
        }
    }

    private fun loadBandLevels(): Map<Int, Int> {
        val count = prefs.getInt(KEY_BAND_COUNT, 0)
        if (count == 0) return emptyMap()
        val levels = mutableMapOf<Int, Int>()
        for (i in 0 until count) {
            if (prefs.contains("$KEY_BAND_PREFIX$i")) {
                levels[i] = prefs.getInt("$KEY_BAND_PREFIX$i", 0)
            }
        }
        return levels
    }

    companion object {
        private const val TAG = "FreqvoxEqualizerManager"
        private const val KEY_ENABLED = "eq_enabled"
        private const val KEY_PRESET = "eq_preset"
        private const val KEY_BAND_COUNT = "eq_band_count"
        private const val KEY_BAND_PREFIX = "eq_band_"
    }
}
