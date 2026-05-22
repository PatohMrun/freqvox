package com.freqvox.ui.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * A ultra-minimal, high-aesthetic volume control to replace the bulky standard Slider.
 * Follows the "Frequency + Voice" synthwave aesthetic with a slim gradient line and subtle glow.
 */
@Composable
fun SleekVolumeControl(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    accentPrimary: Color = Color(0xFF7C4DFF),
    accentSecondary: Color = Color(0xFF4DFFB4), // Neon green as per spec
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeDown,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(14.dp)
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .padding(horizontal = 12.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onVolumeChange((offset.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        onVolumeChange((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val segments = 24
                val spacing = 3.dp.toPx()
                val segmentWidth = (w - (spacing * (segments - 1))) / segments
                val maxSegmentHeight = h * 0.6f
                val minSegmentHeight = 2.dp.toPx()
                val centerY = h / 2f
                
                for (i in 0 until segments) {
                    val fraction = i / (segments - 1).toFloat()
                    val isActive = fraction <= volume
                    
                    // Frequency bars get slightly taller towards the right
                    val segmentHeight = minSegmentHeight + (maxSegmentHeight - minSegmentHeight) * fraction
                    
                    val xPos = i * (segmentWidth + spacing)
                    
                    val color = if (isActive) {
                        // Lerp between primary and secondary accent based on position
                        androidx.compose.ui.graphics.lerp(accentPrimary, accentSecondary, fraction)
                    } else {
                        Color.White.copy(alpha = 0.08f)
                    }
                    
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(xPos, centerY - segmentHeight / 2),
                        size = Size(segmentWidth, segmentHeight),
                        cornerRadius = CornerRadius(segmentWidth / 2)
                    )
                }
            }
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(14.dp)
        )
    }
}
