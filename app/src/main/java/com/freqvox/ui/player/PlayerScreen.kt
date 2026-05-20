package com.freqvox.ui.player

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.ui.player.components.AestheticLikeButton
import com.freqvox.ui.player.components.FreqvoxVisualizer
import com.freqvox.ui.player.components.SleekVolumeControl
import com.freqvox.ui.player.components.WaveformScrubber
import com.freqvox.ui.theme.dynamic.LocalGradientPrimary
import com.freqvox.ui.theme.dynamic.LocalGradientSecondary
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    trackTitle: String,
    artist: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    artworkUri: String?,
    isLiked: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    sleepTimerActive: Boolean,
    sleepTimerTimeLeft: Long,
    onBack: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onLikeClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSleepTimer: (Int) -> Unit,
    onStopSleepTimer: () -> Unit,
    queue: List<QueueItem>,
    currentQueueIndex: Int,
    onQueueItemClick: (Int) -> Unit,
    onQueueItemRemove: (Int) -> Unit,
    onTagSwipeUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress        = if (duration > 0) currentPosition.toFloat() / duration else 0f
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
    var offsetY by remember { mutableStateOf(0f) }

    var likeScale by remember { mutableStateOf(1f) }
    val animatedLikeScale by animateFloatAsState(
        targetValue = likeScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "likeScale",
        finishedListener = { likeScale = 1f }
    )

    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.25f,
        targetValue  = 0.55f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    val currentGlowAlpha = if (isPlaying) glowAlpha else 0.20f

    // Bottom sheet visibility
    var showSleepSheet   by remember { mutableStateOf(false) }
    var showLyricsSheet  by remember { mutableStateOf(false) }
    var showQueueSheet   by remember { mutableStateOf(false) }
    var showEqSheet      by remember { mutableStateOf(false) }
    var showMoreMenu     by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
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
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NOW PLAYING",
                    color = Color.White.copy(alpha = 0.5f),
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
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    modifier = Modifier.background(Color(0xFF1E1E2E))
                ) {
                    DropdownMenuItem(
                        text = { Text("Share track", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMoreMenu = false
                            shareTrack(context, trackTitle, artist)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sleep timer", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null, tint = if (sleepTimerActive) Color(0xFF7C4DFF) else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) },
                        onClick = { showMoreMenu = false; showSleepSheet = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Tag this track", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) },
                        onClick = { showMoreMenu = false; onTagSwipeUp() }
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
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta -> offsetX += delta },
                        onDragStopped = {
                            if (offsetX < -150f) onNextClick()
                            else if (offsetX > 150f) onPreviousClick()
                            offsetX = 0f
                        }
                    )
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> offsetY += delta },
                        onDragStopped = {
                            if (offsetY < -120f) onTagSwipeUp()
                            offsetY = 0f
                        }
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1A1A26).copy(alpha = 0.85f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                FreqvoxVisualizer(
                    isPlaying = isPlaying,
                    trackTitle = trackTitle,
                    accentPrimary = accentPrimary,
                    accentSecondary = accentSecondary,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.heightIn(min = 16.dp).weight(1f))

        // ── Glass Card 1: Track Info + Waveform ───────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trackTitle,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = artist,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AestheticLikeButton(
                    isLiked = isLiked,
                    onClick = onLikeClick
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            WaveformScrubber(
                progress        = progress,
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
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Playback Controls ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = onShuffleClick) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) accentPrimary else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = onPreviousClick, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
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
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                IconButton(onClick = onNextClick, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat — cycles Off → All → One
                IconButton(onClick = onRepeatClick) {
                    Icon(
                        imageVector = if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != 0) accentPrimary else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.08f))

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
                    isActive = sleepTimerActive,
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
        ModalBottomSheet(
            onDismissRequest = { showSleepSheet = false },
            containerColor = Color(0xFF1A1A26)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Sleep Timer",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (sleepTimerActive) {
                    Text(
                        text = "Stops in ${formatTime(sleepTimerTimeLeft)}",
                        color = Color(0xFF7C4DFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

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
                                    .background(Color.White.copy(alpha = 0.07f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { onSleepTimer(minutes); showSleepSheet = false }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$minutes",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "min",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (sleepTimerActive) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { onStopSleepTimer(); showSleepSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Timer", color = Color(0xFFFF4D7C), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // ── Lyrics Sheet ──────────────────────────────────────────────────────────
    if (showLyricsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLyricsSheet = false },
            containerColor = Color(0xFF1A1A26)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Lyrics, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("No lyrics found", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Lyrics sync coming soon",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // ── Queue Sheet ───────────────────────────────────────────────────────────
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = Color(0xFF12121A)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Queue", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (queue.isNotEmpty()) {
                        Text(
                            "${queue.size} tracks",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (queue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Queue is empty", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                            Text(
                                "Play a track from your library",
                                color = Color.White.copy(alpha = 0.25f),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, bottom = 48.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(max = 520.dp)
                    ) {
                        itemsIndexed(queue, key = { i, item -> "${item.trackId}_$i" }) { index, item ->
                            val isCurrent = index == currentQueueIndex
                            val isPast = index < currentQueueIndex
                            val accent = Color(0xFF7C4DFF)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isCurrent -> accent.copy(alpha = 0.12f)
                                            else      -> Color.White.copy(alpha = 0.03f)
                                        }
                                    )
                                    .then(
                                        if (isCurrent) Modifier.border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        else Modifier
                                    )
                                    .clickable(enabled = !isCurrent) { onQueueItemClick(index) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Position / state indicator
                                Box(
                                    modifier = Modifier.width(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        isCurrent -> Icon(
                                            Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        isPast -> Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        else -> Text(
                                            "${index + 1}",
                                            color = Color.White.copy(alpha = 0.3f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = when {
                                            isCurrent -> accent
                                            isPast    -> Color.White.copy(alpha = 0.35f)
                                            else      -> Color.White.copy(alpha = 0.9f)
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.artist,
                                        color = if (isPast) Color.White.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.45f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Remove button — only for non-current items
                                if (!isCurrent) {
                                    IconButton(
                                        onClick = { onQueueItemRemove(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White.copy(alpha = 0.25f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── EQ Sheet ──────────────────────────────────────────────────────────────
    if (showEqSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEqSheet = false },
            containerColor = Color(0xFF1A1A26)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Equalizer, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Equalizer", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Per-band EQ coming soon",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

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
                    if (isActive) Color(0xFF7C4DFF).copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.07f)
                )
                .then(
                    if (isActive) Modifier.border(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    else Modifier
                )
                .clickable(onClick = onClick)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = if (isActive) Color(0xFF7C4DFF) else Color.White.copy(alpha = 0.65f),
                modifier           = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text     = label,
            color    = Color.White.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

private fun shareTrack(context: Context, title: String, artist: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "🎵 $title — $artist")
    }
    context.startActivity(Intent.createChooser(intent, "Share track"))
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
                        Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.9f), accentColor)),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
