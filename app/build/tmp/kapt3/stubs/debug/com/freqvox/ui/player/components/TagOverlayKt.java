package com.freqvox.ui.player.components;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000<\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a2\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u00022\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00040\u000bH\u0007\u001aF\u0010\f\u001a\u00020\u00042\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00060\u000e2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00100\u000e2\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00040\u00122\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00040\u000bH\u0007\"\u001a\u0010\u0000\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00020\u0001X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"tagEmojis", "", "", "TagChip", "", "tag", "Lcom/freqvox/data/local/entities/TagEntity;", "isSelected", "", "emoji", "onClick", "Lkotlin/Function0;", "TagOverlay", "tags", "", "selectedTagIds", "", "onTagClick", "Lkotlin/Function1;", "onClose", "app_debug"})
public final class TagOverlayKt {
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Map<java.lang.String, java.lang.String> tagEmojis = null;
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void TagOverlay(@org.jetbrains.annotations.NotNull()
    java.util.List<com.freqvox.data.local.entities.TagEntity> tags, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.Long> selectedTagIds, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.freqvox.data.local.entities.TagEntity, kotlin.Unit> onTagClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onClose) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void TagChip(@org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.entities.TagEntity tag, boolean isSelected, @org.jetbrains.annotations.Nullable()
    java.lang.String emoji, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
}