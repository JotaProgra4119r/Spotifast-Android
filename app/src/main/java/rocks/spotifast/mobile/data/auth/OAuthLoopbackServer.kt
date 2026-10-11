package rocks.spotifast.mobile.data.auth

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * Servidor loopback local HTTP en 127.0.0.1:8989 idéntico al cliente de escritorio de Spotifast.
 * Permite capturar automáticamente la redirección http://127.0.0.1:8989/login cuando Spotify
 * devuelve el código de autorización en el navegador del dispositivo.
 */
class OAuthLoopbackServer(
    private val port: Int = 8989,
    private val onCodeReceived: (code: String?, error: String?) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    @Volatile
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true

        Thread {
            try {
                serverSocket = ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))
                serverSocket?.soTimeout = 10 * 60 * 1000 // 10 minutos igual que LOGIN_TIMEOUT en desktop
                Log.d("OAuthLoopbackServer", "Servidor loopback escuchando en http://127.0.0.1:$port/login")

                while (isRunning) {
                    val socket: Socket = try {
                        serverSocket?.accept() ?: break
                    } catch (e: SocketTimeoutException) {
                        Log.d("OAuthLoopbackServer", "Tiempo de espera agotado para el inicio de sesión")
                        break
                    } catch (e: Exception) {
                        break
                    }

                    handleClientSocket(socket)
                    break // Detener una vez procesado el callback
                }
            } catch (e: Exception) {
                Log.e("OAuthLoopbackServer", "Error en ServerSocket", e)
            } finally {
                stop()
            }
        }.apply {
            name = "OAuthLoopbackServer-Thread"
            start()
        }
    }

    private fun handleClientSocket(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: return
            Log.d("OAuthLoopbackServer", "Petición HTTP recibida: $requestLine")

            val path = requestLine.split(" ").getOrNull(1) ?: ""
            var code: String? = null
            var error: String? = null

            if (path.contains("?")) {
                val queryString = path.substringAfter("?")
                for (param in queryString.split("&")) {
                    val parts = param.split("=")
                    if (parts.size == 2) {
                        val key = parts[0]
                        val value = parts[1]
                        if (key == "code") code = value
                        if (key == "error") error = value
                    }
                }
            }

            val writer = PrintWriter(socket.getOutputStream(), true)
            val html = if (!code.isNullOrEmpty()) {
                """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Spotifast - Sesión Iniciada</title>
                    <style>
                        body {
                            background-color: #121212;
                            color: #ffffff;
                            font-family: system-ui, -apple-system, sans-serif;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            height: 100vh;
                            margin: 0;
                            padding: 20px;
                            box-sizing: border-box;
                        }
                        .card {
                            background-color: #181818;
                            border: 1px solid #282828;
                            border-radius: 16px;
                            padding: 36px 24px;
                            text-align: center;
                            max-width: 420px;
                            width: 100%;
                            box-shadow: 0 16px 40px rgba(0,0,0,0.6);
                        }
                        .badge {
                            width: 64px;
                            height: 64px;
                            background-color: #1db954;
                            color: #000;
                            border-radius: 50%;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            font-size: 32px;
                            font-weight: bold;
                            margin: 0 auto 20px auto;
                        }
                        h1 {
                            font-size: 22px;
                            margin: 0 0 10px 0;
                            color: #1db954;
                        }
                        p {
                            color: #b3b3b3;
                            font-size: 14px;
                            line-height: 1.6;
                            margin: 0 0 24px 0;
                        }
                        .btn {
                            display: inline-block;
                            background-color: #1db954;
                            color: #000000;
                            font-weight: 700;
                            font-size: 15px;
                            padding: 14px 28px;
                            border-radius: 30px;
                            text-decoration: none;
                        }
                    </style>
                    <script>
                        setTimeout(function() {
                            window.location.href = "spotifast://callback?code=$code";
                        }, 400);
                    </script>
                </head>
                <body>
                    <div class="card">
                        <div class="badge">✓</div>
                        <h1>¡Sesión autorizada!</h1>
                        <p>Tu cuenta de Spotify ha sido conectada con éxito. Ya puedes regresar a Spotifast.</p>
                        <a class="btn" href="spotifast://callback?code=$code">Volver a Spotifast</a>
                    </div>
                </body>
                </html>
                """.trimIndent()
            } else {
                """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Spotifast - Error</title>
                    <style>
                        body { background: #121212; color: #fff; font-family: sans-serif; text-align: center; padding: 60px 20px; }
                        h1 { color: #e91429; }
                        p { color: #b3b3b3; }
                    </style>
                </head>
                <body>
                    <h1>Error de autorización</h1>
                    <p>${error ?: "No se recibió código de autorización de Spotify."}</p>
                </body>
                </html>
                """.trimIndent()
            }

            val bytes = html.toByteArray(Charsets.UTF_8)
            writer.print("HTTP/1.1 200 OK\r\n")
            writer.print("Content-Type: text/html; charset=utf-8\r\n")
            writer.print("Content-Length: ${bytes.size}\r\n")
            writer.print("Connection: close\r\n\r\n")
            writer.print(html)
            writer.flush()
            socket.close()

            onCodeReceived(code, error)
        } catch (e: Exception) {
            Log.e("OAuthLoopbackServer", "Error al procesar socket cliente", e)
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }
}
