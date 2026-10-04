package com.mixcasete.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val duration: Long = 0,
    val isFavorite: Boolean = false,
    val videoUrl: String? = null
)
