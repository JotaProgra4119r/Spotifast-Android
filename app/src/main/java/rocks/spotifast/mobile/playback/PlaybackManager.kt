package rocks.spotifast.mobile.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
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

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progressMs = MutableStateFlow(0L)
    val progressMs: StateFlow<Long> = _progressMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _connectedDevice = MutableStateFlow<Device?>(null)
    val connectedDevice: StateFlow<Device?> = _connectedDevice.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>("Este teléfono (Spotifast)")
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<Device>>(emptyList())
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
                            if (_currentTrack.value?.isLocal == true) {
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
                if (_isPlaying.value && _currentTrack.value?.isLocal == true) {
                    controller?.currentPosition?.let { pos ->
                        if (pos >= 0) _progressMs.value = pos
                    }
                }
                delay(500)
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
                if (SpotifastApp.instance.authManager.isLoggedIn.value && _currentTrack.value?.isLocal != true) {
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

                state.device?.let { dev ->
                    _connectedDevice.value = dev
                    _connectedDeviceName.value = dev.name
                }

                _isPlaying.value = state.isPlaying
                _progressMs.value = state.progressMs ?: 0L

                state.item?.let { track ->
                    _currentTrack.value = track
                    _durationMs.value = track.durationMs
                }

                _isShuffle.value = state.shuffleState ?: false
                _repeatMode.value = state.repeatState ?: "off"
            }

            // Refrescar lista de dispositivos Connect
            val devicesRes = SpotifyClient.apiService.getDevices()
            if (devicesRes.isSuccessful && devicesRes.body() != null) {
                val devs = devicesRes.body()!!.devices
                _availableDevices.value = devs
                val active = devs.firstOrNull { it.isActive }
                if (active != null) {
                    _connectedDevice.value = active
                    _connectedDeviceName.value = active.name
                }
            }
        } catch (_: Exception) {
        }
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

        if (isLocalAudio) {
            // Reproducir archivo local del dispositivo directamente con ExoPlayer
            _connectedDeviceName.value = "Este teléfono (Spotifast Local)"
            controller?.let { exo ->
                try {
                    val mediaItem = MediaItem.Builder()
                        .setUri(track.uri)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(track.name)
                                .setArtist(track.getArtistNames())
                                .setArtworkUri(Uri.parse(track.getCoverUrl() ?: ""))
                                .build()
                        )
                        .build()
                    exo.setMediaItem(mediaItem)
                    exo.prepare()
                    exo.play()
                } catch (e: Exception) {
                    Log.e("PlaybackManager", "Error reproduciendo pista local en ExoPlayer", e)
                }
            }
        } else {
            // Pista de Spotify: reproducir a través de Spotify Connect
            scope.launch(Dispatchers.IO) {
                val activeDeviceId = _connectedDevice.value?.id
                try {
                    val body = mapOf("uris" to listOf(track.uri))
                    val res = SpotifyClient.apiService.play(deviceId = activeDeviceId, body = body)
                    if (!res.isSuccessful) {
                        Log.w("PlaybackManager", "Spotify play respuesta: ${res.code()}")
                        withContext(Dispatchers.Main) {
                            if (res.code() == 404 || _connectedDevice.value == null) {
                                Toast.makeText(
                                    context,
                                    "No hay dispositivo Connect activo. Abriendo Spotify...",
                                    Toast.LENGTH_SHORT
                                ).show()
                                launchSpotifyIntent(track.uri)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PlaybackManager", "Error al enviar comando play a Spotify", e)
                    withContext(Dispatchers.Main) {
                        launchSpotifyIntent(track.uri)
                    }
                }
                syncSpotifyPlayerState()
            }
        }
    }

    private fun launchSpotifyIntent(uriString: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                // Fallback web
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://open.spotify.com/track/${uriString.substringAfterLast(":")}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (_: Exception) {}
        }
    }

    fun playPlaylistContext(contextUri: String, startingUri: String? = null) {
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
                if (!res.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        launchSpotifyIntent(contextUri)
                    }
                }
                syncSpotifyPlayerState()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    launchSpotifyIntent(contextUri)
                }
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
        if (_currentTrack.value?.isLocal != true) {
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
        if (_currentTrack.value?.isLocal == true) {
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
        if (_currentTrack.value?.isLocal != true) {
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
        if (_currentTrack.value?.isLocal != true) {
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
        if (_currentTrack.value?.isLocal == true) {
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
        val devId = device.id ?: return
        _connectedDevice.value = device
        _connectedDeviceName.value = device.name

        scope.launch(Dispatchers.IO) {
            try {
                val body = mapOf(
                    "device_ids" to listOf(devId),
                    "play" to _isPlaying.value
                )
                SpotifyClient.apiService.transferPlayback(body)
                delay(500)
                syncSpotifyPlayerState()
            } catch (e: Exception) {
                Log.e("PlaybackManager", "Error al transferir reproducción a dispositivo", e)
            }
        }
    }

    fun toggleShuffle() {
        val next = !_isShuffle.value
        _isShuffle.value = next
        if (_currentTrack.value?.isLocal != true) {
            scope.launch(Dispatchers.IO) {
                try {
                    SpotifyClient.apiService.setShuffle(state = next, deviceId = _connectedDevice.value?.id)
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
        if (_currentTrack.value?.isLocal != true) {
            scope.launch(Dispatchers.IO) {
                try {
                    SpotifyClient.apiService.setRepeat(state = next, deviceId = _connectedDevice.value?.id)
                } catch (_: Exception) {}
            }
        }
    }

    fun refreshDevices() {
        scope.launch(Dispatchers.IO) {
            try {
                val res = SpotifyClient.apiService.getDevices()
                if (res.isSuccessful && res.body() != null) {
                    val list = res.body()!!.devices
                    _availableDevices.value = list
                    val active = list.firstOrNull { it.isActive }
                    if (active != null) {
                        _connectedDevice.value = active
                        _connectedDeviceName.value = active.name
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
