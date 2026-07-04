package com.freqvox.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import com.freqvox.ui.theme.Fvox
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.R
import com.freqvox.data.local.entities.PlaylistEntity
import com.freqvox.data.local.entities.TagEntity
import com.freqvox.ui.main.MainViewModel
import com.freqvox.ui.tag.formatTagDisplayName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Wraps content in a Material navigation drawer that opens from the RIGHT edge.
 * ModalNavigationDrawer is start-anchored; flipping layout direction to RTL around
 * it (and back to LTR for the actual content) anchors it to the end/right instead.
 */
@Composable
fun RightNavigationDrawer(
    drawerState: DrawerState,
    gesturesEnabled: Boolean,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = gesturesEnabled,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    drawerContent()
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                content()
            }
        }
    }
}

@Composable
fun AppSidePanel(
    viewModel: MainViewModel,
    totalSongs: Int,
    totalDurationMs: Long,
    playlists: List<PlaylistEntity>,
    tags: List<TagEntity>,
    onOpenStats: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenCrossfade: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenArtwork: () -> Unit,
    onOpenAccent: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onRescan: () -> Unit,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    onTagClick: (TagEntity) -> Unit
) {
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.exportBackup(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.importBackup(uri) }

    val hours = totalDurationMs / (1000 * 60 * 60)
    val volumeNormalization by viewModel.volumeNormalization.collectAsStateWithLifecycle()

    ModalDrawerSheet(
        drawerContainerColor = Fvox.surface,
        modifier = Modifier.width(320.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp, bottom = 16.dp)
            ) {
            // ── Library header ──────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = Fvox.onSurface.copy(alpha = 0.1f)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.brand_icon),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Freqvox", color = Fvox.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "$totalSongs tracks • ${hours}h",
                        color = Fvox.onSurface.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }

            AccordionSection(viewModel, "INSIGHTS") {
                PanelRow(Icons.Default.QueryStats, "Listening stats", onOpenStats)
            }

            AccordionSection(viewModel, "AUDIO") {
                PanelRow(Icons.Default.Equalizer, "Equalizer", onOpenEqualizer)
                PanelRow(Icons.Default.CompareArrows, "Crossfade", onOpenCrossfade)
                PanelRow(Icons.Default.Bedtime, "Sleep timer", onOpenSleep)
                PanelToggleRow(
                    icon = Icons.Default.VolumeUp,
                    title = "Volume normalization",
                    subtitle = "Even out loudness between tracks",
                    checked = volumeNormalization,
                    onCheckedChange = { viewModel.setVolumeNormalization(it) }
                )
            }

            AccordionSection(viewModel, "APPEARANCE") {
                PanelRow(Icons.Default.Palette, "Accent color", onOpenAccent)
            }

            AccordionSection(viewModel, "LIBRARY") {
                PanelRow(Icons.Default.Refresh, "Rescan library", onRescan)
                PanelRow(Icons.Default.FolderOff, "Hidden folders", onOpenFolders)
                PanelRow(Icons.Default.Sell, "Manage tags", onOpenTags)
                PanelRow(Icons.Default.Image, "Album art", onOpenArtwork)
                PanelRow(Icons.Default.ContentCopy, "Find duplicates", onOpenDuplicates)
            }

            AccordionSection(viewModel, "BACKUP") {
                PanelRow(Icons.Default.Backup, "Back up library") {
                    val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                    exportLauncher.launch("freqvox-backup-$stamp.json")
                }
                PanelRow(Icons.Default.Restore, "Restore from backup") {
                    importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                }
            }

            // ── Quick jump: playlists ───────────────────────────────────────────
            if (playlists.isNotEmpty()) {
                AccordionSection(viewModel, "PLAYLISTS", initiallyExpanded = false) {
                    playlists.forEach { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPlaylistClick(playlist) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = Fvox.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                playlist.name,
                                color = Fvox.onSurface.copy(alpha = 0.85f),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ── Quick jump: tags ────────────────────────────────────────────────
            if (tags.isNotEmpty()) {
                AccordionSection(viewModel, "TAGS", initiallyExpanded = false) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            val color = Color(tag.color)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.18f))
                                    .border(1.dp, color.copy(alpha = 0.5f), CircleShape)
                                    .clickable { onTagClick(tag) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    formatTagDisplayName(tag.name),
                                    color = Fvox.onSurface.copy(alpha = 0.85f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            }
            
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val copyrightText = if (currentYear > 2026) {
                "© Copyright 2026 - $currentYear • Signiqe"
            } else {
                "© Copyright 2026 • Signiqe"
            }
            Text(
                text = copyrightText,
                color = Fvox.onSurface.copy(alpha = 0.2f),
                fontSize = 10.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 32.dp)
            )
        }
    }
}

/**
 * A foldable section rendered as a soft card so groups read as distinct panels rather than a
 * flat list: a faint tinted fill + a low-contrast "borderless" border (both derived from
 * onSurface, so they invert with the theme). Tappable header (label + rotating chevron) over a
 * vertically expanding/collapsing body. Expand/collapse state is persisted per title (via the
 * ViewModel), so it survives the drawer closing and app restarts.
 */
@Composable
private fun AccordionSection(
    viewModel: MainViewModel,
    title: String,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember {
        mutableStateOf(viewModel.isPanelSectionExpanded(title, initiallyExpanded))
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "accordionChevron"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(14.dp),
        color = Fvox.onSurface.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, Fvox.onSurface.copy(alpha = 0.07f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        expanded = !expanded
                        viewModel.setPanelSectionExpanded(title, expanded)
                    }
                    .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    color = Fvox.onSurface.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = Fvox.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(chevronRotation)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(content = content)
            }
        }
    }
}

@Composable
private fun PanelRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Fvox.onSurface.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, color = Fvox.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

/** A settings row with an inline Switch instead of a navigation tap. Tapping the row or the
 *  switch flips the value. Optional subtitle for a one-line explanation. */
@Composable
private fun PanelToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Fvox.onSurface.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Fvox.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Fvox.onAccent,
                checkedTrackColor = Fvox.accent
            )
        )
    }
}
