package com.freqvox.ui.player.components;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u00008\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\u001a\u0018\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u0003\u001a\u0016\u0010\u0006\u001a\u00020\u00012\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\bH\u0003\u001a(\u0010\t\u001a\u00020\u00012\b\u0010\n\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\f\u001a\u00020\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\bH\u0007\u001a\u001a\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00110\u00102\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00050\u0010\u00a8\u0006\u0013"}, d2 = {"InstructionStep", "", "number", "", "text", "", "LyricsInstructions", "onPickFile", "Lkotlin/Function0;", "LyricsSheet", "track", "Lcom/freqvox/data/local/entities/TrackEntity;", "currentPosition", "", "onDismiss", "parseLrc", "", "Lcom/freqvox/ui/player/components/LyricLine;", "lines", "app_debug"})
public final class LyricsSheetKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void LyricsSheet(@org.jetbrains.annotations.Nullable()
    com.freqvox.data.local.entities.TrackEntity track, long currentPosition, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void LyricsInstructions(kotlin.jvm.functions.Function0<kotlin.Unit> onPickFile) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void InstructionStep(int number, java.lang.String text) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static final java.util.List<com.freqvox.ui.player.components.LyricLine> parseLrc(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> lines) {
        return null;
    }
}