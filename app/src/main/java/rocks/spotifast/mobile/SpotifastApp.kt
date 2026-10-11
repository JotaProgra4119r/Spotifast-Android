package rocks.spotifast.mobile

import android.app.Application
import rocks.spotifast.mobile.data.auth.AuthManager
import rocks.spotifast.mobile.data.local.LocalMediaManager
import rocks.spotifast.mobile.data.local.OfflineCacheManager
import rocks.spotifast.mobile.playback.PlaybackManager

class SpotifastApp : Application() {

    companion object {
        lateinit var instance: SpotifastApp
            private set
    }

    lateinit var authManager: AuthManager
        private set

    lateinit var cacheManager: OfflineCacheManager
        private set

    lateinit var localMediaManager: LocalMediaManager
        private set

    lateinit var settingsManager: rocks.spotifast.mobile.data.local.SettingsManager
        private set

    lateinit var playbackManager: PlaybackManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        authManager = AuthManager(this)
        cacheManager = OfflineCacheManager(this)
        localMediaManager = LocalMediaManager(this)
        settingsManager = rocks.spotifast.mobile.data.local.SettingsManager(this)
        playbackManager = PlaybackManager(this)
    }
}
