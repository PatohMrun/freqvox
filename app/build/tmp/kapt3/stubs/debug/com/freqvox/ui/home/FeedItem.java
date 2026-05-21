package com.freqvox.ui.home;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0006\u0003\u0004\u0005\u0006\u0007\bB\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0006\t\n\u000b\f\r\u000e\u00a8\u0006\u000f"}, d2 = {"Lcom/freqvox/ui/home/FeedItem;", "", "()V", "DoubleHorizontalCarousel", "FeaturedBanner", "Grid", "HorizontalCarousel", "Spotlight", "VerticalList", "Lcom/freqvox/ui/home/FeedItem$DoubleHorizontalCarousel;", "Lcom/freqvox/ui/home/FeedItem$FeaturedBanner;", "Lcom/freqvox/ui/home/FeedItem$Grid;", "Lcom/freqvox/ui/home/FeedItem$HorizontalCarousel;", "Lcom/freqvox/ui/home/FeedItem$Spotlight;", "Lcom/freqvox/ui/home/FeedItem$VerticalList;", "app_debug"})
public abstract class FeedItem {
    
    private FeedItem() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\'\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u0012\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\u0002\u0010\tJ\t\u0010\u0010\u001a\u00020\u0003H\u00c6\u0003J\u000f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H\u00c6\u0003J\u000b\u0010\u0012\u001a\u0004\u0018\u00010\bH\u00c6\u0003J/\u0010\u0013\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\bH\u00c6\u0001J\u0013\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017H\u00d6\u0003J\t\u0010\u0018\u001a\u00020\u0019H\u00d6\u0001J\t\u0010\u001a\u001a\u00020\u0003H\u00d6\u0001R\u0013\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000f\u00a8\u0006\u001b"}, d2 = {"Lcom/freqvox/ui/home/FeedItem$DoubleHorizontalCarousel;", "Lcom/freqvox/ui/home/FeedItem;", "title", "", "tracks", "", "Lcom/freqvox/data/local/entities/TrackEntity;", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "(Ljava/lang/String;Ljava/util/List;Landroidx/compose/ui/graphics/vector/ImageVector;)V", "getIcon", "()Landroidx/compose/ui/graphics/vector/ImageVector;", "getTitle", "()Ljava/lang/String;", "getTracks", "()Ljava/util/List;", "component1", "component2", "component3", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class DoubleHorizontalCarousel extends com.freqvox.ui.home.FeedItem {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String title = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<com.freqvox.data.local.entities.TrackEntity> tracks = null;
        @org.jetbrains.annotations.Nullable()
        private final androidx.compose.ui.graphics.vector.ImageVector icon = null;
        
        public DoubleHorizontalCarousel(@org.jetbrains.annotations.NotNull()
        java.lang.String title, @org.jetbrains.annotations.NotNull()
        java.util.List<com.freqvox.data.local.entities.TrackEntity> tracks, @org.jetbrains.annotations.Nullable()
        androidx.compose.ui.graphics.vector.ImageVector icon) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTitle() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.freqvox.data.local.entities.TrackEntity> getTracks() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final androidx.compose.ui.graphics.vector.ImageVector getIcon() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.freqvox.data.local.entities.TrackEntity> component2() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final androidx.compose.ui.graphics.vector.ImageVector component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.freqvox.ui.home.FeedItem.DoubleHorizontalCarousel copy(@org.jetbrains.annotations.NotNull()
        java.lang.String title, @org.jetbrains.annotations.NotNull()
        java.util.List<com.freqvox.data.local.entities.TrackEntity> tracks, @org.jetbrains.annotations.Nullable()
        androidx.compose.ui.graphics.vector.ImageVector icon) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/freqvox/ui/home/FeedItem$FeaturedBanner;", "Lcom/freqvox/ui/home/FeedItem;", "track", "Lcom/freqvox/data/local/entities/TrackEntity;", "(Lcom/freqvox/data/local/entities/TrackEntity;)V", "getTrack", "()Lcom/freqvox/data/local/entities/TrackEntity;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
    public static final class FeaturedBanner extends com.freqvox.ui.home.FeedItem {
        @org.jetbrains.annotations.NotNull()
        private final com.freqvox.data.local.entities.TrackEntity track = null;
        
        public FeaturedBanner(@org.jetbrains.annotations.NotNull()
        com.freqvox.data.local.entities.TrackEntity track) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.freqvox.data.local.entities.TrackEntity getTrack() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.freqvox.data.local.entities.TrackEntity component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.freqvox.ui.home.FeedItem.FeaturedBanner copy(@org.jetbrains.annotations.NotNull()
        com.freqvox.data.local.entities.TrackEntity track) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\'\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u0012\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\u0002\u0010\tJ\t\u0010\u0010\u001a\u00020\u0003H\u00c6\u0003J\u000f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H\u00c6\u0003J\u000b\u0010\u0012\u001a\u0004\u0018\u00010\bH\u00c6\u0003J/\u0010\u0013\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\bH\u00c6\u0001J\u0013\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017H\u00d6\u0003J\t\u0010\u0018\u001a\u00020\u0019H\u00d6\u0001J\t\u0010\u001a\u001a\u00020\u0003H\u00d6\u0001R\u0013\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000f\u00a8\u0006\u001b"}, d2 = {"Lcom/freqvox/ui/home/FeedItem$Grid;", "Lcom/freqvox/ui/home/FeedItem;", "title", "", "tracks", "", "Lcom/freqvox/data/local/entities/TrackEntity;", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "(Ljava/lang/String;Ljava/util/List;Landroidx/compose/ui/graphics/vector/ImageVector;)V", "getIcon", "()Landroidx/compose/ui/graphics/vector/ImageVector;", "getTitle", "()Ljava/lang/String;", "getTracks", "()Ljava/util/List;", "component1", "component2", "component3", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class Grid extends com.freqvox.ui.home.FeedItem {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String title = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<com.freqvox.data.local.entities.TrackEntity> tracks = null;
        @org.jetbrains.annotations.Nullable()
        private final androidx.compose.ui.graphics.vector.ImageVector icon = null;
        
        public Grid(@org.jetbrains.annotations.NotNull()
        java.lang.String title, @org.jetbrains.annotations.NotNull()
        java.util.List<com.freqvox.data.local.entities.TrackEntity> tracks, @org.jetbrains.annotations.Nullable()
        androidx.compose.ui.graphics.vector.ImageVector icon) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTitle() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.freqvox.data.local.entities.TrackEntity> getTracks() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final androidx.compose.ui.graphics.vector.ImageVector getIcon() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.freqvox.data.local.entities.TrackEntity> component2() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final androidx.compose.ui.graphics.vector.ImageVector component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.freqvox.ui.home.FeedItem.Grid copy(@org.jetbrains.annotations.NotNull()
        java.lang.String title, @org.jetbrains.annotations.NotNull()
        java.util.List<com.freqvox.data.local.entities.TrackEntity> tracks, @org.jetbrains.annotations.Nullable()
        androidx.compose.ui.graphics.vector.ImageVector icon) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\'\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u0012\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\u0002\u0010\tJ\t\u0010\u0010\u001a\u00020\u0003H\u00c6\u0003J\u000f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H\u00c6\u0003J\u000b\u0010\u0012\u001a\u0004\u0018\u00010\bH\u00c6\u0003J/\u0010\u0013\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\bH\u00c6\u0001J\u0013\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017H\u00d6\u0003J\t\u0010\u0018\u001a\u00020\u0019H\u00d6\u0001J\t\u0010\u001a\u001a\u00020\u0003H\u00d6\u0001R\u0013\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000f\u00a8\u0006\u001b"}, d2 = {"Lcom/freqvox/ui/home/FeedItem$HorizontalCarousel;", "Lcom/freqvox/ui/home/FeedItem;", "title", "", "tracks", "", "Lcom/freqvox/data/local/entities/TrackEntity;", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "(Ljava/lang/String;Ljava/util/List;Landroidx/compose/ui/graphics/vector/ImageVector;)V", "getIcon", "()Landroidx/compose/ui/graphics/vector/ImageVector;", "getTitle", "()Ljava/lang/String;", "getTracks", "()Ljava/util/List;", "component1", "component2", "component3", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class HorizontalCarousel extends com.freqvox.ui.home.FeedItem {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String title = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<com.freqvox.data.local.entities.TrackEntity> tracks = null;
        @org.jetbrains.annotations.Nullable()
        private final androidx.compose.ui.graphics.vector.ImageVector icon = null;
        
        public HorizontalCarousel(@org.jetbrains.annotations.NotNull()
        java.lang.String title, @org.jetbrains.annotations.NotNull()
        java.util.List<com.freqvox.data.local.entities.TrackEntity> tracks, @org.jetbrains.annotations.Nullable()
        androidx.compose.ui.graphics.vector.ImageVector icon) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTitle() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.freqvox.data.local.entities.TrackEntity> getTracks() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final androidx.compose.ui.graphics.vector.ImageVector getIcon() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.freqvox.data.local.entities.TrackEntity> component2() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final androidx.compose.ui.graphics.vector.ImageVector component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.freqvox.ui.home.FeedItem.HorizontalCarousel copy(@org.jetbrains.annotations.NotNull()
        java.lang.String title, @org.jetbrains.annotations.NotNull()
        java.util.List<com.freqvox.data.local.entities.TrackEntity> tracks, @org.jetbrains.annotations.Nullable()
        androidx.compose.ui.graphics.vector.ImageVector icon) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/freqvox/ui/home/FeedItem$Spotlight;", "Lcom/freqvox/ui/home/FeedItem;", "track", "Lcom/freqvox/data/local/entities/TrackEntity;", "(Lcom/freqvox/data/local/entities/TrackEntity;)V", "getTrack", "()Lcom/freqvox/data/local/entities/TrackEntity;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
    public static final class Spotlight extends com.freqvox.ui.home.FeedItem {
        @org.jetbrains.annotations.NotNull()
        private final com.freqvox.data.local.entities.TrackEntity track = null;
        
        public Spotlight(@org.jetbrains.annotations.NotNull()
        com.freqvox.data.local.entities.TrackEntity track) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.freqvox.data.local.entities.TrackEntity getTrack() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.freqvox.data.local.entities.TrackEntity component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.freqvox.ui.home.FeedItem.Spotlight copy(@org.jetbrains.annotations.NotNull()
        com.freqvox.data.local.entities.TrackEntity track) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B)\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u0012\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\u0002\u0010\tJ\u000b\u0010\u0010\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H\u00c6\u0003J\u000b\u0010\u0012\u001a\u0004\u0018\u00010\bH\u00c6\u0003J1\u0010\u0013\u001a\u00020\u00002\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u00032\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\bH\u00c6\u0001J\u0013\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017H\u00d6\u0003J\t\u0010\u0018\u001a\u00020\u0019H\u00d6\u0001J\t\u0010\u001a\u001a\u00020\u0003H\u00d6\u0001R\u0013\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0013\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000f\u00a8\u0006\u001b"}, d2 = {"Lcom/freqvox/ui/home/FeedItem$VerticalList;", "Lcom/freqvox/ui/home/FeedItem;", "title", "", "tracks", "", "Lcom/freqvox/data/local/entities/TrackEntity;", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "(Ljava/lang/String;Ljava/util/List;Landroidx/compose/ui/graphics/vector/ImageVector;)V", "getIcon", "()Landroidx/compose/ui/graphics/vector/ImageVector;", "getTitle", "()Ljava/lang/String;", "getTracks", "()Ljava/util/List;", "component1", "component2", "component3", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class VerticalList extends com.freqvox.ui.home.FeedItem {
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String title = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<com.freqvox.data.local.entities.TrackEntity> tracks = null;
        @org.jetbrains.annotations.Nullable()
        private final androidx.compose.ui.graphics.vector.ImageVector icon = null;
        
        public VerticalList(@org.jetbrains.annotations.Nullable()
        java.lang.String title, @org.jetbrains.annotations.NotNull()
        java.util.List<com.freqvox.data.local.entities.TrackEntity> tracks, @org.jetbrains.annotations.Nullable()
        androidx.compose.ui.graphics.vector.ImageVector icon) {
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getTitle() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.freqvox.data.local.entities.TrackEntity> getTracks() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final androidx.compose.ui.graphics.vector.ImageVector getIcon() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.freqvox.data.local.entities.TrackEntity> component2() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final androidx.compose.ui.graphics.vector.ImageVector component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.freqvox.ui.home.FeedItem.VerticalList copy(@org.jetbrains.annotations.Nullable()
        java.lang.String title, @org.jetbrains.annotations.NotNull()
        java.util.List<com.freqvox.data.local.entities.TrackEntity> tracks, @org.jetbrains.annotations.Nullable()
        androidx.compose.ui.graphics.vector.ImageVector icon) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}