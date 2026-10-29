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
import com.example.ui.components.JarvisIridescentVoiceOrb
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisIceWhite
import com.example.ui.theme.JarvisMutedCyan
import com.example.ui.theme.JarvisObsidian
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
    isLiveConversationActive: Boolean = true,
    onToggleVoiceChatMode: (Boolean) -> Unit = {},
    onStartLiveVoiceSession: () -> Unit = {},
    onToggleLiveConversation: (Boolean) -> Unit = {},
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
            pendingConfirmation = pendingConfirmation,
            attachedDocumentName = attachedDocumentName,
            followUpSuggestions = followUpSuggestions,
            errorBanner = errorBanner,
            onBackToHome = {
                onToggleLiveConversation(false)
                onToggleVoiceChatMode(false)
            },
            onOpenSettings = onOpenQuickSettingsTab,
            onMicClick = onMicClick,
            onToggleLiveConversation = onToggleLiveConversation,
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
            pendingConfirmation = pendingConfirmation,
            errorBanner = errorBanner,
            onOpenVoiceStudio = {
                onToggleVoiceChatMode(true)
                onStartLiveVoiceSession()
            },
            onStartVoiceImmediately = {
                onToggleVoiceChatMode(true)
                onStartLiveVoiceSession()
            },
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
 * Matches the CENTER screen in the user's reference photo:
 * - "Welcome 👋 Boss" header with circular frosted icons
 * - "Your AI Creative Journey Starts Up"
 * - Frosted horizontal action pills ("Generate Voice", "Flashlight", "Language", "API Key")
 * - "Start Creating" section with the purple-magenta-amber wave "Voice Studio" hero card +
 *   2 side-by-side square dark cards ("Phone Studio" & "API Key Studio")
 * - "Recent Files / Recent AI Chats" pill list with circular arrow/speaker icons
 */
@Composable
private fun StudioHomeDashboard(
    messages: List<ChatMessageEntity>,
    telemetry: DeviceTelemetry,
    permissionsSnapshot: JarvisPermissionsSnapshot,
    settings: JarvisSettings,
    hudState: VoiceHudState,
    pendingConfirmation: PendingConfirmationAction?,
    errorBanner: String?,
    onOpenVoiceStudio: () -> Unit,
    onStartVoiceImmediately: () -> Unit,
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
    var showQuickInputBar by rememberSaveable { mutableStateOf(false) }
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
        "BN" -> "বাংলা (BN)"
        "EN" -> "English (EN)"
        else -> "Auto (EN/বাংলা)"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Welcome Header Row (Avatar + "Welcome 👋" + "Boss" + Frosted BellIcon)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Glowing Avatar Circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(StudioGoldLight, StudioVioletAccent, StudioPurpleVibrant)
                                )
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
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
                            text = "Welcome 👋",
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisIceWhite.copy(alpha = 0.78f)
                        )
                        Text(
                            text = "${settings.bossTitle} • JARVIS AI",
                            style = MaterialTheme.typography.titleMedium,
                            color = JarvisIceWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Right Frosted Circular Button (Notifications / Telemetry)
                FrostedCircleIconButton(
                    icon = Icons.Filled.NotificationsNone,
                    contentDescription = "System Status & Telemetry",
                    onClick = onOpenDeviceControl,
                    testTag = "home_bell_button"
                )
            }
        }

        // 2. Hero Title + Right Frosted Search/Input Toggle Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Your AI Creative\nJourney Starts Up",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 27.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = JarvisIceWhite,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                FrostedCircleIconButton(
                    icon = Icons.Filled.Search,
                    contentDescription = "Quick Ask JARVIS",
                    onClick = { showQuickInputBar = !showQuickInputBar },
                    testTag = "home_search_button"
                )
            }
        }

        // Optional Expandable Quick Text Input Bar when Search button is tapped
        item {
            AnimatedVisibility(visible = showQuickInputBar) {
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

        // Error Banner or Sensitive Action Confirmation
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

        // 3. Horizontal Frosted Pills Row ("Generate Voice", "API Key", "Language", "Flashlight")
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FrostedActionPill(
                    icon = Icons.Filled.Mic,
                    label = if (hudState == VoiceHudState.LISTENING) "Listening…" else "Generate Voice",
                    onClick = onStartVoiceImmediately,
                    testTag = "pill_generate_voice"
                )
                FrostedActionPill(
                    icon = Icons.Filled.Key,
                    label = activeKeyLabel,
                    onClick = onOpenSettings,
                    testTag = "pill_api_keys"
                )
                FrostedActionPill(
                    icon = Icons.Filled.Language,
                    label = langLabel,
                    onClick = onCycleLanguageMode,
                    testTag = "language_mode_chip"
                )
                FrostedActionPill(
                    icon = Icons.Filled.FlashlightOn,
                    label = if (telemetry.isFlashlightOn) "Torch ON" else "Flashlight",
                    onClick = {
                        onSendCommand(if (telemetry.isFlashlightOn) "Turn off flashlight" else "Turn on flashlight")
                    },
                    testTag = "pill_flashlight"
                )
            }
        }

        // 4. "Start Creating" Section Header
        item {
            Text(
                text = "Start Creating",
                style = MaterialTheme.typography.titleMedium,
                color = JarvisIceWhite,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 5. Large Featured Gradient Card: "Voice Studio" (with purple-pink-amber vertical waveform)
        item {
            VoiceStudioHeroCard(
                hudState = hudState,
                onClick = onOpenVoiceStudio
            )
        }

        // 6. Two Side-by-Side Square Dark Cards ("Phone Studio" & "API Key Studio")
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StudioSquareFeatureCard(
                    icon = Icons.Filled.PhoneAndroid,
                    title = "Phone Studio",
                    subtitle = "Battery ${telemetry.batteryPercent}% • Apps & Control",
                    onClick = onOpenDeviceControl,
                    modifier = Modifier.weight(1f),
                    testTag = "card_phone_studio"
                )
                StudioSquareFeatureCard(
                    icon = Icons.Filled.Key,
                    title = "API Key Studio",
                    subtitle = "Gemini, Groq & All Keys",
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f),
                    testTag = "home_quick_settings_btn"
                )
            }
        }

        // 7. "Recent Files / Recent AI Chats" Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Chats & Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisIceWhite,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Voice Chat",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioSubtleText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenVoiceStudio() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // 8. Recent Messages / Quick Commands Pill Rows (matching "Recent Files" in the photo)
        val recentItems = messages.takeLast(5).reversed()
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

/**
 * Matches the RIGHT screen in the user's reference photo ("Voice chat" / "Smart Voice Assistant"):
 * - Top bar: Circular `<` back button, "Voice chat" title, Circular `⋮` settings button
 * - Center: 3D Iridescent Swirling AI Sphere (`JarvisIridescentVoiceOrb`)
 * - Frosted pill badge: `[ ✨ Your Assistant 3.0 ]`
 * - Big centered title: "Smart Voice Assistant" + live spoken reply / transcript
 * - Bottom 3 circular buttons: `[ 💬 Chat ]`  `[ 🎙️ Glowing Gold-Purple Mic ]`  `[ ✕ Stop/Close ]`
 */
@Composable
private fun SmartVoiceAssistantScreen(
    messages: List<ChatMessageEntity>,
    settings: JarvisSettings,
    hudState: VoiceHudState,
    audioLevel: Float,
    partialTranscript: String,
    voiceStatusMessage: String,
    isLiveConversationActive: Boolean,
    pendingConfirmation: PendingConfirmationAction?,
    attachedDocumentName: String?,
    followUpSuggestions: List<String>,
    errorBanner: String?,
    onBackToHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onMicClick: () -> Unit,
    onToggleLiveConversation: (Boolean) -> Unit,
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
                text = "Voice chat • Live",
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

        Spacer(modifier = Modifier.weight(0.6f))

        // 2. Center 3D Iridescent Swirling AI Sphere
        JarvisIridescentVoiceOrb(
            hudState = hudState,
            audioLevel = audioLevel,
            onClick = onMicClick
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Frosted Pill Badge: `[ ✨ LIVE VOICE ON • EN / বাংলা ]`
        Row(
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
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        tint = if (isLiveConversationActive) JarvisEmerald else StudioGoldLight,
                        modifier = Modifier.size(16.dp)
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
                color = StudioGlassFill,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, StudioGlassBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onCycleLanguageMode() }
                    .testTag("assistant_badge_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = StudioGoldLight,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Big Centered Heading: "Smart Voice Assistant"
        Text(
            text = when (hudState) {
                VoiceHudState.LISTENING -> "Listening to You,\n${settings.bossTitle}…"
                VoiceHudState.PROCESSING -> "JARVIS is\nThinking…"
                VoiceHudState.SPEAKING -> "JARVIS is\nSpeaking…"
                VoiceHudState.IDLE -> "Smart Voice\nAssistant"
            },
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold
            ),
            color = JarvisIceWhite,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Subtitle or Live Transcript / Latest Spoken Response
        if (partialTranscript.isNotBlank()) {
            Text(
                text = "“$partialTranscript”",
                style = MaterialTheme.typography.bodyLarge,
                color = StudioGoldLight,
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = latestJarvisReply.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisIceWhite.copy(alpha = 0.92f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
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
                    "Effortlessly chat with your AI bot, enjoying instant and seamless voice interactions."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = StudioSubtleText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Live Auto-Detected Language & TTS Locale Pill when speaking or typing
        AnimatedVisibility(visible = activeUtteranceForDetection.isNotBlank()) {
            Text(
                text = "Auto-Detected: ${liveDetection.language.nativeLabel} • TTS: ${liveDetection.ttsLocale.toLanguageTag()}",
                style = MaterialTheme.typography.labelSmall,
                color = StudioGoldLight,
                modifier = Modifier.padding(top = 6.dp)
            )
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
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
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
                                colors = listOf(
                                    StudioVioletAccent,
                                    StudioGoldWarm,
                                    StudioGoldLight
                                )
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (hudState == VoiceHudState.SPEAKING) {
                            Icons.Filled.Stop
                        } else {
                            Icons.Filled.Mic
                        },
                        contentDescription = if (hudState == VoiceHudState.SPEAKING) "Stop Speaking" else "Speak to JARVIS",
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
                    if (hudState == VoiceHudState.SPEAKING || hudState == VoiceHudState.LISTENING) {
                        onStopSpeaking()
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
                        text = "Talk naturally in বাংলা & English",
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisIceWhite.copy(alpha = 0.85f)
                    )
                }
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
