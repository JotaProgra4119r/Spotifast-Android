package rocks.spotifast.mobile.data.api.models

import com.google.gson.annotations.SerializedName
import kotlin.math.abs

data class UserProfile(
    val id: String,
    @SerializedName("display_name") val displayName: String?,
    val email: String?,
    val product: String?, // "premium" o "free"
    val country: String?,
    val followers: Followers?,
    val images: List<SpotifyImage>?
) {
    fun getAvatarUrl(): String? = images?.firstOrNull()?.url
}

data class Followers(
    val total: Int
)

data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?
)

data class Artist(
    val id: String,
    val name: String,
    val uri: String?,
    val images: List<SpotifyImage>? = null
) {
    fun getImageUrl(): String? = images?.firstOrNull()?.url
}

data class Album(
    val id: String,
    val name: String,
    val uri: String?,
    val images: List<SpotifyImage>?,
    @SerializedName("release_date") val releaseDate: String?
) {
    fun getCoverUrl(preferredSize: Int = 300): String? {
        val list = images ?: return null
        if (list.isEmpty()) return null
        return list.minByOrNull { abs((it.width ?: 300) - preferredSize) }?.url ?: list.first().url
    }
}

data class Track(
    val id: String,
    val name: String,
    val uri: String,
    @SerializedName("duration_ms") val durationMs: Long,
    @SerializedName("preview_url") val previewUrl: String?,
    val artists: List<Artist>?,
    val album: Album?,
    @SerializedName("is_playable") val isPlayable: Boolean? = true,
    @SerializedName("is_local") val isLocal: Boolean = false,
    var isDownloaded: Boolean = false
) {
    fun getArtistNames(): String = artists?.joinToString(", ") { it.name } ?: "Desconocido"

    fun getCoverUrl(preferredSize: Int = 300): String? {
        val list = album?.images ?: return null
        if (list.isEmpty()) return null
        return list.minByOrNull { abs((it.width ?: 300) - preferredSize) }?.url ?: list.first().url
    }
}

data class SavedTrack(
    @SerializedName("added_at") val addedAt: String,
    val track: Track
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String?,
    val uri: String,
    val images: List<SpotifyImage>?,
    val owner: PlaylistOwner?,
    val tracks: PlaylistTracksInfo?
) {
    fun getCoverUrl(preferredSize: Int = 300): String? {
        val list = images ?: return null
        if (list.isEmpty()) return null
        return list.minByOrNull { abs((it.width ?: 300) - preferredSize) }?.url ?: list.first().url
    }
}

data class PlaylistOwner(
    val id: String,
    @SerializedName("display_name") val displayName: String?
)

data class PlaylistTracksInfo(
    val total: Int
)

data class PlaylistItem(
    @SerializedName("added_at") val addedAt: String?,
    val track: Track?
)

data class Paging<T>(
    val items: List<T>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val next: String?,
    val previous: String?
)

data class SearchResponse(
    val tracks: Paging<Track>?,
    val albums: Paging<Album>?,
    val artists: Paging<Artist>?,
    val playlists: Paging<Playlist>?
)

data class PlaybackState(
    val device: Device?,
    @SerializedName("is_playing") val isPlaying: Boolean,
    val item: Track?,
    @SerializedName("progress_ms") val progressMs: Long?,
    @SerializedName("shuffle_state") val shuffleState: Boolean? = false,
    @SerializedName("repeat_state") val repeatState: String? = "off"
)

data class Device(
    val id: String?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("is_restricted") val isRestricted: Boolean,
    val name: String,
    val type: String,
    @SerializedName("volume_percent") val volumePercent: Int?
)

data class DeviceListResponse(
    val devices: List<Device>
)

data class QueueResponse(
    @SerializedName("currently_playing") val currentlyPlaying: Track?,
    val queue: List<Track>?
)

data class RecentlyPlayedResponse(
    val items: List<PlayHistoryItem>?
)

data class PlayHistoryItem(
    val track: Track,
    @SerializedName("played_at") val playedAt: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("refresh_token") val refreshToken: String?,
    val scope: String?
)

data class RecommendationsResponse(
    val tracks: List<Track>?
)

data class SavedAlbum(
    @SerializedName("added_at") val addedAt: String?,
    val album: Album
)

data class FollowedArtistsResponse(
    val artists: Paging<Artist>?
)
