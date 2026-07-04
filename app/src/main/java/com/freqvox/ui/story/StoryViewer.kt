package com.freqvox.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.freqvox.ui.theme.Fvox

private const val SEGMENT_MS = 5000f

/**
 * Full-screen, Instagram-style story viewer: segmented progress bars that auto-advance, tap
 * left/right to skip, press-and-hold to pause, and drag down to dismiss. Each segment is themed
 * by its accent over a dark gradient.
 */
@Composable
fun StoryViewer(
    drop: StoryDrop,
    onAction: (StoryActionType) -> Unit,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        val segments = drop.segments
        var index by remember { mutableIntStateOf(0) }
        var paused by remember { mutableStateOf(false) }
        var progress by remember { mutableFloatStateOf(0f) }

        val safeIndex = index.coerceIn(0, segments.lastIndex)
        val segment = segments[safeIndex]
        val accent = segment.accent ?: Fvox.accent
        val base = lerp(accent, Color.Black, 0.72f)

        // Frame-driven timeline so press-to-pause is read live each frame; advances on completion.
        LaunchedEffect(index) {
            progress = 0f
            var lastFrame = 0L
            while (progress < 1f) {
                val now = withFrameNanos { it }
                if (lastFrame != 0L && !paused) {
                    val dtMs = (now - lastFrame) / 1_000_000f
                    progress = (progress + dtMs / SEGMENT_MS).coerceAtMost(1f)
                }
                lastFrame = now
            }
            if (index < segments.lastIndex) index++ else onDismiss()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(base, Color.Black)))
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onVerticalDrag = { _, dy -> total += dy },
                        onDragEnd = { if (total > 220f) onDismiss() }
                    )
                }
                .pointerInput(segments.size) {
                    detectTapGestures(
                        onPress = {
                            paused = true
                            tryAwaitRelease()
                            paused = false
                        },
                        onTap = { offset ->
                            if (offset.x < size.width / 2f) {
                                if (index > 0) index-- else progress = 0f
                            } else {
                                if (index < segments.lastIndex) index++ else onDismiss()
                            }
                        }
                    )
                }
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Progress bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    segments.forEachIndexed { i, _ ->
                        val f = when {
                            i < safeIndex -> 1f
                            i == safeIndex -> progress
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.28f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (onShare != null && drop.kind == SegmentKind.RECAP) {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    SegmentVisualView(visual = segment.visual, accent = accent)
                    Spacer(Modifier.height(28.dp))
                    Text(
                        segment.title,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        segment.body,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 23.sp
                    )
                }

                // CTA
                segment.cta?.let { cta ->
                    Button(
                        onClick = { onAction(cta.action); onDismiss() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = base)
                    ) {
                        Text(cta.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
