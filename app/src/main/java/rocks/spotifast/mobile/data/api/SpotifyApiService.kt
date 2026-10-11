package rocks.spotifast.mobile.data.api

import retrofit2.Response
import retrofit2.http.*
import rocks.spotifast.mobile.data.api.models.*

interface SpotifyApiService {

    @GET("v1/me")
    suspend fun getMe(): Response<UserProfile>

    @GET("v1/me/tracks")
    suspend fun getSavedTracks(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<Paging<SavedTrack>>

    @GET("v1/me/playlists")
    suspend fun getUserPlaylists(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<Paging<Playlist>>

    @GET("v1/playlists/{playlist_id}")
    suspend fun getPlaylist(
        @Path("playlist_id") playlistId: String
    ): Response<Playlist>

    @GET("v1/playlists/{playlist_id}/items")
    suspend fun getPlaylistItems(
        @Path("playlist_id") playlistId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("additional_types") additionalTypes: String = "track,episode"
    ): Response<Paging<PlaylistItem>>

    @GET("v1/playlists/{playlist_id}/tracks")
    suspend fun getPlaylistTracksFallback(
        @Path("playlist_id") playlistId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<Paging<PlaylistItem>>

    @GET("v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): Response<SearchResponse>

    @GET("v1/me/player")
    suspend fun getPlaybackState(): Response<PlaybackState>

    @GET("v1/me/player/devices")
    suspend fun getDevices(): Response<DeviceListResponse>

    @PUT("v1/me/player/play")
    suspend fun play(
        @Query("device_id") deviceId: String? = null,
        @Body body: Map<String, Any>? = null
    ): Response<Unit>

    @PUT("v1/me/player/pause")
    suspend fun pause(
        @Query("device_id") deviceId: String? = null
    ): Response<Unit>

    @POST("v1/me/player/next")
    suspend fun next(
        @Query("device_id") deviceId: String? = null
    ): Response<Unit>

    @POST("v1/me/player/previous")
    suspend fun previous(
        @Query("device_id") deviceId: String? = null
    ): Response<Unit>

    @PUT("v1/me/player/seek")
    suspend fun seek(
        @Query("position_ms") positionMs: Long,
        @Query("device_id") deviceId: String? = null
    ): Response<Unit>

    @PUT("v1/me/player/volume")
    suspend fun setVolume(
        @Query("volume_percent") volumePercent: Int,
        @Query("device_id") deviceId: String? = null
    ): Response<Unit>

    @PUT("v1/me/player/shuffle")
    suspend fun setShuffle(
        @Query("state") state: Boolean,
        @Query("device_id") deviceId: String? = null
    ): Response<Unit>

    @PUT("v1/me/player/repeat")
    suspend fun setRepeat(
        @Query("state") state: String,
        @Query("device_id") deviceId: String? = null
    ): Response<Unit>

    @PUT("v1/me/player")
    suspend fun transferPlayback(
        @Body body: Map<String, Any>
    ): Response<Unit>

    @POST("v1/me/player/queue")
    suspend fun addToQueue(
        @Query("uri") uri: String,
        @Query("device_id") deviceId: String? = null
    ): Response<Unit>

    @GET("v1/me/player/queue")
    suspend fun getQueue(): Response<QueueResponse>

    @GET("v1/me/player/recently-played")
    suspend fun getRecentlyPlayed(
        @Query("limit") limit: Int = 20
    ): Response<RecentlyPlayedResponse>

    @PUT("v1/me/tracks")
    suspend fun saveTrack(
        @Query("ids") ids: String
    ): Response<Unit>

    @DELETE("v1/me/tracks")
    suspend fun removeTrack(
        @Query("ids") ids: String
    ): Response<Unit>

    @GET("v1/me/tracks/contains")
    suspend fun checkSavedTracks(
        @Query("ids") ids: String
    ): Response<List<Boolean>>
}

interface SpotifyAuthService {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun exchangeToken(
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("client_id") clientId: String,
        @Field("code_verifier") codeVerifier: String
    ): Response<TokenResponse>

    @FormUrlEncoded
    @POST("api/token")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String
    ): Response<TokenResponse>
}
