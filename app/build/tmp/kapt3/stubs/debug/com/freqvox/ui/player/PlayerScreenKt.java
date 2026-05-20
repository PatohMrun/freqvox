package com.freqvox.ui.player;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000<\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\u001a\b\u0010\u0000\u001a\u00020\u0001H\u0007\u001a\u00a6\u0001\u0010\u0002\u001a\u00020\u00012\u0006\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\b\u0010\u000b\u001a\u0004\u0018\u00010\u00042\u0006\u0010\f\u001a\u00020\u00072\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00010\u000e2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\u000e2\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00010\u000e2\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00122\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00010\u000e2\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00010\u000e2\b\b\u0002\u0010\u0016\u001a\u00020\u0017H\u0007\u001a\u0010\u0010\u0018\u001a\u00020\u00042\u0006\u0010\u0019\u001a\u00020\tH\u0002\u00a8\u0006\u001a"}, d2 = {"EqualizerAnimation", "", "PlayerScreen", "trackTitle", "", "artist", "isPlaying", "", "currentPosition", "", "duration", "artworkUri", "isLiked", "onPlayPauseClick", "Lkotlin/Function0;", "onNextClick", "onPreviousClick", "onSeek", "Lkotlin/Function1;", "", "onLikeClick", "onTagSwipeUp", "modifier", "Landroidx/compose/ui/Modifier;", "formatTime", "ms", "app_debug"})
public final class PlayerScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void PlayerScreen(@org.jetbrains.annotations.NotNull()
    java.lang.String trackTitle, @org.jetbrains.annotations.NotNull()
    java.lang.String artist, boolean isPlaying, long currentPosition, long duration, @org.jetbrains.annotations.Nullable()
    java.lang.String artworkUri, boolean isLiked, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onPlayPauseClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNextClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onPreviousClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Float, kotlin.Unit> onSeek, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onLikeClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onTagSwipeUp, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    private static final java.lang.String formatTime(long ms) {
        return null;
    }
    
    @androidx.compose.runtime.Composable()
    public static final void EqualizerAnimation() {
    }
}