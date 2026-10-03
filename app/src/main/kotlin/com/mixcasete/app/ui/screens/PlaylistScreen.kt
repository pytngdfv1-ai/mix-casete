package com.mixcasete.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mixcasete.app.audio.AudioPlayerViewModel
import com.mixcasete.app.data.Playlist
import com.mixcasete.app.data.Song
import kotlinx.coroutines.flow.flowOf

@Composable
fun PlaylistContent(viewModel: AudioPlayerViewModel) {
    val playlists by viewModel.allPlaylists.collectAsState()
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var renameText by remember { mutableStateOf("") }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1C1C1C))
                .padding(8.dp)
        ) {
            if (selectedPlaylist == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mis listas",
                        color = Color(0xFFF2F2F2),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Crear lista", tint = Color(0xFFD6D6D6))
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(playlists) { _, playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedPlaylist = playlist }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = playlist.name,
                                color = Color(0xFFF2F2F2),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                renameTarget = playlist
                                renameText = playlist.name
                            }) {
                                Text("Renombrar", fontSize = 11.sp)
                            }
                            IconButton(onClick = { viewModel.deletePlaylist(playlist) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Borrar", tint = Color(0xFFE57373))
                            }
                        }
                    }
                }
            } else {
                val playlist = selectedPlaylist!!
                val songs by viewModel.playlistSongs(playlist.id)
                    .collectAsState(initial = emptyList())
                var order by remember(playlist.id) { mutableStateOf<List<Song>>(emptyList()) }
                if (order.map { it.id } != songs.map { it.id } && !dragActive) {
                    order = songs
                }
                val rowHeightPx = with(LocalDensity.current) { 64.dp.toPx() }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedPlaylist = null }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color(0xFFD6D6D6))
                    }
                    Text(
                        text = playlist.name,
                        color = Color(0xFFF2F2F2),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.playPlaylistSongs(order) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir", tint = Color(0xFF81C784))
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(order, key = { _, song -> song.id }) { index, song ->
                        var dragOffset by remember { mutableStateOf(0f) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF242424))
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DragHandle,
                                contentDescription = "Arrastrar",
                                tint = Color(0xFF888888),
                                modifier = Modifier
                                    .size(28.dp)
                                    .pointerInput(order) {
                                        detectDragGesturesAfterLongPress(
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount.y
                                                if (dragOffset > rowHeightPx && index < order.size - 1) {
                                                    order = order.toMutableList().also {
                                                        it[index] = it[index + 1].also { v -> it[index + 1] = it[index] }
                                                    }
                                                    dragOffset = 0f
                                                } else if (dragOffset < -rowHeightPx && index > 0) {
                                                    order = order.toMutableList().also {
                                                        it[index] = it[index - 1].also { v -> it[index - 1] = it[index] }
                                                    }
                                                    dragOffset = 0f
                                                }
                                            },
                                            onDragEnd = {
                                                dragOffset = 0f
                                                dragActive = false
                                                viewModel.savePlaylistOrder(playlist.id, order.map { it.id })
                                            },
                                            onDragCancel = {
                                                dragOffset = 0f
                                                dragActive = false
                                            }
                                        )
                                    }
                            )
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                                Text(song.title, color = Color(0xFFF2F2F2), fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                                Text(song.artist, color = Color(0xFFAAAAAA), fontSize = 10.sp, maxLines = 1)
                            }
                            IconButton(onClick = {
                                if (index > 0) {
                                    order = order.toMutableList().also {
                                        val tmp = it[index - 1]
                                        it[index - 1] = it[index]
                                        it[index] = tmp
                                    }
                                    viewModel.savePlaylistOrder(playlist.id, order.map { it.id })
                                }
                            }) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir", tint = Color(0xFFD6D6D6))
                            }
                            IconButton(onClick = {
                                if (index < order.size - 1) {
                                    order = order.toMutableList().also {
                                        val tmp = it[index + 1]
                                        it[index + 1] = it[index]
                                        it[index] = tmp
                                    }
                                    viewModel.savePlaylistOrder(playlist.id, order.map { it.id })
                                }
                            }) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Bajar", tint = Color(0xFFD6D6D6))
                            }
                            IconButton(onClick = { viewModel.removeSongFromPlaylist(playlist.id, song.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Quitar", tint = Color(0xFFE57373))
                            }
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Nueva lista") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("Nombre de la lista") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.createPlaylist(newName.trim())
                            newName = ""
                        }
                        showCreateDialog = false
                    }) { Text("Crear") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") }
                }
            )
        }

        if (renameTarget != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { renameTarget = null },
                title = { Text("Renombrar lista") },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val target = renameTarget
                        if (target != null && renameText.isNotBlank()) {
                            viewModel.renamePlaylist(target, renameText.trim())
                        }
                        renameTarget = null
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { renameTarget = null }) { Text("Cancelar") }
                }
            )
        }
    }
}

private var dragActive: Boolean = false
