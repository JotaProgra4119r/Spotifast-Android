package rocks.spotifast.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.auth.AuthManager
import rocks.spotifast.mobile.ui.theme.*

@Composable
fun LoginScreen(
    onStartLogin: (clientId: String?, redirectUri: String) -> Unit,
    onCancelLogin: () -> Unit,
    isWaitingForBrowser: Boolean = false,
    authUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val authManager = SpotifastApp.instance.authManager
    val authError by authManager.authError.collectAsState()
    val uriHandler = LocalUriHandler.current

    var isUsingPersonalApi by remember {
        mutableStateOf(!authManager.getCustomClientId().isNullOrBlank())
    }
    var personalClientId by remember {
        mutableStateOf(authManager.getCustomClientId() ?: "")
    }
    var selectedRedirectUri by remember {
        mutableStateOf(authManager.getRedirectUri())
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(vertical = 32.dp)
        ) {
            // Icono de Spotifast
            Surface(
                shape = CircleShape,
                color = SpotifyGreen,
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = SpotifyBlack,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Spotifast",
                color = SpotifyWhite,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Port nativo de Spotifast para Android\nSin telemetría • Consumo ultra bajo • Conexión directa",
                color = SpotifySubtext,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Si está esperando la aprobación en el navegador (Igual que Spotifast desktop src/ui/login.rs)
            if (isWaitingForBrowser) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SpotifyDarkGray,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = SpotifyGreen,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Esperando autorización en el navegador...",
                            color = SpotifyWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Inicia sesión y pulsa Aceptar en Spotify para conceder acceso a Spotifast.",
                            color = SpotifySubtext,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        if (!authUrl.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "¿No se abrió? Reabrir navegador",
                                color = SpotifyGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { uriHandler.openUri(authUrl) }
                                    .padding(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedButton(
                            onClick = onCancelLogin,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SpotifyWhite)
                        ) {
                            Text("Cancelar", fontSize = 13.sp)
                        }
                    }
                }
            } else {
                // Mensaje de error si la autorización falló
                if (!authError.isNullOrEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = authError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Tarjeta de configuración de Client ID
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SpotifyDarkGray,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Configuración de tu API de Spotify",
                                color = SpotifyWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Opción 1: API personal
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isUsingPersonalApi = true }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = isUsingPersonalApi,
                                onClick = { isUsingPersonalApi = true },
                                colors = RadioButtonDefaults.colors(selectedColor = SpotifyGreen)
                            )
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(
                                    text = "Mi Client ID personal de Spotify",
                                    color = SpotifyWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Máxima velocidad, cuota independiente sin límites",
                                    color = SpotifySubtext,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        AnimatedVisibility(visible = isUsingPersonalApi) {
                            Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)) {
                                OutlinedTextField(
                                    value = personalClientId,
                                    onValueChange = { personalClientId = it },
                                    label = { Text("Pega tu Client ID (32 caracteres)", fontSize = 12.sp) },
                                    placeholder = { Text("ej. a5b9f63718034d14b1d36d2cd...", fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SpotifyGreen,
                                        unfocusedBorderColor = SpotifySubtext,
                                        focusedTextColor = SpotifyWhite,
                                        unfocusedTextColor = SpotifyWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "🔒 Tu llave privada se guarda exclusivamente en este teléfono. Nunca se comparte ni se sube a internet.",
                                    color = SpotifySubtext,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Opción 2: App pública compartida
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isUsingPersonalApi = false }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = !isUsingPersonalApi,
                                onClick = { isUsingPersonalApi = false },
                                colors = RadioButtonDefaults.colors(selectedColor = SpotifyGreen)
                            )
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(
                                    text = "App compartida estándar de Spotifast",
                                    color = SpotifyWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Sin necesidad de crear app en Spotify Developer",
                                    color = SpotifySubtext,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = SpotifyCardGray
                        )

                        // Selector de Redirect URI
                        Text(
                            text = "URI de redirección configurada en tu app:",
                            color = SpotifySubtext,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRedirectUri = AuthManager.DEFAULT_LOOPBACK_REDIRECT_URI }
                        ) {
                            RadioButton(
                                selected = selectedRedirectUri == AuthManager.DEFAULT_LOOPBACK_REDIRECT_URI,
                                onClick = { selectedRedirectUri = AuthManager.DEFAULT_LOOPBACK_REDIRECT_URI },
                                colors = RadioButtonDefaults.colors(selectedColor = SpotifyGreen)
                            )
                            Text(
                                text = "http://127.0.0.1:8989/login (Estándar Spotifast)",
                                color = SpotifyWhite,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRedirectUri = AuthManager.APP_SCHEME_REDIRECT_URI }
                        ) {
                            RadioButton(
                                selected = selectedRedirectUri == AuthManager.APP_SCHEME_REDIRECT_URI,
                                onClick = { selectedRedirectUri = AuthManager.APP_SCHEME_REDIRECT_URI },
                                colors = RadioButtonDefaults.colors(selectedColor = SpotifyGreen)
                            )
                            Text(
                                text = "spotifast://callback (App nativa)",
                                color = SpotifyWhite,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón principal
                Button(
                    onClick = {
                        val finalClientId = if (isUsingPersonalApi) personalClientId.trim() else null
                        onStartLogin(finalClientId, selectedRedirectUri)
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null, tint = SpotifyBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Iniciar sesión con Spotify",
                        color = SpotifyBlack,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Spotifast nunca ve tu contraseña. La autenticación se realiza de forma 100% segura mediante OAuth 2.0 PKCE en los servidores oficiales de Spotify.",
                    color = SpotifySubtext,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
