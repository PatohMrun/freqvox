package com.freqvox.ui.player.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A26),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Audio Equalizer",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { equalizerManager.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF7C4DFF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Presets
            Text(
                "Presets",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(presets) { index, name ->
                    val isSelected = currentPreset == index.toShort()
                    FilterChip(
                        selected = isSelected,
                        onClick = { equalizerManager.usePreset(index.toShort()) },
                        label = { Text(name) },
                        enabled = isEnabled,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White.copy(alpha = 0.05f),
                            labelColor = Color.White.copy(alpha = 0.6f),
                            selectedContainerColor = Color(0xFF7C4DFF).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF7C4DFF)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = isEnabled,
                            selected = isSelected,
                            borderColor = Color.White.copy(alpha = 0.1f),
                            selectedBorderColor = Color(0xFF7C4DFF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bands
            Text(
                "Frequencies",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until equalizerManager.numBands) {
                    val freq = equalizerManager.centerFreqs[i]
                    val level = bandLevels[i] ?: 0
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .width(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Slider(
                                value = level.toFloat(),
                                onValueChange = { equalizerManager.setBandLevel(i, it.toInt()) },
                                valueRange = equalizerManager.minLevel.toFloat()..equalizerManager.maxLevel.toFloat(),
                                enabled = isEnabled,
                                modifier = Modifier
                                    .graphicsLayer {
                                        rotationZ = -90f
                                    }
                                    .width(180.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF7C4DFF),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.05f)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = formatFreq(freq),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun formatFreq(hz: Int): String {
    // getCenterFreq returns milliHertz
    val actualHz = hz / 1000
    return if (actualHz >= 1000) "${actualHz / 1000}k" else "${actualHz}Hz"
}
