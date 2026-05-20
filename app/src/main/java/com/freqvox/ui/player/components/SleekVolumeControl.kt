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
                val trackHeight = 2.dp.toPx()
                val centerY = h / 2f
                
                // Inactive Track
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.08f),
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = Size(w, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2)
                )
                
                // Active Track with Gradient
                val activeWidth = w * volume
                if (activeWidth > 0) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(accentPrimary, accentSecondary)
                        ),
                        topLeft = Offset(0f, centerY - trackHeight / 2),
                        size = Size(activeWidth, trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2)
                    )
                    
                    // Subtle Glow "Thumb" - just a small circle at the end of the active track
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                    
                    // Extra faint outer glow
                    drawCircle(
                        color = accentSecondary.copy(alpha = 0.3f),
                        radius = 6.dp.toPx(),
                        center = Offset(activeWidth, centerY)
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
