package com.freqvox.ui.player

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.freqvox.playback.QueueItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import com.freqvox.ui.theme.Fvox
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.IntOffset
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.playback.FreqvoxEqualizerManager
import com.freqvox.ui.player.components.AestheticLikeButton
import com.freqvox.ui.player.components.DislikeButton
import com.freqvox.ui.player.components.EqualizerSheet
import com.freqvox.ui.player.components.FreqvoxVisualizer
import com.freqvox.ui.player.components.LyricsSheet
import com.freqvox.ui.player.components.SleekVolumeControl
import com.freqvox.ui.player.components.WaveformScrubber
import com.freqvox.ui.theme.dynamic.LocalGradientPrimary
import com.freqvox.ui.theme.dynamic.LocalGradientSecondary
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.freqvox.ui.player.components.QueueSheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    trackTitle: String,
    artist: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    artworkUri: String?,
    isLiked: Boolean,
    isDisliked: Boolean = false,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    sleepTimerActive: Boolean,
    sleepTimerTimeLeft: Long,
    stopAfterTrack: Boolean = false,
    onStopAfterTrack: () -> Unit = {},
    onBack: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onLikeClick: () -> Unit,
    onDislikeClick: () -> Unit = {},
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSleepTimer: (Int) -> Unit,
    onStopSleepTimer: () -> Unit,
    queue: List<QueueItem>,
    currentQueueIndex: Int,
    onQueueItemClick: (Int) -> Unit,
    onQueueItemRemove: (Int) -> Unit,
    onQueueItemMove: (Int, Int) -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit = {},
    onTagSwipeUp: () -> Unit,
    equalizerManager: FreqvoxEqualizerManager,
    currentTrack: TrackEntity?,
    playbackSpeed: Float = 1f,
    onSetSpeed: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interpolatedPos = com.freqvox.ui.components.rememberInterpolatedPositionMs(
        isPlaying = isPlaying,
        positionMs = currentPosition,
        durationMs = duration
    )
    val currentPosStr   = formatTime(currentPosition)
    val durationStr     = formatTime(duration)
    val accentPrimary   = LocalGradientPrimary.current
    val accentSecondary = LocalGradientSecondary.current

    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var volume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }

    // Observe system volume changes in real-time
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                volume = currentVol.toFloat() / maxVolume
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            observer
        )
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    var offsetX by remember { mutableStateOf(0f) }

    // ── Seek-bubble feedback (rapid tap pairs on artwork) ─────────────────
    val latestPosition by rememberUpdatedState(currentPosition)
    val latestDuration by rememberUpdatedState(duration)
    var seekBubbleKey by remember { mutableIntStateOf(0) }
    var seekBubbleIsForward by remember { mutableStateOf(true) }
    var showSeekBubble by remember { mutableStateOf(false) }
    var lastArtworkTapTime by remember { mutableLongStateOf(0L) }
    val seekBubbleAlpha by animateFloatAsState(
        targetValue = if (showSeekBubble) 1f else 0f,
        animationSpec = if (showSeekBubble) tween(80) else tween(450, easing = FastOutSlowInEasing),
        label = "seekBubble"
    )
    LaunchedEffect(seekBubbleKey) {
        if (seekBubbleKey > 0) {
            showSeekBubble = true
            delay(500)
            showSeekBubble = false
        }
    }

    // ── Swipe-down-to-dismiss ─────────────────────────────────────────────
    val density          = LocalDensity.current
    val screenHeightPx   = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val dismissCommitPx  by remember(density) { mutableFloatStateOf(with(density) { 80.dp.toPx() }) }
    val dismissMaxPx     by remember(density) { mutableFloatStateOf(with(density) { 360.dp.toPx() }) }
    var dismissOffsetY   by remember { mutableFloatStateOf(0f) }
    // While the slide-off animation is running, ignore further finger input so
    // we don't restart partway through and end up snapping between states.
    var isDismissing     by remember { mutableStateOf(false) }

    val dismissConnection = remember(dismissCommitPx, dismissMaxPx, screenHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isDismissing) return Offset.Zero
                if (available.y < 0f && dismissOffsetY > 0f && source == NestedScrollSource.UserInput) {
                    val take = kotlin.math.max(available.y, -dismissOffsetY)
                    dismissOffsetY += take
                    return Offset(0f, take)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (isDismissing) return Offset.Zero
                if (available.y > 0f && source == NestedScrollSource.UserInput) {
                    dismissOffsetY = (dismissOffsetY + available.y).coerceAtMost(dismissMaxPx)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (isDismissing) return Velocity.Zero
                val shouldDismiss = dismissOffsetY > dismissCommitPx || available.y > 800f
                if (shouldDismiss) {
                    // Slide DOWN off-screen while the parent fades us out in parallel.
                    // The old code animated dismissOffsetY back to 0 (i.e. UP), which
                    // is the wrong direction and is what made the dismiss "flicker".
                    isDismissing = true
                    onBack()
                    androidx.compose.animation.core.animate(
                        initialValue = dismissOffsetY,
                        targetValue  = screenHeightPx,
                        animationSpec = tween(durationMillis = 280, easing = FastOutLinearInEasing)
                    ) { value, _ -> dismissOffsetY = value }
                } else {
                    androidx.compose.animation.core.animate(
                        initialValue = dismissOffsetY,
                        targetValue  = 0f,
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                    ) { value, _ -> dismissOffsetY = value }
                }
                return Velocity.Zero
            }
        }
    }

    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.25f,
        targetValue  = 0.55f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    val playingMultiplier by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "glowFade"
    )
    val currentGlowAlpha = glowAlpha * playingMultiplier + 0.20f * (1f - playingMultiplier)

    // Bottom sheet visibility
    var showSleepSheet   by remember { mutableStateOf(false) }
    var showLyricsSheet  by remember { mutableStateOf(false) }
    var showQueueSheet   by remember { mutableStateOf(false) }
    var showEqSheet      by remember { mutableStateOf(false) }
    var showSpeedSheet   by remember { mutableStateOf(false) }
    var showMoreMenu     by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(com.freqvox.ui.components.sharedBoundsModifier("player.container"))
            .graphicsLayer {
                translationY = dismissOffsetY
                val progress = (dismissOffsetY / dismissMaxPx).coerceIn(0f, 1f)
                val scale = 1f - progress * 0.05f
                scaleX = scale
                scaleY = scale
            }
            // Subtle dynamic wash so the player isn't a flat slab (matches the gentle
            // album-tinted gradient the other pages carry). Opaque via compositeOver so it
            // still fully covers what's behind it.
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accentPrimary.copy(alpha = 0.18f).compositeOver(Fvox.surface),
                        accentSecondary.copy(alpha = 0.07f).compositeOver(Fvox.surface),
                        Fvox.surface
                    )
                )
            )
            .nestedScroll(dismissConnection)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Top Bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Back",
                    tint = Fvox.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NOW PLAYING",
                    color = Fvox.onSurface.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Fvox.onSurface.copy(alpha = 0.8f)
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    modifier = Modifier.background(Fvox.surfaceVariant)
                ) {
                    DropdownMenuItem(
                        text = { Text("Sleep timer", color = Fvox.onSurface.copy(alpha = 0.85f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null, tint = if (sleepTimerActive) Fvox.accent else Fvox.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) },
                        onClick = { showMoreMenu = false; showSleepSheet = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Tag this track", color = Fvox.onSurface.copy(alpha = 0.85f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, tint = Fvox.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) },
                        onClick = { showMoreMenu = false; onTagSwipeUp() }
                    )
                    DropdownMenuItem(
                        text = { Text("Playback speed", color = Fvox.onSurface.copy(alpha = 0.85f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = if (playbackSpeed != 1f) Fvox.accent else Fvox.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) },
                        trailingIcon = { if (playbackSpeed != 1f) Text("${formatSpeed(playbackSpeed)}×", color = Fvox.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        onClick = { showMoreMenu = false; showSpeedSheet = true }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.heightIn(min = 16.dp).weight(1f))

        // ── Artwork ───────────────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(duration) {
                    detectTapGestures(
                        onTap = { offset ->
                            val dur = latestDuration
                            if (dur <= 0) return@detectTapGestures
                            val now = System.currentTimeMillis()
                            val isForward = offset.x > size.width / 2f
                            if (now - lastArtworkTapTime <= 400L) {
                                val seekDelta = if (isForward) 10_000L else -10_000L
                                val newPos = (latestPosition + seekDelta).coerceIn(0L, dur)
                                onSeek(newPos.toFloat() / dur)
                                seekBubbleIsForward = isForward
                                seekBubbleKey++
                            }
                            lastArtworkTapTime = now
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentPrimary.copy(alpha = currentGlowAlpha),
                            accentSecondary.copy(alpha = currentGlowAlpha * 0.5f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta -> offsetX += delta },
                        onDragStopped = {
                            if (offsetX < -150f) onNextClick()
                            else if (offsetX > 150f) onPreviousClick()
                            offsetX = 0f
                        }
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(Fvox.surface.copy(alpha = 0.85f))
                    .border(
                        width = 1.dp,
                        color = Fvox.onSurface.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val artPath = currentTrack?.artworkPath
                if (artPath != null) {
                    coil.compose.AsyncImage(
                        model = java.io.File(artPath),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    FreqvoxVisualizer(
                        isPlaying = isPlaying,
                        trackTitle = trackTitle,
                        accentPrimary = accentPrimary,
                        accentSecondary = accentSecondary,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Double-tap seek bubble
                if (seekBubbleAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f)
                            .align(if (seekBubbleIsForward) Alignment.CenterEnd else Alignment.CenterStart)
                            .graphicsLayer { alpha = seekBubbleAlpha }
                            .background(Fvox.onSurface.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (seekBubbleIsForward) Icons.Default.Forward10 else Icons.Default.Replay10,
                                contentDescription = null,
                                tint = Fvox.onSurface,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (seekBubbleIsForward) "+10 sec" else "-10 sec",
                                color = Fvox.onSurface.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.heightIn(min = 16.dp).weight(1f))

        // ── Glass Card 1: Track Info + Waveform ───────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Fvox.onSurface.copy(alpha = 0.07f))
                .border(1.dp, Fvox.onSurface.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trackTitle,
                        color = Fvox.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = artist,
                        color = Fvox.onSurface.copy(alpha = 0.55f),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DislikeButton(
                        isDisliked = isDisliked,
                        onClick = onDislikeClick,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    AestheticLikeButton(
                        isLiked = isLiked,
                        onClick = onLikeClick,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            WaveformScrubber(
                progress        = { if (duration > 0) interpolatedPos.value.toFloat() / duration else 0f },
                durationStr     = durationStr,
                currentPosStr   = currentPosStr,
                onSeek          = onSeek,
                accentPrimary   = accentPrimary,
                accentSecondary = accentSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Glass Card 2: Controls + Volume + Extra ───────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Fvox.onSurface.copy(alpha = 0.06f))
                .border(1.dp, Fvox.onSurface.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Playback Controls ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = onShuffleClick, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) accentPrimary else Fvox.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Previous
                IconButton(onClick = onPreviousClick, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Fvox.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Seek back 10 s
                IconButton(
                    onClick = {
                        if (duration > 0) {
                            val newPos = (currentPosition - 10_000L).coerceAtLeast(0L)
                            onSeek(newPos.toFloat() / duration)
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Replay10,
                        contentDescription = "-10s",
                        tint = Fvox.onSurface.copy(alpha = 0.65f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Play / Pause
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(
                            elevation    = 20.dp,
                            shape        = CircleShape,
                            spotColor    = accentPrimary.copy(alpha = 0.9f),
                            ambientColor = accentSecondary.copy(alpha = 0.4f)
                        )
                        .clip(CircleShape)
                        .background(Brush.linearGradient(colors = listOf(accentPrimary, accentSecondary)))
                ) {
                    IconButton(onClick = onPlayPauseClick, modifier = Modifier.fillMaxSize()) {
                        androidx.compose.animation.AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                (androidx.compose.animation.scaleIn(animationSpec = tween(320, easing = FastOutSlowInEasing), initialScale = 0.6f) +
                                        androidx.compose.animation.fadeIn(tween(320))) togetherWith
                                        (androidx.compose.animation.scaleOut(animationSpec = tween(240, easing = FastOutSlowInEasing), targetScale = 0.6f) +
                                                androidx.compose.animation.fadeOut(tween(240)))
                            },
                            label = "playerPlayPause"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Fvox.onAccent,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // Seek forward 10 s
                IconButton(
                    onClick = {
                        if (duration > 0) {
                            val newPos = (currentPosition + 10_000L).coerceAtMost(duration)
                            onSeek(newPos.toFloat() / duration)
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Forward10,
                        contentDescription = "+10s",
                        tint = Fvox.onSurface.copy(alpha = 0.65f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Next
                IconButton(onClick = onNextClick, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Fvox.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Repeat — cycles Off → All → One
                IconButton(onClick = onRepeatClick, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != 0) accentPrimary else Fvox.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Fvox.onSurface.copy(alpha = 0.08f))

            // ── Volume Row ─────────────────────────────────────────────────────
            SleekVolumeControl(
                volume = volume,
                onVolumeChange = { v ->
                    volume = v
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        (v * maxVolume).roundToInt(),
                        0
                    )
                },
                accentPrimary = accentPrimary,
                accentSecondary = Color(0xFF4DFFB4) // Spec neon green
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Extra Controls ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExtraControlButton(
                    icon = Icons.Default.Lyrics,
                    label = "Lyrics",
                    onClick = { showLyricsSheet = true }
                )
                ExtraControlButton(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    label = "Queue",
                    onClick = { showQueueSheet = true }
                )
                ExtraControlButton(
                    icon = Icons.Default.Equalizer,
                    label = "EQ",
                    onClick = { showEqSheet = true }
                )
                ExtraControlButton(
                    icon = Icons.Default.Bedtime,
                    label = "Sleep",
                    isActive = sleepTimerActive || stopAfterTrack,
                    onClick = { showSleepSheet = true }
                )
                ExtraControlButton(
                    icon = Icons.Default.Label,
                    label = "Tag",
                    onClick = { onTagSwipeUp() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // ── Sleep Timer Sheet ──────────────────────────────────────────────────────
    if (showSleepSheet) {
        // Disable the sheet's own IME-aware inset so we can manage it ourselves
        // (ModalBottomSheet under edge-to-edge does NOT auto-resize for the keyboard).
        // Combined with .imePadding() + verticalScroll + bringIntoViewRequester below,
        // this makes the custom-minutes TextField pop above the keyboard on focus.
        ModalBottomSheet(
            onDismissRequest = { showSleepSheet = false },
            containerColor   = Fvox.surface,
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            val scrollState = rememberScrollState()
            val bringIntoView = remember { BringIntoViewRequester() }
            val coroutineScope = rememberCoroutineScope()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Sleep Timer",
                    color = Fvox.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (sleepTimerActive) {
                    Text(
                        text = "Stops in ${formatTime(sleepTimerTimeLeft)}",
                        color = Fvox.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else if (stopAfterTrack) {
                    Text(
                        text = "Stops at the end of this track",
                        color = Fvox.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                // End-of-track option — a count-down-less "finish this song, then stop".
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (stopAfterTrack) Fvox.accent.copy(alpha = 0.22f)
                            else Fvox.onSurface.copy(alpha = 0.07f)
                        )
                        .border(
                            1.dp,
                            if (stopAfterTrack) Fvox.accent.copy(alpha = 0.6f) else Fvox.onSurface.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onStopAfterTrack(); showSleepSheet = false }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (stopAfterTrack) Fvox.accent else Fvox.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "End of current track",
                            color = if (stopAfterTrack) Fvox.onSurface else Fvox.onSurface.copy(alpha = 0.85f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                val presets = listOf(5, 10, 15, 30, 45, 60)
                presets.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { minutes ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Fvox.onSurface.copy(alpha = 0.07f))
                                    .border(1.dp, Fvox.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { onSleepTimer(minutes); showSleepSheet = false }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$minutes",
                                        color = Fvox.onSurface,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "min",
                                        color = Fvox.onSurface.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Fvox.onSurface.copy(alpha = 0.07f)
                )

                Text(
                    "CUSTOM",
                    color = Fvox.onSurface.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                var customMinutes by remember { mutableStateOf("") }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { customMinutes = it.filter { c -> c.isDigit() }.take(3) },
                        placeholder = { Text("Minutes", color = Fvox.onSurface.copy(alpha = 0.3f), fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .bringIntoViewRequester(bringIntoView)
                            .onFocusChanged { state ->
                                if (state.isFocused) {
                                    coroutineScope.launch {
                                        // Small delay so the IME has started its animation
                                        // and imePadding has actually grown before we scroll.
                                        delay(220L)
                                        bringIntoView.bringIntoView()
                                    }
                                }
                            },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Fvox.onSurface,
                            unfocusedTextColor = Fvox.onSurface,
                            focusedBorderColor = Fvox.accent,
                            unfocusedBorderColor = Fvox.onSurface.copy(alpha = 0.15f),
                            cursorColor = Fvox.accent,
                            focusedContainerColor = Fvox.onSurface.copy(alpha = 0.04f),
                            unfocusedContainerColor = Fvox.onSurface.copy(alpha = 0.04f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    val customMins = customMinutes.toIntOrNull()
                    Button(
                        onClick = {
                            if (customMins != null && customMins > 0) {
                                onSleepTimer(customMins)
                                showSleepSheet = false
                            }
                        },
                        enabled = customMins != null && customMins > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Fvox.accent,
                            disabledContainerColor = Fvox.accent.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Text("Set", fontWeight = FontWeight.Bold, color = Fvox.onAccent)
                    }
                }

                if (sleepTimerActive || stopAfterTrack) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { onStopSleepTimer(); showSleepSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Timer", color = Fvox.accentAlt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // ── Lyrics Sheet ──────────────────────────────────────────────────────────
    if (showLyricsSheet) {
        LyricsSheet(
            track = currentTrack,
            currentPosition = currentPosition,
            onDismiss = { showLyricsSheet = false },
            onSeek = { ts -> if (duration > 0) onSeek(ts.toFloat() / duration) }
        )
    }

    // ── Queue Sheet ───────────────────────────────────────────────────────────
    if (showQueueSheet) {
        QueueSheet(
            queue              = queue,
            currentQueueIndex  = currentQueueIndex,
            accentColor        = accentPrimary,
            onQueueItemClick   = onQueueItemClick,
            onQueueItemRemove  = onQueueItemRemove,
            onQueueItemMove    = onQueueItemMove,
            onSaveAsPlaylist   = onSaveQueueAsPlaylist,
            onDismiss          = { showQueueSheet = false }
        )
    }

    // ── EQ Sheet ──────────────────────────────────────────────────────────────
    if (showEqSheet) {
        EqualizerSheet(
            equalizerManager = equalizerManager,
            onDismiss = { showEqSheet = false }
        )
    }

    // ── Playback Speed Sheet ────────────────────────────────────────────────────
    if (showSpeedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            containerColor = Fvox.surface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Playback Speed", color = Fvox.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
                        val selected = kotlin.math.abs(playbackSpeed - speed) < 0.01f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) Fvox.accent.copy(alpha = 0.25f)
                                    else Fvox.onSurface.copy(alpha = 0.06f)
                                )
                                .border(
                                    1.dp,
                                    if (selected) Fvox.accent.copy(alpha = 0.6f) else Fvox.onSurface.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSetSpeed(speed); showSpeedSheet = false }
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Text(
                                "${formatSpeed(speed)}×",
                                color = if (selected) Fvox.onSurface else Fvox.onSurface.copy(alpha = 0.6f),
                                fontSize = 15.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatSpeed(speed: Float): String =
    if (speed == speed.toLong().toFloat()) speed.toLong().toString()
    else speed.toString().trimEnd('0').trimEnd('.')

@Composable
private fun ExtraControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isActive) Fvox.accent.copy(alpha = 0.2f)
                    else Fvox.onSurface.copy(alpha = 0.07f)
                )
                .then(
                    if (isActive) Modifier.border(1.dp, Fvox.accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    else Modifier
                )
                .clickable(onClick = onClick)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = if (isActive) Fvox.accent else Fvox.onSurface.copy(alpha = 0.65f),
                modifier           = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text     = label,
            color    = Fvox.onSurface.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds  = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

@Composable
fun EqualizerAnimation(accentColor: Color = Color(0xFF4DFFB4)) {
    val transition = rememberInfiniteTransition(label = "eq")
    Row(
        modifier = Modifier.height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(7) { index ->
            val height by transition.animateFloat(
                initialValue = 0.15f,
                targetValue  = 1f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(350 + (index * 80), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(height)
                    .background(
                        Brush.verticalGradient(colors = listOf(Fvox.onSurface.copy(alpha = 0.9f), accentColor)),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
