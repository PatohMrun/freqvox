package com.freqvox.ui.player.components;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000@\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a2\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u00032\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00050\fH\u0007\u001aF\u0010\r\u001a\u00020\u00052\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00070\u000f2\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\u000f2\u0012\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00050\u00132\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00050\fH\u0007\"\u001a\u0010\u0000\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00030\u0001X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0015"}, d2 = {"tagIcons", "", "", "Landroidx/compose/ui/graphics/vector/ImageVector;", "TagChip", "", "tag", "Lcom/freqvox/data/local/entities/TagEntity;", "isSelected", "", "icon", "onClick", "Lkotlin/Function0;", "TagOverlay", "tags", "", "selectedTagIds", "", "onTagClick", "Lkotlin/Function1;", "onClose", "app_debug"})
public final class TagOverlayKt {
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Map<java.lang.String, androidx.compose.ui.graphics.vector.ImageVector> tagIcons = null;
    
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
    androidx.compose.ui.graphics.vector.ImageVector icon, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
}