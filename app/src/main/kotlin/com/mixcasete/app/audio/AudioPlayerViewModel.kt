package com.mixcasete.app.audio

import android.app.Application
import android.content.ComponentNamepackage com.mixcasete.app.audio

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mixcasete.app.data.AppDatabase
import com.mixcasete.app.data.Playlist
import com.mixcasete.app.data.PlaylistSongCrossRef
import com.mixcasete.app.data.PlaylistWithSongs
import com.mixcasete.app.data.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PlayState {
    STOPPED, PLAYING, PAUSED, EJECTED, ERROR
}

enum class RepeatMode {
    OFF, ONE, ALL
}

data class CassetteState(
    val title: String = "Mix Tape Vol. 1",
    val artist: String = "DJ Retro",
    val progress: Float = 0f
)

data class ErrorInfo(
    val message: String,
    val source: SourceType?
)

class AudioPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val sourceManager = SourceManager(application)
    private val searchManager = SearchManager(application)
    private val database = AppDatabase.getDatabase(application)
    private val songDao = database.songDao()
    private val playlistDao = database.playlistDao()
    
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    
    private val _playState = MutableStateFlow(PlayState.STOPPED)
    val playState: StateFlow<PlayState> = _playState.asStateFlow()

    private val _isLidOpen = MutableStateFlow(false)
    val isLidOpen: StateFlow<Boolean> = _isLidOpen.asStateFlow()

    private val _cassette = MutableStateFlow(CassetteState())
    val cassette: StateFlow<CassetteState> = _cassette.asStateFlow()

    private val _calibrationMode = MutableStateFlow(false)
    val calibrationMode: StateFlow<Boolean> = _calibrationMode.asStateFlow()

    private val _errorInfo = MutableStateFlow<ErrorInfo?>(null)
    val errorInfo: StateFlow<ErrorInfo?> = _errorInfo.asStateFlow()

    private val _debugLog = MutableStateFlow<List<String>>(emptyList())
    val debugLog: StateFlow<List<String>> = _debugLog.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _currentPlaylist = MutableStateFlow<List<Song>>(emptyList())
    val currentPlaylist: StateFlow<List<Song>> = _currentPlaylist.asStateFlow()

    private val _currentSongIndex = MutableStateFlow(0)
    val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _showSearchScreen = MutableStateFlow(false)
    val showSearchScreen: StateFlow<Boolean> = _showSearchScreen.asStateFlow()

    private val _showPlaylistScreen = MutableStateFlow(false)
    val showPlaylistScreen: StateFlow<Boolean> = _showPlaylistScreen.asStateFlow()

    val allPlaylists = playlistDao.getAllPlaylists().stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    val favoriteSongs = songDao.getFavoriteSongs().stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    private var currentSource: AudioSource? = null
    private var localUri: Uri? = null
    private var retryCount = 0
    private val maxRetries = 3

    init {
        initMediaController()
    }

    private fun initMediaController() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.let { 
                try {
                    it.get()
                } catch (e: Exception) {
                    null
                }
            }
            setupPlayerListener()
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        controller?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (controller?.isPlaying == true) {
                            _playState.value = PlayState.PLAYING
                            _errorInfo.value = null
                            retryCount = 0
                        } else {
                            _playState.value = PlayState.PAUSED
                        }
                        updateProgress()
                    }
                    Player.STATE_ENDED -> {
                        handleSongEnded()
                    }
                    Player.STATE_IDLE -> {
                        if (_playState.value != PlayState.STOPPED) {
                            handlePlaybackError("Playback ended unexpectedly")
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    _playState.value = PlayState.PLAYING
                    updateProgress()
                } else if (controller?.playbackState == Player.STATE_READY) {
                    _playState.value = PlayState.PAUSED
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                handlePlaybackError(error.message ?: "Unknown error")
            }
        })
    }

    private fun handleSongEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                controller?.seekTo(0)
                controller?.play()
            }
            RepeatMode.ALL -> {
                if (_currentSongIndex.value < _currentPlaylist.value.size - 1) {
                    playNextSong()
                } else {
                    _currentSongIndex.value = 0
                    playSongAt(0)
                }
            }
            RepeatMode.OFF -> {
                if (_currentSongIndex.value < _currentPlaylist.value.size - 1) {
                    playNextSong()
                } else {
                    _playState.value = PlayState.STOPPED
                }
            }
        }
    }

    private fun handlePlaybackError(message: String) {
        addLog("ERROR: $message (Source: ${currentSource?.type})")
        _errorInfo.value = ErrorInfo(message, currentSource?.type)
        
        if (retryCount < maxRetries) {
            retryCount++
            viewModelScope.launch {
                val backoffDelay = (retryCount * 2000).toLong()
                delay(backoffDelay)
                loadNextSource()
            }
        } else {
            _playState.value = PlayState.ERROR
            viewModelScope.launch {
                loadNextSource()
            }
        }
    }

    private fun addLog(message: String) {
        val timestamp = System.currentTimeMillis()
        _debugLog.value = (_debugLog.value + "[$timestamp] $message").takeLast(50)
    }

    fun togglePlayPause() {
        if (_isLidOpen.value) return
        val player = controller ?: return
        
        if (player.isPlaying) {
            player.pause()
            _playState.value = PlayState.PAUSED
        } else {
            if (player.playbackState == Player.STATE_IDLE || currentSource == null) {
                viewModelScope.launch {
                    loadNextSource()
                }
            } else {
                player.play()
                _playState.value = PlayState.PLAYING
            }
        }
    }

    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        _playState.value = PlayState.STOPPED
        _cassette.value = _cassette.value.copy(progress = 0f)
        currentSource = null
    }

    fun eject() {
        stop()
        _isLidOpen.value = !_isLidOpen.value
    }

    fun rewind() {
        val player = controller ?: return
        val newPosition = (player.currentPosition - 10000).coerceAtLeast(0)
        player.seekTo(newPosition)
    }

    fun fastForward() {
        val player = controller ?: return
        val duration = player.duration
        if (duration > 0) {
            val newPosition = (player.currentPosition + 10000).coerceAtMost(duration)
            player.seekTo(newPosition)
        }
    }

    fun toggleCalibration() {
        _calibrationMode.value = !_calibrationMode.value
    }

    fun setLocalUri(uri: Uri) {
        localUri = uri
    }

    fun search(query: String) {
        viewModelScope.launch {
            val results = searchManager.search(query)
            _searchResults.value = results
        }
    }

    fun playSearchResult(result: SearchResult) {
        viewModelScope.launch {
            val song = Song(
                url = result.url,
                title = result.title,
                artist = result.artist,
                thumbnailUrl = result.thumbnailUrl
            )
            val songId = songDao.insertSong(song)
            val savedSong = song.copy(id = songId)
            _currentPlaylist.value = listOf(savedSong)
            _currentSongIndex.value = 0
            playSongAt(0)
            _showSearchScreen.value = false
        }
    }

    fun playPlaylist(playlist: PlaylistWithSongs) {
        _currentPlaylist.value = playlist.songs
        _currentSongIndex.value = 0
        if (playlist.songs.isNotEmpty()) {
            playSongAt(0)
        }
        _showPlaylistScreen.value = false
    }

    fun playNextSong() {
        if (_currentPlaylist.value.isEmpty()) return
        
        val nextIndex = if (_isShuffleEnabled.value) {
            (0 until _currentPlaylist.value.size).random()
        } else {
            (_currentSongIndex.value + 1) % _currentPlaylist.value.size
        }
        
        _currentSongIndex.value = nextIndex
        playSongAt(nextIndex)
    }

    fun playPreviousSong() {
        if (_currentPlaylist.value.isEmpty()) return
        
        val prevIndex = if (_currentSongIndex.value > 0) {
            _currentSongIndex.value - 1
        } else {
            _currentPlaylist.value.size - 1
        }
        
        _currentSongIndex.value = prevIndex
        playSongAt(prevIndex)
    }

    private fun playSongAt(index: Int) {
        if (index < 0 || index >= _currentPlaylist.value.size) return
        
        val song = _currentPlaylist.value[index]
        val audioSource = AudioSource(
            url = song.url,
            type = SourceType.YOUTUBE,
            title = song.title,
            artist = song.artist
        )
        
        currentSource = audioSource
        _cassette.value = _cassette.value.copy(
            title = song.title,
            artist = song.artist,
            progress = 0f
        )
        
        val mediaItem = MediaItem.Builder()
            .setUri(song.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .build()
            )
            .build()
        
        controller?.setMediaItem(mediaItem)
        controller?.prepare()
        controller?.play()
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            songDao.updateFavorite(song.id, !song.isFavorite)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistDao.insertPlaylist(Playlist(name = name))
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlist)
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            val songId = if (song.id == 0L) songDao.insertSong(song) else song.id
            val savedSong = song.copy(id = songId)
            val currentCount = playlistDao.getPlaylistWithSongs(playlistId)
            playlistDao.insertPlaylistSongCrossRef(
                PlaylistSongCrossRef(
                    playlistId = playlistId,
                    songId = savedSong.id,
                    position = 0
                )
            )
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun toggleSearchScreen() {
        _showSearchScreen.value = !_showSearchScreen.value
    }

    fun togglePlaylistScreen() {
        _showPlaylistScreen.value = !_showPlaylistScreen.value
    }

    private suspend fun loadNextSource() {
        val nextSource = sourceManager.getNextSource(currentSource, localUri)
        
        if (nextSource != null) {
            currentSource = nextSource
            _cassette.value = _cassette.value.copy(
                title = nextSource.title,
                artist = nextSource.artist,
                progress = 0f
            )
            
            val mediaItem = MediaItem.Builder()
                .setUri(nextSource.url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(nextSource.title)
                        .setArtist(nextSource.artist)
                        .build()
                )
                .build()
            
            controller?.setMediaItem(mediaItem)
            controller?.prepare()
            controller?.play()
        } else {
            _playState.value = PlayState.ERROR
        }
    }

    private fun updateProgress() {
        viewModelScope.launch {
            while (_playState.value == PlayState.PLAYING) {
                val player = controller ?: break
                if (player.duration > 0) {
                    val progress = player.currentPosition.toFloat() / player.duration.toFloat()
                    _cassette.value = _cassette.value.copy(progress = progress)
                }
                delay(100)
            }
        }
    }

    override fun onCleared() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        super.onCleared()
    }
}
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PlayState {
    STOPPED, PLAYING, PAUSED, EJECTED, ERROR
}

data class CassetteState(
    val title: String = "Mix Tape Vol. 1",
    val artist: String = "DJ Retro",
    val progress: Float = 0f
)

data class ErrorInfo(
    val message: String,
    val source: SourceType?
)

class AudioPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val sourceManager = SourceManager(application)
    
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    
    private val _playState = MutableStateFlow(PlayState.STOPPED)
    val playState: StateFlow<PlayState> = _playState.asStateFlow()

    private val _isLidOpen = MutableStateFlow(false)
    val isLidOpen: StateFlow<Boolean> = _isLidOpen.asStateFlow()

    private val _cassette = MutableStateFlow(CassetteState())
    val cassette: StateFlow<CassetteState> = _cassette.asStateFlow()

    private val _calibrationMode = MutableStateFlow(false)
    val calibrationMode: StateFlow<Boolean> = _calibrationMode.asStateFlow()

    private val _errorInfo = MutableStateFlow<ErrorInfo?>(null)
    val errorInfo: StateFlow<ErrorInfo?> = _errorInfo.asStateFlow()

    private val _debugLog = MutableStateFlow<List<String>>(emptyList())
    val debugLog: StateFlow<List<String>> = _debugLog.asStateFlow()

    private var currentSource: AudioSource? = null
    private var localUri: Uri? = null
    private var retryCount = 0
    private val maxRetries = 3

    init {
        initMediaController()
    }

    private fun initMediaController() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.let { 
                try {
                    it.get()
                } catch (e: Exception) {
                    null
                }
            }
            setupPlayerListener()
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        controller?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (controller?.isPlaying == true) {
                            _playState.value = PlayState.PLAYING
                            _errorInfo.value = null
                            retryCount = 0
                            addLog("Playback started: ${currentSource?.title}")
                        } else {
                            _playState.value = PlayState.PAUSED
                        }
                        updateProgress()
                    }
                    Player.STATE_ENDED -> {
                        viewModelScope.launch {
                            loadNextSource()
                        }
                    }
                    Player.STATE_IDLE -> {
                        if (_playState.value != PlayState.STOPPED) {
                            handlePlaybackError("Playback ended unexpectedly")
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    _playState.value = PlayState.PLAYING
                    updateProgress()
                } else if (controller?.playbackState == Player.STATE_READY) {
                    _playState.value = PlayState.PAUSED
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                handlePlaybackError(error.message ?: "Unknown error")
            }
        })
    }

    private fun handlePlaybackError(message: String) {
        addLog("ERROR: $message (Source: ${currentSource?.type})")
        _errorInfo.value = ErrorInfo(message, currentSource?.type)
        
        if (retryCount < maxRetries) {
            retryCount++
            viewModelScope.launch {
                val backoffDelay = (retryCount * 2000).toLong()
                addLog("Retrying in ${backoffDelay}ms (attempt $retryCount/$maxRetries)")
                delay(backoffDelay)
                loadNextSource()
            }
        } else {
            _playState.value = PlayState.ERROR
            addLog("Max retries reached. Switching to next source.")
            viewModelScope.launch {
                loadNextSource()
            }
        }
    }

    private fun addLog(message: String) {
        val timestamp = System.currentTimeMillis()
        _debugLog.value = (_debugLog.value + "[$timestamp] $message").takeLast(50)
    }

    fun togglePlayPause() {
        if (_isLidOpen.value) return
        val player = controller ?: return
        
        if (player.isPlaying) {
            player.pause()
            _playState.value = PlayState.PAUSED
        } else {
            if (player.playbackState == Player.STATE_IDLE || currentSource == null) {
                viewModelScope.launch {
                    loadNextSource()
                }
            } else {
                player.play()
                _playState.value = PlayState.PLAYING
            }
        }
    }

    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        _playState.value = PlayState.STOPPED
        _cassette.value = _cassette.value.copy(progress = 0f)
        currentSource = null
        addLog("Stopped")
    }

    fun eject() {
        stop()
        _isLidOpen.value = !_isLidOpen.value
    }

    fun rewind() {
        val player = controller ?: return
        val newPosition = (player.currentPosition - 10000).coerceAtLeast(0)
        player.seekTo(newPosition)
        updateProgress()
    }

    fun fastForward() {
        val player = controller ?: return
        val duration = player.duration
        if (duration > 0) {
            val newPosition = (player.currentPosition + 10000).coerceAtMost(duration)
            player.seekTo(newPosition)
            updateProgress()
        }
    }

    fun toggleCalibration() {
        _calibrationMode.value = !_calibrationMode.value
    }

    fun setLocalUri(uri: Uri) {
        localUri = uri
        addLog("Local file set: $uri")
    }

    private suspend fun loadNextSource() {
        addLog("Loading next source...")
        val nextSource = sourceManager.getNextSource(currentSource, localUri)
        
        if (nextSource != null) {
            currentSource = nextSource
            _cassette.value = _cassette.value.copy(
                title = nextSource.title,
                artist = nextSource.artist,
                progress = 0f
            )
            
            val mediaItem = MediaItem.Builder()
                .setUri(nextSource.url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(nextSource.title)
                        .setArtist(nextSource.artist)
                        .build()
                )
                .build()
            
            controller?.setMediaItem(mediaItem)
            controller?.prepare()
            controller?.play()
            
            addLog("Loaded: ${nextSource.title} (${nextSource.type})")
        } else {
            addLog("No sources available")
            _playState.value = PlayState.ERROR
        }
    }

    private fun updateProgress() {
        viewModelScope.launch {
            while (_playState.value == PlayState.PLAYING) {
                val player = controller ?: break
                if (player.duration > 0) {
                    val progress = player.currentPosition.toFloat() / player.duration.toFloat()
                    _cassette.value = _cassette.value.copy(progress = progress)
                }
                delay(100)
            }
        }
    }

    override fun onCleared() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        super.onCleared()
    }
}
