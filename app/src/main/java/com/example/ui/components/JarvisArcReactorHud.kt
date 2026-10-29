package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisIceWhite
import com.example.ui.theme.JarvisObsidian
import com.example.voice.VoiceHudState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisArcReactorHud(
    hudState: VoiceHudState,
    audioLevel: Float,
    partialTranscript: String,
    voiceStatusMessage: String,
    onMicClick: () -> Unit,
    onStopSpeakingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_hud_transition")

    // Smooth color transitions across IDLE, LISTENING, THINKING (PROCESSING), and SPEAKING
    val primaryAccent by animateColorAsState(
        targetValue = when (hudState) {
            VoiceHudState.IDLE -> JarvisCyan
            VoiceHudState.LISTENING -> JarvisEmerald
            VoiceHudState.PROCESSING -> JarvisAmber
            VoiceHudState.SPEAKING -> JarvisCyan
        },
        animationSpec = tween(durationMillis = 350),
        label = "primary_accent_color"
    )

    val secondaryAccent by animateColorAsState(
        targetValue = when (hudState) {
            VoiceHudState.IDLE -> JarvisElectricBlue
            VoiceHudState.LISTENING -> JarvisCyan
            VoiceHudState.PROCESSING -> JarvisElectricBlue
            VoiceHudState.SPEAKING -> JarvisEmerald
        },
        animationSpec = tween(durationMillis = 350),
        label = "secondary_accent_color"
    )

    // Continuous outer ring rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 12000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_ring_rotation"
    )

    // Counter-rotating inner ring
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_ring_rotation"
    )

    // High-speed neural sweep angle for the 'Thinking' (PROCESSING) state
    val thinkingSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1600,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "thinking_sweep_angle"
    )

    // Phase-shifted circular pulse progression (0f..1f) for expanding sonar & neural rings
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "circular_pulse_phase"
    )

    // Rhythmic breathing scale for the central core
    val breathingCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_cycle"
    )

    // State-driven intensity multiplier for Listening and Thinking states
    val stateIntensity by animateFloatAsState(
        targetValue = when (hudState) {
            VoiceHudState.LISTENING -> 1.0f
            VoiceHudState.PROCESSING -> 0.9f
            VoiceHudState.SPEAKING -> 0.85f
            VoiceHudState.IDLE -> 0.25f
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "state_intensity"
    )

    val buttonScale = when (hudState) {
        VoiceHudState.LISTENING -> 1f + (0.08f * breathingCycle) + (audioLevel * 0.10f)
        VoiceHudState.PROCESSING -> 0.96f + (0.10f * sin(pulsePhase * 2f * PI.toFloat()))
        VoiceHudState.SPEAKING -> 1f + (audioLevel * 0.08f)
        VoiceHudState.IDLE -> 1f + (0.02f * breathingCycle)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("jarvis_arc_reactor_hud"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(224.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("jarvis_core_pulse_canvas")
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2f

                // 1. Ambient Multi-Layer Radial Core Glow
                val glowMultiplier = 0.75f + (0.25f * breathingCycle * stateIntensity)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryAccent.copy(alpha = 0.34f * glowMultiplier),
                            secondaryAccent.copy(alpha = 0.14f * glowMultiplier),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius
                    ),
                    radius = maxRadius,
                    center = center
                )

                // 2. LISTENING STATE: 3 Staggered Expanding Circular Sonar Pulse Waves
                if (hudState == VoiceHudState.LISTENING || stateIntensity > 0.35f) {
                    val waveCount = 3
                    for (w in 0 until waveCount) {
                        val waveProgress = (pulsePhase + w / waveCount.toFloat()) % 1f
                        val startRadius = maxRadius * 0.36f
                        val endRadius = maxRadius * (0.94f + audioLevel * 0.06f)
                        val currentRadius = startRadius + (endRadius - startRadius) * waveProgress
                        val waveAlpha = (1f - waveProgress) * (0.55f + audioLevel * 0.35f) * stateIntensity

                        drawCircle(
                            color = primaryAccent.copy(alpha = waveAlpha.coerceIn(0f, 0.85f)),
                            radius = currentRadius,
                            center = center,
                            style = Stroke(
                                width = (3.5f - 2f * waveProgress).dp.toPx()
                            )
                        )
                    }
                }

                // 3. THINKING (PROCESSING) STATE: Converging Neural Pulse Rings & Synaptic Orbitals
                if (hudState == VoiceHudState.PROCESSING) {
                    val neuralWaveCount = 3
                    for (n in 0 until neuralWaveCount) {
                        // Reverse phase (1f -> 0f) so energy pulses both inward to the brain and outward
                        val rawProgress = (pulsePhase + n / neuralWaveCount.toFloat()) % 1f
                        val inwardProgress = 1f - rawProgress
                        val ringRadius = maxRadius * (0.38f + 0.52f * inwardProgress)
                        val ringAlpha = sin(rawProgress * PI.toFloat()) * 0.75f

                        drawCircle(
                            color = if (n % 2 == 0) {
                                primaryAccent.copy(alpha = ringAlpha.coerceIn(0f, 0.85f))
                            } else {
                                secondaryAccent.copy(alpha = ringAlpha.coerceIn(0f, 0.85f))
                            },
                            radius = ringRadius,
                            center = center,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), rawProgress * 60f)
                            )
                        )
                    }

                    // 3 High-Speed Rotating Neural Reasoning Arcs
                    rotate(degrees = thinkingSweepAngle, pivot = center) {
                        val neuralArcSize = Size(maxRadius * 1.26f, maxRadius * 1.26f)
                        val neuralArcTopLeft = Offset(
                            center.x - neuralArcSize.width / 2f,
                            center.y - neuralArcSize.height / 2f
                        )
                        for (startDeg in listOf(0f, 120f, 240f)) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        secondaryAccent.copy(alpha = 0.7f),
                                        primaryAccent
                                    ),
                                    center = center
                                ),
                                startAngle = startDeg,
                                sweepAngle = 68f,
                                useCenter = false,
                                topLeft = neuralArcTopLeft,
                                size = neuralArcSize,
                                style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    // 6 Pulsing Synaptic Nodes around the Thinking Ring
                    for (node in 0 until 6) {
                        val nodeAngleRad = Math.toRadians((node * 60.0) + thinkingSweepAngle * 1.4)
                        val nodeOscillation = 0.62f + 0.06f * sin((pulsePhase * 2 * PI + node).toFloat())
                        val nodeRadius = maxRadius * nodeOscillation
                        val nx = center.x + (nodeRadius * cos(nodeAngleRad)).toFloat()
                        val ny = center.y + (nodeRadius * sin(nodeAngleRad)).toFloat()

                        drawCircle(
                            color = primaryAccent.copy(alpha = 0.35f),
                            radius = 7.dp.toPx(),
                            center = Offset(nx, ny)
                        )
                        drawCircle(
                            color = JarvisIceWhite,
                            radius = 3.dp.toPx(),
                            center = Offset(nx, ny)
                        )
                    }
                }

                // 4. Outer Segmented HUD Telemetry Ring
                val speedFactor = if (hudState == VoiceHudState.PROCESSING) 2.5f else 1f
                rotate(degrees = (outerRotation * speedFactor) % 360f, pivot = center) {
                    drawCircle(
                        color = primaryAccent.copy(alpha = 0.48f),
                        radius = maxRadius * 0.92f,
                        center = center,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(28f, 18f, 8f, 18f), 0f)
                        )
                    )

                    // 4 HUD Cardinal Arcs
                    val arcSize = Size(maxRadius * 1.70f, maxRadius * 1.70f)
                    val arcTopLeft = Offset(center.x - arcSize.width / 2f, center.y - arcSize.height / 2f)
                    for (angle in listOf(0f, 90f, 180f, 270f)) {
                        drawArc(
                            color = primaryAccent.copy(alpha = 0.78f),
                            startAngle = angle,
                            sweepAngle = 42f,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // 5. Counter-Rotating Inner Geometric Ring
                rotate(degrees = (innerRotation * speedFactor) % 360f, pivot = center) {
                    drawCircle(
                        color = secondaryAccent.copy(alpha = 0.62f),
                        radius = maxRadius * 0.72f,
                        center = center,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 14f), 0f)
                        )
                    )
                }

                // 6. 28 Audio- & State-Reactive Radial Waveform Ticks
                val barCount = 28
                val baseBarRadius = maxRadius * 0.50f
                val activeWaveAmplitude = when (hudState) {
                    VoiceHudState.LISTENING -> (0.28f + audioLevel * 0.72f)
                    VoiceHudState.PROCESSING -> (0.45f + 0.35f * abs(sin(pulsePhase * 2f * PI.toFloat())))
                    VoiceHudState.SPEAKING -> (0.30f + audioLevel * 0.70f)
                    VoiceHudState.IDLE -> 0.14f
                }

                for (i in 0 until barCount) {
                    val angleRad = Math.toRadians((i * (360.0 / barCount)) + outerRotation * 0.5)
                    val waveMod = abs(sin(i * 0.65f + pulsePhase * 2f * PI.toFloat() + audioLevel * 8f))
                    val barLen = maxRadius * (0.05f + 0.17f * activeWaveAmplitude * waveMod)
                    val startX = center.x + (baseBarRadius * cos(angleRad)).toFloat()
                    val startY = center.y + (baseBarRadius * sin(angleRad)).toFloat()
                    val endX = center.x + ((baseBarRadius + barLen) * cos(angleRad)).toFloat()
                    val endY = center.y + ((baseBarRadius + barLen) * sin(angleRad)).toFloat()

                    drawLine(
                        color = if (i % 2 == 0) primaryAccent.copy(alpha = 0.9f) else secondaryAccent.copy(alpha = 0.75f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 7. Pulsing Core Reactor Halo
                val haloScale = 0.40f + (0.07f * breathingCycle) + (audioLevel * 0.08f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryAccent.copy(alpha = 0.45f),
                            secondaryAccent.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius * haloScale * 1.3f
                    ),
                    radius = (maxRadius * haloScale * 1.3f).coerceAtMost(maxRadius * 0.65f),
                    center = center
                )
            }

            // Central Interactive Pulsing Core Button (86dp touch target)
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryAccent.copy(alpha = 0.42f),
                                JarvisObsidian.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .border(
                        width = if (hudState == VoiceHudState.LISTENING || hudState == VoiceHudState.PROCESSING) {
                            3.dp
                        } else {
                            2.dp
                        },
                        color = primaryAccent,
                        shape = CircleShape
                    )
                    .clickable { onMicClick() }
                    .testTag("central_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (hudState) {
                        VoiceHudState.LISTENING -> Icons.Filled.MicOff
                        VoiceHudState.PROCESSING -> Icons.Filled.AutoAwesome
                        VoiceHudState.SPEAKING -> Icons.Filled.GraphicEq
                        VoiceHudState.IDLE -> Icons.Filled.Mic
                    },
                    contentDescription = when (hudState) {
                        VoiceHudState.LISTENING -> "JARVIS Listening — Tap to Stop"
                        VoiceHudState.PROCESSING -> "JARVIS Thinking — Processing Neural Request"
                        VoiceHudState.SPEAKING -> "JARVIS Speaking"
                        VoiceHudState.IDLE -> "Tap to Speak with JARVIS"
                    },
                    tint = JarvisIceWhite,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // Animated State Badge & Stop Speaking Control
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            val stateLabel = when (hudState) {
                VoiceHudState.IDLE -> "CORE IDLE // TAP MIC TO SPEAK"
                VoiceHudState.LISTENING -> "LISTENING // VOICE SONAR PULSE ACTIVE"
                VoiceHudState.PROCESSING -> "THINKING // GEMINI NEURAL REASONING"
                VoiceHudState.SPEAKING -> "SPEAKING // VOICE SYNTHESIS ACTIVE"
            }

            Surface(
                color = primaryAccent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, primaryAccent.copy(alpha = 0.65f)),
                modifier = Modifier.testTag("hud_state_badge")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    // Pulsing status indicator dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .graphicsLayer {
                                val dotScale = 0.8f + 0.45f * breathingCycle
                                scaleX = dotScale
                                scaleY = dotScale
                            }
                            .clip(CircleShape)
                            .background(primaryAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stateLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = primaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (hudState == VoiceHudState.SPEAKING) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = JarvisCrimson.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCrimson),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onStopSpeakingClick() }
                        .testTag("hud_stop_speaking_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Stop Speaking",
                            tint = JarvisCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "STOP VOICE",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (partialTranscript.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "“$partialTranscript…”",
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisEmerald,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = voiceStatusMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * 3D Iridescent Swirling AI Sphere ("Voice chat" orb matching the reference photo's right screen).
 * Features layered blue, indigo, violet, magenta, and cyan 3D ribbons, specular glass highlights,
 * and smooth circular pulsing for Listening, Thinking, and Speaking states.
 */
@Composable
fun JarvisIridescentVoiceOrb(
    hudState: VoiceHudState,
    audioLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "iridescent_orb_transition")

    val swirlRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (hudState == VoiceHudState.IDLE) 14000 else 5500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "swirl_rotation"
    )

    val counterSwirl by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (hudState == VoiceHudState.IDLE) 11000 else 4200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_swirl"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (hudState) {
                    VoiceHudState.LISTENING -> 850
                    VoiceHudState.PROCESSING -> 650
                    VoiceHudState.SPEAKING -> 750
                    VoiceHudState.IDLE -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_breathing"
    )

    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (hudState == VoiceHudState.PROCESSING) 1100 else 1600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_ripple"
    )

    val dynamicAudioBoost by animateFloatAsState(
        targetValue = if (hudState == VoiceHudState.LISTENING || hudState == VoiceHudState.SPEAKING) {
            audioLevel.coerceIn(0.08f, 1.0f) * 0.14f
        } else {
            0f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "orb_audio_boost"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(230.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .testTag("iridescent_voice_orb")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = breathingScale + dynamicAudioBoost
                    scaleX = s
                    scaleY = s
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f
            val sphereR = maxR * 0.80f

            // 1. Soft outer ambient halo when Listening, Thinking, or Speaking
            val haloColor = when (hudState) {
                VoiceHudState.LISTENING -> Color(0xFFA855F7)
                VoiceHudState.PROCESSING -> Color(0xFFF59E0B)
                VoiceHudState.SPEAKING -> Color(0xFF38BDF8)
                VoiceHudState.IDLE -> Color(0xFF7C3AED)
            }
            if (hudState != VoiceHudState.IDLE) {
                val rippleR = sphereR * (1.0f + rippleProgress * 0.25f)
                drawCircle(
                    color = haloColor.copy(alpha = (1f - rippleProgress) * 0.42f),
                    radius = rippleR,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        haloColor.copy(alpha = 0.36f),
                        Color(0xFF3B82F6).copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR
                ),
                radius = maxR,
                center = center
            )

            // 2. Deep 3D Sphere Base (Royal Indigo -> Deep Cobalt -> Midnight Violet)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF4F46E5),
                        Color(0xFF1E1B4B),
                        Color(0xFF0F0728)
                    ),
                    center = Offset(center.x - sphereR * 0.22f, center.y - sphereR * 0.25f),
                    radius = sphereR * 1.25f
                ),
                radius = sphereR,
                center = center
            )

            // 3. Swirling 3D Iridescent Ribbons inside the sphere
            rotate(degrees = swirlRotation, pivot = center) {
                drawOval(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.85f),
                            Color(0xFF6366F1).copy(alpha = 0.90f),
                            Color(0xFFD946EF).copy(alpha = 0.80f),
                            Color(0xFF1D4ED8).copy(alpha = 0.40f)
                        )
                    ),
                    topLeft = Offset(center.x - sphereR * 0.92f, center.y - sphereR * 0.62f),
                    size = Size(sphereR * 1.84f, sphereR * 1.24f)
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF60A5FA).copy(alpha = 0.9f),
                            Color(0xFFC084FC).copy(alpha = 0.95f),
                            Color(0xFF38BDF8).copy(alpha = 0.8f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 20f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(center.x - sphereR * 0.78f, center.y - sphereR * 0.78f),
                    size = Size(sphereR * 1.56f, sphereR * 1.56f),
                    style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            rotate(degrees = counterSwirl, pivot = center) {
                drawOval(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFA855F7).copy(alpha = 0.75f),
                            Color(0xFF2563EB).copy(alpha = 0.85f),
                            Color(0xFFEC4899).copy(alpha = 0.70f)
                        )
                    ),
                    topLeft = Offset(center.x - sphereR * 0.65f, center.y - sphereR * 0.90f),
                    size = Size(sphereR * 1.30f, sphereR * 1.80f)
                )

                // Dark S-curve ribbon fold like the center crease in the reference sphere
                drawArc(
                    color = Color(0xFF09051A).copy(alpha = 0.68f),
                    startAngle = 110f,
                    sweepAngle = 190f,
                    useCenter = false,
                    topLeft = Offset(center.x - sphereR * 0.62f, center.y - sphereR * 0.62f),
                    size = Size(sphereR * 1.24f, sphereR * 1.24f),
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Glossy 3D Glass Rim & Specular Highlights
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color(0xFF93C5FD).copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - sphereR * 0.34f, center.y - sphereR * 0.36f),
                    radius = sphereR * 0.55f
                ),
                radius = sphereR,
                center = center
            )

            // Bottom cyan-lavender rim reflection
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF67E8F9).copy(alpha = 0.75f),
                        Color(0xFFF0ABFC).copy(alpha = 0.85f),
                        Color(0xFF818CF8).copy(alpha = 0.65f)
                    )
                ),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(center.x - sphereR * 0.95f, center.y - sphereR * 0.95f),
                size = Size(sphereR * 1.90f, sphereR * 1.90f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Outer glass border
            drawCircle(
                color = Color.White.copy(alpha = 0.28f),
                radius = sphereR,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

