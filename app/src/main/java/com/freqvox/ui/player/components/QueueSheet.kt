package com.freqvox.ui.player.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.freqvox.playback.QueueItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.rememberScroller

private val SheetBackground = Color(0xFF12121A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<QueueItem>,
    currentQueueIndex: Int,
    accentColor: Color = Color(0xFF7C4DFF),
    onQueueItemClick: (Int) -> Unit,
    onQueueItemRemove: (Int) -> Unit,
    onQueueItemMove: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic     = LocalHapticFeedback.current
    val listState  = rememberLazyListState()
    val localQueue = remember { mutableStateListOf<QueueItem>() }

    // Highlight tracks the playing item by stable id so it follows the track across reorders.
    val playingTrackId = queue.getOrNull(currentQueueIndex)?.trackId

    // Captured once per drag: the original index where the gesture started,
    // and the latest index as the user reorders. Committed upstream on drop.
    var isReordering  by remember { mutableStateOf(false) }
    var dragFromIndex by remember { mutableIntStateOf(-1) }
    var dragToIndex   by remember { mutableIntStateOf(-1) }

    // ----- Auto-scroll tuning -------------------------------------------------
    // Wider threshold = longer 0x→5x ramp as finger nears the edge (gentler accel/decel).
    // Lower speed factor = lower peak velocity, so the deceleration glides instead of cuts.
    // Library defaults: 48.dp threshold, 0.05f speed factor (≈ viewportH * 0.5 px/s base).
    val scrollEdgeThreshold = 88.dp
    val scrollSpeedFactor   = 0.035f

    val scroller = rememberScroller(
        scrollableState     = listState,
        pixelAmountProvider = { listState.layoutInfo.viewportSize.height * scrollSpeedFactor }
    )

    val reorderState = rememberReorderableLazyListState(
        lazyListState   = listState,
        scrollThreshold = scrollEdgeThreshold,
        scroller        = scroller
    ) { from, to ->
        val f = from.index
        val t = to.index
        if (f in localQueue.indices && t in localQueue.indices) {
            localQueue.add(t, localQueue.removeAt(f))
            if (dragFromIndex == -1) dragFromIndex = f
            dragToIndex = t
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Mirror upstream into local list — but never while a drag is in flight.
    // Re-runs both when the upstream queue mutates and when reordering ends,
    // so we re-sync even if upstream pushed an update mid-drag.
    LaunchedEffect(queue, isReordering) {
        if (!isReordering && localQueue.toList() != queue) {
            localQueue.clear()
            localQueue.addAll(queue)
        }
    }

    LaunchedEffect(Unit) {
        if (localQueue.isEmpty() && queue.isNotEmpty()) {
            localQueue.addAll(queue)
        }
        if (queue.isNotEmpty()) {
            listState.scrollToItem(currentQueueIndex.coerceIn(0, queue.lastIndex))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF12121A),
        dragHandle       = { BottomSheetDefaults.DragHandle() },
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                Text("Queue", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (queue.isNotEmpty())
                    Text("${queue.size} tracks", color = Color.White.copy(.4f), fontSize = 13.sp)
            }

            if (queue.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, null,
                            tint = Color.White.copy(.2f), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Queue is empty", color = Color.White.copy(.4f), fontSize = 14.sp)
                        Text("Play a track from your library", color = Color.White.copy(.25f), fontSize = 12.sp)
                    }
                }
                return@Column
            }

            val playingIdxLocal = if (playingTrackId == null) -1
                                  else localQueue.indexOfFirst { it.trackId == playingTrackId }

            // How rows that aren't being dragged slide out of the dragged row's way.
            // Lower stiffness  = slower, gentler glide.   Higher = snappier, more abrupt.
            // Lower dampingRatio = more bouncy overshoot. Higher = more critically damped.
            val flowSpec = remember {
                spring<androidx.compose.ui.unit.IntOffset>(
                    dampingRatio = 0.78f,
                    stiffness    = Spring.StiffnessMediumLow
                )
            }

            LazyColumn(
                state          = listState,
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 48.dp),
                modifier       = Modifier.heightIn(max = 540.dp)
            ) {
                itemsIndexed(localQueue, key = { _, item -> item.trackId }) { index, item ->
                    ReorderableItem(
                        state               = reorderState,
                        key                 = item.trackId,
                        animateItemModifier = Modifier.animateItem(placementSpec = flowSpec)
                    ) { isDragging ->
                        val isCurrent = item.trackId == playingTrackId
                        val isPast    = playingIdxLocal != -1 && index < playingIdxLocal

                        val rowBackground = when {
                            isDragging -> accentColor.copy(alpha = 0.28f).compositeOver(SheetBackground)
                            isCurrent  -> accentColor.copy(alpha = 0.10f)
                            else       -> Color.White.copy(alpha = 0.03f)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        isReordering  = true
                                        dragFromIndex = -1
                                        dragToIndex   = -1
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        val from = dragFromIndex
                                        val to   = dragToIndex
                                        isReordering  = false
                                        dragFromIndex = -1
                                        dragToIndex   = -1
                                        if (from != -1 && to != -1 && from != to) {
                                            onQueueItemMove(from, to)
                                        }
                                    },
                                )
                                .zIndex(if (isDragging) 10f else 0f)
                                .graphicsLayer {
                                    scaleX          = if (isDragging) 1.04f else 1f
                                    scaleY          = if (isDragging) 1.04f else 1f
                                    shadowElevation = if (isDragging) 24f else 0f
                                    shape           = RoundedCornerShape(12.dp)
                                    alpha           = if (isPast && !isDragging) 0.55f else 1f
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(rowBackground)
                                .then(when {
                                    isDragging -> Modifier.border(1.dp, accentColor.copy(.4f), RoundedCornerShape(12.dp))
                                    isCurrent  -> Modifier.border(1.dp, accentColor.copy(.28f), RoundedCornerShape(12.dp))
                                    else       -> Modifier
                                })
                                .clickable(enabled = !isCurrent && !isDragging) { onQueueItemClick(index) }
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DragHandle, null,
                                tint = if (isDragging) accentColor.copy(.9f) else Color.White.copy(.20f),
                                modifier = Modifier.size(40.dp).padding(11.dp)
                            )

                            Box(Modifier.width(26.dp), Alignment.Center) {
                                when {
                                    isCurrent -> Icon(Icons.AutoMirrored.Filled.VolumeUp, null,
                                        tint = accentColor, modifier = Modifier.size(14.dp))
                                    isPast -> Icon(Icons.Default.Check, null,
                                        tint = Color.White.copy(.2f), modifier = Modifier.size(13.dp))
                                    else -> Text("${index + 1}", color = Color.White.copy(.3f),
                                        fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Column(Modifier.weight(1f)) {
                                Text(item.title,
                                    color = when {
                                        isCurrent -> accentColor
                                        isPast    -> Color.White.copy(.35f)
                                        else      -> Color.White.copy(.9f)
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.artist,
                                    color = if (isPast) Color.White.copy(.2f) else Color.White.copy(.45f),
                                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            if (!isCurrent && !isDragging) {
                                IconButton(onClick = { onQueueItemRemove(index) },
                                    modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, "Remove",
                                        tint = Color.White.copy(.25f), modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
