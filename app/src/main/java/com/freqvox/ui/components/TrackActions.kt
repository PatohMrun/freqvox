package com.freqvox.ui.components

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.freqvox.data.local.entities.PlaylistEntity
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.data.repository.AddTrackResult
import com.freqvox.media.MediaTrackDeleter
import com.freqvox.ui.library.DeleteTrackStepOneDialog
import com.freqvox.ui.library.DeleteTrackStepTwoDialog
import com.freqvox.ui.library.SelectPlaylistDialog
import com.freqvox.ui.main.MainViewModel
import kotlinx.coroutines.launch

/**
 * Reusable holder for the per-row "more options" actions on a track —
 * Add to Playlist and the two-step Delete-from-device flow. Both the
 * Library Songs tab and every search surface (Search tab, Search sheet)
 * need these same dialogs + the MediaStore IntentSender launcher, so we
 * factor them out instead of duplicating ~40 lines per call site.
 *
 * Snackbar feedback is routed through [MainViewModel.postSnackbar] so the
 * single root SnackbarHost in MainScreen renders it consistently across
 * pages (search results in particular have no local host of their own).
 */
@Stable
class TrackActionsState internal constructor() {
    internal var trackToAdd by mutableStateOf<TrackEntity?>(null)
    internal var trackToDelete by mutableStateOf<TrackEntity?>(null)
    internal var deleteStep by mutableIntStateOf(0)

    fun openAddToPlaylist(track: TrackEntity) {
        trackToAdd = track
    }

    fun openDeleteFlow(track: TrackEntity) {
        trackToDelete = track
        deleteStep = 1
    }

    internal fun closeAddDialog() {
        trackToAdd = null
    }

    internal fun closeDeleteDialog() {
        trackToDelete = null
        deleteStep = 0
    }
}

@Composable
fun rememberTrackActionsState(): TrackActionsState =
    remember { TrackActionsState() }

/**
 * Renders the playlist-pick + two-step delete dialogs and hosts the
 * MediaStore delete IntentSender launcher. Call this anywhere inside the
 * composition that hosts [state]; the dialogs only appear when the state
 * has been opened via [TrackActionsState.openAddToPlaylist] /
 * [TrackActionsState.openDeleteFlow].
 */
@Composable
fun TrackActionsDialogs(
    state: TrackActionsState,
    viewModel: MainViewModel,
    playlists: List<PlaylistEntity>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingDeleteTrackId by rememberSaveable { mutableStateOf<Long?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDeleteTrackId?.let { id ->
                viewModel.completeTrackDeletion(id)
                viewModel.postSnackbar("Song deleted from device")
            }
        }
        pendingDeleteTrackId = null
    }

    state.trackToAdd?.let { track ->
        SelectPlaylistDialog(
            playlists = playlists.filter { it.type == "manual" },
            onSelect = { playlist ->
                scope.launch {
                    val result = viewModel.addTrackToPlaylist(playlist.id, track.id)
                    val message = when (result) {
                        AddTrackResult.Added              -> "Added to ${playlist.name}"
                        AddTrackResult.AlreadyInPlaylist  -> "Already in ${playlist.name}"
                    }
                    viewModel.postSnackbar(message)
                    state.closeAddDialog()
                }
            },
            onDismiss = { state.closeAddDialog() }
        )
    }

    state.trackToDelete?.let { track ->
        when (state.deleteStep) {
            1 -> DeleteTrackStepOneDialog(
                track = track,
                onContinue = { state.deleteStep = 2 },
                onDismiss = { state.closeDeleteDialog() }
            )
            2 -> DeleteTrackStepTwoDialog(
                track = track,
                onConfirmDelete = {
                    val trackId = track.id
                    pendingDeleteTrackId = trackId
                    state.closeDeleteDialog()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val request = MediaTrackDeleter.createDeleteRequest(context, trackId)
                        if (request != null) {
                            deleteLauncher.launch(request)
                        } else {
                            viewModel.postSnackbar("Could not delete file")
                            pendingDeleteTrackId = null
                        }
                    } else {
                        // Pre-Android 11 path runs synchronously through the resolver.
                        scope.launch {
                            if (MediaTrackDeleter.tryDeleteViaResolver(context, trackId)) {
                                viewModel.completeTrackDeletion(trackId)
                                viewModel.postSnackbar("Song deleted from device")
                            } else {
                                viewModel.postSnackbar(
                                    "Could not delete file — check storage permission"
                                )
                            }
                            pendingDeleteTrackId = null
                        }
                    }
                },
                onDismiss = { state.closeDeleteDialog() }
            )
        }
    }
}
