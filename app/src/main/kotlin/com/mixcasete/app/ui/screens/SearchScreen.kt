package com.mixcasete.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mixcasete.app.audio.AudioPlayerViewModel
import com.mixcasete.app.audio.SearchResult
import com.mixcasete.app.ui.components.RemoteImage

private val searchQueryState = mutableStateOf("")

@Composable
fun SearchContent(viewModel: AudioPlayerViewModel) {
    var query by searchQueryState
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    var pendingQueue by remember { mutableStateOf<SearchResult?>(null) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1C1C1C))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar tema o artista...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search(query) })
                )
                IconButton(
                    onClick = { viewModel.search(query) },
                    modifier = Modifier.size(48.dp).background(Color(0xFF2E4A2E), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = Color(0xFF81C784))
                }
            }

            when {
                isSearching -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator(color = Color(0xFF81C784)) }
                }
                searchError != null -> {
                    Text(
                        text = "Error de búsqueda: ${searchError}",
                        color = Color(0xFFE57373),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                results.isEmpty() && query.isNotBlank() -> {
                    Text(
                        text = "Pulsa la lupa o Enter para buscar",
                        color = Color(0xFF888888),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(results) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .combinedClickable(
                                onClick = { viewModel.playSearchResult(result) },
                                onLongClick = { pendingQueue = result }
                            )
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RemoteImage(
                            url = result.thumbnailUrl,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                text = result.title,
                                color = Color(0xFFF2F2F2),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = result.artist,
                                color = Color(0xFFAAAAAA),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        pendingQueue?.let { r ->
            AlertDialog(
                onDismissRequest = { pendingQueue = null },
                title = { Text("Agregar a continuación") },
                text = { Text("¿Colocar '${r.title}' como SIGUIENTE tema a reproducir?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.queueNextFromSearch(r)
                        pendingQueue = null
                    }) { Text("Sí, siguiente") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingQueue = null }) { Text("Cancelar") }
                }
            )
        }
    }
}
