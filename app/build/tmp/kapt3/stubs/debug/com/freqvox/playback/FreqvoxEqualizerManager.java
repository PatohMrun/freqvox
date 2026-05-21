package com.freqvox.playback;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000X\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010$\n\u0002\u0010\b\n\u0000\n\u0002\u0010\n\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u000e\u0018\u0000 42\u00020\u0001:\u00014B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\f\u0010$\u001a\b\u0012\u0004\u0012\u00020%0\u0012J\u000e\u0010&\u001a\u00020\'2\u0006\u0010(\u001a\u00020\bJ\u0014\u0010)\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\b0\u0007H\u0002J\u0006\u0010*\u001a\u00020\'J\u001c\u0010+\u001a\u00020\'2\u0012\u0010,\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\b0\u0007H\u0002J\u0016\u0010-\u001a\u00020\'2\u0006\u0010.\u001a\u00020\b2\u0006\u0010/\u001a\u00020\bJ\u000e\u00100\u001a\u00020\'2\u0006\u00101\u001a\u00020\fJ\u000e\u00102\u001a\u00020\'2\u0006\u00103\u001a\u00020\nR \u0010\u0005\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\b0\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\f0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R#\u0010\r\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\b0\u00070\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R*\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\b0\u00122\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\b0\u0012@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0017\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\n0\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0010R\u0010\u0010\u0018\u001a\u0004\u0018\u00010\u0019X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\f0\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0010R\u001e\u0010\u001b\u001a\u00020\b2\u0006\u0010\u0011\u001a\u00020\b@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001dR\u001e\u0010\u001e\u001a\u00020\b2\u0006\u0010\u0011\u001a\u00020\b@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u001dR\u001e\u0010 \u001a\u00020\b2\u0006\u0010\u0011\u001a\u00020\b@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\u001dR\u000e\u0010\"\u001a\u00020#X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00065"}, d2 = {"Lcom/freqvox/playback/FreqvoxEqualizerManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "_bandLevels", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "", "_currentPreset", "", "_isEnabled", "", "bandLevels", "Lkotlinx/coroutines/flow/StateFlow;", "getBandLevels", "()Lkotlinx/coroutines/flow/StateFlow;", "<set-?>", "", "centerFreqs", "getCenterFreqs", "()Ljava/util/List;", "currentPreset", "getCurrentPreset", "equalizer", "Landroid/media/audiofx/Equalizer;", "isEnabled", "maxLevel", "getMaxLevel", "()I", "minLevel", "getMinLevel", "numBands", "getNumBands", "prefs", "Landroid/content/SharedPreferences;", "getPresets", "", "init", "", "audioSessionId", "loadBandLevels", "release", "saveBandLevels", "levels", "setBandLevel", "band", "level", "setEnabled", "enabled", "usePreset", "presetIndex", "Companion", "app_debug"})
public final class FreqvoxEqualizerManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences prefs = null;
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
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "FreqvoxEqualizerManager";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_ENABLED = "eq_enabled";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_PRESET = "eq_preset";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_BAND_COUNT = "eq_band_count";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_BAND_PREFIX = "eq_band_";
    @org.jetbrains.annotations.NotNull()
    public static final com.freqvox.playback.FreqvoxEqualizerManager.Companion Companion = null;
    
    public FreqvoxEqualizerManager(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
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
    
    private final void saveBandLevels(java.util.Map<java.lang.Integer, java.lang.Integer> levels) {
    }
    
    private final java.util.Map<java.lang.Integer, java.lang.Integer> loadBandLevels() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lcom/freqvox/playback/FreqvoxEqualizerManager$Companion;", "", "()V", "KEY_BAND_COUNT", "", "KEY_BAND_PREFIX", "KEY_ENABLED", "KEY_PRESET", "TAG", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}