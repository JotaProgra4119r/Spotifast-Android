package rocks.spotifast.mobile.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.api.SpotifyClient
import rocks.spotifast.mobile.data.api.models.Device
import rocks.spotifast.mobile.data.api.models.Track

class PlaybackManager(private val context: Context) {

    companion object {
        const val LOCAL_DEVICE_ID = "spotifast_local_player"
        val LOCAL_PHONE_DEVICE = Device(
            id = LOCAL_DEVICE_ID,
            isActive = true,
            isRestricted = false,
            name = "Este teléfono (Spotifast Player - Reproductor Interno)",
            type = "Smartphone",
            volumePercent = 100
        )
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progressMs = MutableStateFlow(0L)
    val progressMs: StateFlow<Long> = _progressMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _connectedDevice = MutableStateFlow<Device?>(LOCAL_PHONE_DEVICE)
    val connectedDevice: StateFlow<Device?> = _connectedDevice.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>("Este teléfono (Spotifast Player)")
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<Device>>(listOf(LOCAL_PHONE_DEVICE))
    val availableDevices: StateFlow<List<Device>> = _availableDevices.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow("off") // "off", "context", "track"
    val repeatMode: StateFlow<String> = _repeatMode.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private var pollingJob: Job? = null
    private var progressJob: Job? = null

    val isPlayingOnLocalPhone: Boolean
        get() = _connectedDevice.value == null || _connectedDevice.value?.id == LOCAL_DEVICE_ID

    init {
        initializeMediaController()
        startPlaybackSyncPolling()
        startLocalProgressTracker()
    }

    private fun initializeMediaController() {
        try {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture?.addListener({
                try {
                    controller = controllerFuture?.get()
                    controller?.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlayingOnLocalPhone) {
                                _isPlaying.value = isPlaying
                            }
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) {
                                val dur = controller?.duration?.coerceAtLeast(0L) ?: 0L
                                if (dur > 0) _durationMs.value = dur
                            } else if (playbackState == Player.STATE_ENDED) {
                                next()
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.e("PlaybackManager", "Error inicializando MediaController", e)
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            Log.e("PlaybackManager", "No se pudo iniciar servicio de audio", e)
        }
    }

    private fun startLocalProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                if (_isPlaying.value && isPlayingOnLocalPhone) {
                    controller?.currentPosition?.let { pos ->
                        if (pos >= 0) _progressMs.value = pos
                    }
                }
                delay(300)
            }
        }
    }

    /**
     * Sincronización continua de estado con Spotify Connect idéntico a Spotifast desktop
     */
    private fun startPlaybackSyncPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                if (SpotifastApp.instance.authManager.isLoggedIn.value) {
                    syncSpotifyPlayerState()
                }
                delay(2500)
            }
        }
    }

    suspend fun syncSpotifyPlayerState() = withContext(Dispatchers.IO) {
        try {
            val response = SpotifyClient.apiService.getPlaybackState()
            if (response.isSuccessful && response.body() != null) {
                val state = response.body()!!
                val dev = state.device

                // Si hay un dispositivo remoto activo reportado por Spotify Connect (Alexa, PC, altavoz, etc.)
                if (dev != null && dev.isActive && dev.id != LOCAL_DEVICE_ID) {
                    val isLocalExoPlaying = controller?.isPlaying == true

                    // Si el reproductor local no está sonando activamente, reflejar el dispositivo remoto
                    if (!isLocalExoPlaying) {
                        _connectedDevice.value = dev
                        _connectedDeviceName.value = dev.name
                        _isPlaying.value = state.isPlaying
                        _progressMs.value = state.progressMs ?: 0L

                        state.item?.let { track ->
                            _currentTrack.value = track
                            _durationMs.value = track.durationMs
                        }

                        _isShuffle.value = state.shuffleState ?: false
                        _repeatMode.value = state.repeatState ?: "off"
                    }
                }
            }

            // Refrescar lista de dispositivos Connect disponibles
            refreshDevicesInternal()
        } catch (e: Exception) {
            Log.d("PlaybackManager", "syncSpotifyPlayerState: ${e.message}")
        }
    }

    private suspend fun refreshDevicesInternal() {
        try {
            val devicesRes = SpotifyClient.apiService.getDevices()
            val remoteDevs = if (devicesRes.isSuccessful && devicesRes.body() != null) {
                devicesRes.body()!!.devices
            } else {
                emptyList()
            }

            val allDevs = mutableListOf(LOCAL_PHONE_DEVICE)
            allDevs.addAll(remoteDevs.filter { it.id != LOCAL_DEVICE_ID })
            _availableDevices.value = allDevs

            val activeRemote = remoteDevs.firstOrNull { it.isActive }
            val isLocalExoPlaying = controller?.isPlaying == true
            if (activeRemote != null && !isLocalExoPlaying && activeRemote.id != LOCAL_DEVICE_ID) {
                _connectedDevice.value = activeRemote
                _connectedDeviceName.value = activeRemote.name
            }
        } catch (_: Exception) {}
    }

    fun playTrack(track: Track, newQueue: List<Track> = emptyList()) {
        _currentTrack.value = track
        if (newQueue.isNotEmpty()) {
            _queue.value = newQueue
        }
        _durationMs.value = track.durationMs
        _progressMs.value = 0L
        _isPlaying.value = true

        val isLocalAudio = track.isLocal || track.uri.startsWith("content://") || track.uri.startsWith("file://")

        if (isLocalAudio || isPlayingOnLocalPhone) {
            // Reproducción 100% nativa en ExoPlayer dentro de Spotifast
            _connectedDevice.value = LOCAL_PHONE_DEVICE
            _connectedDeviceName.value = LOCAL_PHONE_DEVICE.name

            // Pausar cualquier dispositivo Spotify Connect remoto activo (como Alexa)
            // para que no sigan reproduciendo cosas distintas a la vez
            scope.launch(Dispatchers.IO) {
                try {
                    SpotifyClient.apiService.pause()
                } catch (_: Exception) {}
            }

            scope.launch(Dispatchers.Main) {
                playInLocalExoPlayer(track)
            }
        } else {
            // Reproducción en dispositivo externo de Spotify Connect (Alexa, PC, Altavoz, TV)
            controller?.pause()

            scope.launch(Dispatchers.IO) {
                val activeDeviceId = _connectedDevice.value?.id
                try {
                    val body = mapOf("uris" to listOf(track.uri))
                    val res = SpotifyClient.apiService.play(deviceId = activeDeviceId, body = body)
                    if (!res.isSuccessful) {
                        Log.w("PlaybackManager", "Spotify Connect remoto falló (${res.code()}), cambiando a reproductor local...")
                        withContext(Dispatchers.Main) {
                            _connectedDevice.value = LOCAL_PHONE_DEVICE
                            _connectedDeviceName.value = LOCAL_PHONE_DEVICE.name
                            playInLocalExoPlayer(track)
                        }
                    } else {
                        syncSpotifyPlayerState()
                    }
                } catch (e: Exception) {
                    Log.e("PlaybackManager", "Fallo al enviar a Connect remoto, reproduciendo en teléfono", e)
                    withContext(Dispatchers.Main) {
                        _connectedDevice.value = LOCAL_PHONE_DEVICE
                        _connectedDeviceName.value = LOCAL_PHONE_DEVICE.name
                        playInLocalExoPlayer(track)
                    }
                }
            }
        }
    }

    private suspend fun playInLocalExoPlayer(track: Track) {
        val exo = controller ?: run {
            Log.w("PlaybackManager", "MediaController no listo, reintentando...")
            delay(300)
            controller ?: return
        }

        try {
            val isLocalUri = track.isLocal || track.uri.startsWith("content://") || track.uri.startsWith("file://")
            val offlineUri = SpotifastApp.instance.cacheManager.getTrackAudioUri(track.id)

            val mediaUriString = if (!offlineUri.isNullOrEmpty()) {
                Log.i("PlaybackManager", "Reproduciendo desde caché offline de Spotifast: ${track.name}")
                offlineUri
            } else if (isLocalUri) {
                track.uri
            } else {
                AudioStreamResolver.resolveStreamUrl(
                    title = track.name,
                    artist = track.getArtistNames(),
                    previewUrl = track.previewUrl
                ) ?: track.previewUrl
            }

            if (mediaUriString.isNullOrEmpty()) {
                Log.w("PlaybackManager", "No se encontró fuente de audio para ${track.name}")
                Toast.makeText(context, "Buscando fuente de audio para ${track.name}...", Toast.LENGTH_SHORT).show()
                return
            }

            val mediaItem = MediaItem.Builder()
                .setUri(mediaUriString)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.name)
                        .setArtist(track.getArtistNames())
                        .setAlbumTitle(track.album?.name)
                        .setArtworkUri(Uri.parse(track.getCoverUrl() ?: ""))
                        .build()
                )
                .build()

            exo.setMediaItem(mediaItem)
            exo.prepare()
            exo.play()
            _isPlaying.value = true
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Error al reproducir pista en ExoPlayer", e)
        }
    }

    fun playPlaylistContext(contextUri: String, startingUri: String? = null, tracksFallback: List<Track> = emptyList()) {
        if (!isPlayingOnLocalPhone) {
            controller?.pause()

            scope.launch(Dispatchers.IO) {
                try {
                    val body = mutableMapOf<String, Any>("context_uri" to contextUri)
                    if (startingUri != null) {
                        body["offset"] = mapOf("uri" to startingUri)
                    }
                    val res = SpotifyClient.apiService.play(
                        deviceId = _connectedDevice.value?.id,
                        body = body
                    )
                    if (!res.isSuccessful && tracksFallback.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _connectedDevice.value = LOCAL_PHONE_DEVICE
                            _connectedDeviceName.value = LOCAL_PHONE_DEVICE.name
                            playTrack(tracksFallback.first(), tracksFallback)
                        }
                    } else {
                        syncSpotifyPlayerState()
                    }
                } catch (e: Exception) {
                    if (tracksFallback.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _connectedDevice.value = LOCAL_PHONE_DEVICE
                            _connectedDeviceName.value = LOCAL_PHONE_DEVICE.name
                            playTrack(tracksFallback.first(), tracksFallback)
                        }
                    }
                }
            }
        } else {
            // En modo local: pausar remoto si hubiera y reproducir directamente en la app
            scope.launch(Dispatchers.IO) {
                try { SpotifyClient.apiService.pause() } catch (_: Exception) {}
            }
            if (tracksFallback.isNotEmpty()) {
                val start = if (startingUri != null) {
                    tracksFallback.firstOrNull { it.uri == startingUri } ?: tracksFallback.first()
                } else {
                    tracksFallback.first()
                }
                playTrack(start, tracksFallback)
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        _isPlaying.value = false
        controller?.pause()
        if (!isPlayingOnLocalPhone) {
            scope.launch(Dispatchers.IO) {
                try {
                    SpotifyClient.apiService.pause(deviceId = _connectedDevice.value?.id)
                } catch (_: Exception) {}
                syncSpotifyPlayerState()
            }
        }
    }

    fun resume() {
        _isPlaying.value = true
        if (isPlayingOnLocalPhone) {
            controller?.play()
        } else {
            scope.launch(Dispatchers.IO) {
                try {
                    SpotifyClient.apiService.play(deviceId = _connectedDevice.value?.id)
                } catch (_: Exception) {}
                syncSpotifyPlayerState()
            }
        }
    }

    fun next() {
        val q = _queue.value
        val cur = _currentTrack.value
        if (q.isNotEmpty() && cur != null) {
            val idx = q.indexOfFirst { it.id == cur.id }
            if (idx != -1 && idx + 1 < q.size) {
                playTrack(q[idx + 1], q)
                return
            }
        }
        if (!isPlayingOnLocalPhone) {
            scope.launch(Dispatchers.IO) {
                try {
                    SpotifyClient.apiService.next(deviceId = _connectedDevice.value?.id)
                } catch (_: Exception) {}
                syncSpotifyPlayerState()
            }
        }
    }

    fun previous() {
        val q = _queue.value
        val cur = _currentTrack.value
        if (q.isNotEmpty() && cur != null) {
            val idx = q.indexOfFirst { it.id == cur.id }
            if (idx > 0) {
                playTrack(q[idx - 1], q)
                return
            }
        }
        if (!isPlayingOnLocalPhone) {
            scope.launch(Dispatchers.IO) {
                try {
                    SpotifyClient.apiService.previous(deviceId = _connectedDevice.value?.id)
                } catch (_: Exception) {}
                syncSpotifyPlayerState()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        _progressMs.value = positionMs
        if (isPlayingOnLocalPhone) {
            controller?.seekTo(positionMs)
        } else {
            scope.launch(Dispatchers.IO) {
                try {
                    SpotifyClient.apiService.seek(
                        positionMs = positionMs,
                        deviceId = _connectedDevice.value?.id
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun transferToDevice(device: Device) {
        if (device.id == LOCAL_DEVICE_ID) {
            _connectedDevice.value = LOCAL_PHONE_DEVICE
            _connectedDeviceName.value = LOCAL_PHONE_DEVICE.name

            // Pausar cualquier dispositivo Spotify Connect remoto activo (como Alexa)
            scope.launch(Dispatchers.IO) {
                try {
                    SpotifyClient.apiService.pause()
                } catch (_: Exception) {}
            }

            _currentTrack.value?.let { track ->
                scope.launch(Dispatchers.Main) {
                    playInLocalExoPlayer(track)
                }
            }
            return
        }

        val devId = device.id ?: return
        _connectedDevice.value = device
        _connectedDeviceName.value = device.name

        // Pausar ExoPlayer local para que no suenen dos audios a la vez
        controller?.pause()

        scope.launch(Dispatchers.IO) {
            try {
                val body = mapOf(
                    "device_ids" to listOf(devId),
                    "play" to _isPlaying.value
                )
                val res = SpotifyClient.apiService.transferPlayback(body)
                if (!res.isSuccessful) {
                    val curUri = _currentTrack.value?.uri
                    if (!curUri.isNullOrEmpty()) {
                        SpotifyClient.apiService.play(deviceId = devId, body = mapOf("uris" to listOf(curUri)))
                    }
                }
                delay(600)
                syncSpotifyPlayerState()
            } catch (e: Exception) {
                Log.e("PlaybackManager", "Error al transferir reproducción a dispositivo", e)
            }
        }
    }

    fun toggleShuffle() {
        setShuffle(!_isShuffle.value)
    }

    fun setShuffle(enabled: Boolean) {
        _isShuffle.value = enabled
        if (!isPlayingOnLocalPhone) {
            scope.launch(Dispatchers.IO) {
                try {
                    SpotifyClient.apiService.setShuffle(state = enabled, deviceId = _connectedDevice.value?.id)
                } catch (_: Exception) {}
            }
        }
    }

    fun cycleRepeat() {
        val next = when (_repeatMode.value) {
            "off" -> "context"
            "context" -> "track"
            else -> "off"
        }
        _repeatMode.value = next
        if (!isPlayingOnLocalPhone) {
            scope.launch(Dispatchers.IO) {
                try {
                    SpotifyClient.apiService.setRepeat(state = next, deviceId = _connectedDevice.value?.id)
                } catch (_: Exception) {}
            }
        }
    }

    fun refreshDevices() {
        scope.launch(Dispatchers.IO) {
            refreshDevicesInternal()
        }
    }
}
