package com.freqvox.ui.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ThumbDownOffAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DislikeButton(
    isDisliked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    var buttonPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (buttonPressed) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dislikePop",
        finishedListener = { buttonPressed = false }
    )
    val activeColor = Color(0xFF78909C)
    val inactiveColor = Color.White.copy(alpha = 0.3f)
    val iconColor by animateColorAsState(
        targetValue = if (isDisliked) activeColor else inactiveColor,
        animationSpec = tween(300),
        label = "dislikeColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    buttonPressed = true
                    onClick()
                }
            )
    ) {
        Icon(
            imageVector = if (isDisliked) Icons.Default.ThumbDown else Icons.Outlined.ThumbDownOffAlt,
            contentDescription = if (isDisliked) "Remove dislike" else "Dislike",
            tint = iconColor,
            modifier = Modifier
                .size(26.dp)
                .scale(scale)
        )
    }
}
