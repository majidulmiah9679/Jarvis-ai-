package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.JarvisNotificationListenerService
import com.example.ui.screens.CoreHudScreen
import com.example.ui.screens.DeviceControlScreen
import com.example.ui.screens.MemoryCoreScreen
import com.example.ui.screens.SystemStatusScreen
import com.example.ui.screens.VoiceLabScreen
import com.example.ui.theme.AuroraDarkPlum
import com.example.ui.theme.AuroraObsidianBottom
import com.example.ui.theme.AuroraPeachTop
import com.example.ui.theme.AuroraRoseMid
import com.example.ui.theme.AuroraVioletDeep
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisIceWhite
import com.example.ui.theme.JarvisMutedCyan
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudioDarkCard
import com.example.ui.theme.StudioGlassBorder
import com.example.ui.theme.StudioGoldLight
import com.example.ui.theme.StudioGoldWarm
import com.example.ui.theme.StudioVioletAccent
import com.example.util.configureAppCheck
import com.example.util.rememberJarvisPermissionsController
import com.example.viewmodel.JarvisViewModel
import com.example.voice.VoiceHudState

enum class JarvisTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HUD("hud", "HOME", Icons.Filled.RadioButtonChecked),
    CONTROL("control", "CONTROL", Icons.Filled.Tune),
    VOICE("voice", "VOICE LAB", Icons.Filled.GraphicEq),
    MEMORY("memory", "MEMORY", Icons.Filled.Memory),
    SYSTEM("system", "SETTINGS", Icons.Filled.Security)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                JarvisAppRoot()
            }
        }
    }
}

@Composable
fun JarvisAppRoot(
    jarvisViewModel: JarvisViewModel = viewModel()
) {
    var currentTab by rememberSaveable { mutableStateOf(JarvisTab.HUD) }
    var isVoiceChatMode by rememberSaveable { mutableStateOf(false) }

    val messages by jarvisViewModel.messages.collectAsStateWithLifecycle()
    val memories by jarvisViewModel.memories.collectAsStateWithLifecycle()
    val actionLogs by jarvisViewModel.actionLogs.collectAsStateWithLifecycle()
    val settings by jarvisViewModel.settings.collectAsStateWithLifecycle()
    val telemetry by jarvisViewModel.telemetry.collectAsStateWithLifecycle()
    val installedApps by jarvisViewModel.installedApps.collectAsStateWithLifecycle()
    val hudState by jarvisViewModel.hudState.collectAsStateWithLifecycle()
    val audioLevel by jarvisViewModel.audioLevel.collectAsStateWithLifecycle()
    val partialTranscript by jarvisViewModel.partialTranscript.collectAsStateWithLifecycle()
    val voiceStatusMessage by jarvisViewModel.voiceStatusMessage.collectAsStateWithLifecycle()
    val activeVoiceEngineLabel by jarvisViewModel.activeVoiceEngineLabel.collectAsStateWithLifecycle()
    val ttsDiagnostics by jarvisViewModel.ttsDiagnostics.collectAsStateWithLifecycle()
    val pendingConfirmation by jarvisViewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val attachedDocumentName by jarvisViewModel.attachedDocumentName.collectAsStateWithLifecycle()
    val followUpSuggestions by jarvisViewModel.followUpSuggestions.collectAsStateWithLifecycle()
    val errorBanner by jarvisViewModel.errorBanner.collectAsStateWithLifecycle()
    val isForegroundServiceRunning by jarvisViewModel.isForegroundServiceRunning.collectAsStateWithLifecycle()
    val isLiveConversationActive by jarvisViewModel.isLiveConversationActive.collectAsStateWithLifecycle()
    val activeNotifications by JarvisNotificationListenerService.notifications.collectAsStateWithLifecycle()

    // Dedicated Android Permissions & Network Access Handler
    val permissionsController = rememberJarvisPermissionsController(
        onPermissionsUpdated = {
            jarvisViewModel.refreshTelemetryNow()
        }
    )

    // BackHandler for secondary tabs or Voice Chat mode to return cleanly to Home Dashboard
    BackHandler(enabled = currentTab != JarvisTab.HUD || isVoiceChatMode) {
        if (currentTab != JarvisTab.HUD) {
            currentTab = JarvisTab.HUD
        } else {
            isVoiceChatMode = false
        }
    }

    // Android Document Picker Launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            jarvisViewModel.attachDocumentFromUri(uri)
        }
    }

    val handleMicAction: () -> Unit = {
        if (hudState == VoiceHudState.LISTENING) {
            jarvisViewModel.stopVoiceListening()
        } else {
            permissionsController.requestMicPermission {
                permissionsController.verifyNetworkOrPrompt {
                    jarvisViewModel.startVoiceListening()
                }
            }
        }
    }

    val handleStartLiveVoiceSession: () -> Unit = {
        permissionsController.requestMicPermission {
            permissionsController.verifyNetworkOrPrompt {
                jarvisViewModel.startLiveVoiceGreetingAndListen()
            }
        }
    }

    val handleToggleLiveConversation: (Boolean) -> Unit = { enabled ->
        if (enabled) {
            permissionsController.requestMicPermission {
                permissionsController.verifyNetworkOrPrompt {
                    jarvisViewModel.setLiveConversationMode(true)
                    jarvisViewModel.startVoiceListening()
                }
            }
        } else {
            jarvisViewModel.setLiveConversationMode(false)
        }
    }

    val handleToggleHandsFree: (Boolean) -> Unit = { enable ->
        if (enable) {
            permissionsController.requestVoiceSuitePermissions {
                jarvisViewModel.toggleHandsFreeService(true)
            }
        } else {
            jarvisViewModel.toggleHandsFreeService(false)
        }
    }

    val hasConfiguredElevenLabsKey = jarvisViewModel.voiceManager.resolveElevenLabsApiKey(settings).isNotBlank()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to AuroraPeachTop,
                        0.18f to AuroraRoseMid,
                        0.38f to AuroraVioletDeep,
                        0.68f to AuroraDarkPlum,
                        1.0f to AuroraObsidianBottom
                    )
                )
            )
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = JarvisSurfaceCard.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            JarvisTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        currentTab = tab
                                        if (tab != JarvisTab.HUD) isVoiceChatMode = false
                                    },
                                    icon = {
                                        Icon(imageVector = tab.icon, contentDescription = tab.label)
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = JarvisObsidian,
                                        selectedTextColor = StudioGoldLight,
                                        indicatorColor = StudioGoldLight,
                                        unselectedIconColor = JarvisMutedCyan,
                                        unselectedTextColor = JarvisMutedCyan
                                    ),
                                    modifier = Modifier.testTag("nav_rail_${tab.route}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when (currentTab) {
                            JarvisTab.HUD -> CoreHudScreen(
                                messages = messages,
                                telemetry = telemetry,
                                permissionsSnapshot = permissionsController.snapshot,
                                settings = settings,
                                hudState = hudState,
                                audioLevel = audioLevel,
                                partialTranscript = partialTranscript,
                                voiceStatusMessage = voiceStatusMessage,
                                activeVoiceEngineLabel = activeVoiceEngineLabel,
                                pendingConfirmation = pendingConfirmation,
                                attachedDocumentName = attachedDocumentName,
                                followUpSuggestions = followUpSuggestions,
                                errorBanner = errorBanner,
                                isVoiceChatMode = isVoiceChatMode,
                                isLiveConversationActive = isLiveConversationActive,
                                onToggleVoiceChatMode = { isVoiceChatMode = it },
                                onStartLiveVoiceSession = handleStartLiveVoiceSession,
                                onToggleLiveConversation = handleToggleLiveConversation,
                                onOpenDeviceControlTab = { currentTab = JarvisTab.CONTROL },
                                onMicClick = handleMicAction,
                                onRequestMicPermission = {
                                    permissionsController.requestMicPermission {
                                        jarvisViewModel.startVoiceListening()
                                    }
                                },
                                onOpenAppSettings = { permissionsController.openAppPermissionSettings() },
                                onOpenNetworkSettings = { permissionsController.openWifiSettings() },
                                onOpenQuickSettingsTab = { currentTab = JarvisTab.SYSTEM },
                                onStopSpeaking = { jarvisViewModel.stopSpeaking() },
                                onSendCommand = { cmd ->
                                    permissionsController.verifyNetworkOrPrompt {
                                        jarvisViewModel.sendCommand(cmd)
                                    }
                                },
                                onSpeakMessage = { text, lang -> jarvisViewModel.speakCustomText(text, lang) },
                                onAttachDocumentClick = {
                                    documentPickerLauncher.launch(arrayOf("text/*", "application/json", "*/*"))
                                },
                                onClearAttachedDocument = { jarvisViewModel.clearAttachedDocument() },
                                onConfirmAction = { jarvisViewModel.confirmPendingAction() },
                                onCancelAction = { jarvisViewModel.cancelPendingAction() },
                                onCycleLanguageMode = {
                                    val next = when (settings.languageMode) {
                                        "AUTO" -> "BN"
                                        "BN" -> "EN"
                                        else -> "AUTO"
                                    }
                                    jarvisViewModel.updateLanguageMode(next)
                                },
                                onDismissError = { jarvisViewModel.dismissErrorBanner() }
                            )

                            JarvisTab.CONTROL -> DeviceControlScreen(
                                telemetry = telemetry,
                                installedApps = installedApps,
                                actionLogs = actionLogs,
                                activeNotifications = activeNotifications,
                                onTriggerAction = { action, target, secondary ->
                                    jarvisViewModel.triggerDirectAction(action, target, secondary)
                                },
                                onClearActionLogs = { jarvisViewModel.clearActionLogs() }
                            )

                            JarvisTab.VOICE -> VoiceLabScreen(
                                settings = settings,
                                hudState = hudState,
                                voiceStatusMessage = voiceStatusMessage,
                                activeVoiceEngineLabel = activeVoiceEngineLabel,
                                ttsDiagnostics = ttsDiagnostics,
                                hasConfiguredElevenLabsKey = hasConfiguredElevenLabsKey,
                                isForegroundServiceRunning = isForegroundServiceRunning,
                                onUpdateTtsEngine = { jarvisViewModel.updateTtsEngine(it) },
                                onUpdateElevenLabsVoiceId = { jarvisViewModel.updateElevenLabsVoiceId(it) },
                                onUpdateCustomElevenLabsKey = { jarvisViewModel.updateCustomElevenLabsKey(it) },
                                onUpdateSpeechRate = { jarvisViewModel.updateSpeechRate(it) },
                                onUpdateSpeechPitch = { jarvisViewModel.updateSpeechPitch(it) },
                                onUpdateLanguageMode = { jarvisViewModel.updateLanguageMode(it) },
                                onUpdateAutoSpeak = { jarvisViewModel.updateAutoSpeak(it) },
                                onToggleHandsFreeService = handleToggleHandsFree,
                                onTestSpeak = { text, lang -> jarvisViewModel.speakCustomText(text, lang) },
                                onTestAndroidTtsFallback = { text, lang ->
                                    jarvisViewModel.speakWithAndroidTtsFallback(text, lang)
                                },
                                onOpenTtsSettings = { jarvisViewModel.openAndroidTtsSettings() },
                                onInstallTtsData = { jarvisViewModel.installAndroidTtsVoiceData() },
                                onRefreshTtsLocales = { jarvisViewModel.refreshTtsLocales() },
                                onStopSpeaking = { jarvisViewModel.stopSpeaking() }
                            )

                            JarvisTab.MEMORY -> MemoryCoreScreen(
                                memories = memories,
                                settings = settings,
                                messageCount = messages.size,
                                actionLogCount = actionLogs.size,
                                onAddMemory = { cat, fact -> jarvisViewModel.addManualMemory(cat, fact) },
                                onDeleteMemory = { id -> jarvisViewModel.deleteMemory(id) },
                                onUpdateBossTitle = { title -> jarvisViewModel.updateBossTitle(title) },
                                onUpdateConfirmSensitive = { jarvisViewModel.updateConfirmSensitiveActions(it) },
                                onClearConversationHistory = { jarvisViewModel.clearConversationHistory() },
                                onWipeAllData = { jarvisViewModel.wipeAllStoredData() }
                            )

                            JarvisTab.SYSTEM -> SystemStatusScreen(
                                telemetry = telemetry,
                                permissionsSnapshot = permissionsController.snapshot,
                                settings = settings,
                                hasConfiguredElevenLabsKey = hasConfiguredElevenLabsKey,
                                onRequestMicPermission = {
                                    permissionsController.requestMicPermission(null)
                                },
                                onRequestCameraPermission = {
                                    permissionsController.requestCameraPermission()
                                },
                                onOpenAppSettings = {
                                    permissionsController.openAppPermissionSettings()
                                },
                                onOpenWifiSettings = {
                                    permissionsController.openWifiSettings()
                                },
                                onRefreshPermissions = {
                                    permissionsController.refreshNow()
                                },
                                onOpenSettingsSection = { section ->
                                    jarvisViewModel.triggerDirectAction("open_settings", section, "")
                                },
                                onSelectGeminiModel = { model ->
                                    jarvisViewModel.updateGeminiModel(model)
                                },
                                onSaveUniversalAiConfig = { primaryKey, secondaryKey, providerMode, baseUrl, modelName, maxTokens, elevenKey ->
                                    jarvisViewModel.saveUniversalAiConfig(
                                        primaryKey = primaryKey,
                                        secondaryKey = secondaryKey,
                                        providerMode = providerMode,
                                        customBaseUrl = baseUrl,
                                        customModelName = modelName,
                                        maxTokens = maxTokens,
                                        elevenLabsKey = elevenKey
                                    )
                                },
                                onUpdateLanguageMode = { mode ->
                                    jarvisViewModel.updateLanguageMode(mode)
                                },
                                onUpdateAutoSpeak = { enabled ->
                                    jarvisViewModel.updateAutoSpeak(enabled)
                                },
                                onTestAiBrain = {
                                    currentTab = JarvisTab.HUD
                                    isVoiceChatMode = true
                                    jarvisViewModel.sendCommand("হ্যালো জারভিস, তোমার এআই ব্রেইন এবং সিস্টেম স্ট্যাটাস জানাও।")
                                }
                            )
                        }
                    }
                }

                // Floating 3-Icon Pill Bottom Bar (matching the center screen in the reference photo)
                if (!isWideScreen) {
                    FloatingStudioBottomPill(
                        isHomeActive = currentTab == JarvisTab.HUD && !isVoiceChatMode,
                        isVoiceStudioActive = currentTab == JarvisTab.HUD && isVoiceChatMode,
                        isSettingsActive = currentTab == JarvisTab.SYSTEM,
                        onSelectHome = {
                            currentTab = JarvisTab.HUD
                            isVoiceChatMode = false
                        },
                        onSelectVoiceStudio = {
                            currentTab = JarvisTab.HUD
                            isVoiceChatMode = true
                            handleStartLiveVoiceSession()
                        },
                        onSelectSettings = {
                            currentTab = JarvisTab.SYSTEM
                            isVoiceChatMode = false
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Floating 3-Button Glassmorphic Pill Navigation Bar matching the bottom dock in the user's photo:
 * - Left: Home (`🏠`)
 * - Middle: Voice Chat / Studio (`🎙️`)
 * - Right: Settings & API Keys (`⚙️`)
 */
@Composable
private fun FloatingStudioBottomPill(
    isHomeActive: Boolean,
    isVoiceStudioActive: Boolean,
    isSettingsActive: Boolean,
    onSelectHome: () -> Unit,
    onSelectVoiceStudio: () -> Unit,
    onSelectSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioDarkCard.copy(alpha = 0.90f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        tonalElevation = 12.dp,
        shadowElevation = 12.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingPillNavIcon(
                icon = Icons.Filled.Home,
                contentDescription = "Home",
                selected = isHomeActive,
                onClick = onSelectHome,
                testTag = "nav_tab_hud"
            )
            FloatingPillNavIcon(
                icon = Icons.Filled.GraphicEq,
                contentDescription = "Voice Studio",
                selected = isVoiceStudioActive,
                onClick = onSelectVoiceStudio,
                testTag = "nav_tab_voice"
            )
            FloatingPillNavIcon(
                icon = Icons.Filled.Settings,
                contentDescription = "Settings & API Keys",
                selected = isSettingsActive,
                onClick = onSelectSettings,
                testTag = "nav_tab_system"
            )
        }
    }
}

@Composable
private fun FloatingPillNavIcon(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val activeBrush = Brush.verticalGradient(
        colors = listOf(
            StudioVioletAccent,
            StudioGoldWarm,
            StudioGoldLight
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(activeBrush)
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.06f))
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.White.copy(alpha = 0.45f) else StudioGlassBorder,
                shape = CircleShape
            )
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = JarvisIceWhite,
            modifier = Modifier.size(22.dp)
        )
    }
}
