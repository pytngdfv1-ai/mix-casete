package com.mixcasete.app.audio

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
import com.mixcasete.app.data.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PlayState { STOPPED, PLAYING, PAUSED, EJECTED, ERROR }
enum class RepeatMode { OFF, ONE, ALL }

data class CassetteState(
    val title: String = "Mix Tape Vol. 1",
    val artist: String = "DJ Retro",
    val progress: Float = 0f,
    val sourceLabel: String = ""
)
data class ErrorInfo(val message: String, val source: SourceType?)
data class YtCmd(val type: String, val sec: Float = 0f, val seq: Long = 0)

class AudioPlayerViewModel(application: Application) : AndroidViewModel(application) {

    companion object { const val REC_PLAYLIST = "Grabaciones" }

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
    private val _recMessage = MutableStateFlow<String?>(null)
    val recMessage: StateFlow<String?> = _recMessage.asStateFlow()
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()
    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    private val _currentPlaylist = MutableStateFlow<List<Song>>(emptyList())
    val currentPlaylist: StateFlow<List<Song>> = _currentPlaylist.asStateFlow()
    private val _currentSongIndex = MutableStateFlow(0)
    val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()
    private val _currentVideoUrl = MutableStateFlow<String?>(null)
    val currentVideoUrl: StateFlow<String?> = _currentVideoUrl.asStateFlow()
    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    private val _showSearchScreen = MutableStateFlow(false)
    val showSearchScreen: StateFlow<Boolean> = _showSearchScreen.asStateFlow()
    private val _showPlaylistScreen = MutableStateFlow(false)
    val showPlaylistScreen: StateFlow<Boolean> = _showPlaylistScreen.asStateFlow()
    private val _showLoginScreen = MutableStateFlow(false)
    val showLoginScreen: StateFlow<Boolean> = _showLoginScreen.asStateFlow()

    private val _ytCommand = MutableStateFlow<YtCmd?>(null)
    val ytCommand: StateFlow<YtCmd?> = _ytCommand.asStateFlow()
    private val _ytPosition = MutableStateFlow(0f)
    private val _ytDuration = MutableStateFlow(0f)
    private var ytSeq = 0L

    val allPlaylists = playlistDao.getAllPlaylists().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val favoriteSongs = songDao.getFavoriteSongs().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var currentSource: AudioSource? = null
    private var localUri: Uri? = null

    init { initMediaController() }

    private fun initMediaController() {
        val token = SessionToken(getApplication(), ComponentName(getApplication(), PlaybackService::class.java))
        controllerFuture = MediaController.Builder(getApplication(), token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.let { try { it.get() } catch (e: Exception) { null } }
            setupPlayerListener()
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        controller?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(s: Int) {
                if (isYoutubeCurrent()) return
                when (s) {
                    Player.STATE_READY -> { _playState.value = if (controller?.isPlaying == true) PlayState.PLAYING else PlayState.PAUSED; updateProgress() }
                    Player.STATE_ENDED -> handleSongEnded()
                    Player.STATE_IDLE -> if (_playState.value != PlayState.STOPPED) _playState.value = PlayState.STOPPED
                }
            }
            override fun onIsPlayingChanged(p: Boolean) {
                if (isYoutubeCurrent()) return
                _playState.value = if (p) PlayState.PLAYING else PlayState.PAUSED
                if (p) updateProgress()
            }
            override fun onPlayerError(e: androidx.media3.common.PlaybackException) {
                if (!isYoutubeCurrent()) _errorInfo.value = ErrorInfo(e.message ?: "Error", currentSource?.type)
            }
        })
    }

    // ---- Bridge del reproductor oficial ----
    fun reportYtState(s: Int) {
        when (s) {
            1 -> { _playState.value = PlayState.PLAYING; _errorInfo.value = null }
            2 -> _playState.value = PlayState.PAUSED
            0 -> handleSongEnded()
        }
    }
    fun reportYtTime(t: Float) {
        _ytPosition.value = t
        val d = _ytDuration.value
        if (d > 0) _cassette.value = _cassette.value.copy(progress = t / d)
    }
    fun reportYtDuration(d: Float) { _ytDuration.value = d }
    fun reportYtError(e: Int) { _errorInfo.value = ErrorInfo("YouTube player error $e", SourceType.YOUTUBE) }

    private fun yt(type: String, sec: Float = 0f) {
        ytSeq++
        _ytCommand.value = YtCmd(type, sec, ytSeq)
    }

    private fun isYoutubeCurrent() = extractVideoId(_currentVideoUrl.value) != null

    fun playExternalVideoUrl(url: String?) {
        val id = extractVideoId(url) ?: return
        _currentVideoUrl.value = url
        _cassette.value = _cassette.value.copy(progress = 0f)
        yt("load")
    }

    private fun handleSongEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> if (isYoutubeCurrent()) yt("load") else { controller?.seekTo(0); controller?.play() }
            RepeatMode.ALL -> when {
                _currentPlaylist.value.isEmpty() -> _playState.value = PlayState.STOPPED
                _currentSongIndex.value < _currentPlaylist.value.size - 1 -> playNextSong()
                else -> playSongAt(0)
            }
            RepeatMode.OFF -> if (_currentPlaylist.value.isNotEmpty() && _currentSongIndex.value < _currentPlaylist.value.size - 1) playNextSong()
            else _playState.value = PlayState.STOPPED
        }
    }

    private fun flash(m: String) { _recMessage.value = m; viewModelScope.launch { delay(2000); _recMessage.value = null } }

    fun togglePlayPause() {
        if (isYoutubeCurrent()) {
            if (_playState.value == PlayState.PLAYING) yt("pause") else yt("play")
            return
        }
        val p = controller ?: return
        if (p.isPlaying) { p.pause(); _playState.value = PlayState.PAUSED }
        else {
            if (p.playbackState == Player.STATE_IDLE || currentSource == null) viewModelScope.launch { loadNextSource() }
            else { p.play(); _playState.value = PlayState.PLAYING }
        }
    }

    fun pauseForTv() { if (isYoutubeCurrent()) yt("pause") else controller?.pause(); _playState.value = PlayState.PAUSED }

    fun stop() {
        if (isYoutubeCurrent()) { yt("pause"); yt("seek", 0f) } else { controller?.stop(); controller?.clearMediaItems() }
        _playState.value = PlayState.STOPPED
        _cassette.value = _cassette.value.copy(progress = 0f)
        currentSource = null
    }

    fun recordCurrentTrack() {
        viewModelScope.launch {
            val song = _currentPlaylist.value.getOrNull(_currentSongIndex.value)
            if (song == null) { flash("Nada sonando para grabar"); return@launch }
            val existing = allPlaylists.value.firstOrNull { it.name == REC_PLAYLIST }
            val pid = existing?.id ?: playlistDao.insertPlaylist(Playlist(name = REC_PLAYLIST))
            addSongToPlaylist(pid, song)
            flash("● Grabado: ${song.title}")
        }
    }

    fun queueNextFromSearch(result: SearchResult) {
        val song = Song(url = "", title = result.title, artist = result.artist, thumbnailUrl = result.thumbnailUrl,
            videoUrl = "https://www.youtube.com/watch?v=${result.videoId}")
        val at = (_currentSongIndex.value + 1).coerceAtMost(_currentPlaylist.value.size)
        val list = _currentPlaylist.value.toMutableList(); list.add(at, song); _currentPlaylist.value = list
        flash("● '${result.title}' quedo como siguiente")
    }

    fun rewind() {
        if (isYoutubeCurrent()) { yt("seek", (_ytPosition.value - 10).coerceAtLeast(0f)); return }
        controller?.let { it.seekTo((it.currentPosition - 10000).coerceAtLeast(0)) }
    }
    fun fastForward() {
        if (isYoutubeCurrent()) { yt("seek", (_ytPosition.value + 10).coerceAtMost(_ytDuration.value)); return }
        controller?.let { val d = it.duration; if (d > 0) it.seekTo((it.currentPosition + 10000).coerceAtMost(d)) }
    }

    fun toggleCalibration() { _calibrationMode.value = !_calibrationMode.value }
    fun setLocalUri(uri: Uri) { localUri = uri }

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSearching.value = true; _searchError.value = null
            val o = searchManager.search(query)
            _searchResults.value = o.results; _searchError.value = o.error; _isSearching.value = false
        }
    }

    fun playSearchResult(result: SearchResult) {
        val song = Song(url = "", title = result.title, artist = result.artist, thumbnailUrl = result.thumbnailUrl,
            videoUrl = "https://www.youtube.com/watch?v=${result.videoId}")
        _currentPlaylist.value = listOf(song)
        _currentSongIndex.value = 0
        _currentVideoUrl.value = song.videoUrl
        _cassette.value = _cassette.value.copy(title = song.title, artist = song.artist, progress = 0f, sourceLabel = "YouTube")
        viewModelScope.launch { songDao.insertSong(song) }
        yt("load")
        _showSearchScreen.value = false
    }

    fun playPlaylistSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        _currentPlaylist.value = songs; _currentSongIndex.value = 0
        playSongAt(0); _showPlaylistScreen.value = false
    }

    fun playNextSong() {
        if (_currentPlaylist.value.isEmpty()) return
        playSongAt(if (_isShuffleEnabled.value) (0 until _currentPlaylist.value.size).random() else (_currentSongIndex.value + 1) % _currentPlaylist.value.size)
    }
    fun playPreviousSong() {
        if (_currentPlaylist.value.isEmpty()) return
        playSongAt(if (_currentSongIndex.value > 0) _currentSongIndex.value - 1 else _currentPlaylist.value.size - 1)
    }
    fun tvNext() = playNextSong()
    fun tvPrev() = playPreviousSong()

    private fun playSongAt(index: Int) {
        if (index < 0 || index >= _currentPlaylist.value.size) return
        val song = _currentPlaylist.value[index]
        _currentSongIndex.value = index
        _cassette.value = _cassette.value.copy(title = song.title, artist = song.artist, progress = 0f)

        val vid = extractVideoId(song.videoUrl)
        if (vid != null) {
            // Fuente YouTube -> reproductor oficial
            currentSource = null
            _currentVideoUrl.value = song.videoUrl
            yt("load")
        } else {
            // Fuente local -> ExoPlayer
            _currentVideoUrl.value = null
            currentSource = AudioSource(url = song.url, type = SourceType.LOCAL, title = song.title, artist = song.artist)
            controller?.setMediaItem(MediaItem.Builder().setUri(song.url).setMediaMetadata(MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).build()).build())
            controller?.prepare(); controller?.play()
        }
    }

    private fun extractVideoId(url: String?): String? {
        if (url == null) return null
        return when {
            url.contains("watch?v=") -> url.substringAfter("watch?v=").substringBefore("&")
            url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
            else -> null
        }
    }

    fun toggleShuffle() { _isShuffleEnabled.value = !_isShuffleEnabled.value }
    fun cycleRepeatMode() { _repeatMode.value = when (_repeatMode.value) { RepeatMode.OFF -> RepeatMode.ALL; RepeatMode.ALL -> RepeatMode.ONE; RepeatMode.ONE -> RepeatMode.OFF } }
    fun toggleFavorite(song: Song) { viewModelScope.launch { songDao.updateFavorite(song.id, !song.isFavorite) } }
    fun playlistSongs(pid: Long): Flow<List<Song>> = playlistDao.getSongsInPlaylist(pid)
    fun createPlaylist(name: String) { viewModelScope.launch { playlistDao.insertPlaylist(Playlist(name = name)) } }
    fun renamePlaylist(p: Playlist, n: String) { viewModelScope.launch { playlistDao.updatePlaylist(p.copy(name = n)) } }
    fun deletePlaylist(p: Playlist) { viewModelScope.launch { playlistDao.deletePlaylistSongs(p.id); playlistDao.deletePlaylist(p) } }
    fun addSongToPlaylist(pid: Long, song: Song) { viewModelScope.launch { val sid = if (song.id == 0L) songDao.insertSong(song) else song.id; playlistDao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(pid, sid, playlistDao.countSongs(pid))) } }
    fun removeSongFromPlaylist(pid: Long, sid: Long) { viewModelScope.launch { playlistDao.removeSongFromPlaylist(pid, sid) } }
    fun savePlaylistOrder(pid: Long, ids: List<Long>) { viewModelScope.launch { ids.forEachIndexed { i, s -> playlistDao.updatePosition(pid, s, i) } } }
    fun toggleSearchScreen() { _showSearchScreen.value = !_showSearchScreen.value }
    fun togglePlaylistScreen() { _showPlaylistScreen.value = !_showPlaylistScreen.value }
    fun toggleLoginScreen() { _showLoginScreen.value = !_showLoginScreen.value }

    private suspend fun loadNextSource() {
        val n = sourceManager.getNextSource(currentSource, localUri)
        if (n != null) {
            currentSource = n
            _cassette.value = _cassette.value.copy(title = n.title, artist = n.artist, progress = 0f)
            controller?.setMediaItem(MediaItem.Builder().setUri(n.url).setMediaMetadata(MediaMetadata.Builder().setTitle(n.title).setArtist(n.artist).build()).build())
            controller?.prepare(); controller?.play()
        } else _playState.value = PlayState.ERROR
    }

    private fun updateProgress() {
        viewModelScope.launch {
            while (_playState.value == PlayState.PLAYING && !isYoutubeCurrent()) {
                val p = controller ?: break
                if (p.duration > 0) _cassette.value = _cassette.value.copy(progress = p.currentPosition.toFloat() / p.duration)
                delay(100)
            }
        }
    }

    override fun onCleared() { controllerFuture?.let { MediaController.releaseFuture(it) }; super.onCleared() }
}
