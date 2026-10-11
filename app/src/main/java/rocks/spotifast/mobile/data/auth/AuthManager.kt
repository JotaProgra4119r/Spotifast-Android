package rocks.spotifast.mobile.data.auth

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rocks.spotifast.mobile.data.api.SpotifyClient
import java.security.MessageDigest
import java.security.SecureRandom

class AuthManager(private val context: Context) {

    companion object {
        // App pública compartida oficial de Spotifast (ncspot / spotify-player)
        const val DEFAULT_SHARED_CLIENT_ID = "d420a117a32841c2b3474932e49fb54b"

        // Redirección loopback local idéntica al cliente de escritorio de Spotifast
        const val DEFAULT_LOOPBACK_REDIRECT_URI = "http://127.0.0.1:8989/login"
        const val APP_SCHEME_REDIRECT_URI = "spotifast://callback"

        const val AUTH_ENDPOINT = "https://accounts.spotify.com/authorize"

        // Scopes idénticos a src/auth.rs de Spotifast
        val SCOPES = listOf(
            "ugc-image-upload",
            "playlist-modify-private",
            "playlist-modify-public",
            "playlist-read-collaborative",
            "playlist-read-private",
            "user-follow-modify",
            "user-follow-read",
            "user-library-modify",
            "user-library-read",
            "user-modify-playback-state",
            "user-read-playback-position",
            "user-read-playback-state",
            "user-read-private",
            "user-read-email",
            "user-read-recently-played",
            "user-top-read"
        ).joinToString(" ")
    }

    private val prefs = context.getSharedPreferences("spotifast_auth", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(!getAccessToken().isNullOrEmpty())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private var loopbackServer: OAuthLoopbackServer? = null

    fun getEffectiveClientId(): String {
        val custom = prefs.getString("custom_client_id", null)?.trim()
        return if (!custom.isNullOrEmpty()) custom else DEFAULT_SHARED_CLIENT_ID
    }

    fun getCustomClientId(): String? {
        return prefs.getString("custom_client_id", null)?.trim()
    }

    fun setCustomClientId(clientId: String?) {
        val clean = clientId?.trim()
        if (clean.isNullOrEmpty()) {
            prefs.edit().remove("custom_client_id").apply()
        } else {
            prefs.edit().putString("custom_client_id", clean).apply()
        }
    }

    fun getRedirectUri(): String {
        return prefs.getString("redirect_uri", DEFAULT_LOOPBACK_REDIRECT_URI) ?: DEFAULT_LOOPBACK_REDIRECT_URI
    }

    fun setRedirectUri(uri: String) {
        prefs.edit().putString("redirect_uri", uri).apply()
    }

    fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString("refresh_token", null)
    }

    fun saveTokens(accessToken: String, refreshToken: String?) {
        prefs.edit()
            .putString("access_token", accessToken)
            .apply {
                if (!refreshToken.isNullOrEmpty()) {
                    putString("refresh_token", refreshToken)
                }
            }
            .apply()
        _isLoggedIn.value = true
        _authError.value = null
    }

    fun clearSession() {
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("code_verifier")
            .apply()
        _isLoggedIn.value = false
    }

    /**
     * Inicia el flujo PKCE y levanta el servidor loopback en 127.0.0.1:8989
     */
    fun startAuthFlow(
        customClientId: String? = null,
        redirectUri: String = DEFAULT_LOOPBACK_REDIRECT_URI,
        onCodeReceived: (code: String?, error: String?) -> Unit
    ): String {
        if (!customClientId.isNullOrBlank()) {
            setCustomClientId(customClientId)
        }
        setRedirectUri(redirectUri)

        val verifier = generateCodeVerifier()
        prefs.edit()
            .putString("code_verifier", verifier)
            .putString("active_redirect_uri", redirectUri)
            .apply()

        val challenge = generateCodeChallenge(verifier)
        val clientId = getEffectiveClientId()

        // Detener servidor previo si existía
        loopbackServer?.stop()

        // Si la redirección usa el puerto loopback 8989, arrancar el listener
        if (redirectUri.contains("127.0.0.1") || redirectUri.contains("localhost")) {
            loopbackServer = OAuthLoopbackServer(port = 8989) { code, error ->
                onCodeReceived(code, error)
            }.apply { start() }
        }

        return Uri.parse(AUTH_ENDPOINT).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .build()
            .toString()
    }

    fun cancelAuthFlow() {
        loopbackServer?.stop()
        loopbackServer = null
    }

    suspend fun exchangeCodeForToken(code: String, redirectUriOverride: String? = null): Boolean = withContext(Dispatchers.IO) {
        val verifier = prefs.getString("code_verifier", null)
        if (verifier.isNullOrEmpty()) {
            _authError.value = "Error: verificador de código PKCE no encontrado. Vuelve a iniciar sesión."
            return@withContext false
        }

        val clientId = getEffectiveClientId()
        val redirectUri = redirectUriOverride ?: prefs.getString("active_redirect_uri", getRedirectUri()) ?: DEFAULT_LOOPBACK_REDIRECT_URI

        Log.d("AuthManager", "Canjeando código para Client ID $clientId con redirect_uri $redirectUri")

        try {
            val response = SpotifyClient.authService.exchangeToken(
                code = code,
                redirectUri = redirectUri,
                clientId = clientId,
                codeVerifier = verifier
            )

            if (response.isSuccessful && response.body() != null) {
                val tokenBody = response.body()!!
                saveTokens(tokenBody.accessToken, tokenBody.refreshToken)
                loopbackServer?.stop()
                loopbackServer = null
                Log.d("AuthManager", "¡Token intercambiado con éxito!")
                true
            } else {
                val errorBodyStr = response.errorBody()?.string() ?: "Código HTTP ${response.code()}"
                Log.e("AuthManager", "Error al canjear token: $errorBodyStr")
                _authError.value = "Error de Spotify: $errorBodyStr"
                false
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Excepción al canjear token", e)
            _authError.value = "Error de conexión: ${e.localizedMessage}"
            false
        }
    }

    suspend fun refreshAccessToken(): Boolean = withContext(Dispatchers.IO) {
        val refreshToken = getRefreshToken() ?: return@withContext false
        val clientId = getEffectiveClientId()

        try {
            val response = SpotifyClient.authService.refreshToken(
                refreshToken = refreshToken,
                clientId = clientId
            )
            if (response.isSuccessful && response.body() != null) {
                val tokenBody = response.body()!!
                saveTokens(tokenBody.accessToken, tokenBody.refreshToken)
                true
            } else {
                Log.w("AuthManager", "Fallo al refrescar token: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Excepción al refrescar token", e)
            false
        }
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
