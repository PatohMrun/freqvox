package com.freqvox.playback

import android.media.audiofx.Equalizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the Android System Equalizer and exposes reactive state for the UI.
 * Scoped to the audio session of the current player.
 */
class FreqvoxEqualizerManager {
    private var equalizer: Equalizer? = null
    
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()
    
    private val _bandLevels = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val bandLevels: StateFlow<Map<Int, Int>> = _bandLevels.asStateFlow()
    
    private val _currentPreset = MutableStateFlow<Short>(-1)
    val currentPreset: StateFlow<Short> = _currentPreset.asStateFlow()
    
    var numBands: Int = 0
        private set
        
    var centerFreqs: List<Int> = emptyList()
        private set

    var minLevel: Int = -1500
        private set
        
    var maxLevel: Int = 1500
        private set

    /**
     * Connects to the given audio session.
     */
    fun init(audioSessionId: Int) {
        if (audioSessionId == 0) return
        
        try {
            // Ensure any previous instance is released
            equalizer?.release()
            
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            numBands = eq.numberOfBands.toInt()
            
            val freqs = mutableListOf<Int>()
            for (i in 0 until numBands) {
                freqs.add(eq.getCenterFreq(i.toShort()))
            }
            centerFreqs = freqs
            
            val range = eq.bandLevelRange
            if (range != null && range.size >= 2) {
                minLevel = range[0].toInt()
                maxLevel = range[1].toInt()
            }
            
            eq.enabled = _isEnabled.value
            
            // Sync current state to new audio session
            if (_bandLevels.value.isNotEmpty()) {
                _bandLevels.value.forEach { (band, level) ->
                    try {
                        eq.setBandLevel(band.toShort(), level.toShort())
                    } catch (e: Exception) {
                        Log.e("FreqvoxEqualizerManager", "Band $band failed", e)
                    }
                }
            } else {
                // Initialize default levels from current system state
                val initialLevels = mutableMapOf<Int, Int>()
                for (i in 0 until numBands) {
                    initialLevels[i] = eq.getBandLevel(i.toShort()).toInt()
                }
                _bandLevels.value = initialLevels
            }
            
        } catch (e: Exception) {
            Log.e("FreqvoxEqualizerManager", "Init failed", e)
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        try {
            equalizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e("FreqvoxEqualizerManager", "Toggle failed", e)
        }
    }

    fun setBandLevel(band: Int, level: Int) {
        val coercedLevel = level.coerceIn(minLevel, maxLevel)
        val newLevels = _bandLevels.value.toMutableMap()
        newLevels[band] = coercedLevel
        _bandLevels.value = newLevels
        
        try {
            equalizer?.setBandLevel(band.toShort(), coercedLevel.toShort())
        } catch (e: Exception) {
            Log.e("FreqvoxEqualizerManager", "Level set failed", e)
        }
        _currentPreset.value = -1 
    }

    fun getPresets(): List<String> {
        val presets = mutableListOf<String>()
        try {
            equalizer?.let { eq ->
                for (i in 0 until eq.numberOfPresets) {
                    presets.add(eq.getPresetName(i.toShort()))
                }
            }
        } catch (e: Exception) {
            Log.e("FreqvoxEqualizerManager", "Preset fetch failed", e)
        }
        return presets
    }

    fun usePreset(presetIndex: Short) {
        if (presetIndex < 0) return
        try {
            equalizer?.usePreset(presetIndex)
            _currentPreset.value = presetIndex
            
            val newLevels = mutableMapOf<Int, Int>()
            for (i in 0 until numBands) {
                newLevels[i] = equalizer?.getBandLevel(i.toShort())?.toInt() ?: 0
            }
            _bandLevels.value = newLevels
        } catch (e: Exception) {
            Log.e("FreqvoxEqualizerManager", "Preset apply failed", e)
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            Log.e("FreqvoxEqualizerManager", "Release failed", e)
        }
        equalizer = null
    }
}
