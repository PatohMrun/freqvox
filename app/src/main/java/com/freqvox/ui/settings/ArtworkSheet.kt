package com.freqvox.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val accent = Color(0xFF7C4DFF)

/**
 * One-time disclosure shown on first launch after the feature ships. Online lookup is
 * blocked until this is acknowledged (Gate A), so a fresh install never reaches the
 * network before the user has read it.
 */
@Composable
fun ArtworkConsentDialog(
    onManageSettings: () -> Unit,
    onAcknowledge: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onAcknowledge,
        containerColor = Color(0xFF1A1A26),
        icon = { Icon(Icons.Default.Image, contentDescription = null, tint = accent) },
        title = { Text("Album art", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Freqvox can fill in missing album art by looking it up online. It's on by " +
                    "default and works over any connection, including mobile data. You can " +
                    "limit it to Wi-Fi — or turn it off — anytime under Album art.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text("Got it", color = accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onManageSettings) {
                Text("Settings", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtworkSheet(
    onlineEnabled: Boolean,
    allowCellular: Boolean,
    allowMeteredWifi: Boolean,
    onToggleOnline: (Boolean) -> Unit,
    onToggleCellular: (Boolean) -> Unit,
    onToggleMeteredWifi: (Boolean) -> Unit,
    onRetryFailed: () -> Unit,
    retrying: Boolean,
    onPhotoAccessGranted: () -> Unit,
    vidmateLinkedCount: Int,
    onLinkVidmate: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // SAF folder pick for the VidMate directory; take persistable read so we can re-read its
    // DB later, then hand the tree URI to the ViewModel to parse.
    val vidmateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onLinkVidmate(uri)
        }
    }
    val imagesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var photoGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, imagesPermission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        photoGranted = granted
        // Re-scan so sidecar images get picked up for tracks still missing art.
        if (granted) onPhotoAccessGranted()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12121A),
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
                text = "Album art",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            Text(
                text = "Freqvox shows embedded covers and a generated cover when none exists. " +
                    "It can also fetch missing art online.",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 16.dp)
            )

            SectionLabel("ONLINE LOOKUP")
            ToggleRow(
                icon = Icons.Default.CloudDownload,
                title = "Find missing covers online",
                subtitle = "Look up album art from the internet",
                checked = onlineEnabled,
                onCheckedChange = onToggleOnline
            )

            // Per-transport controls only matter while online lookup is enabled.
            if (onlineEnabled) {
                ToggleRow(
                    icon = Icons.Default.CloudDownload,
                    title = "Use mobile data",
                    subtitle = "Fetch art over a cellular connection",
                    checked = allowCellular,
                    onCheckedChange = onToggleCellular,
                    indent = true
                )
                ToggleRow(
                    icon = Icons.Default.CloudDownload,
                    title = "Use metered Wi-Fi",
                    subtitle = "Fetch art on Wi-Fi the system marks as metered",
                    checked = allowMeteredWifi,
                    onCheckedChange = onToggleMeteredWifi,
                    indent = true
                )
                ActionRow(
                    icon = Icons.Default.Refresh,
                    title = if (retrying) "Searching for covers…" else "Retry failed lookups",
                    subtitle = if (retrying) "Looking online for missing art"
                        else "Try again for covers that weren't found before",
                    enabled = !retrying,
                    loading = retrying,
                    onClick = onRetryFailed
                )
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("LOCAL COVERS")
            ActionRow(
                icon = Icons.Default.Image,
                title = if (photoGranted) "Folder cover images enabled" else "Use folder cover images",
                subtitle = if (photoGranted) {
                    "Reads cover.jpg / folder.jpg next to your tracks"
                } else {
                    "Allow photo access to read cover.jpg / folder.jpg files"
                },
                enabled = !photoGranted,
                onClick = { permissionLauncher.launch(imagesPermission) }
            )

            Spacer(Modifier.height(16.dp))
            SectionLabel("VIDMATE")
            ActionRow(
                icon = Icons.Default.Folder,
                title = if (vidmateLinkedCount > 0) {
                    "VidMate linked · $vidmateLinkedCount songs"
                } else {
                    "Link VidMate folder"
                },
                subtitle = "Use the exact thumbnail each song was downloaded with",
                onClick = { vidmateLauncher.launch(null) }
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = accent.copy(alpha = 0.8f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    indent: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .clickable { onCheckedChange(!checked) }
            .padding(12.dp)
            .padding(start = if (indent) 12.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = accent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) accent else Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
        }
    }
}
