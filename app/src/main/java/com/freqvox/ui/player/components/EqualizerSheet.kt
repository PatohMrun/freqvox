package com.freqvox.ui.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.playback.FreqvoxEqualizerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    equalizerManager: FreqvoxEqualizerManager,
    onDismiss: () -> Unit
) {
    val isEnabled by equalizerManager.isEnabled.collectAsState()
    val bandLevels by equalizerManager.bandLevels.collectAsState()
    val currentPreset by equalizerManager.currentPreset.collectAsState()
    val presets = remember { equalizerManager.getPresets() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161621),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Audio Equalizer",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isEnabled) "Active" else "Disabled",
                        color = if (isEnabled) Color(0xFF4DFFB4) else Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { equalizerManager.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF7C4DFF),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "PRESETS",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(presets) { index, name ->
                    val isSelected = currentPreset == index.toShort()
                    FilterChip(
                        selected = isSelected,
                        onClick = { equalizerManager.usePreset(index.toShort()) },
                        label = { Text(name, fontSize = 13.sp) },
                        enabled = isEnabled,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White.copy(alpha = 0.04f),
                            labelColor = Color.White.copy(alpha = 0.6f),
                            selectedContainerColor = Color(0xFF7C4DFF).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFF7C4DFF),
                            disabledContainerColor = Color.White.copy(alpha = 0.02f),
                            disabledLabelColor = Color.White.copy(alpha = 0.2f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = isEnabled,
                            selected = isSelected,
                            borderColor = Color.White.copy(alpha = 0.1f),
                            selectedBorderColor = Color(0xFF7C4DFF).copy(alpha = 0.5f),
                            disabledBorderColor = Color.White.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                "BANDS",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(vertical = 20.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 0 until equalizerManager.numBands) {
                    val freq = equalizerManager.centerFreqs[i]
                    val level = bandLevels[i] ?: 0

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                    ) {
                        Text(
                            text = "${level / 100}",
                            color = if (isEnabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.2f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        VerticalEqSlider(
                            value = level.toFloat(),
                            onValueChange = { equalizerManager.setBandLevel(i, it.toInt()) },
                            valueRange = equalizerManager.minLevel.toFloat()..equalizerManager.maxLevel.toFloat(),
                            enabled = isEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatFreq(freq),
                            color = if (isEnabled) Color(0xFF7C4DFF).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.2f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalEqSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var trackHeight by remember { mutableIntStateOf(0) }
    val activeColor = if (enabled) Color(0xFF7C4DFF) else Color(0xFF7C4DFF).copy(alpha = 0.2f)
    val inactiveColor = Color.White.copy(alpha = 0.08f)
    val thumbColor = if (enabled) Color.White else Color.White.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .onSizeChanged { trackHeight = it.height }
            .pointerInput(enabled, trackHeight, valueRange) {
                if (!enabled || trackHeight == 0) return@pointerInput
                awaitEachGesture {
                    val firstEvent = awaitPointerEvent()
                    val firstChange = firstEvent.changes.firstOrNull() ?: return@awaitEachGesture
                    if (!firstChange.pressed) return@awaitEachGesture

                    fun updateFromY(y: Float) {
                        val fraction = 1f - (y / trackHeight).coerceIn(0f, 1f)
                        onValueChange(
                            (valueRange.start + fraction * (valueRange.endInclusive - valueRange.start))
                                .coerceIn(valueRange.start, valueRange.endInclusive)
                        )
                    }

                    updateFromY(firstChange.position.y)
                    firstChange.consume()

                    var event = firstEvent
                    while (event.changes.any { it.pressed }) {
                        event = awaitPointerEvent()
                        event.changes.forEach { c ->
                            if (c.pressed) {
                                updateFromY(c.position.y)
                                c.consume()
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val thumbR = 5.dp.toPx()
            val padding = thumbR + 2.dp.toPx()
            val usableHeight = size.height - padding * 2

            val fraction = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val thumbY = padding + usableHeight * (1f - fraction)

            val zeroFraction = (-valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val zeroY = padding + usableHeight * (1f - zeroFraction)

            // Thin inactive track
            drawLine(
                color = inactiveColor,
                start = Offset(cx, padding),
                end = Offset(cx, size.height - padding),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Active segment between zero line and thumb
            val top = minOf(thumbY, zeroY)
            val bottom = maxOf(thumbY, zeroY)
            if (bottom > top) {
                drawLine(
                    color = activeColor,
                    start = Offset(cx, top),
                    end = Offset(cx, bottom),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Thumb dot
            drawCircle(
                color = thumbColor,
                radius = thumbR,
                center = Offset(cx, thumbY)
            )
        }
    }
}

private fun formatFreq(hz: Int): String {
    val actualHz = hz / 1000
    return when {
        actualHz >= 1000 -> "${actualHz / 1000}k"
        else -> "${actualHz}Hz"
    }
}
