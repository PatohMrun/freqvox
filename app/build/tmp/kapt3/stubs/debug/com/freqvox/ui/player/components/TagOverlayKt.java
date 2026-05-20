package com.freqvox.ui.player.components;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u00000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a&\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0007\u001aF\u0010\b\u001a\u00020\u00012\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00030\n2\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\f0\n2\u0012\u0010\r\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u000e2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0007\u00a8\u0006\u0010"}, d2 = {"TagChip", "", "tag", "Lcom/freqvox/data/local/entities/TagEntity;", "isSelected", "", "onClick", "Lkotlin/Function0;", "TagOverlay", "tags", "", "selectedTagIds", "", "onTagClick", "Lkotlin/Function1;", "onClose", "app_debug"})
public final class TagOverlayKt {
    
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
    com.freqvox.data.local.entities.TagEntity tag, boolean isSelected, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
}