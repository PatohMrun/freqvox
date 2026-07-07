package com.freqvox.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.data.prefs.AccentPreferences
import com.freqvox.ui.theme.Fvox

/**
 * Custom accent colour with two independent effects: the swatch always recolours the UI
 * controls (Fvox.accent); "Pin background gradient" additionally freezes the backdrop to the
 * swatch — off, the gradient keeps flowing from each track's album art as usual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccentSheet(
    enabled: Boolean,
    selectedColor: Int,
    pinGradient: Boolean,
    onToggle: (Boolean) -> Unit,
    onSelectColor: (Int) -> Unit,
    onTogglePinGradient: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Fvox.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Accent color",
                color = Fvox.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            Text(
                text = "Colour the app's buttons, switches and highlights with a swatch of your choice. Pin the background gradient too, or leave it flowing from each track's album art.",
                color = Fvox.onSurface.copy(alpha = 0.4f),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggle(!enabled) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Custom accent",
                    color = Fvox.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Fvox.onAccent,
                        checkedTrackColor = Fvox.accent
                    )
                )
            }

            // Sub-option: only meaningful while the custom accent is on.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = enabled) { onTogglePinGradient(!pinGradient) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val alpha = if (enabled) 1f else 0.4f
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Pin background gradient",
                        color = Fvox.onSurface.copy(alpha = alpha),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Off: the backdrop keeps following each track's album art.",
                        color = Fvox.onSurface.copy(alpha = alpha * 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, end = 8.dp)
                    )
                }
                Switch(
                    checked = enabled && pinGradient,
                    onCheckedChange = onTogglePinGradient,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Fvox.onAccent,
                        checkedTrackColor = Fvox.accent
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            val swatchAlpha = if (enabled) 1f else 0.35f
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentPreferences.SWATCHES.chunked(8).forEach { rowSwatches ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowSwatches.forEach { argb ->
                            val selected = enabled && argb == selectedColor
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(Color(argb).copy(alpha = swatchAlpha))
                                    .then(
                                        if (selected) Modifier.border(2.5.dp, Fvox.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable {
                                        onSelectColor(argb)
                                        if (!enabled) onToggle(true)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        // Pad the last row so partial rows keep equal swatch sizes.
                        repeat(8 - rowSwatches.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
