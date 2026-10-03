package com.mixcasete.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val imageCache = mutableMapOf<String, Bitmap>()

@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(url) { mutableStateOf(url?.let { imageCache[it] }) }

    LaunchedEffect(url) {
        if (url != null && bitmap.value == null) {
            val loaded = withContext(Dispatchers.IO) {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val input = connection.inputStream
                    val decoded = BitmapFactory.decodeStream(input)
                    input.close()
                    decoded
                } catch (e: Exception) {
                    null
                }
            }
            if (loaded != null) {
                imageCache[url] = loaded
                bitmap.value = loaded
            }
        }
    }

    Box(
        modifier = modifier.background(Color(0xFF2A2A2A)),
        contentAlignment = Alignment.Center
    ) {
        val current = bitmap.value
        if (current != null) {
            Image(
                bitmap = current.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color(0xFF777777)
            )
        }
    }
}
