package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisIceWhite
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisVisualTheme
import com.example.ui.theme.StudioDarkCard
import com.example.ui.theme.StudioDarkCardBorder
import com.example.ui.theme.StudioGlassBorder
import com.example.ui.theme.StudioGlassFill
import com.example.ui.theme.StudioGoldLight
import com.example.ui.theme.StudioGoldWarm
import com.example.ui.theme.StudioSubtleText
import com.example.util.JarvisPermissionAndNetworkBanner
import com.example.util.JarvisPermissionsSnapshot
import com.example.util.LanguageDetectionHelper
import com.example.viewmodel.VoiceFeatureMode
import com.example.voice.VoiceHudState

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
            visualTheme = visualTheme,
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
 * Simple Native Android System Style Home Screen ("সিম্পল ভাবে এন্ড্রয়েড সিস্টেমে বানাও একদম সিম্পল"):
 * - Clean flat Material 3 cards, standard Android switches, simple clear buttons, zero visual clutter.
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
            JarvisBrainService.detectKeyType(key).substringBefore(" (")
        } else {
            "Built-in AI"
        }
    }

    val langLabel = when (settings.languageMode) {
        "BN" -> "বাংলা"
        "EN" -> "English"
        else -> "বাংলা + EN"
    }

    val latestJarvisReply = remember(messages) {
        messages.lastOrNull { it.role == "jarvis" }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Simple Native Android Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "JARVIS AI",
                        style = MaterialTheme.typography.headlineSmall,
                        color = JarvisIceWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "হ্যালো ${settings.bossTitle} • চার্জ ${telemetry.batteryPercent}% • $activeKeyLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioSubtleText
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = visualTheme.cardBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, visualTheme.cardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCycleLanguageMode() }
                            .testTag("language_mode_chip")
                    ) {
                        Text(
                            text = langLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = visualTheme.primaryAccent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(visualTheme.cardBg)
                            .testTag("home_bell_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "সেটিংস",
                            tint = JarvisIceWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Simple Android System Theme Selector Row
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
                        color = if (selected) themeOption.primaryAccent else visualTheme.cardBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) themeOption.primaryAccent else visualTheme.cardBorder
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectVisualTheme(themeOption) }
                            .testTag("theme_chip_${themeOption.id}")
                    ) {
                        Text(
                            text = themeOption.titleBn,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) JarvisObsidian else JarvisIceWhite,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Permission & Network Alert Banner (only visible if mic permission is missing)
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

        // 3. Simple Android System STT + TTS Voice Card
        item {
            val isMicActive = hudState == VoiceHudState.LISTENING || hudState == VoiceHudState.SPEAKING
            Surface(
                color = visualTheme.cardBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, visualTheme.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_studio_hero_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // STT & TTS Dual Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sttActive = hudState == VoiceHudState.LISTENING
                        val ttsActive = hudState == VoiceHudState.SPEAKING

                        Surface(
                            color = if (sttActive) JarvisEmerald.copy(alpha = 0.18f) else visualTheme.topBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (sttActive) JarvisEmerald else visualTheme.cardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (sttActive) onStopVoice() else onStartVoiceImmediately()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "STT",
                                    tint = if (sttActive) JarvisEmerald else visualTheme.primaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (sttActive) "STT: কথা শুনছে…" else "STT: কথা শোনা প্রস্তুত",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (sttActive) JarvisEmerald else JarvisIceWhite,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            color = if (ttsActive) visualTheme.primaryAccent.copy(alpha = 0.18f) else visualTheme.topBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (ttsActive) visualTheme.primaryAccent else visualTheme.cardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (ttsActive) {
                                        onStopVoice()
                                    } else {
                                        val sample = latestJarvisReply?.text
                                            ?: "জি বস, আমার এসটিটি এবং টিটিএস ভয়েস সিস্টেম একদম প্রস্তুত আছে।"
                                        onSpeakMessage(sample, "bn")
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "TTS",
                                    tint = visualTheme.primaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (ttsActive) "TTS: কথা বলছে…" else "TTS: কথা বলা প্রস্তুত",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (ttsActive) visualTheme.primaryAccent else JarvisIceWhite,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simple Status Text
                    Text(
                        text = when (hudState) {
                            VoiceHudState.LISTENING -> if (voiceFeatureMode == VoiceFeatureMode.LISTEN_ONLY) {
                                "🎙️ STT চালু: আপনার কথা শুনছি…"
                            } else {
                                "🎙️ STT চালু: কথা শুনছি ও TTS দিয়ে উত্তর বলব…"
                            }
                            VoiceHudState.PROCESSING -> "✨ উত্তর তৈরি হচ্ছে…"
                            VoiceHudState.SPEAKING -> "🔊 TTS চালু: JARVIS কথা বলছে (শেষ হলেই STT শুনবে)"
                            VoiceHudState.IDLE -> "STT ও TTS প্রস্তুত (নিচের মাইকে ট্যাপ করে কথা বলুন)"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isMicActive) JarvisEmerald else visualTheme.primaryAccent,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Clean Flat Android System Round Mic Button (Fixed 76.dp, never bounces)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                if (isMicActive) JarvisEmerald else visualTheme.primaryAccent
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
                        Icon(
                            imageVector = if (isMicActive) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (isMicActive) "Stop Voice" else "Tap to Speak",
                            tint = JarvisObsidian,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fixed-height live speech / reply preview box
                    Surface(
                        color = visualTheme.topBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, visualTheme.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp)
                        ) {
                            val displayLine = when {
                                partialTranscript.isNotBlank() -> "STT শুনছে: “$partialTranscript”"
                                lastHeardTranscript.isNotBlank() && hudState == VoiceHudState.IDLE ->
                                    "STT শুনেছে: “$lastHeardTranscript”"
                                latestJarvisReply != null -> latestJarvisReply.text
                                else -> "বাংলা বা ইংরেজিতে যেকোনো কথা বলুন"
                            }
                            Text(
                                text = displayLine,
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisIceWhite,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // TWO DEDICATED STT & TTS MODE BUTTONS:
                    // 1. "STT (কথা শুনবে)" (Listen Only)
                    // 2. "STT + TTS (শুনে বলবে)" (Listen & Talk Back)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isListenOnly = voiceFeatureMode == VoiceFeatureMode.LISTEN_ONLY
                        val isListenSpeak = voiceFeatureMode == VoiceFeatureMode.LISTEN_AND_SPEAK

                        OutlinedButton(
                            onClick = onStartListenOnlyFeature,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = if (isListenOnly) 1.5.dp else 1.dp,
                                color = if (isListenOnly) visualTheme.primaryAccent else visualTheme.cardBorder
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isListenOnly) {
                                    visualTheme.primaryAccent.copy(alpha = 0.15f)
                                } else {
                                    Color.Transparent
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_feature_listen_only")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = null,
                                tint = visualTheme.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "STT (কথা শুনবে)",
                                style = MaterialTheme.typography.labelMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onStartListenAndSpeakFeature,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListenSpeak) visualTheme.primaryAccent else visualTheme.cardBorder,
                                contentColor = if (isListenSpeak) JarvisObsidian else JarvisIceWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_feature_listen_and_speak")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "STT + TTS (বলবে)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = visualTheme.cardBorder)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Native Android System Switch Row: Continuous Background Mic
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleContinuousBackgroundMic(!settings.continuousBackgroundMic) }
                            .testTag("home_continuous_mic_toggle"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "নন-স্টপ ব্যাকগ্রাউন্ড মাইক",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (settings.continuousBackgroundMic) {
                                    "চালু আছে — কথা বলার মাঝেও শুনবে, বন্ধ হবে না"
                                } else {
                                    "বন্ধ আছে — ট্যাপ করে চালু করুন"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (settings.continuousBackgroundMic) JarvisEmerald else StudioSubtleText
                            )
                        }
                        Switch(
                            checked = settings.continuousBackgroundMic,
                            onCheckedChange = { onToggleContinuousBackgroundMic(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = JarvisEmerald)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = visualTheme.cardBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Simple Voice Persona Selector Row (6 Real Male & Female Voices)
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
                                text = "ভয়েস সিলেক্ট করুন (Male / Female):",
                                style = MaterialTheme.typography.labelMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.SemiBold
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
                            Surface(
                                color = if (isSelected) visualTheme.primaryAccent else visualTheme.topBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) visualTheme.primaryAccent else visualTheme.cardBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectVoicePersona(persona.id) }
                                    .testTag("home_voice_persona_${persona.id}")
                            ) {
                                Text(
                                    text = "$genderEmoji ${persona.nameBn}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) JarvisObsidian else JarvisIceWhite,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Simple Text Input Box on Home Screen
        item {
            StudioQuickAskBar(
                value = quickInputText,
                onValueChange = { quickInputText = it },
                settings = settings,
                visualTheme = visualTheme,
                onSend = {
                    if (quickInputText.isNotBlank()) {
                        onSendCommand(quickInputText)
                        quickInputText = ""
                    }
                }
            )
        }

        // 5. Simple 2x2 Android System Settings / Control Tiles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SimpleHomeControlTile(
                        icon = Icons.Filled.FlashlightOn,
                        title = if (telemetry.isFlashlightOn) "ফ্ল্যাশলাইট: ON" else "ফ্ল্যাশলাইট",
                        subtitle = "টর্চ লাইট চালু/বন্ধ",
                        accentColor = if (telemetry.isFlashlightOn) JarvisEmerald else visualTheme.primaryAccent,
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
                        title = "API Key সেটিংস",
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SimpleHomeControlTile(
                        icon = Icons.Filled.PhoneAndroid,
                        title = "ফোন কন্ট্রোল",
                        subtitle = "চার্জ ${telemetry.batteryPercent}% • অ্যাপস",
                        accentColor = visualTheme.primaryAccent,
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
                        accentColor = if (settings.wakeWordEnabled) JarvisEmerald else StudioSubtleText,
                        cardBg = visualTheme.cardBg,
                        cardBorder = visualTheme.cardBorder,
                        onClick = { onToggleWakeWord(!settings.wakeWordEnabled) },
                        modifier = Modifier.weight(1f),
                        testTag = "pill_wake_word"
                    )
                }
            }
        }

        // 6. Recent Replies (Simple Android List Items)
        val recentItems = messages.takeLast(3).reversed()
        if (recentItems.isNotEmpty()) {
            item {
                Text(
                    text = "সাম্প্রতিক কথোপকথন",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioSubtleText,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(recentItems, key = { it.id }) { msg ->
                RecentConversationPillRow(
                    message = msg,
                    bossTitle = settings.bossTitle,
                    visualTheme = visualTheme,
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
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisIceWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
    visualTheme: JarvisVisualTheme,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Simple Android Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(visualTheme.cardBg)
                    .testTag("voice_chat_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = JarvisIceWhite
                )
            }

            Text(
                text = if (voiceFeatureMode == VoiceFeatureMode.LISTEN_ONLY) {
                    "ভয়েস • আমার কথা শুনবে"
                } else {
                    "ভয়েস • কথা শুনে কথা বলবে"
                },
                style = MaterialTheme.typography.titleMedium,
                color = JarvisIceWhite,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(visualTheme.cardBg)
                    .testTag("voice_chat_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = JarvisIceWhite
                )
            }
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

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Simple Android Voice Mode Switch Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isListenOnlySelected = voiceFeatureMode == VoiceFeatureMode.LISTEN_ONLY
            val isListenSpeakSelected = voiceFeatureMode == VoiceFeatureMode.LISTEN_AND_SPEAK

            OutlinedButton(
                onClick = onStartListenOnlyFeature,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (isListenOnlySelected) visualTheme.primaryAccent else visualTheme.cardBorder
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isListenOnlySelected) {
                        visualTheme.primaryAccent.copy(alpha = 0.15f)
                    } else {
                        visualTheme.cardBg
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("mode_btn_listen_only")
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = visualTheme.primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "আমার কথা শুনবে",
                    color = JarvisIceWhite,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onStartListenAndSpeakFeature,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListenSpeakSelected) visualTheme.primaryAccent else visualTheme.cardBg,
                    contentColor = if (isListenSpeakSelected) JarvisObsidian else JarvisIceWhite
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("mode_btn_listen_and_speak")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "কথা শুনে বলবে",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Simple Voice Persona Selector Strip
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
                Surface(
                    color = if (isSelected) visualTheme.primaryAccent else visualTheme.cardBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) visualTheme.primaryAccent else visualTheme.cardBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectVoicePersona(persona.id) }
                        .testTag("voice_chat_persona_${persona.id}")
                ) {
                    Text(
                        text = "$genderEmoji ${persona.nameBn}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) JarvisObsidian else JarvisIceWhite,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // 4. Simple Clean Android System Status Card
        Surface(
            color = visualTheme.cardBg,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, visualTheme.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (hudState) {
                        VoiceHudState.LISTENING -> if (voiceFeatureMode == VoiceFeatureMode.LISTEN_ONLY) {
                            "আপনার কথা শুনছি, ${settings.bossTitle}…"
                        } else {
                            "কথা শুনছি ও উত্তর দেব, ${settings.bossTitle}…"
                        }
                        VoiceHudState.PROCESSING -> "JARVIS চিন্তা করছে… (🎙️ মাইক চালু আছে)"
                        VoiceHudState.SPEAKING -> "JARVIS কথা বলছে… (🎙️ মাইক চালু — মাঝে বললেও শুনবে)"
                        VoiceHudState.IDLE -> "মাইক্রোফোন প্রস্তুত"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hudState == VoiceHudState.LISTENING || hudState == VoiceHudState.SPEAKING) {
                        JarvisEmerald
                    } else {
                        visualTheme.primaryAccent
                    },
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (partialTranscript.isNotBlank()) {
                    Text(
                        text = "শুনছি: “$partialTranscript”",
                        style = MaterialTheme.typography.bodyLarge,
                        color = JarvisIceWhite,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                } else if (latestJarvisReply != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeakMessage(latestJarvisReply.text, latestJarvisReply.language) }
                    ) {
                        if (lastHeardTranscript.isNotBlank()) {
                            Text(
                                text = "আপনি বলেছেন: “$lastHeardTranscript”",
                                style = MaterialTheme.typography.labelMedium,
                                color = visualTheme.primaryAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = latestJarvisReply.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisIceWhite,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = voiceStatusMessage.ifBlank {
                            "নিচের মাইক বাটনে ট্যাপ করে বাংলা বা ইংরেজিতে কথা বলুন।"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioSubtleText,
                        textAlign = TextAlign.Center
                    )
                }

                if (activeUtteranceForDetection.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ভাষা: ${liveDetection.language.nativeLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioSubtleText
                    )
                }
            }
        }

        // Optional Expandable Keyboard Input Bar
        AnimatedVisibility(visible = showKeyboardDrawer) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    followUpSuggestions.forEachIndexed { idx, suggestion ->
                        Surface(
                            color = visualTheme.cardBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, visualTheme.cardBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSendCommand(suggestion) }
                                .testTag("quick_chip_$idx")
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.labelMedium,
                                color = JarvisIceWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (attachedDocumentName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(visualTheme.cardBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, tint = visualTheme.primaryAccent, modifier = Modifier.size(16.dp))
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(visualTheme.cardBg)
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = "Attach", tint = JarvisIceWhite)
                    }

                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        placeholder = {
                            Text("বাংলা বা ইংরেজিতে লিখুন…", color = StudioSubtleText, style = MaterialTheme.typography.bodySmall)
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
                            focusedBorderColor = visualTheme.primaryAccent,
                            unfocusedBorderColor = visualTheme.cardBorder,
                            focusedContainerColor = visualTheme.cardBg,
                            unfocusedContainerColor = visualTheme.cardBg,
                            focusedTextColor = JarvisIceWhite,
                            unfocusedTextColor = JarvisIceWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(visualTheme.primaryAccent)
                            .testTag("send_command_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = JarvisObsidian)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // 5. Simple Bottom Control Row (Keyboard | Solid Mic | Close)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FrostedCircleIconButton(
                icon = Icons.Filled.ChatBubbleOutline,
                contentDescription = "Toggle Keyboard Chat",
                sizeDp = 50,
                onClick = { showKeyboardDrawer = !showKeyboardDrawer },
                testTag = "voice_chat_keyboard_toggle"
            )

            val isMicOrVoiceActive = hudState == VoiceHudState.LISTENING || hudState == VoiceHudState.SPEAKING
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMicOrVoiceActive) JarvisEmerald else visualTheme.primaryAccent
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
                Icon(
                    imageVector = if (isMicOrVoiceActive) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isMicOrVoiceActive) "Stop Microphone / Voice" else "Speak to JARVIS",
                    tint = JarvisObsidian,
                    modifier = Modifier.size(32.dp)
                )
            }

            FrostedCircleIconButton(
                icon = Icons.Filled.Close,
                contentDescription = "Stop or Exit Voice Chat",
                sizeDp = 50,
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

@Composable
private fun RecentConversationPillRow(
    message: ChatMessageEntity,
    bossTitle: String,
    visualTheme: JarvisVisualTheme,
    onSpeakClick: () -> Unit,
    onOpenVoiceChat: () -> Unit
) {
    val isUser = message.role == "user"
    Surface(
        color = visualTheme.cardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, visualTheme.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (isUser) onOpenVoiceChat() else onSpeakClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isUser) Icons.Filled.Mic else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = if (isUser) visualTheme.primaryAccent else JarvisEmerald,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isUser) "$bossTitle" else "JARVIS AI",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUser) visualTheme.primaryAccent else JarvisEmerald,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisIceWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
    visualTheme: JarvisVisualTheme,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text("এখানে লিখে প্রশ্ন করুন (বাংলা / English)…", color = StudioSubtleText, style = MaterialTheme.typography.bodySmall)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = visualTheme.primaryAccent,
                unfocusedBorderColor = visualTheme.cardBorder,
                focusedContainerColor = visualTheme.cardBg,
                unfocusedContainerColor = visualTheme.cardBg,
                focusedTextColor = JarvisIceWhite,
                unfocusedTextColor = JarvisIceWhite
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(visualTheme.primaryAccent)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = JarvisObsidian)
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
            .background(StudioDarkCard)
            .border(1.dp, StudioDarkCardBorder, CircleShape)
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
private fun ErrorGlassBanner(
    errorBanner: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = JarvisCrimson.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, JarvisCrimson),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
        border = BorderStroke(1.dp, StudioGoldLight),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sensitive_confirmation_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = StudioGoldLight
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm Action: ${pending.actionName.uppercase()}",
                    style = MaterialTheme.typography.titleSmall,
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
            Spacer(modifier = Modifier.height(10.dp))
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
                Spacer(modifier = Modifier.width(8.dp))
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
