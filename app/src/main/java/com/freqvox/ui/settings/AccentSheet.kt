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
 * Picks a fixed accent for the background gradient, or leaves it album-derived (the default).
 * When "Fixed accent" is on, every screen's gradient is pinned to the chosen swatch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccentSheet(
    enabled: Boolean,
    selectedColor: Int,
    onToggle: (Boolean) -> Unit,
    onSelectColor: (Int) -> Unit,
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
                text = "By default the gradient is drawn from each track's album art. Pin it to one colour for a consistent look.",
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
                    "Fixed accent",
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

            Spacer(Modifier.height(12.dp))

            val swatchAlpha = if (enabled) 1f else 0.35f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccentPreferences.SWATCHES.forEach { argb ->
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
            }
        }
    }
}
