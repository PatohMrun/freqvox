package com.freqvox.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF0E0E15),
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
                    color = Color.White.copy(alpha = 0.1f)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.brand_icon),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Freqvox", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "$totalSongs tracks • ${hours}h",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }

            PanelSection("INSIGHTS")
            PanelRow(Icons.Default.QueryStats, "Listening stats", onOpenStats)

            PanelSection("AUDIO")
            PanelRow(Icons.Default.Equalizer, "Equalizer", onOpenEqualizer)
            PanelRow(Icons.Default.CompareArrows, "Crossfade", onOpenCrossfade)
            PanelRow(Icons.Default.Bedtime, "Sleep timer", onOpenSleep)

            PanelSection("LIBRARY")
            PanelRow(Icons.Default.Refresh, "Rescan library", onRescan)
            PanelRow(Icons.Default.FolderOff, "Hidden folders", onOpenFolders)
            PanelRow(Icons.Default.Sell, "Manage tags", onOpenTags)
            PanelRow(Icons.Default.Image, "Album art", onOpenArtwork)

            PanelSection("BACKUP")
            PanelRow(Icons.Default.Backup, "Back up library") {
                val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                exportLauncher.launch("freqvox-backup-$stamp.json")
            }
            PanelRow(Icons.Default.Restore, "Restore from backup") {
                importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            }

            // ── Quick jump: playlists ───────────────────────────────────────────
            if (playlists.isNotEmpty()) {
                PanelSection("PLAYLISTS")
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
                            tint = Color(0xFF7C4DFF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            playlist.name,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ── Quick jump: tags ────────────────────────────────────────────────
            if (tags.isNotEmpty()) {
                PanelSection("TAGS")
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
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
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
                color = Color.White.copy(alpha = 0.2f),
                fontSize = 10.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun PanelSection(title: String) {
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        title,
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
    )
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
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
