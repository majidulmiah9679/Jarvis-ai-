package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.actions.DeviceTelemetry
import com.example.actions.PendingConfirmationAction
import com.example.ai.JarvisBrainService
import com.example.data.ChatMessageEntity
import com.example.data.JarvisSettings
import com.example.data.REAL_VOICE_PERSONAS
import com.example.data.VoiceGender
import com.example.data.findVoicePersonaById
import com.example.ui.components.JarvisIridescentVoiceOrb
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisIceWhite
import com.example.ui.theme.JarvisMutedCyan
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisVisualTheme
import com.example.ui.theme.StudioDarkCard
import com.example.ui.theme.StudioDarkCardBorder
import com.example.ui.theme.StudioGlassBorder
import com.example.ui.theme.StudioGlassFill
import com.example.ui.theme.StudioGoldLight
import com.example.ui.theme.StudioGoldWarm
import com.example.ui.theme.StudioMagentaGlow
import com.example.ui.theme.StudioPurpleVibrant
import com.example.ui.theme.StudioSubtleText
import com.example.ui.theme.StudioVioletAccent
import com.example.util.JarvisPermissionAndNetworkBanner
import com.example.util.JarvisPermissionsSnapshot
import com.example.util.LanguageDetectionHelper
import com.example.viewmodel.VoiceFeatureMode
import com.example.voice.VoiceHudState
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun CoreHudScreen(
    messages: List<ChatMessageEntity>,
    telemetry: DeviceTelemetry,
    permissionsSnapshot: JarvisPermissionsSnapshot,
    settings: JarvisSettings,
    hudState: VoiceHudState,
    audioLevel: Float,
    partialTranscript: String,
    voiceStatusMessage: String,
    activeVoiceEngineLabel: String,
    pendingConfirmation: PendingConfirmationAction?,
    attachedDocumentName: String?,
    followUpSuggestions: List<String>,
    errorBanner: String?,
    isVoiceChatMode: Boolean = false,
    isLiveConversationActive: Boolean = false,
    voiceFeatureMode: VoiceFeatureMode = VoiceFeatureMode.LISTEN_AND_SPEAK,
    lastHeardTranscript: String = "",
    visualTheme: JarvisVisualTheme = JarvisVisualTheme.OCEAN_CYBER,
    onSelectVisualTheme: (JarvisVisualTheme) -> Unit = {},
    onToggleVoiceChatMode: (Boolean) -> Unit = {},
    onStartLiveVoiceSession: () -> Unit = {},
    onStartListenOnlyFeature: () -> Unit = {},
    onStartListenAndSpeakFeature: () -> Unit = {},
    onToggleLiveConversation: (Boolean) -> Unit = {},
    onToggleWakeWord: (Boolean) -> Unit = {},
    onSelectVoicePersona: (String) -> Unit = {},
    onToggleContinuousBackgroundMic: (Boolean) -> Unit = {},
    onOpenDeviceControlTab: () -> Unit = {},
    onMicClick: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenNetworkSettings: () -> Unit,
    onOpenQuickSettingsTab: () -> Unit,
    onStopSpeaking: () -> Unit,
    onSendCommand: (String) -> Unit,
    onSpeakMessage: (String, String) -> Unit,
    onAttachDocumentClick: () -> Unit,
    onClearAttachedDocument: () -> Unit,
    onConfirmAction: () -> Unit,
    onCancelAction: () -> Unit,
    onCycleLanguageMode: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVoiceChatMode) {
        SmartVoiceAssistantScreen(
            messages = messages,
            settings = settings,
            hudState = hudState,
            audioLevel = audioLevel,
            partialTranscript = partialTranscript,
            voiceStatusMessage = voiceStatusMessage,
            isLiveConversationActive = isLiveConversationActive,
            voiceFeatureMode = voiceFeatureMode,
            lastHeardTranscript = lastHeardTranscript,
            pendingConfirmation = pendingConfirmation,
            attachedDocumentName = attachedDocumentName,
            followUpSuggestions = followUpSuggestions,
            errorBanner = errorBanner,
            onBackToHome = {
                onToggleVoiceChatMode(false)
            },
            onOpenSettings = onOpenQuickSettingsTab,
            onMicClick = onMicClick,
            onStartListenOnlyFeature = onStartListenOnlyFeature,
            onStartListenAndSpeakFeature = onStartListenAndSpeakFeature,
            onToggleLiveConversation = onToggleLiveConversation,
            onToggleWakeWord = onToggleWakeWord,
            onSelectVoicePersona = onSelectVoicePersona,
            onToggleContinuousBackgroundMic = onToggleContinuousBackgroundMic,
            onStopSpeaking = onStopSpeaking,
            onSendCommand = onSendCommand,
            onSpeakMessage = onSpeakMessage,
            onAttachDocumentClick = onAttachDocumentClick,
            onClearAttachedDocument = onClearAttachedDocument,
            onConfirmAction = onConfirmAction,
            onCancelAction = onCancelAction,
            onCycleLanguageMode = onCycleLanguageMode,
            onDismissError = onDismissError,
            modifier = modifier
        )
    } else {
        StudioHomeDashboard(
            messages = messages,
            telemetry = telemetry,
            permissionsSnapshot = permissionsSnapshot,
            settings = settings,
            hudState = hudState,
            partialTranscript = partialTranscript,
            lastHeardTranscript = lastHeardTranscript,
            voiceFeatureMode = voiceFeatureMode,
            visualTheme = visualTheme,
            onSelectVisualTheme = onSelectVisualTheme,
            pendingConfirmation = pendingConfirmation,
            errorBanner = errorBanner,
            onOpenVoiceStudio = {
                onToggleVoiceChatMode(true)
            },
            onStartVoiceImmediately = {
                onStartListenAndSpeakFeature()
            },
            onStartListenOnlyFeature = {
                onStartListenOnlyFeature()
            },
            onStartListenAndSpeakFeature = {
                onStartListenAndSpeakFeature()
            },
            onStopVoice = onMicClick,
            onToggleWakeWord = onToggleWakeWord,
            onSelectVoicePersona = onSelectVoicePersona,
            onToggleContinuousBackgroundMic = onToggleContinuousBackgroundMic,
            onOpenDeviceControl = onOpenDeviceControlTab,
            onOpenSettings = onOpenQuickSettingsTab,
            onRequestMicPermission = onRequestMicPermission,
            onOpenAppSettings = onOpenAppSettings,
            onOpenNetworkSettings = onOpenNetworkSettings,
            onSendCommand = onSendCommand,
            onSpeakMessage = onSpeakMessage,
            onConfirmAction = onConfirmAction,
            onCancelAction = onCancelAction,
            onCycleLanguageMode = onCycleLanguageMode,
            onDismissError = onDismissError,
            modifier = modifier
        )
    }
}

/**
 * Brand-New, Simple, Clean & Modern Home Screen UI ("নিউ স্টাইলে নতুনভাবে সিম্পল হোম স্ক্রিন"):
 * 1. Minimal Top Bar with Greeting, Battery, Language Toggle & Settings
 * 2. 1-Tap Live Theme Switcher Bar (Ocean Cyber, Emerald Green, Royal Indigo, Gold Onyx)
 * 3. Simple Central Voice Hub Card with Big Mic Button + 2 Direct Feature Buttons:
 *    - "🎙️ আমার কথা শুনবে" (Listen Only)
 *    - "🔊 কথা শুনে কথা বলবে" (Listen & Talk Back)
 * 4. Always-Visible Simple Type/Ask Box
 * 5. Clean 2x2 Quick Action Grid (Flashlight, API Key, Phone Control, Hey JARVIS)
 */
@Composable
private fun StudioHomeDashboard(
    messages: List<ChatMessageEntity>,
    telemetry: DeviceTelemetry,
    permissionsSnapshot: JarvisPermissionsSnapshot,
    settings: JarvisSettings,
    hudState: VoiceHudState,
    partialTranscript: String,
    lastHeardTranscript: String,
    voiceFeatureMode: VoiceFeatureMode,
    visualTheme: JarvisVisualTheme,
    onSelectVisualTheme: (JarvisVisualTheme) -> Unit,
    pendingConfirmation: PendingConfirmationAction?,
    errorBanner: String?,
    onOpenVoiceStudio: () -> Unit,
    onStartVoiceImmediately: () -> Unit,
    onStartListenOnlyFeature: () -> Unit,
    onStartListenAndSpeakFeature: () -> Unit,
    onStopVoice: () -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onSelectVoicePersona: (String) -> Unit,
    onToggleContinuousBackgroundMic: (Boolean) -> Unit,
    onOpenDeviceControl: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenNetworkSettings: () -> Unit,
    onSendCommand: (String) -> Unit,
    onSpeakMessage: (String, String) -> Unit,
    onConfirmAction: () -> Unit,
    onCancelAction: () -> Unit,
    onCycleLanguageMode: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var quickInputText by rememberSaveable { mutableStateOf("") }

    val activeKeyLabel = remember(settings.customGeminiApiKey, settings.secondaryAiApiKey) {
        val key = settings.customGeminiApiKey.ifBlank { settings.secondaryAiApiKey }
        if (key.isNotBlank()) {
            JarvisBrainService.detectKeyType(key).substringBefore(" API")
        } else {
            "Built-in AI"
        }
    }

    val langLabel = when (settings.languageMode) {
        "BN" -> "বাংলা"
        "EN" -> "EN"
        else -> "EN/বাংলা"
    }

    val latestJarvisReply = remember(messages) {
        messages.lastOrNull { it.role == "jarvis" }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Simple Clean Top Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(visualTheme.primaryAccent, visualTheme.secondaryAccent)
                                )
                            )
                            .clickable { onOpenSettings() }
                    ) {
                        Text(
                            text = settings.bossTitle.take(1).uppercase().ifBlank { "B" },
                            style = MaterialTheme.typography.titleMedium,
                            color = JarvisObsidian,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "হ্যালো, ${settings.bossTitle} 👋",
                            style = MaterialTheme.typography.titleMedium,
                            color = JarvisIceWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "JARVIS AI • 🔋 ${telemetry.batteryPercent}% • $activeKeyLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioSubtleText
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Language Chip
                    Surface(
                        color = visualTheme.cardBg,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, visualTheme.cardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onCycleLanguageMode() }
                            .testTag("language_mode_chip")
                    ) {
                        Text(
                            text = langLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = visualTheme.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    // Fullscreen Voice Studio Button
                    FrostedCircleIconButton(
                        icon = Icons.Filled.AutoAwesome,
                        contentDescription = "Open Full Voice Studio",
                        sizeDp = 40,
                        onClick = onOpenVoiceStudio,
                        testTag = "home_bell_button"
                    )
                }
            }
        }

        // 2. 1-Tap New Style Theme Switcher Bar ("🎨 থিম পরিবর্তন")
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                JarvisVisualTheme.entries.forEach { themeOption ->
                    val selected = themeOption.id == visualTheme.id
                    Surface(
                        color = if (selected) {
                            themeOption.primaryAccent.copy(alpha = 0.20f)
                        } else {
                            visualTheme.cardBg.copy(alpha = 0.75f)
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) themeOption.primaryAccent else visualTheme.cardBorder
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelectVisualTheme(themeOption) }
                            .testTag("theme_chip_${themeOption.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(themeOption.primaryAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = themeOption.titleBn,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) JarvisIceWhite else StudioSubtleText,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Permission & Network Alert Banner (only visible if mic or network is missing)
        item {
            JarvisPermissionAndNetworkBanner(
                snapshot = permissionsSnapshot,
                onGrantMicClick = onRequestMicPermission,
                onOpenAppSettingsClick = onOpenAppSettings,
                onOpenNetworkSettingsClick = onOpenNetworkSettings
            )
        }

        if (!errorBanner.isNullOrBlank()) {
            item {
                ErrorGlassBanner(errorBanner = errorBanner, onDismiss = onDismissError)
            }
        }
        if (pendingConfirmation != null) {
            item {
                SensitiveActionConfirmationCard(
                    pending = pendingConfirmation,
                    onConfirm = onConfirmAction,
                    onCancel = onCancelAction
                )
            }
        }

        // 3. Centerpiece Simple Voice Assistant Card (With Big Mic + 2 Main Voice Features right on Home!)
        item {
            val isMicActive = hudState == VoiceHudState.LISTENING || hudState == VoiceHudState.SPEAKING
            Surface(
                color = visualTheme.cardBg.copy(alpha = 0.92f),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.5.dp, visualTheme.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_studio_hero_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Badge Row
                    Surface(
                        color = visualTheme.primaryAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, visualTheme.primaryAccent.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = when (hudState) {
                                VoiceHudState.LISTENING -> if (voiceFeatureMode == VoiceFeatureMode.LISTEN_ONLY) {
                                    "🎙️ আপনার কথা শুনছি (Listen Only)…"
                                } else {
                                    "🔊 কথা শুনছি ও উত্তর বলব…"
                                }
                                VoiceHudState.PROCESSING -> "✨ JARVIS চিন্তা করছে…"
                                VoiceHudState.SPEAKING -> "🔊 JARVIS কথা বলছে…"
                                VoiceHudState.IDLE -> "✨ ভয়েস অ্যাসিস্ট্যান্ট প্রস্তুত"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = visualTheme.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Big Clean Center Microphone Button (Fixed size, never jumps)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(visualTheme.primaryAccent.copy(alpha = 0.16f))
                            .border(
                                width = if (isMicActive) 2.5.dp else 1.5.dp,
                                color = visualTheme.primaryAccent,
                                shape = CircleShape
                            )
                            .clickable {
                                if (isMicActive) {
                                    onStopVoice()
                                } else {
                                    onStartVoiceImmediately()
                                }
                            }
                            .testTag("pill_generate_voice")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(visualTheme.primaryAccent, visualTheme.secondaryAccent)
                                    )
                                )
                        ) {
                            Icon(
                                imageVector = if (isMicActive) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = if (isMicActive) "Stop Voice" else "Tap to Speak",
                                tint = JarvisObsidian,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fixed-height live speech / reply text preview inside the Home Hero Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val displayLine = when {
                            partialTranscript.isNotBlank() -> "শুনছি: “$partialTranscript”"
                            lastHeardTranscript.isNotBlank() && hudState == VoiceHudState.IDLE ->
                                "আপনি বলেছেন: “$lastHeardTranscript”"
                            latestJarvisReply != null -> latestJarvisReply.text
                            else -> "নিচের যেকোনো বাটনে ট্যাপ করে বাংলা বা ইংরেজিতে কথা বলুন"
                        }
                        Text(
                            text = displayLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisIceWhite.copy(alpha = 0.92f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // TWO DEDICATED SIMPLE FEATURE BUTTONS right inside the Hero Card:
                    // 1. "আমার কথা শুনবে" (Listen Only)
                    // 2. "কথা শুনে কথা বলবে" (Listen & Talk Back)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isListenOnlyActive =
                            voiceFeatureMode == VoiceFeatureMode.LISTEN_ONLY && hudState == VoiceHudState.LISTENING
                        val isListenSpeakActive =
                            voiceFeatureMode == VoiceFeatureMode.LISTEN_AND_SPEAK &&
                                (hudState == VoiceHudState.LISTENING || hudState == VoiceHudState.SPEAKING)

                        // Button 1: আমার কথা শুনবে
                        Surface(
                            color = if (isListenOnlyActive) {
                                visualTheme.primaryAccent.copy(alpha = 0.26f)
                            } else {
                                Color.White.copy(alpha = 0.06f)
                            },
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(
                                width = if (voiceFeatureMode == VoiceFeatureMode.LISTEN_ONLY) 1.5.dp else 1.dp,
                                color = if (voiceFeatureMode == VoiceFeatureMode.LISTEN_ONLY) {
                                    visualTheme.primaryAccent
                                } else {
                                    visualTheme.cardBorder
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onStartListenOnlyFeature() }
                                .testTag("card_feature_listen_only")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "আমার কথা শুনবে",
                                    tint = visualTheme.primaryAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "আমার কথা শুনবে",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = JarvisIceWhite,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "শুধু শুনবে ও লিখবে",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioSubtleText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Button 2: কথা শুনে কথা বলবে
                        Surface(
                            color = if (isListenSpeakActive) {
                                visualTheme.secondaryAccent.copy(alpha = 0.26f)
                            } else {
                                Color.White.copy(alpha = 0.06f)
                            },
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(
                                width = if (voiceFeatureMode == VoiceFeatureMode.LISTEN_AND_SPEAK) 1.5.dp else 1.dp,
                                color = if (voiceFeatureMode == VoiceFeatureMode.LISTEN_AND_SPEAK) {
                                    visualTheme.secondaryAccent
                                } else {
                                    visualTheme.cardBorder
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onStartListenAndSpeakFeature() }
                                .testTag("card_feature_listen_and_speak")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "কথা শুনে কথা বলবে",
                                    tint = visualTheme.secondaryAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "কথা শুনে কথা বলবে",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = JarvisIceWhite,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "শুনে ভয়েসে উত্তর বলবে",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioSubtleText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Continuous Background Mic Status Bar
                    Surface(
                        color = if (settings.continuousBackgroundMic) {
                            JarvisEmerald.copy(alpha = 0.16f)
                        } else {
                            Color.White.copy(alpha = 0.05f)
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (settings.continuousBackgroundMic) JarvisEmerald.copy(alpha = 0.6f) else visualTheme.cardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onToggleContinuousBackgroundMic(!settings.continuousBackgroundMic) }
                            .testTag("home_continuous_mic_toggle")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(if (settings.continuousBackgroundMic) JarvisEmerald else StudioSubtleText)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (settings.continuousBackgroundMic) {
                                        "নন-স্টপ ব্যাকগ্রাউন্ড মাইক: ON (মাঝখানে বন্ধ হবে না)"
                                    } else {
                                        "নন-স্টপ ব্যাকগ্রাউন্ড মাইক: OFF (ট্যাপ করে চালু করুন)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (settings.continuousBackgroundMic) JarvisEmerald else StudioSubtleText,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 6 REAL HUMAN VOICES (3 MALE 👨 + 3 FEMALE 👩) 1-TAP SELECTOR STRIP
                    val activePersona = findVoicePersonaById(settings.selectedVoicePersonaId)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.RecordVoiceOver,
                                contentDescription = null,
                                tint = visualTheme.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "রিয়েল মানুষের ভয়েস (Male / Female):",
                                style = MaterialTheme.typography.labelMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = activePersona.nameBn,
                            style = MaterialTheme.typography.labelSmall,
                            color = visualTheme.primaryAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        REAL_VOICE_PERSONAS.forEach { persona ->
                            val isSelected = persona.id == settings.selectedVoicePersonaId
                            val genderEmoji = if (persona.gender == VoiceGender.FEMALE) "👩" else "👨"
                            val accent = if (persona.gender == VoiceGender.FEMALE) {
                                visualTheme.secondaryAccent
                            } else {
                                visualTheme.primaryAccent
                            }
                            Surface(
                                color = if (isSelected) accent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) accent else visualTheme.cardBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSelectVoicePersona(persona.id) }
                                    .testTag("home_voice_persona_${persona.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = genderEmoji,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = persona.nameBn,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) JarvisIceWhite else StudioSubtleText,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = persona.subtitleBn,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) accent else StudioSubtleText.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Always-Visible Simple Type/Ask Input Box on Home Screen
        item {
            StudioQuickAskBar(
                value = quickInputText,
                onValueChange = { quickInputText = it },
                settings = settings,
                onSend = {
                    if (quickInputText.isNotBlank()) {
                        onSendCommand(quickInputText)
                        quickInputText = ""
                    }
                }
            )
        }

        // 5. Simple 2x2 Quick Control Grid (4 Clean Cards)
        item {
            Text(
                text = "কুইক কন্ট্রোল ও সেটিংস",
                style = MaterialTheme.typography.titleSmall,
                color = StudioSubtleText,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SimpleHomeControlTile(
                        icon = Icons.Filled.FlashlightOn,
                        title = if (telemetry.isFlashlightOn) "ফ্ল্যাশলাইট: ON" else "ফ্ল্যাশলাইট",
                        subtitle = "টর্চ লাইট চালু/বন্ধ",
                        accentColor = if (telemetry.isFlashlightOn) visualTheme.primaryAccent else StudioSubtleText,
                        cardBg = visualTheme.cardBg,
                        cardBorder = visualTheme.cardBorder,
                        onClick = {
                            onSendCommand(if (telemetry.isFlashlightOn) "Turn off flashlight" else "Turn on flashlight")
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "pill_flashlight"
                    )
                    SimpleHomeControlTile(
                        icon = Icons.Filled.Key,
                        title = "API Key সেটআপ",
                        subtitle = activeKeyLabel,
                        accentColor = visualTheme.primaryAccent,
                        cardBg = visualTheme.cardBg,
                        cardBorder = visualTheme.cardBorder,
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f),
                        testTag = "home_quick_settings_btn"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SimpleHomeControlTile(
                        icon = Icons.Filled.PhoneAndroid,
                        title = "ফোন কন্ট্রোল",
                        subtitle = "চার্জ ${telemetry.batteryPercent}% • অ্যাপস",
                        accentColor = visualTheme.secondaryAccent,
                        cardBg = visualTheme.cardBg,
                        cardBorder = visualTheme.cardBorder,
                        onClick = onOpenDeviceControl,
                        modifier = Modifier.weight(1f),
                        testTag = "card_phone_studio"
                    )
                    SimpleHomeControlTile(
                        icon = Icons.Filled.AutoAwesome,
                        title = if (settings.wakeWordEnabled) "Hey JARVIS: ON" else "Hey JARVIS: OFF",
                        subtitle = "ওয়েক ওয়ার্ড টগল",
                        accentColor = if (settings.wakeWordEnabled) visualTheme.primaryAccent else StudioSubtleText,
                        cardBg = visualTheme.cardBg,
                        cardBorder = visualTheme.cardBorder,
                        onClick = { onToggleWakeWord(!settings.wakeWordEnabled) },
                        modifier = Modifier.weight(1f),
                        testTag = "pill_wake_word"
                    )
                }
            }
        }

        // 6. Recent Replies (Clean & Compact)
        val recentItems = messages.takeLast(3).reversed()
        if (recentItems.isNotEmpty()) {
            item {
                Text(
                    text = "সাম্প্রতিক কথোপকথন (Recent Chats)",
                    style = MaterialTheme.typography.titleSmall,
                    color = StudioSubtleText,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(recentItems, key = { it.id }) { msg ->
                RecentConversationPillRow(
                    message = msg,
                    bossTitle = settings.bossTitle,
                    onSpeakClick = { onSpeakMessage(msg.text, msg.language) },
                    onOpenVoiceChat = onOpenVoiceStudio
                )
            }
        }
    }
}

@Composable
private fun SimpleHomeControlTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    cardBg: Color,
    cardBorder: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        color = cardBg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.16f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = JarvisIceWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioSubtleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SmartVoiceAssistantScreen(
    messages: List<ChatMessageEntity>,
    settings: JarvisSettings,
    hudState: VoiceHudState,
    audioLevel: Float,
    partialTranscript: String,
    voiceStatusMessage: String,
    isLiveConversationActive: Boolean,
    voiceFeatureMode: VoiceFeatureMode,
    lastHeardTranscript: String,
    pendingConfirmation: PendingConfirmationAction?,
    attachedDocumentName: String?,
    followUpSuggestions: List<String>,
    errorBanner: String?,
    onBackToHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onMicClick: () -> Unit,
    onStartListenOnlyFeature: () -> Unit,
    onStartListenAndSpeakFeature: () -> Unit,
    onToggleLiveConversation: (Boolean) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onSelectVoicePersona: (String) -> Unit,
    onToggleContinuousBackgroundMic: (Boolean) -> Unit,
    onStopSpeaking: () -> Unit,
    onSendCommand: (String) -> Unit,
    onSpeakMessage: (String, String) -> Unit,
    onAttachDocumentClick: () -> Unit,
    onClearAttachedDocument: () -> Unit,
    onConfirmAction: () -> Unit,
    onCancelAction: () -> Unit,
    onCycleLanguageMode: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showKeyboardDrawer by rememberSaveable { mutableStateOf(false) }
    var commandInput by rememberSaveable { mutableStateOf("") }

    val latestJarvisReply = remember(messages) {
        messages.lastOrNull { it.role == "jarvis" }
    }

    val activeUtteranceForDetection = commandInput.ifBlank { partialTranscript }
    val liveDetection = remember(activeUtteranceForDetection, settings.languageMode, settings.bossTitle) {
        LanguageDetectionHelper.detectUserLanguage(
            inputText = activeUtteranceForDetection,
            preferredLanguageMode = settings.languageMode,
            bossTitle = settings.bossTitle
        )
    }

    val activeKeyShort = remember(settings.customGeminiApiKey, settings.secondaryAiApiKey) {
        val key = settings.customGeminiApiKey.ifBlank { settings.secondaryAiApiKey }
        if (key.isNotBlank()) {
            JarvisBrainService.detectKeyType(key).substringBefore(" API")
        } else {
            "Your Assistant 3.0"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Bar: `<` Back, "Voice chat", `⋮` Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FrostedCircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Home",
                onClick = onBackToHome,
                testTag = "voice_chat_back_button"
            )

            Text(
                text = if (voiceFeatureMode == VoiceFeatureMode.LISTEN_ONLY) {
                    "Voice • আমার কথা শুনবে"
                } else {
                    "Voice • কথা শুনে কথা বলবে"
                },
                style = MaterialTheme.typography.titleMedium,
                color = JarvisIceWhite,
                fontWeight = FontWeight.SemiBold
            )

            FrostedCircleIconButton(
                icon = Icons.Filled.MoreVert,
                contentDescription = "API Keys & Settings",
                onClick = onOpenSettings,
                testTag = "voice_chat_settings_button"
            )
        }

        if (!errorBanner.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            ErrorGlassBanner(errorBanner = errorBanner, onDismiss = onDismissError)
        }

        if (pendingConfirmation != null) {
            Spacer(modifier = Modifier.height(8.dp))
            SensitiveActionConfirmationCard(
                pending = pendingConfirmation,
                onConfirm = onConfirmAction,
                onCancel = onCancelAction
            )
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // 2. Center 3D Iridescent Swirling AI Sphere
        JarvisIridescentVoiceOrb(
            hudState = hudState,
            audioLevel = audioLevel,
            onClick = onMicClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2B. NEW: Dual Feature Buttons ("১. আমার কথা শুনবে" | "২. কথা শুনে কথা বলবে")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isListenOnlySelected = voiceFeatureMode == VoiceFeatureMode.LISTEN_ONLY
            val isListenSpeakSelected = voiceFeatureMode == VoiceFeatureMode.LISTEN_AND_SPEAK

            Surface(
                color = if (isListenOnlySelected) JarvisCyan.copy(alpha = 0.22f) else StudioDarkCard.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (isListenOnlySelected) 1.5.dp else 1.dp,
                    color = if (isListenOnlySelected) JarvisCyan else StudioGlassBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onStartListenOnlyFeature() }
                    .testTag("mode_btn_listen_only")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        tint = if (isListenOnlySelected) JarvisCyan else StudioGoldLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "আমার কথা শুনবে",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisIceWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "শুধু শুনবে ও লিখবে",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioSubtleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Surface(
                color = if (isListenSpeakSelected) JarvisEmerald.copy(alpha = 0.22f) else StudioDarkCard.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (isListenSpeakSelected) 1.5.dp else 1.dp,
                    color = if (isListenSpeakSelected) JarvisEmerald else StudioGlassBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onStartListenAndSpeakFeature() }
                    .testTag("mode_btn_listen_and_speak")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = if (isListenSpeakSelected) JarvisEmerald else StudioGoldLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "কথা শুনে কথা বলবে",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisIceWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "শুনে ভয়েসে উত্তর বলবে",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioSubtleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Frosted Pill Badges: `[ ✨ LIVE VOICE ON ] [ "Hey JARVIS": ON ] [ EN / বাংলা ]`
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isLiveConversationActive) JarvisEmerald.copy(alpha = 0.22f) else StudioGlassFill,
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    1.dp,
                    if (isLiveConversationActive) JarvisEmerald else StudioGlassBorder
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onToggleLiveConversation(!isLiveConversationActive) }
                    .testTag("live_voice_mode_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        tint = if (isLiveConversationActive) JarvisEmerald else StudioGoldLight,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLiveConversationActive) "LIVE TALK: ON" else "LIVE TALK: PAUSED",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisIceWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                color = if (settings.wakeWordEnabled) StudioGoldLight.copy(alpha = 0.20f) else StudioGlassFill,
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    1.dp,
                    if (settings.wakeWordEnabled) StudioGoldLight else StudioGlassBorder
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onToggleWakeWord(!settings.wakeWordEnabled) }
                    .testTag("voice_chat_wake_word_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = StudioGoldLight,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (settings.wakeWordEnabled) "\"Hey JARVIS\": ON" else "\"Hey JARVIS\": OFF",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisIceWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                color = StudioGlassFill,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, StudioGlassBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onCycleLanguageMode() }
                    .testTag("assistant_badge_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val langCode = when (settings.languageMode) {
                        "BN" -> "বাংলা"
                        "EN" -> "EN"
                        else -> "EN/বাংলা"
                    }
                    Text(
                        text = "$activeKeyShort • $langCode",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisIceWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3B. 6 Real Male & Female Voice Persona Selector Row inside Live Voice Screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            REAL_VOICE_PERSONAS.forEach { persona ->
                val isSelected = persona.id == settings.selectedVoicePersonaId
                val genderEmoji = if (persona.gender == VoiceGender.FEMALE) "👩" else "👨"
                val accentColor = if (persona.gender == VoiceGender.FEMALE) StudioGoldLight else JarvisCyan
                Surface(
                    color = if (isSelected) accentColor.copy(alpha = 0.24f) else StudioGlassFill,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) accentColor else StudioGlassBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectVoicePersona(persona.id) }
                        .testTag("voice_chat_persona_${persona.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$genderEmoji ${persona.nameBn}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) JarvisIceWhite else StudioSubtleText,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Big Centered Heading: "Smart Voice Assistant" (Fixed 2-line height so layout never jumps)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (hudState) {
                    VoiceHudState.LISTENING -> if (voiceFeatureMode == VoiceFeatureMode.LISTEN_ONLY) {
                        "আপনার কথা শুনছি,\n${settings.bossTitle}…"
                    } else {
                        "কথা শুনছি ও উত্তর দেব,\n${settings.bossTitle}…"
                    }
                    VoiceHudState.PROCESSING -> "JARVIS চিন্তা করছে…"
                    VoiceHudState.SPEAKING -> "JARVIS কথা বলছে…"
                    VoiceHudState.IDLE -> if (voiceFeatureMode == VoiceFeatureMode.LISTEN_ONLY) {
                        "আমার কথা শুনবে\n(Listen Mode)"
                    } else {
                        "কথা শুনে কথা বলবে\n(Talk Back Mode)"
                    }
                },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = JarvisIceWhite,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5. Fixed-Height Subtitle / Live Transcript / Latest Spoken Response Box (Prevents vertical bouncing)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (partialTranscript.isNotBlank()) {
                    Text(
                        text = "শুনছি: “$partialTranscript”",
                        style = MaterialTheme.typography.bodyLarge,
                        color = StudioGoldLight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else if (latestJarvisReply != null) {
                    Surface(
                        color = StudioDarkCard.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, StudioDarkCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeakMessage(latestJarvisReply.text, latestJarvisReply.language) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (lastHeardTranscript.isNotBlank()) {
                                    Text(
                                        text = "আপনি বলেছেন: “$lastHeardTranscript”",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioGoldLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = latestJarvisReply.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = JarvisIceWhite.copy(alpha = 0.92f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Replay voice",
                                tint = StudioGoldLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = voiceStatusMessage.ifBlank {
                            "Tap the microphone button below to speak in বাংলা or English."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioSubtleText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                if (activeUtteranceForDetection.isNotBlank()) {
                    Text(
                        text = "Auto-Detected: ${liveDetection.language.nativeLabel} • TTS: ${liveDetection.ttsLocale.toLanguageTag()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioGoldLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Optional Expandable Keyboard Input Bar (when left Chat icon is tapped)
        AnimatedVisibility(visible = showKeyboardDrawer) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick suggestion chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    followUpSuggestions.forEachIndexed { idx, suggestion ->
                        Surface(
                            color = StudioDarkCard,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, StudioGlassBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onSendCommand(suggestion) }
                                .testTag("quick_chip_$idx")
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.labelMedium,
                                color = JarvisIceWhite,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (attachedDocumentName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StudioDarkCard, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, tint = StudioGoldLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(attachedDocumentName, style = MaterialTheme.typography.labelMedium, color = JarvisIceWhite, modifier = Modifier.weight(1f))
                        IconButton(onClick = onClearAttachedDocument, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = StudioSubtleText, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onAttachDocumentClick,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(StudioDarkCard)
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = "Attach", tint = JarvisIceWhite)
                    }

                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        placeholder = {
                            Text("Type in বাংলা or English…", color = StudioSubtleText, style = MaterialTheme.typography.bodySmall)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (commandInput.isNotBlank()) {
                                    onSendCommand(commandInput)
                                    commandInput = ""
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioGoldLight,
                            unfocusedBorderColor = StudioGlassBorder,
                            focusedContainerColor = StudioDarkCard,
                            unfocusedContainerColor = StudioDarkCard,
                            focusedTextColor = JarvisIceWhite,
                            unfocusedTextColor = JarvisIceWhite
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("command_input_field")
                    )

                    IconButton(
                        onClick = {
                            if (commandInput.isNotBlank()) {
                                onSendCommand(commandInput)
                                commandInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(StudioGoldLight, StudioVioletAccent))
                            )
                            .testTag("send_command_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = JarvisObsidian)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.8f))

        // 6. Bottom 3 Circular Controls Row (Matching Right Phone in Reference Image: Chat | Glowing Mic | Close/Stop)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 96.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Circular Frosted Chat Button
            FrostedCircleIconButton(
                icon = Icons.Filled.ChatBubbleOutline,
                contentDescription = "Toggle Keyboard Chat",
                sizeDp = 54,
                onClick = { showKeyboardDrawer = !showKeyboardDrawer },
                testTag = "voice_chat_keyboard_toggle"
            )

            // Center: Glowing Golden-to-Violet Microphone Button with Outer Translucent Halo
            val isMicOrVoiceActive = hudState == VoiceHudState.LISTENING || hudState == VoiceHudState.SPEAKING
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        if (hudState == VoiceHudState.LISTENING) {
                            JarvisEmerald.copy(alpha = 0.22f)
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        }
                    )
                    .border(
                        width = if (hudState == VoiceHudState.LISTENING) 2.dp else 1.dp,
                        color = if (hudState == VoiceHudState.LISTENING) {
                            JarvisEmerald
                        } else {
                            Color.White.copy(alpha = 0.16f)
                        },
                        shape = CircleShape
                    )
                    .clickable {
                        if (hudState == VoiceHudState.SPEAKING) {
                            onStopSpeaking()
                        } else {
                            onMicClick()
                        }
                    }
                    .testTag("voice_chat_center_mic_button")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = if (hudState == VoiceHudState.LISTENING) {
                                    listOf(JarvisEmerald, StudioGoldWarm, StudioGoldLight)
                                } else {
                                    listOf(StudioVioletAccent, StudioGoldWarm, StudioGoldLight)
                                }
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMicOrVoiceActive) {
                            Icons.Filled.Stop
                        } else {
                            Icons.Filled.Mic
                        },
                        contentDescription = if (isMicOrVoiceActive) "Stop Microphone / Voice" else "Speak to JARVIS",
                        tint = JarvisIceWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Right: Circular Frosted Cancel / Stop / Back Button
            FrostedCircleIconButton(
                icon = Icons.Filled.Close,
                contentDescription = "Stop or Exit Voice Chat",
                sizeDp = 54,
                onClick = {
                    if (hudState == VoiceHudState.SPEAKING) {
                        onStopSpeaking()
                    } else if (hudState == VoiceHudState.LISTENING) {
                        onMicClick()
                    } else {
                        onBackToHome()
                    }
                },
                testTag = "voice_chat_close_button"
            )
        }
    }
}

/**
 * Large Featured Purple-Magenta-Amber Waveform Card ("Voice Studio" in the center screen of the photo).
 */
@Composable
private fun VoiceStudioHeroCard(
    hudState: VoiceHudState,
    wakeWordEnabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() }
            .testTag("voice_studio_hero_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF5B189A),
                            Color(0xFF7B2CBF),
                            Color(0xFF9D4EDD),
                            Color(0xFFC77DFF)
                        )
                    )
                )
        ) {
            // Soundwave vertical bars artwork on the right side (matching the Voice Studio card in the photo)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barCount = 38
                val spacing = size.width / barCount
                for (i in 0 until barCount) {
                    val norm = i.toFloat() / barCount.toFloat()
                    val wave = abs(sin(norm * 6.2f)) * (0.25f + norm * 0.65f)
                    val barHeight = size.height * wave.coerceIn(0.15f, 0.85f)
                    val x = i * spacing + spacing / 2f
                    val barColor = when {
                        norm > 0.75f -> Color(0xFF38BDF8).copy(alpha = 0.65f)
                        norm > 0.50f -> Color(0xFFF59E0B).copy(alpha = 0.70f)
                        norm > 0.30f -> Color(0xFFEC4899).copy(alpha = 0.55f)
                        else -> Color.White.copy(alpha = 0.16f)
                    }
                    drawLine(
                        color = barColor,
                        start = Offset(x, size.height),
                        end = Offset(x, size.height - barHeight),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top-left frosted mic icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Voice Studio",
                            tint = JarvisIceWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Top-right frosted arrow icon (`↗`)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NorthEast,
                            contentDescription = "Open Voice Studio",
                            tint = JarvisIceWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = if (hudState == VoiceHudState.LISTENING) "Voice Studio • Listening…" else "Voice Studio",
                        style = MaterialTheme.typography.titleLarge,
                        color = JarvisIceWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (wakeWordEnabled) {
                            "Say \"Hey JARVIS\" or tap • বাংলা & English"
                        } else {
                            "Talk naturally in বাংলা & English"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisIceWhite.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

/**
 * Highlighted Voice Feature Card for the two requested features:
 * 1. "আমার কথা শুনবে" (Listen to My Speech)
 * 2. "কথা শুনে কথা বলবে" (Listen & Talk Back)
 */
@Composable
private fun StudioVoiceFeatureHighlightCard(
    icon: ImageVector,
    badgeText: String,
    title: String,
    subtitle: String,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        color = if (isActive) accentColor.copy(alpha = 0.22f) else StudioDarkCard,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) accentColor else accentColor.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .height(152.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.20f))
                        .border(1.dp, accentColor.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = if (isActive) "ACTIVE" else badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisIceWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioSubtleText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Square Dark Frosted Feature Card (matching "Image Studio" / "Video Studio" in the center screen of the photo).
 */
@Composable
private fun StudioSquareFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        color = StudioDarkCard,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, StudioDarkCardBorder),
        modifier = modifier
            .height(142.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = JarvisIceWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NorthEast,
                        contentDescription = "Open $title",
                        tint = JarvisIceWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisIceWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioSubtleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Pill-shaped Dark Glass Row matching "Recent Files" items in the center screen of the photo.
 */
@Composable
private fun RecentConversationPillRow(
    message: ChatMessageEntity,
    bossTitle: String,
    onSpeakClick: () -> Unit,
    onOpenVoiceChat: () -> Unit
) {
    val isUser = message.role == "user"
    Surface(
        color = StudioDarkCard,
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, StudioDarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .clickable {
                if (isUser) onOpenVoiceChat() else onSpeakClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isUser) Icons.Filled.Mic else Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = if (isUser) StudioGoldLight else StudioVioletAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isUser) "$bossTitle (${message.language.uppercase()})" else "JARVIS AI Response",
                    style = MaterialTheme.typography.titleSmall,
                    color = JarvisIceWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioSubtleText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                    .clickable { onSpeakClick() }
            ) {
                Icon(
                    imageVector = if (isUser) Icons.Filled.NorthEast else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Speak or Open",
                    tint = JarvisIceWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StudioQuickAskBar(
    value: String,
    onValueChange: (String) -> Unit,
    settings: JarvisSettings,
    onSend: () -> Unit
) {
    val detection = remember(value, settings.languageMode, settings.bossTitle) {
        LanguageDetectionHelper.detectUserLanguage(
            inputText = value,
            preferredLanguageMode = settings.languageMode,
            bossTitle = settings.bossTitle
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (value.isNotBlank()) {
            Text(
                text = "Auto-Detected: ${detection.language.nativeLabel} • TTS: ${detection.ttsLocale.toLanguageTag()}",
                style = MaterialTheme.typography.labelSmall,
                color = StudioGoldLight,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text("Ask JARVIS in বাংলা or English…", color = StudioSubtleText, style = MaterialTheme.typography.bodySmall)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioGoldLight,
                    unfocusedBorderColor = StudioGlassBorder,
                    focusedContainerColor = StudioDarkCard,
                    unfocusedContainerColor = StudioDarkCard,
                    focusedTextColor = JarvisIceWhite,
                    unfocusedTextColor = JarvisIceWhite
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(StudioGoldLight, StudioVioletAccent)))
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = JarvisObsidian)
            }
        }
    }
}

@Composable
fun FrostedCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    sizeDp: Int = 46,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(StudioGlassFill)
            .border(1.dp, StudioGlassBorder, CircleShape)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = JarvisIceWhite,
            modifier = Modifier.size((sizeDp * 0.44f).dp)
        )
    }
}

@Composable
private fun FrostedActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Surface(
        color = StudioGlassFill,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, StudioGlassBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = JarvisIceWhite,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = JarvisIceWhite
            )
        }
    }
}

@Composable
private fun ErrorGlassBanner(
    errorBanner: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = JarvisCrimson.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, JarvisCrimson),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = errorBanner,
                style = MaterialTheme.typography.bodySmall,
                color = JarvisIceWhite,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = JarvisIceWhite)
            }
        }
    }
}

@Composable
private fun SensitiveActionConfirmationCard(
    pending: PendingConfirmationAction,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        color = StudioDarkCard,
        border = BorderStroke(1.5.dp, StudioGoldLight),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sensitive_confirmation_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = StudioGoldLight
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm Action: ${pending.actionName.uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioGoldLight,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = pending.promptToUser,
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisIceWhite
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(1.dp, StudioGlassBorder),
                    modifier = Modifier.testTag("cancel_sensitive_action_button")
                ) {
                    Text("Cancel", color = JarvisIceWhite)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioGoldWarm,
                        contentColor = JarvisObsidian
                    ),
                    modifier = Modifier.testTag("confirm_sensitive_action_button")
                ) {
                    Text("Authorize", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
