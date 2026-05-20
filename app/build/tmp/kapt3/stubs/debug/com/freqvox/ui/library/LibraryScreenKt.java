package com.freqvox.ui.library;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u00000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\u001a \u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u0006H\u0007\u001a\u0018\u0010\u0007\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u0006H\u0007\u001a\u0018\u0010\b\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u0006H\u0007\u001a&\u0010\t\u001a\u00020\u00012\u0012\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\u00010\u000b2\b\b\u0002\u0010\r\u001a\u00020\u000eH\u0007\u001a\u001e\u0010\u000f\u001a\u00020\u00012\u0006\u0010\u0010\u001a\u00020\f2\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00010\u0012H\u0007\u00a8\u0006\u0013"}, d2 = {"AlbumItem", "", "name", "", "artist", "trackCount", "", "ArtistItem", "FolderItem", "LibraryScreen", "onTrackClick", "Lkotlin/Function1;", "Lcom/freqvox/data/local/entities/TrackEntity;", "viewModel", "Lcom/freqvox/ui/main/MainViewModel;", "TrackItem", "track", "onClick", "Lkotlin/Function0;", "app_debug"})
public final class LibraryScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void LibraryScreen(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.freqvox.data.local.entities.TrackEntity, kotlin.Unit> onTrackClick, @org.jetbrains.annotations.NotNull()
    com.freqvox.ui.main.MainViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void TrackItem(@org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.entities.TrackEntity track, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void AlbumItem(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    java.lang.String artist, int trackCount) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void ArtistItem(@org.jetbrains.annotations.NotNull()
    java.lang.String name, int trackCount) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void FolderItem(@org.jetbrains.annotations.NotNull()
    java.lang.String name, int trackCount) {
    }
}