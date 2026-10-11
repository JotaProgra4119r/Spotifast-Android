# Spotifast Android - Arquitectura y Plan de Implementación

## 1. Dispositivo Objetivo y Optimización
* **Dispositivo:** Oppo A58 (ColorOS 13.1 / 14, Android 13/14).
* **Hardware:** Procesador MediaTek Helio G85 (8 núcleos: 2x Cortex-A75 @ 2.0 GHz + 6x Cortex-A55 @ 1.8 GHz), GPU Mali-G52 MC2, 6GB/8GB RAM.
* **Reto:** La app oficial de Spotify consume más de 350 MB de memoria RAM y ejecuta múltiples procesos en segundo plano con telemetría y anuncios.
* **Objetivo de Spotifast Android:** Consumir menos de **60 MB de RAM**, mantener **60/90 FPS fluidos** en scroll y renderizado nativo sin pausas.

---

## 2. Respuestas a los Requerimientos Clave

### A. Sincronización entre la App Oficial y Spotifast ("Aparecer en ambas")
* **Sincronización en la Nube (Online):** 
  * Al usar la misma cuenta de Spotify y el Client ID del usuario (`a5b9f63718034d14b1d36d2cd`), **cualquier cambio se refleja en ambas aplicaciones en tiempo real**.
  * Si añades una canción a "Tus me gusta" o creas una playlist en la app oficial de Spotify, aparece inmediatamente en Spotifast Android, y viceversa.
* **Archivos Descargados Offline (La realidad técnica de Android):**
  * Android aplica una estricta política de seguridad (*Sandbox*): ninguna aplicación puede leer los archivos privados de otra aplicación (`/data/data/com.spotify.music/`).
  * Además, Spotify cifra las descargas de la app oficial con Widevine DRM propietario que no se puede desencriptar legalmente fuera de su app.
  * **Solución nativa:** Spotifast Android cuenta con su **propio motor de descargas y caché local**. Puedes marcar cualquier canción o playlist para descargar; la app la almacena en el almacenamiento de tu Oppo A58 a máxima calidad (320 kbps) en una caché protegida y reproducible sin conexión a internet.

---

## 3. Pantallas Replicadas (según capturas del usuario)
1. **Inicio (Home):**
   * Pestañas superiores: *Todas*, *Música*, *Podcasts*, *Audiolibros*.
   * Cuadrícula de accesos rápidos recientes (Mix relajante, Tus me gusta, Mix diario, etc.).
   * Secciones de recomendaciones y episodios recientes.
2. **Buscar (Search):**
   * Barra de búsqueda con autocompletado en tiempo real.
   * Tarjetas de categorías: *Música*, *Podcasts*, *Audiolibros*, *Eventos en vivo*.
3. **Tu Biblioteca (Library & "Tus me gusta"):**
   * Vista de 1480 canciones de "Tus me gusta".
   * Chips de filtrado rápido (*Pop*, *Chill*, *Latino*, *J-pop*, *K-pop*, *Soundtrack*).
   * Botón de reproducción aleatoria y descarga offline.
4. **Reproductor (MiniPlayer & Fullscreen):**
   * Barra flotante inferior conectable con Spotify Connect.
   * Reproductor expandido con carátula grande, barra de progreso (*scrubber*), letras y controles.
5. **Configuración y Perfil:**
   * Panel lateral con avatar, nombre (*José Javier Pérez Caal*) y plan Premium.
   * Ajustes de calidad de audio (320 kbps), reproducción sin pausas (*Gapless*), caché local y llave de API personal.

---

## 4. Stack Tecnológico Elegido
* **Lenguaje:** Kotlin 2.0+
* **UI Toolkit:** Jetpack Compose + Material 3 (Tema Spotify Dark)
* **Arquitectura:** MVVM (Model-View-ViewModel) con Kotlin Coroutines y StateFlow
* **Audio Engine:** AndroidX Media3 (`MediaSessionService` + ExoPlayer) para reproducción en segundo plano con pantalla apagada y controles en pantalla de bloqueo
* **Red & API:** Retrofit 2 + OkHttp 4 con OAuth2 PKCE y soporte para Client ID personal
* **Caché y Persistencia:** Room Database + Coil para carátulas en disco
