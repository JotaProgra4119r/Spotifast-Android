# Spotifast Mobile (Android) 📱⚡

[![Platform](https://img.shields.io/badge/Platform-Android_15_%2F_ColorOS_15-green.svg)](https://developer.android.com)
[![Device](https://img.shields.io/badge/Target-OPPO_A58_(CPH2577)-blue.svg)]()
[![License](https://img.shields.io/badge/License-GPLv3-orange.svg)](https://github.com/crmne/spotifast)
[![Architecture](https://img.shields.io/badge/UI-Jetpack_Compose_Material3-purple.svg)]()

Port nativo, ultraligero y de alto rendimiento de **Spotifast** (cliente de escritorio en Rust) para **Android**, desarrollado en Kotlin con Jetpack Compose y optimizado a medida para el **OPPO A58** y dispositivos móviles modernos.

---

## 📥 Descarga Directa del APK

Puedes descargar e instalar el archivo APK compilado directamente en tu teléfono:
👉 **[Descargar Spotifast-OppoA58.apk](https://github.com/JotaProgra4119r/Spotifast-Android/raw/main/Spotifast-OppoA58.apk)** *(23.4 MB)*

---

## 🌟 Características y Paridad 1:1 con Spotifast PC

### 📱🖥️ Interfaz Responsiva Adaptativa (Móvil, Horizontal y Tableta)
* **Modo Vertical (Smartphone):** Diseñado para uso cómodo con una sola mano, barra de navegación inferior, MiniPlayer flotante y reproductor deslizable a pantalla completa.
* **Modo Horizontal y Pantalla Grande / Tableta (>= 600dp):** Al rotar el teléfono o conectarlo a una pantalla más grande/tableta, la interfaz **se transforma automáticamente en la experiencia exacta de escritorio de Spotifast**:
  * **Barra lateral fija a la izquierda:** Acceso rápido a Inicio, Buscar, Biblioteca, Letras, Cola, Ecualizador, Winamp y Ajustes.
  * **Panel central dinámico:** Listas de canciones, carátulas y resultados de búsqueda.
  * **Barra inferior de escritorio (Player Bar):** Control de volumen con control deslizante dedicado, visualizador de frecuencias en vivo, selector de dispositivos Connect y botones rápidos.

### 🎵 Motor de Audio Dual
* **Música Local del Teléfono:** Reproduce directamente canciones almacenadas en la memoria interna o tarjeta SD del Oppo A58 usando **AndroidX Media3 (ExoPlayer)**, sin necesidad de conexión ni cuenta premium.
* **Integración con Spotify Connect:** Control remoto bidireccional y reproducción fluida en cualquier altavoz, PC o dispositivo compatible.

### 🎤 Letras Sincronizadas en Vivo (LRC)
* Integración directa con **LRCLIB** (idéntica a `src/lyrics.rs` de PC).
* Seguimiento estrofa por estrofa en tiempo real con autodesplazamiento suave.
* **Toca para saltar:** Al presionar cualquier verso, la canción salta automáticamente a ese segundo exacto.

### 📻 Modo Retro Winamp
* Port de `src/winamp.rs` y `src/ui/winamp/`.
* Chasis de titanio con pantalla de matriz LED verde fosforescente.
* Ticker de texto marquesina con información de pista, bitrate (320 kbps) y frecuencia de muestreo (44 kHz).
* Analizador de espectro reactivo integrado.
* Botones clásicos de transporte (`PREV`, `PLAY`, `PAUSE`, `STOP`, `NEXT`, `EQ`) y modo enrollable (*shaded*).

### 🎚️ Ecualizador Profesional de 10 Bandas
* Port de `src/eq.rs`.
* Bandas de frecuencia de 32 Hz a 16 kHz: `32Hz`, `64Hz`, `125Hz`, `250Hz`, `500Hz`, `1kHz`, `2kHz`, `4kHz`, `8kHz`, `16kHz`.
* Refuerzo de graves (**Bass Boost**), sonido envolvente 3D y ganancia pre-amp.
* Preajustes incluidos: *Plano, Rock, Pop, Jazz, Bass Boost, Vocal, etc.*

### 🌈 Visualizador Reactivo & Simulación MilkDrop
* Port de `src/vis.rs` y `src/milkdrop.rs`.
* **Espectro de 19 barras:** Con física de caída de picos flotantes estilo clásico Winamp.
* **Osciloscopio (Scope):** Trazado de onda continua suave.
* **MilkDrop Waves:** Ondas sinusoidales psicodélicas neón que pulsan al ritmo de la música.
* Sigue la señal post-ecualizador y pre-volumen (Spotifast standard).

### 📁 Escaneo de Almacenamiento Local
* Escaneo automático de archivos de audio mediante `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`.
* Pestaña dedicada **"Descargas y Locales"** en la biblioteca para ver y reproducir tu música guardada.
* Botón para volver a escanear en cualquier momento.

### 🛡️ Ajustes Avanzados de PC y Spotify
* **Calidad de transmisión:** Selección de bitrate (96 kbps Normal, 160 kbps Alta, 320 kbps Muy Alta).
* **Reproducción:** Reproducción sin pausas (*Gapless*), Normalización de volumen, Fundido (*Crossfade* de 0 a 12s) y Audio mono.
* **Modos de Spotify:** Modo sin conexión (*Offline*), Sesión privada y Modo Ahorro de datos.
* **Configuración de Proxy:** Modos *Directo*, *Sistema*, *HTTP* y *SOCKS5* con Host y Puerto.
* **Temas visuales:** *Spotify Dark*, *OLED Puro*, *Winamp Classic* y *Cyberpunk Neón*, más acento dinámico según la carátula del álbum.
* **Gestión de Client ID:** Posibilidad de vincular tu propio Client ID personal en modo desarrollador sin compartir tus credenciales.

---

## 💡 Inspiración y Reconocimientos

Este proyecto nace como un port dedicado para Android basado e inspirado en:

* **[Spotifast (Desktop)](https://spotifast.rocks/)**: Creado por **Carmine** ([@crmne](https://github.com/crmne/spotifast)). El cliente de escritorio en Rust más rápido, ligero y elegante para Spotify. Toda la arquitectura, diseño y módulos de esta versión móvil toman como referencia directa su código fuente.
* **[librespot](https://github.com/librespot-org/librespot)**: La biblioteca de código abierto para clientes de Spotify.
* **[LRCLIB](https://lrclib.net)**: El servicio abierto y comunitario de letras sincronizadas.
* **[Winamp](https://www.winamp.com)**: Por el icónico diseño de reproductor de audio, visualizadores y ecualizador que marcaron una época.

---

## 🛠️ Compilación desde el Código Fuente

### Requisitos
* Android SDK 36 (Android 15)
* JDK 17 o superior
* Gradle 9.1+

### Pasos
```bash
# Clonar el repositorio
git clone https://github.com/JotaProgra4119r/Spotifast-Android.git
cd Spotifast-Android

# Compilar el APK Debug
./gradlew assembleDebug

# Ejecutar las pruebas unitarias de paridad
./gradlew testDebugUnitTest
```

El APK resultante se ubicará en `app/build/outputs/apk/debug/app-debug.apk`.

---

## 👤 Autor y Mantenimiento

* **JotaProgra4119r** ([@JotaProgra4119r](https://github.com/JotaProgra4119r))
  * Adaptación, desarrollo nativo en Kotlin/Compose y optimizaciones específicas para Android y dispositivos OPPO (ColorOS).

---

## ⚖️ Licencia

Distribuido bajo la misma licencia de código abierto de Spotifast. Sin telemetría, sin rastreadores y respetando la privacidad del usuario.
