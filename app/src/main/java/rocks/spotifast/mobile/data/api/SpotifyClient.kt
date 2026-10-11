package rocks.spotifast.mobile.data.api

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import rocks.spotifast.mobile.SpotifastApp
import java.util.concurrent.TimeUnit

object SpotifyClient {

    private const val BASE_URL = "https://api.spotify.com/"
    private const val ACCOUNTS_URL = "https://accounts.spotify.com/"

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = SpotifastApp.instance.authManager.getAccessToken()

        val request = if (!token.isNullOrEmpty()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "SpotifastAndroid/1.0.0")
                .build()
        } else {
            original
        }

        val response = chain.proceed(request)

        // Si devuelve 401 (token expirado), intentar refrescar
        if (response.code == 401 && token != null) {
            response.close()
            val refreshed = runBlocking {
                SpotifastApp.instance.authManager.refreshAccessToken()
            }
            if (refreshed) {
                val newToken = SpotifastApp.instance.authManager.getAccessToken()
                val newRequest = original.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .header("User-Agent", "SpotifastAndroid/1.0.0")
                    .build()
                return@Interceptor chain.proceed(newRequest)
            }
        }

        response
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val authOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val apiService: SpotifyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifyApiService::class.java)
    }

    val authService: SpotifyAuthService by lazy {
        Retrofit.Builder()
            .baseUrl(ACCOUNTS_URL)
            .client(authOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifyAuthService::class.java)
    }
}
