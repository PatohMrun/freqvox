package com.freqvox.ui.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A highly aesthetic synthwave like button with neon glow and "pop" physics.
 */
@Composable
fun AestheticLikeButton(
    isLiked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Physics-based scale for the "pop" effect
    var buttonPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (buttonPressed) 1.3f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "likePop",
        finishedListener = { buttonPressed = false }
    )

    // Smooth color transition
    val activeColor = Color(0xFFFF4D7C) // Terracotta pink from spec
    val inactiveColor = Color.White.copy(alpha = 0.3f)
    val heartColor by animateColorAsState(
        targetValue = if (isLiked) activeColor else inactiveColor,
        animationSpec = tween(300),
        label = "heartColor"
    )

    // Glow alpha animation
    val infiniteTransition = rememberInfiniteTransition(label = "glowPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    buttonPressed = true
                    onClick()
                }
            )
    ) {
        // Neon Glow behind heart
        if (isLiked) {
            Canvas(modifier = Modifier.size(40.dp)) {
                drawCircle(
                    color = activeColor.copy(alpha = glowAlpha * 0.3f),
                    radius = size.minDimension / 1.5f
                )
                drawCircle(
                    color = activeColor.copy(alpha = glowAlpha * 0.1f),
                    radius = size.minDimension
                )
            }
        }

        // The Heart Icon
        Icon(
            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Like",
            tint = heartColor,
            modifier = Modifier
                .size(28.dp)
                .scale(scale)
        )
    }
}
