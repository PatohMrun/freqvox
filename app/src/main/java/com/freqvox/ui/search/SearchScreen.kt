package com.freqvox.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.playback.PlaybackController
import com.freqvox.ui.library.LibraryTrackItem
import com.freqvox.ui.main.MainViewModel

@Composable
fun SearchScreen(
    playbackController: PlaybackController,
    focusTrigger: Long = 0L,
    onFocusConsumed: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    
    val currentTrackTitle by playbackController.currentTrackTitle.collectAsState()
    val isPlaying by playbackController.isPlaying.collectAsState()
    
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

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
            color = Color.White,
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
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF7C4DFF),
                unfocusedBorderColor = Color(0xFF1E1E2E),
                focusedContainerColor = Color(0xFF1E1E2E),
                unfocusedContainerColor = Color(0xFF1E1E2E),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(50), // Pill shaped
            singleLine = true
        )

        if (searchQuery.isBlank()) {
            Text(
                text = "Browse by Tag",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
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
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No tags yet.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tag your songs to easily find them here.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                TagsGrid(tags = allTags, onTagClick = { viewModel.onSearchQueryChange(it) })
            }
        } else {
            if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results found for \"$searchQuery\"", color = Color.Gray)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                    itemsIndexed(searchResults) { index, track ->
                        LibraryTrackItem(
                            track = track,
                            isCurrentTrack = track.title == currentTrackTitle,
                            isPlaying = isPlaying,
                            onClick = {
                                if (track.title == currentTrackTitle) {
                                    playbackController.togglePlayPause()
                                } else {
                                    playbackController.playQueue(searchResults, index)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagsGrid(tags: List<com.freqvox.data.local.entities.TagEntity>, onTagClick: (String) -> Unit) {
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
                .background(Color.Black.copy(alpha = 0.2f))
        )
        
        Text(
            text = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() },
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )
    }
}
