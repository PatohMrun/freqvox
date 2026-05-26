package com.freqvox.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.freqvox.data.local.entities.TagEntity
import com.freqvox.ui.player.components.TagOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPickerSheet(
    tags: List<TagEntity>,
    selectedTagIds: List<Long>,
    onTagClick: (TagEntity) -> Unit,
    onCreateCustomTag: (String) -> Unit,
    onDismiss: () -> Unit,
    isDisliked: Boolean = false,
    onToggleDislike: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF12121A),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        TagOverlay(
            tags = tags,
            selectedTagIds = selectedTagIds,
            onTagClick = onTagClick,
            onCreateCustomTag = onCreateCustomTag,
            onClose = onDismiss,
            isDisliked = isDisliked,
            onToggleDislike = onToggleDislike,
            modifier = Modifier.navigationBarsPadding()
        )
    }
}
