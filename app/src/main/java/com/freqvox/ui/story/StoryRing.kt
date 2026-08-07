package com.freqvox.ui.story

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.freqvox.ui.theme.Fvox

/**
 * WhatsApp-style story ring: broken into one arc per card of the active drop, each arc
 * coloured by whether THAT card has been displayed — vivid gradient for unviewed, faint
 * for viewed. A single-card drop renders as an unbroken circle. Pulses while anything
 * is unviewed. The avatar (logo, etc.) is drawn by [content] inside the ring.
 *
 * [seenFlags] is one flag per card in drop order (true = seen). Empty = no drop yet:
 * renders the faint idle ring.
 */
@Composable
fun StoryRing(
    seenFlags: List<Boolean>,
    hasUnseen: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val accent = Fvox.accent
    val accentAlt = Fvox.accentAlt
    val seenColor = Fvox.onSurface.copy(alpha = 0.18f)

    val ringPulse = rememberInfiniteTransition(label = "ring")
    val pulse by ringPulse.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "ringScale"
    )

    Box(
        modifier = modifier
            .size(46.dp)
            .scale(if (hasUnseen) pulse else 1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeUnseen = 2.5.dp.toPx()
            val strokeSeen = 1.5.dp.toPx()
            val inset = strokeUnseen / 2f
            val arcSize = Size(size.width - strokeUnseen, size.height - strokeUnseen)
            val topLeft = Offset(inset, inset)
            val unseenBrush = Brush.sweepGradient(listOf(accent, accentAlt, accent))

            val n = seenFlags.size
            if (n <= 1) {
                // Zero or one card: unbroken circle, coloured by state.
                val seen = seenFlags.firstOrNull() ?: true
                if (seen) drawArc(
                    color = seenColor, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(strokeSeen)
                ) else drawArc(
                    brush = unseenBrush, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(strokeUnseen)
                )
            } else {
                // One arc per card, WhatsApp-style gaps, starting at 12 o'clock.
                val slice = 360f / n
                val gap = (slice * 0.18f).coerceIn(3f, 8f)
                val sweep = slice - gap
                seenFlags.forEachIndexed { i, seen ->
                    val start = -90f + i * slice + gap / 2f
                    if (seen) drawArc(
                        color = seenColor, startAngle = start, sweepAngle = sweep,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(strokeSeen, cap = StrokeCap.Round)
                    ) else drawArc(
                        brush = unseenBrush, startAngle = start, sweepAngle = sweep,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(strokeUnseen, cap = StrokeCap.Round)
                    )
                }
            }
        }
        // 3dp matches the pre-segmentation ring: stroke inner edge sits at 2.5dp, so the
        // avatar hugs the ring with only a hairline of breathing room — no visible moat.
        Box(modifier = Modifier.fillMaxSize().padding(3.dp)) {
            content()
        }
    }
}
