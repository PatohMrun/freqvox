package com.freqvox.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.freqvox.playback.QueueItem
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    // Local copy — ORDER IS NEVER CHANGED DURING DRAG, only on drop.
    // This keeps pointerInput stable for the entire gesture lifecycle.
    val localQueue = remember { mutableStateListOf<QueueItem>() }
    var localCurrentIdx by remember { mutableIntStateOf(currentQueueIndex) }

    // Use MutableState objects (not 'by' delegates) so reads inside graphicsLayer
    // lambdas are properly deferred — only a draw pass fires, never a recomposition.
    val draggedIdx    = remember { mutableIntStateOf(-1) }   // item being held
    val targetIdx     = remember { mutableIntStateOf(-1) }   // where it will land
    val dragTY        = remember { mutableFloatStateOf(0f) } // screen-space Y delta
    val pointerY      = remember { mutableFloatStateOf(0f) } // viewport Y for autoscroll

    val isDragging by remember { derivedStateOf { draggedIdx.intValue != -1 } }

    val listState    = rememberLazyListState()
    val density      = LocalDensity.current
    val itemHeightPx = remember(density) { with(density) { 64.dp.toPx() } }

    // Sync from canonical queue only when idle (not dragging)
    LaunchedEffect(queue, currentQueueIndex) {
        if (draggedIdx.intValue == -1) {
            if (localQueue.toList() != queue) {
                localQueue.clear()
                localQueue.addAll(queue)
            }
            localCurrentIdx = currentQueueIndex
        }
    }

    // Scroll to currently playing item on open
    LaunchedEffect(Unit) {
        if (queue.isNotEmpty()) {
            listState.scrollToItem(currentQueueIndex.coerceIn(0, queue.lastIndex))
        }
    }

    // Autoscroll loop — only scrolls, NEVER touches index state
    LaunchedEffect(isDragging) {
        if (!isDragging) return@LaunchedEffect
        while (isActive) {
            val vpH   = listState.layoutInfo.viewportSize.height.toFloat()
            if (vpH > 0f) {
                val edge  = with(density) { 80.dp.toPx() }
                val py    = pointerY.floatValue
                val speed = when {
                    py < edge      -> -(1f - (py / edge).coerceIn(0f, 1f)) * 18f
                    py > vpH-edge  -> ((py - (vpH - edge)) / edge).coerceIn(0f, 1f) * 18f
                    else           -> 0f
                }
                if (speed != 0f) {
                    val consumed = listState.scrollBy(speed)
                    // When the list scrolls, the finger moves relative to the list.
                    // Compensate so the held item tracks the finger and slot math stays correct.
                    if (consumed != 0f) {
                        dragTY.floatValue += consumed
                        val slots = (dragTY.floatValue / itemHeightPx).roundToInt()
                        targetIdx.intValue = (draggedIdx.intValue + slots)
                            .coerceIn(0, localQueue.lastIndex)
                    }
                }
            }
            delay(16L)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF12121A),
        dragHandle       = null,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxWidth()) {

            // Header
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

            LazyColumn(
                state          = listState,
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 48.dp),
                modifier       = Modifier.heightIn(max = 540.dp)
            ) {
                itemsIndexed(localQueue, key = { _, item -> item.trackId }) { index, item ->
                    val isCurrent      = index == localCurrentIdx
                    val isPast         = index < localCurrentIdx
                    val isBeingDragged = index == draggedIdx.intValue

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isBeingDragged) 10f else 0f)
                            .graphicsLayer {
                                // ── All drag-state reads are DEFERRED here ──────────────────
                                // Changing dragTY / targetIdx only causes a GPU layer redraw.
                                // No list-item recompositions fire during drag → no flicker.
                                val di = draggedIdx.intValue
                                val ti = targetIdx.intValue
                                val ty = dragTY.floatValue
                                translationY = when {
                                    index == di              ->  ty
                                    ti > di && index in (di+1)..ti -> -itemHeightPx
                                    ti < di && index in ti until di ->  itemHeightPx
                                    else                     ->  0f
                                }
                                scaleX          = if (index == di) 1.04f else 1f
                                scaleY          = if (index == di) 1.04f else 1f
                                shadowElevation = if (index == di) 24f   else 0f
                                alpha           = if (isPast && !isBeingDragged) 0.55f else 1f
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(when {
                                isBeingDragged -> accentColor.copy(alpha = 0.22f)
                                isCurrent      -> accentColor.copy(alpha = 0.10f)
                                else           -> Color.White.copy(alpha = 0.03f)
                            })
                            .then(when {
                                isBeingDragged -> Modifier.border(1.dp, accentColor.copy(.4f), RoundedCornerShape(12.dp))
                                isCurrent      -> Modifier.border(1.dp, accentColor.copy(.28f), RoundedCornerShape(12.dp))
                                else           -> Modifier
                            })
                            .pointerInput(item.trackId) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        draggedIdx.intValue = index
                                        targetIdx.intValue  = index
                                        dragTY.floatValue   = 0f
                                        val info = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.index == index }
                                        pointerY.floatValue = (info?.offset?.toFloat()
                                            ?: (index * itemHeightPx)) + offset.y
                                    },
                                    onDrag = { change, delta ->
                                        change.consume()
                                        dragTY.floatValue   += delta.y
                                        pointerY.floatValue += delta.y
                                        // Slot-based target: how many item-heights from start?
                                        val di    = draggedIdx.intValue
                                        val slots = (dragTY.floatValue / itemHeightPx).roundToInt()
                                        targetIdx.intValue = (di + slots)
                                            .coerceIn(0, localQueue.lastIndex)
                                    },
                                    onDragEnd = {
                                        val from = draggedIdx.intValue
                                        val to   = targetIdx.intValue
                                        // Reset visual state first
                                        draggedIdx.intValue = -1
                                        targetIdx.intValue  = -1
                                        dragTY.floatValue   = 0f
                                        // Single atomic reorder only on drop
                                        if (from != to &&
                                            from in localQueue.indices &&
                                            to   in localQueue.indices) {
                                            localQueue.add(to, localQueue.removeAt(from))
                                            localCurrentIdx = when {
                                                localCurrentIdx == from -> to
                                                from < localCurrentIdx && to >= localCurrentIdx -> localCurrentIdx - 1
                                                from > localCurrentIdx && to <= localCurrentIdx -> localCurrentIdx + 1
                                                else -> localCurrentIdx
                                            }
                                            onQueueItemMove(from, to)
                                        }
                                    },
                                    onDragCancel = {
                                        draggedIdx.intValue = -1
                                        targetIdx.intValue  = -1
                                        dragTY.floatValue   = 0f
                                        localQueue.clear()
                                        localQueue.addAll(queue)
                                        localCurrentIdx = currentQueueIndex
                                    }
                                )
                            }
                            .clickable(enabled = !isCurrent && !isDragging) { onQueueItemClick(index) }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drag handle
                        Icon(
                            Icons.Default.DragHandle, null,
                            tint = if (isBeingDragged) accentColor.copy(.9f)
                                   else Color.White.copy(.20f),
                            modifier = Modifier.size(40.dp).padding(11.dp)
                        )

                        // Position indicator
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
