package com.freqvox.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
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
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val dislikedTrackIds by viewModel.dislikedTrackIds.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val includeTagNames by viewModel.includeTagNames.collectAsStateWithLifecycle()
    val excludeTagNames by viewModel.excludeTagNames.collectAsStateWithLifecycle()
    val tagFilterResults by viewModel.tagFilterResults.collectAsStateWithLifecycle()

    val currentTrackTitle by playbackController.currentTrackTitle.collectAsStateWithLifecycle()
    val isPlaying by playbackController.isPlaying.collectAsStateWithLifecycle()

    val trackActions = rememberTrackActionsState()
    TrackActionsDialogs(state = trackActions, viewModel = viewModel, playlists = allPlaylists)
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Back peels ONE layer at a time, mirroring how the state was built up:
    // the system hides the IME first; then back clears the typed query; then it
    // clears any active tag filter; only with everything at rest does it pop to
    // Home. Previously filters were never cleared by back (it just popped),
    // while a query took an extra press — inconsistent in both directions.
    val tagFilterActive = includeTagNames.isNotEmpty() || excludeTagNames.isNotEmpty()
    BackHandler(enabled = searchQuery.isNotEmpty() || tagFilterActive) {
        if (searchQuery.isNotEmpty()) {
            viewModel.clearSearch()
            focusManager.clearFocus()
            keyboardController?.hide()
        } else {
            viewModel.clearTagFilter()
        }
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
            text = "Search",
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
            placeholder = { Text("What do you want to listen to?", color = Fvox.onSurface.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Fvox.onSurface.copy(alpha = 0.5f)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.clearSearch()
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Fvox.onSurface.copy(alpha = 0.5f))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Fvox.accent,
                unfocusedBorderColor = Fvox.surfaceVariant,
                focusedContainerColor = Fvox.surfaceVariant,
                unfocusedContainerColor = Fvox.surfaceVariant,
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
                // Boolean tag filter: tap a chip to require it, tap again to exclude it.
                BooleanTagFilterRow(
                    tags = allTags,
                    include = includeTagNames,
                    exclude = excludeTagNames,
                    onCycle = { viewModel.cycleTagFilterState(it) },
                    onClear = { viewModel.clearTagFilter() },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                if (!tagFilterActive) {
                    // No filter set → colorful browse grid. A card tap REQUIRES that tag —
                    // the same first step as tapping its chip above — so every tag touch on
                    // this page speaks one language (the tri-state require/exclude cycle)
                    // instead of grid taps secretly running a text search.
                    TagsGrid(tags = allTags, onTagClick = { viewModel.cycleTagFilterState(it) })
                } else if (tagFilterResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No songs match these tags", color = Fvox.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                        itemsIndexed(
                            tagFilterResults,
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
                                    if (track.title == currentTrackTitle) playbackController.togglePlayPause()
                                    else playbackController.playQueue(tagFilterResults, index)
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
        } else {
            if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results found for \"$searchQuery\"", color = Fvox.onSurface.copy(alpha = 0.5f))
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

/**
 * Tri-state boolean tag filter. Each chip cycles neutral → require (AND) → exclude (NOT) →
 * neutral. A track matches when it has ALL required tags and NONE of the excluded ones.
 * Required chips fill with the tag's own colour + a check; excluded chips go rose-outlined
 * with a block glyph. A summary line spells out the active expression.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BooleanTagFilterRow(
    tags: List<TagEntity>,
    include: Set<String>,
    exclude: Set<String>,
    onCycle: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                val name = tag.name.lowercase()
                val required = name in include
                val excluded = name in exclude
                val tagColor = Color(tag.color)
                val bg = when {
                    required -> tagColor
                    excluded -> Fvox.accentAlt.copy(alpha = 0.14f)
                    else -> Fvox.surfaceVariant
                }
                val labelColor = when {
                    required -> Color.White
                    excluded -> Fvox.accentAlt
                    else -> Fvox.onSurface.copy(alpha = 0.75f)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(bg)
                        .then(
                            if (excluded) Modifier.border(1.dp, Fvox.accentAlt, RoundedCornerShape(50))
                            else Modifier
                        )
                        .clickable { onCycle(tag.name) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (required) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                    } else if (excluded) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = Fvox.accentAlt, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = formatTagDisplayName(tag.name),
                        color = labelColor,
                        fontSize = 13.sp,
                        fontWeight = if (required || excluded) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        if (include.isNotEmpty() || exclude.isNotEmpty()) {
            val summary = buildString {
                if (include.isNotEmpty()) append(include.joinToString(" AND ") { formatTagDisplayName(it) })
                if (exclude.isNotEmpty()) {
                    if (isNotEmpty()) append(" · ")
                    append("not ")
                    append(exclude.joinToString(", ") { formatTagDisplayName(it) })
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary,
                    color = Fvox.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Clear",
                    color = Fvox.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClear() }.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        } else {
            Text(
                text = "Tap a tag to require it · tap again to exclude",
                color = Fvox.onSurface.copy(alpha = 0.4f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
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
            // White in both themes — the card is a saturated tag-color gradient, not a theme
            // surface, so onSurface (black in light) would wash out on it.
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )
    }
}
