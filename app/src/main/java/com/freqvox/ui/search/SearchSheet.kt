package com.freqvox.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import com.freqvox.ui.theme.Fvox
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.playback.PlaybackController
import com.freqvox.ui.components.TrackActionsDialogs
import com.freqvox.ui.components.rememberTrackActionsState
import com.freqvox.ui.library.LibraryTrackItem
import com.freqvox.ui.main.MainViewModel

/**
 * Standalone search overlay. Lives independently of the Search bottom-nav tab
 * so that tapping the magnifier on Home / Library / etc. doesn't move the user
 * off their current screen.
 *
 * Reuses MainViewModel's searchQuery / searchResults so results are consistent
 * with the Search tab, but maintains its OWN query local state and does NOT
 * mutate the ViewModel's searchQuery (so closing the sheet leaves the Search
 * tab in whatever state it was).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSheet(
    playbackController: PlaybackController,
    onDismiss: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    // Local query — keeps the search overlay independent from the Search tab's query.
    var localQuery by remember { mutableStateOf(TextFieldValue("")) }
    val currentTrackTitle by playbackController.currentTrackTitle.collectAsStateWithLifecycle()
    val isPlaying          by playbackController.isPlaying.collectAsStateWithLifecycle()
    val activeTracks       by viewModel.activeTracks.collectAsStateWithLifecycle()
    val taggedTrackIds     by viewModel.taggedTrackIds.collectAsStateWithLifecycle()
    val dislikedTrackIds   by viewModel.dislikedTrackIds.collectAsStateWithLifecycle()
    val allPlaylists       by viewModel.allPlaylists.collectAsStateWithLifecycle()

    val trackActions = rememberTrackActionsState()
    // Dialogs render at the sheet's composition root — Compose hoists
    // AlertDialogs into their own window so they appear ON TOP of the
    // ModalBottomSheet without being clipped.
    TrackActionsDialogs(state = trackActions, viewModel = viewModel, playlists = allPlaylists)

    // Filter locally so we don't disturb the ViewModel's persistent search state.
    // The Search tab has a Disliked chip that puts the sheet into a strict
    // tag-only mode (see MainViewModel.applyTagFilter); this overlay has no
    // chips at all, so we keep the keyword pipeline pure here — typing
    // "disliked" behaves like any other word and matches titles/artists,
    // letting a track legitimately named "Disliked Mix" still be found.
    val results: List<TrackEntity> = remember(localQuery.text, activeTracks, taggedTrackIds) {
        val q = localQuery.text
        if (q.isBlank()) {
            emptyList()
        } else {
            val normQ = q.replace(Regex("[^A-Za-z0-9]"), "").lowercase()
            val matchingTagIds = taggedTrackIds
                .filterKeys { it.replace(Regex("[^A-Za-z0-9]"), "").lowercase().contains(normQ) }
                .values.flatten().toSet()
            activeTracks.filter { t ->
                val nT = t.title.replace(Regex("[^A-Za-z0-9]"), "").lowercase()
                val nA = t.artist.replace(Regex("[^A-Za-z0-9]"), "").lowercase()
                val nAl = t.album.replace(Regex("[^A-Za-z0-9]"), "").lowercase()
                nT.contains(normQ) || nA.contains(normQ) || nAl.contains(normQ) || t.id in matchingTagIds
            }
        }
    }

    val focusRequester     = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Fvox.surface,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle       = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Search",
                color = Fvox.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp)
            )

            OutlinedTextField(
                value = localQuery,
                onValueChange = {
                    localQuery = it.copy(selection = TextRange(it.text.length).takeIf { _ -> it.selection == TextRange.Zero } ?: it.selection)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("Search your library", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (localQuery.text.isNotEmpty()) {
                        IconButton(onClick = { localQuery = TextFieldValue("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor    = Color(0xFF7C4DFF),
                    unfocusedBorderColor  = Color(0xFF1E1E2E),
                    focusedContainerColor   = Color(0xFF1E1E2E),
                    unfocusedContainerColor = Color(0xFF1E1E2E),
                    focusedTextColor      = Fvox.onSurface,
                    unfocusedTextColor    = Fvox.onSurface,
                    cursorColor           = Color(0xFF7C4DFF)
                ),
                shape = RoundedCornerShape(50),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                localQuery.text.isBlank() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp, max = 320.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Search, null,
                                tint = Fvox.onSurface.copy(alpha = 0.18f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Find a track, artist, or album", color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 14.sp)
                        }
                    }
                }
                results.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp, max = 320.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No results for \"${localQuery.text}\"", color = Color.Gray, fontSize = 14.sp)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(
                            results,
                            key = { _, t -> t.id },
                            contentType = { _, _ -> "track" }
                        ) { index, track ->
                            LibraryTrackItem(
                                track = track,
                                isCurrentTrack = track.title == currentTrackTitle,
                                isPlaying = isPlaying,
                                isDisliked = track.id in dislikedTrackIds,
                                onClick = {
                                    if (track.title == currentTrackTitle) {
                                        playbackController.togglePlayPause()
                                    } else {
                                        playbackController.playQueue(results, index)
                                    }
                                    onDismiss()
                                },
                                onAddToPlaylist = { trackActions.openAddToPlaylist(track) },
                                onRequestDelete = { trackActions.openDeleteFlow(track) },
                                onToggleDislike = { viewModel.toggleDislikeForTrack(track.id) },
                                onPlayNext = {
                                    playbackController.playTrackNext(track)
                                    viewModel.postSnackbar("Playing next")
                                },
                                onAddToQueue = {
                                    playbackController.addToQueue(track)
                                    viewModel.postSnackbar("Added to queue")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
