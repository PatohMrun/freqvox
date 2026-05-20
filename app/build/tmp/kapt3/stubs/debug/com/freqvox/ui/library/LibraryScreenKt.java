package com.freqvox.ui.library;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000`\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a(\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0007\u001a \u0010\t\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0007\u001a<\u0010\n\u001a\u00020\u00012\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\f2\u0006\u0010\u000e\u001a\u00020\r2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\u00010\u00102\b\b\u0002\u0010\u0011\u001a\u00020\u0012H\u0007\u001a\u0018\u0010\u0013\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u0006H\u0007\u001a.\u0010\u0014\u001a\u00020\u00012\u0006\u0010\u0015\u001a\u00020\u00162\u0012\u0010\u0017\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00010\u00102\b\b\u0002\u0010\u0018\u001a\u00020\u0019H\u0007\u001a2\u0010\u001a\u001a\u00020\u00012\u0006\u0010\u001b\u001a\u00020\b2\b\b\u0002\u0010\u001c\u001a\u00020\u001d2\b\b\u0002\u0010\u001e\u001a\u00020\u001d2\f\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u00010 H\u0007\u001a$\u0010!\u001a\u00020\u00012\u0006\u0010\"\u001a\u00020#2\u0012\u0010$\u001a\u000e\u0012\u0004\u0012\u00020#\u0012\u0004\u0012\u00020\u00010\u0010H\u0007\u00a8\u0006%"}, d2 = {"AlbumItem", "", "name", "", "artist", "trackCount", "", "representativeTrack", "Lcom/freqvox/data/local/entities/TrackEntity;", "ArtistItem", "DraggableTabRow", "tabOrder", "", "Lcom/freqvox/ui/library/LibraryTab;", "selectedTab", "onTabSelected", "Lkotlin/Function1;", "modifier", "Landroidx/compose/ui/Modifier;", "FolderItem", "LibraryScreen", "playbackController", "Lcom/freqvox/playback/PlaybackController;", "onTrackClick", "viewModel", "Lcom/freqvox/ui/main/MainViewModel;", "LibraryTrackItem", "track", "isCurrentTrack", "", "isPlaying", "onClick", "Lkotlin/Function0;", "SortButton", "sortOrder", "Lcom/freqvox/ui/library/SortOrder;", "onSortSelected", "app_debug"})
public final class LibraryScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void LibraryScreen(@org.jetbrains.annotations.NotNull()
    com.freqvox.playback.PlaybackController playbackController, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.freqvox.data.local.entities.TrackEntity, kotlin.Unit> onTrackClick, @org.jetbrains.annotations.NotNull()
    com.freqvox.ui.main.MainViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void DraggableTabRow(@org.jetbrains.annotations.NotNull()
    java.util.List<com.freqvox.ui.library.LibraryTab> tabOrder, @org.jetbrains.annotations.NotNull()
    com.freqvox.ui.library.LibraryTab selectedTab, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.freqvox.ui.library.LibraryTab, kotlin.Unit> onTabSelected, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void SortButton(@org.jetbrains.annotations.NotNull()
    com.freqvox.ui.library.SortOrder sortOrder, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.freqvox.ui.library.SortOrder, kotlin.Unit> onSortSelected) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void LibraryTrackItem(@org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.entities.TrackEntity track, boolean isCurrentTrack, boolean isPlaying, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void AlbumItem(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    java.lang.String artist, int trackCount, @org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.entities.TrackEntity representativeTrack) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void ArtistItem(@org.jetbrains.annotations.NotNull()
    java.lang.String name, int trackCount, @org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.entities.TrackEntity representativeTrack) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void FolderItem(@org.jetbrains.annotations.NotNull()
    java.lang.String name, int trackCount) {
    }
}