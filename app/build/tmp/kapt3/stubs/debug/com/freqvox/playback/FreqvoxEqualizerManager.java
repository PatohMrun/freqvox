package com.freqvox.playback;

/**
 * Manages the Android System Equalizer and exposes reactive state for the UI.
 * Scoped to the audio session of the current player.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010$\n\u0002\u0010\b\n\u0000\n\u0002\u0010\n\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0002\b\n\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\f\u0010 \u001a\b\u0012\u0004\u0012\u00020!0\u0010J\u000e\u0010\"\u001a\u00020#2\u0006\u0010$\u001a\u00020\u0006J\u0006\u0010%\u001a\u00020#J\u0016\u0010&\u001a\u00020#2\u0006\u0010\'\u001a\u00020\u00062\u0006\u0010(\u001a\u00020\u0006J\u000e\u0010)\u001a\u00020#2\u0006\u0010*\u001a\u00020\nJ\u000e\u0010+\u001a\u00020#2\u0006\u0010,\u001a\u00020\bR \u0010\u0003\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00060\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R#\u0010\u000b\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00060\u00050\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR*\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00060\u00102\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00060\u0010@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0017\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\b0\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u000eR\u0010\u0010\u0016\u001a\u0004\u0018\u00010\u0017X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\n0\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u000eR\u001e\u0010\u0019\u001a\u00020\u00062\u0006\u0010\u000f\u001a\u00020\u0006@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u001bR\u001e\u0010\u001c\u001a\u00020\u00062\u0006\u0010\u000f\u001a\u00020\u0006@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001bR\u001e\u0010\u001e\u001a\u00020\u00062\u0006\u0010\u000f\u001a\u00020\u0006@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u001b\u00a8\u0006-"}, d2 = {"Lcom/freqvox/playback/FreqvoxEqualizerManager;", "", "()V", "_bandLevels", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "", "_currentPreset", "", "_isEnabled", "", "bandLevels", "Lkotlinx/coroutines/flow/StateFlow;", "getBandLevels", "()Lkotlinx/coroutines/flow/StateFlow;", "<set-?>", "", "centerFreqs", "getCenterFreqs", "()Ljava/util/List;", "currentPreset", "getCurrentPreset", "equalizer", "Landroid/media/audiofx/Equalizer;", "isEnabled", "maxLevel", "getMaxLevel", "()I", "minLevel", "getMinLevel", "numBands", "getNumBands", "getPresets", "", "init", "", "audioSessionId", "release", "setBandLevel", "band", "level", "setEnabled", "enabled", "usePreset", "presetIndex", "app_debug"})
public final class FreqvoxEqualizerManager {
    @org.jetbrains.annotations.Nullable()
    private android.media.audiofx.Equalizer equalizer;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isEnabled = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isEnabled = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.Map<java.lang.Integer, java.lang.Integer>> _bandLevels = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.Integer, java.lang.Integer>> bandLevels = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Short> _currentPreset = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Short> currentPreset = null;
    private int numBands = 0;
    @org.jetbrains.annotations.NotNull()
    private java.util.List<java.lang.Integer> centerFreqs;
    private int minLevel = -1500;
    private int maxLevel = 1500;
    
    public FreqvoxEqualizerManager() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isEnabled() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.Integer, java.lang.Integer>> getBandLevels() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Short> getCurrentPreset() {
        return null;
    }
    
    public final int getNumBands() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.Integer> getCenterFreqs() {
        return null;
    }
    
    public final int getMinLevel() {
        return 0;
    }
    
    public final int getMaxLevel() {
        return 0;
    }
    
    /**
     * Connects to the given audio session.
     */
    public final void init(int audioSessionId) {
    }
    
    public final void setEnabled(boolean enabled) {
    }
    
    public final void setBandLevel(int band, int level) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getPresets() {
        return null;
    }
    
    public final void usePreset(short presetIndex) {
    }
    
    public final void release() {
    }
}