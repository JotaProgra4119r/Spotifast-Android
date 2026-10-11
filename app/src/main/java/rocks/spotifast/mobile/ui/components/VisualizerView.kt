package rocks.spotifast.mobile.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import rocks.spotifast.mobile.data.local.VisualizerType
import rocks.spotifast.mobile.ui.theme.SpotifyGreen
import kotlin.math.sin

@Composable
fun VisualizerView(
    type: VisualizerType,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    if (type == VisualizerType.OFF) return

    val infiniteTransition = rememberInfiniteTransition(label = "vis_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) 6.28318f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 1f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val width = size.width
        val height = size.height

        when (type) {
            VisualizerType.SPECTRUM_BARS -> {
                // 19 barras idénticas a Spotifast desktop src/vis.rs
                val numBars = 19
                val gap = 4.dp.toPx()
                val barWidth = (width - gap * (numBars - 1)) / numBars

                val barHeights = floatArrayOf(
                    0.85f, 0.95f, 0.70f, 0.88f, 0.60f, 0.75f, 0.90f, 0.65f,
                    0.50f, 0.80f, 0.72f, 0.58f, 0.45f, 0.62f, 0.78f, 0.40f,
                    0.52f, 0.35f, 0.48f
                )

                for (i in 0 until numBars) {
                    val variation = if (isPlaying) {
                        val s = (sin(phase + i * 0.45) * 0.45 + 0.55).toFloat()
                        barHeights[i] * s * pulse
                    } else {
                        0.08f
                    }
                    val currentH = (height * variation).coerceIn(4f, height)
                    val left = i * (barWidth + gap)
                    val top = height - currentH

                    val barBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1ED760),
                            Color(0xFF1DB954),
                            Color(0xFF0F662E)
                        ),
                        startY = top,
                        endY = height
                    )

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, currentH),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )

                    // Pico flotante Winamp style
                    if (isPlaying) {
                        val peakTop = (top - 5f).coerceAtLeast(0f)
                        drawRoundRect(
                            color = Color(0xFFB3FFC6),
                            topLeft = Offset(left, peakTop),
                            size = Size(barWidth, 3.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }

            VisualizerType.OSCILLOSCOPE -> {
                // Osciloscopio con onda suave (Scope) de src/vis.rs
                val path = Path()
                val midY = height / 2f
                val points = 80
                val step = width / points

                for (i in 0..points) {
                    val x = i * step
                    val wave = if (isPlaying) {
                        sin(phase * 2f + i * 0.18f) * (height * 0.38f * pulse) +
                                sin(phase * 4f + i * 0.32f) * (height * 0.12f)
                    } else {
                        0f
                    }
                    val y = midY + wave.toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = SpotifyGreen,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            VisualizerType.MILKDROP_WAVES -> {
                // Simulación MilkDrop con ondas psicodélicas y degradados neón
                val midY = height / 2f
                val path1 = Path()
                val path2 = Path()
                val points = 60
                val step = width / points

                for (i in 0..points) {
                    val x = i * step
                    val y1 = midY + if (isPlaying) {
                        (sin(phase * 1.5f + i * 0.2f) * (height * 0.4f * pulse)).toFloat()
                    } else 0f
                    val y2 = midY + if (isPlaying) {
                        (sin(-phase * 1.8f + i * 0.25f + 1f) * (height * 0.3f * pulse)).toFloat()
                    } else 0f

                    if (i == 0) {
                        path1.moveTo(x, y1)
                        path2.moveTo(x, y2)
                    } else {
                        path1.lineTo(x, y1)
                        path2.lineTo(x, y2)
                    }
                }

                drawPath(
                    path = path1,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF1ED760), Color(0xFF00E5FF), Color(0xFF9D00FF))
                    ),
                    style = Stroke(width = 3.5.dp.toPx())
                )
                drawPath(
                    path = path2,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFF007F), Color(0xFF7000FF), Color(0xFF00FFC8))
                    ),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            else -> {}
        }
    }
}
