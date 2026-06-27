package com.freqvox.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.freqvox.ui.theme.Fvox
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freqvox.data.local.entities.TagEntity
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.playback.PlaybackController
import com.freqvox.ui.components.TrackActionsDialogs
import com.freqvox.ui.components.rememberTrackActionsState
import com.freqvox.ui.library.LibraryTrackItem
import com.freqvox.ui.main.MainViewModel
import com.freqvox.ui.tag.formatTagDisplayName

@Composable
fun SearchScreen(
    playbackController: PlaybackController,
    focusTrigger: Long = 0L,
    onFocusConsumed: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchTagFilter by viewModel.searchTagFilter.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val dislikedTrackIds by viewModel.dislikedTrackIds.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    val currentTrackTitle by playbackController.currentTrackTitle.collectAsStateWithLifecycle()
    val isPlaying by playbackController.isPlaying.collectAsStateWithLifecycle()

    val trackActions = rememberTrackActionsState()
    TrackActionsDialogs(state = trackActions, viewModel = viewModel, playlists = allPlaylists)
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // After the IME hides on the first back press, the next back should clear
    // the active search — not pop the Search tab and land on Home.
    BackHandler(enabled = searchQuery.isNotEmpty()) {
        viewModel.clearSearch()
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // Keep a local TextFieldValue to control cursor position
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = searchQuery, selection = TextRange(searchQuery.length)))
    }

    // Sync external query changes (e.g. from Clear button or Tags) to the local TextFieldValue
    LaunchedEffect(searchQuery) {
        if (searchQuery != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = searchQuery,
                selection = TextRange(searchQuery.length)
            )
        }
    }

    LaunchedEffect(focusTrigger) {
        if (focusTrigger > 0L) {
            // Force cursor to the end when focused programmatically
            textFieldValue = textFieldValue.copy(selection = TextRange(textFieldValue.text.length))
            focusRequester.requestFocus()
            keyboardController?.show()
            onFocusConsumed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Browse",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Fvox.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { 
                textFieldValue = it
                viewModel.onSearchQueryChange(it.text) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .focusRequester(focusRequester),
            placeholder = { Text("What do you want to listen to?", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.clearSearch()
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF7C4DFF),
                unfocusedBorderColor = Color(0xFF1E1E2E),
                focusedContainerColor = Color(0xFF1E1E2E),
                unfocusedContainerColor = Color(0xFF1E1E2E),
                focusedTextColor = Fvox.onSurface,
                unfocusedTextColor = Fvox.onSurface
            ),
            shape = RoundedCornerShape(50), // Pill shaped
            singleLine = true
        )

        if (searchQuery.isBlank()) {
            Text(
                text = "Browse by Tag",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Fvox.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (allTags.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = null, 
                            tint = Fvox.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No tags yet.",
                            color = Fvox.onSurface.copy(alpha = 0.7f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tag your songs to easily find them here.",
                            color = Fvox.onSurface.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // applyTagFilter (not onSearchQueryChange) so the ViewModel
                // can tell this came from a chip tap vs. free typing. Only
                // matters for the Disliked chip today, but the distinction
                // is now available for any future strict-tag behaviour.
                TagsGrid(tags = allTags, onTagClick = { viewModel.applyTagFilter(it) })
            }
        } else {
            // Chips stay visible while results are shown so tapping Disliked (or
            // any tag) always re-applies chip mode — even after editing the bar.
            if (allTags.isNotEmpty()) {
                TagFilterRow(
                    tags = allTags,
                    selectedTagFilter = searchTagFilter,
                    onTagClick = { viewModel.applyTagFilter(it) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results found for \"$searchQuery\"", color = Color.Gray)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                    itemsIndexed(
                        searchResults,
                        key = { _, track -> track.id },
                        contentType = { _, _ -> "track" }
                    ) { index, track ->
                        LibraryTrackItem(
                            track = track,
                            isCurrentTrack = track.title == currentTrackTitle,
                            isPlaying = isPlaying,
                            isDisliked = track.id in dislikedTrackIds,
                            modifier = Modifier.animateItem(),
                            onClick = {
                                if (track.title == currentTrackTitle) {
                                    playbackController.togglePlayPause()
                                } else {
                                    playbackController.playQueue(searchResults, index)
                                }
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

@Composable
fun TagFilterRow(
    tags: List<TagEntity>,
    selectedTagFilter: String?,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            val isSelected = selectedTagFilter != null &&
                tag.name.equals(selectedTagFilter, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onTagClick(tag.name) },
                label = {
                    Text(
                        text = formatTagDisplayName(tag.name),
                        fontSize = 13.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(tag.color).copy(alpha = 0.35f),
                    selectedLabelColor = Fvox.onSurface,
                    containerColor = Color(0xFF1E1E2E),
                    labelColor = Fvox.onSurface.copy(alpha = 0.75f)
                )
            )
        }
    }
}

@Composable
fun TagsGrid(tags: List<TagEntity>, onTagClick: (String) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        val rows = tags.chunked(2)
        items(rows) { rowTags ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TagCard(rowTags[0].name, Color(rowTags[0].color), Modifier.weight(1f), onTagClick = { onTagClick(rowTags[0].name) })
                if (rowTags.size > 1) {
                    TagCard(rowTags[1].name, Color(rowTags[1].color), Modifier.weight(1f), onTagClick = { onTagClick(rowTags[1].name) })
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun TagCard(name: String, baseColor: Color, modifier: Modifier = Modifier, onTagClick: () -> Unit = {}) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            baseColor,
            baseColor.copy(alpha = 0.5f)
        )
    )

    Box(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .clickable { onTagClick() },
    ) {
        // Decorative overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Fvox.scrim.copy(alpha = 0.2f))
        )
        
        Text(
            text = formatTagDisplayName(name),
            color = Fvox.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )
    }
}
