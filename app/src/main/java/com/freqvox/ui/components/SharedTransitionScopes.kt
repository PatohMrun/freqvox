package com.freqvox.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
private val DefaultMorphBounds: BoundsTransform = BoundsTransform { _, _ ->
    tween(durationMillis = 420, easing = FastOutSlowInEasing)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun sharedBoundsModifier(key: String): Modifier {
    val sts = LocalSharedTransitionScope.current ?: return Modifier
    val avs = LocalAnimatedVisibilityScope.current ?: return Modifier
    return with(sts) {
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = avs,
            boundsTransform = DefaultMorphBounds
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun sharedElementModifier(key: String): Modifier {
    val sts = LocalSharedTransitionScope.current ?: return Modifier
    val avs = LocalAnimatedVisibilityScope.current ?: return Modifier
    return with(sts) {
        Modifier.sharedElement(
            state = rememberSharedContentState(key = key),
            animatedVisibilityScope = avs,
            boundsTransform = DefaultMorphBounds
        )
    }
}
