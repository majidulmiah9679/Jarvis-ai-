package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.actions.DeviceTelemetry
import com.example.ai.JarvisBrainService
import com.example.data.JarvisSettings
import com.example.data.MemoryEntity
import com.example.data.REAL_VOICE_PERSONAS
import com.example.data.VoiceGender
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisIceWhite
import com.example.ui.theme.JarvisMutedCyan
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.StudioDarkCard
import com.example.ui.theme.StudioDarkCardBorder
import com.example.ui.theme.StudioGlassBorder
import com.example.ui.theme.StudioGlassFill
import com.example.ui.theme.StudioGoldLight
import com.example.ui.theme.StudioGoldWarm
import com.example.ui.theme.StudioSubtleText
import com.example.ui.theme.StudioVioletAccent
import com.example.util.JarvisPermissionsSnapshot
import com.example.util.NetworkAndMicDiagnosticsCard

@Composable
fun MemoryCoreScreen(
    memories: List<MemoryEntity>,
    settings: JarvisSettings,
    messageCount: Int,
    actionLogCount: Int,
    onAddMemory: (String, String) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onUpdateBossTitle: (String) -> Unit,
    onUpdateConfirmSensitive: (Boolean) -> Unit,
    onClearConversationHistory: () -> Unit,
    onWipeAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var newMemoryCategory by rememberSaveable { mutableStateOf("PREFERENCE") }
    var newMemoryFact by rememberSaveable { mutableStateOf("") }
    var bossTitleInput by rememberSaveable(settings.bossTitle) { mutableStateOf(settings.bossTitle) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "MEMORY & PRIVACY",
                style = MaterialTheme.typography.headlineMedium,
                color = JarvisCyan
            )
            Text(
                text = "Save personal facts for JARVIS to remember or clear your chat history.",
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisMutedCyan
            )
        }

        // 1. Add Persistent Memory Card
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Memory, contentDescription = null, tint = JarvisCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ADD MEMORY FOR JARVIS",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PREFERENCE", "FACT", "CONTACT", "ROUTINE").forEach { cat ->
                            val selected = newMemoryCategory == cat
                            Surface(
                                color = if (selected) JarvisCyan else JarvisObsidian,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, JarvisCyan),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { newMemoryCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) JarvisObsidian else JarvisIceWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newMemoryFact,
                            onValueChange = { newMemoryFact = it },
                            placeholder = { Text("e.g. Call me Boss, I live in Dhaka") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisCyan.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_memory_input")
                        )
                        Button(
                            onClick = {
                                if (newMemoryFact.isNotBlank()) {
                                    onAddMemory(newMemoryCategory, newMemoryFact)
                                    newMemoryFact = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = JarvisObsidian
                            ),
                            modifier = Modifier.testTag("add_memory_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Save Memory")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SAVE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Saved Memories List
        item {
            Text(
                text = "SAVED MEMORIES (${memories.size})",
                style = MaterialTheme.typography.labelLarge,
                color = JarvisCyan
            )
        }

        if (memories.isEmpty()) {
            item {
                Surface(
                    color = JarvisSurfaceCard.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No memories saved yet. Tell JARVIS 'Remember that...' or add one above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisMutedCyan,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(memories, key = { it.id }) { mem ->
                Surface(
                    color = JarvisSurfaceCard,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, JarvisElectricBlue.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "[${mem.category}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = mem.fact,
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisIceWhite
                            )
                        }
                        IconButton(onClick = { onDeleteMemory(mem.id) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Memory",
                                tint = JarvisCrimson
                            )
                        }
                    }
                }
            }
        }

        // 3. Personalization & Privacy Controls
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = JarvisCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NAME & DATA RESET",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bossTitleInput,
                            onValueChange = { bossTitleInput = it },
                            label = { Text("Your Name / Title (Default: Boss)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onUpdateBossTitle(bossTitleInput) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisElectricBlue,
                                contentColor = JarvisIceWhite
                            )
                        ) {
                            Text("SAVE")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Confirm Before Call / SMS",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ask permission before opening Dialer or SMS.",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisMutedCyan
                            )
                        }
                        Switch(
                            checked = settings.confirmSensitiveActions,
                            onCheckedChange = onUpdateConfirmSensitive,
                            colors = SwitchDefaults.colors(checkedThumbColor = JarvisEmerald)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClearConversationHistory,
                            border = BorderStroke(1.dp, JarvisAmber),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_history_button")
                        ) {
                            Text("Clear Chat ($messageCount)", color = JarvisAmber)
                        }
                        Button(
                            onClick = onWipeAllData,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCrimson,
                                contentColor = JarvisIceWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wipe_all_data_button")
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wipe All ($actionLogCount)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemStatusScreen(
    telemetry: DeviceTelemetry,
    permissionsSnapshot: JarvisPermissionsSnapshot,
    settings: JarvisSettings,
    hasConfiguredElevenLabsKey: Boolean,
    onRequestMicPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onOpenSettingsSection: (String) -> Unit,
    onSelectGeminiModel: (String) -> Unit,
    onSaveUniversalAiConfig: (
        primaryKey: String,
        secondaryKey: String,
        providerMode: String,
        customBaseUrl: String,
        customModelName: String,
        maxTokens: Int,
        elevenLabsKey: String
    ) -> Unit,
    onUpdateLanguageMode: (String) -> Unit,
    onUpdateAutoSpeak: (Boolean) -> Unit,
    onUpdateWakeWord: (Boolean) -> Unit = {},
    onSelectVoicePersona: (String) -> Unit = {},
    onUpdateContinuousBackgroundMic: (Boolean) -> Unit = {},
    onTestAiBrain: () -> Unit,
    onValidateApiKey: (suspend (apiKey: String, providerMode: String, customModel: String, customBaseUrl: String) -> Result<String>)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var primaryKeyInput by rememberSaveable(settings.customGeminiApiKey) {
        mutableStateOf(settings.customGeminiApiKey)
    }
    var secondaryKeyInput by rememberSaveable(settings.secondaryAiApiKey) {
        mutableStateOf(settings.secondaryAiApiKey)
    }
    var providerModeInput by rememberSaveable(settings.aiProviderMode) {
        mutableStateOf(settings.aiProviderMode)
    }
    var customBaseUrlInput by rememberSaveable(settings.customBaseUrl) {
        mutableStateOf(settings.customBaseUrl)
    }
    var customModelInput by rememberSaveable(settings.customModelName) {
        mutableStateOf(settings.customModelName)
    }
    var maxTokensInput by rememberSaveable(settings.maxOutputTokens) {
        mutableIntStateOf(settings.maxOutputTokens)
    }
    var elevenLabsKeyInput by rememberSaveable(settings.customElevenLabsKey) {
        mutableStateOf(settings.customElevenLabsKey)
    }

    var showPrimaryKey by rememberSaveable { mutableStateOf(false) }
    var showAdvancedOptions by rememberSaveable { mutableStateOf(false) }
    var statusConfirmation by rememberSaveable { mutableStateOf("") }

    // Live validation states: null = not tested, true = valid (GREEN), false = invalid (RED)
    var keyValidationStatus by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var keyValidationMessage by rememberSaveable { mutableStateOf("") }
    var isValidatingKey by rememberSaveable { mutableStateOf(false) }

    val detectedKeyLabel = JarvisBrainService.detectKeyType(primaryKeyInput)

    // Auto-save & auto-verify whenever the user pastes or edits an API key
    LaunchedEffect(primaryKeyInput, providerModeInput) {
        val clean = primaryKeyInput.trim()
        if (clean.isEmpty()) {
            keyValidationStatus = null
            keyValidationMessage = ""
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(450L)
        onSaveUniversalAiConfig(
            primaryKeyInput,
            secondaryKeyInput,
            providerModeInput,
            customBaseUrlInput,
            customModelInput,
            maxTokensInput,
            elevenLabsKeyInput
        )
        if (clean.length >= 16) {
            isValidatingKey = true
            val testResult = if (onValidateApiKey != null) {
                onValidateApiKey(
                    primaryKeyInput,
                    providerModeInput,
                    customModelInput,
                    customBaseUrlInput
                )
            } else {
                JarvisBrainService().testApiKeyOrToken(
                    apiKey = primaryKeyInput,
                    providerMode = providerModeInput,
                    customModel = customModelInput,
                    customBaseUrl = customBaseUrlInput
                )
            }
            isValidatingKey = false
            if (testResult.isSuccess) {
                keyValidationStatus = true
                keyValidationMessage = "🟢 ${testResult.getOrNull() ?: "API Key সঠিক ও সম্পূর্ণ সক্রিয়!"}"
            } else {
                keyValidationStatus = false
                val err = testResult.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                keyValidationMessage = "🔴 ভুল API Key বা সংযোগে ত্রুটি: $err"
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "সেটিংস ও API Key",
                style = MaterialTheme.typography.headlineSmall,
                color = JarvisIceWhite,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "সিম্পল অ্যান্ড্রয়েড সেটিংস — Gemini (AQ. / AIza), ভয়েস ও মাইক কন্ট্রোল",
                style = MaterialTheme.typography.bodySmall,
                color = StudioSubtleText
            )
        }

        // 1. SIMPLE ANDROID SYSTEM API KEY CARD
        item {
            Surface(
                color = StudioDarkCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StudioDarkCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_api_key_setup_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Key, contentDescription = null, tint = StudioGoldLight, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "১. AI API Key সেটআপ",
                                style = MaterialTheme.typography.titleMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = StudioGlassFill,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, StudioGlassBorder)
                        ) {
                            Text(
                                text = detectedKeyLabel.substringBefore(" ("),
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioGoldLight,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Provider Mode Chips (Simple Rectangular Android Chips)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "AUTO" to "Auto-Detect",
                            "GEMINI" to "Gemini",
                            "GROQ" to "Groq",
                            "OPENAI" to "OpenAI",
                            "OPENROUTER" to "OpenRouter",
                            "FIREBASE" to "Built-in Free"
                        ).forEach { (code, label) ->
                            val selected = providerModeInput == code
                            Surface(
                                color = if (selected) StudioGoldLight else StudioGlassFill,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selected) StudioGoldLight else StudioGlassBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { providerModeInput = code }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) JarvisObsidian else JarvisIceWhite,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Main API Key Input with Live Green/Red Status
                    val borderColor = when (keyValidationStatus) {
                        true -> JarvisEmerald
                        false -> JarvisCrimson
                        null -> StudioGoldLight
                    }

                    OutlinedTextField(
                        value = primaryKeyInput,
                        onValueChange = {
                            primaryKeyInput = it
                            statusConfirmation = ""
                            keyValidationStatus = null
                            keyValidationMessage = ""
                        },
                        label = {
                            Text(
                                text = when (keyValidationStatus) {
                                    true -> "Primary API Key (✅ সঠিক ও সক্রিয়)"
                                    false -> "Primary API Key (❌ ভুল বা মেয়াদ শেষ)"
                                    null -> "Primary API Key (AQ...., AIza..., gsk_..., sk-...)"
                                }
                            )
                        },
                        placeholder = { Text("এখানে আপনার API Key পেস্ট করুন") },
                        singleLine = true,
                        visualTransformation = if (showPrimaryKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (keyValidationStatus == true) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Valid Key",
                                        tint = JarvisEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else if (keyValidationStatus == false) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "Invalid Key",
                                        tint = JarvisCrimson,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = { showPrimaryKey = !showPrimaryKey }) {
                                    Icon(
                                        imageVector = if (showPrimaryKey) {
                                            Icons.Filled.VisibilityOff
                                        } else {
                                            Icons.Filled.Visibility
                                        },
                                        contentDescription = "Toggle Key Visibility",
                                        tint = StudioGoldLight
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = when (keyValidationStatus) {
                                true -> JarvisEmerald
                                false -> JarvisCrimson
                                null -> StudioGlassBorder
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_gemini_api_key_input")
                    )

                    // Live Status Banner right below Key Input: Green for Valid, Red for Invalid
                    if (keyValidationMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = if (keyValidationStatus == true) {
                                JarvisEmerald.copy(alpha = 0.15f)
                            } else {
                                JarvisCrimson.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (keyValidationStatus == true) JarvisEmerald else JarvisCrimson
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (keyValidationStatus == true) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = if (keyValidationStatus == true) JarvisEmerald else JarvisCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = keyValidationMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (keyValidationStatus == true) JarvisEmerald else JarvisCrimson,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Optional Expandable Advanced Fields (Backup Key, ElevenLabs Key, Model Name)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showAdvancedOptions = !showAdvancedOptions }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (showAdvancedOptions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = StudioGoldLight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "অতিরিক্ত অপশন (Backup Key, ElevenLabs, Model Name)",
                            style = MaterialTheme.typography.labelMedium,
                            color = StudioGoldLight
                        )
                    }

                    AnimatedVisibility(visible = showAdvancedOptions) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = secondaryKeyInput,
                                onValueChange = {
                                    secondaryKeyInput = it
                                    statusConfirmation = ""
                                },
                                label = { Text("Backup / 2nd API Key (Optional)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("secondary_ai_key_input")
                            )

                            OutlinedTextField(
                                value = elevenLabsKeyInput,
                                onValueChange = {
                                    elevenLabsKeyInput = it
                                    statusConfirmation = ""
                                },
                                label = { Text("ElevenLabs Voice Key (Optional)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_elevenlabs_key_input")
                            )

                            OutlinedTextField(
                                value = customModelInput,
                                onValueChange = { customModelInput = it },
                                label = { Text("Custom Model ID (e.g. gemini-2.5-flash)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = customBaseUrlInput,
                                onValueChange = { customBaseUrlInput = it },
                                label = { Text("Custom Endpoint URL") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (statusConfirmation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = statusConfirmation,
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 Clean Android System Buttons: Save | Check Key (Green/Red) | Test AI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveUniversalAiConfig(
                                    primaryKeyInput,
                                    secondaryKeyInput,
                                    providerModeInput,
                                    customBaseUrlInput,
                                    customModelInput,
                                    maxTokensInput,
                                    elevenLabsKeyInput
                                )
                                statusConfirmation = "✅ সেভ হয়েছে!"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioGoldLight,
                                contentColor = JarvisObsidian
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_gemini_api_key_button")
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (primaryKeyInput.isBlank()) {
                                    keyValidationStatus = false
                                    keyValidationMessage = "❌ আগে API Key বসান।"
                                    return@Button
                                }
                                isValidatingKey = true
                                keyValidationMessage = "Key চেক করা হচ্ছে…"
                                coroutineScope.launch {
                                    val testResult = if (onValidateApiKey != null) {
                                        onValidateApiKey(
                                            primaryKeyInput,
                                            providerModeInput,
                                            customModelInput,
                                            customBaseUrlInput
                                        )
                                    } else {
                                        JarvisBrainService().testApiKeyOrToken(
                                            apiKey = primaryKeyInput,
                                            providerMode = providerModeInput,
                                            customModel = customModelInput,
                                            customBaseUrl = customBaseUrlInput
                                        )
                                    }
                                    isValidatingKey = false
                                    if (testResult.isSuccess) {
                                        keyValidationStatus = true
                                        keyValidationMessage = "🟢 ${testResult.getOrNull() ?: "API Key সঠিক ও সক্রিয়!"}"
                                        onSaveUniversalAiConfig(
                                            primaryKeyInput,
                                            secondaryKeyInput,
                                            providerModeInput,
                                            customBaseUrlInput,
                                            customModelInput,
                                            maxTokensInput,
                                            elevenLabsKeyInput
                                        )
                                    } else {
                                        keyValidationStatus = false
                                        val err = testResult.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                                        keyValidationMessage = "🔴 ভুল API Key বা সংযোগে ত্রুটি: $err"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (keyValidationStatus) {
                                    true -> JarvisEmerald
                                    false -> JarvisCrimson
                                    null -> JarvisElectricBlue
                                },
                                contentColor = if (keyValidationStatus == true) JarvisObsidian else JarvisIceWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("verify_api_key_button")
                        ) {
                            if (isValidatingKey) {
                                CircularProgressIndicator(
                                    color = JarvisIceWhite,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("যাচাই…", style = MaterialTheme.typography.labelSmall)
                            } else {
                                Icon(
                                    imageVector = when (keyValidationStatus) {
                                        true -> Icons.Filled.CheckCircle
                                        false -> Icons.Filled.Warning
                                        null -> Icons.Filled.Key
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (keyValidationStatus) {
                                        true -> "সঠিক (Green)"
                                        false -> "ভুল Key (Red)"
                                        null -> "Check Key"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                onSaveUniversalAiConfig(
                                    primaryKeyInput,
                                    secondaryKeyInput,
                                    providerModeInput,
                                    customBaseUrlInput,
                                    customModelInput,
                                    maxTokensInput,
                                    elevenLabsKeyInput
                                )
                                onTestAiBrain()
                            },
                            border = BorderStroke(1.dp, StudioGlassBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_ai_brain_button")
                        ) {
                            Text("Test AI", color = JarvisIceWhite, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 2. SIMPLE ANDROID SYSTEM VOICE & MICROPHONE SETTINGS CARD
        item {
            Surface(
                color = StudioDarkCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StudioDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = StudioGoldLight, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "২. ভয়েস ও মাইক সেটিংস",
                            style = MaterialTheme.typography.titleMedium,
                            color = JarvisIceWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Language Mode Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "AUTO" to "বাংলা + EN",
                            "BN" to "শুধু বাংলা",
                            "EN" to "শুধু English"
                        ).forEach { (code, label) ->
                            val selected = settings.languageMode == code
                            Surface(
                                color = if (selected) StudioGoldLight else StudioGlassFill,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selected) StudioGoldLight else StudioGlassBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onUpdateLanguageMode(code) }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) JarvisObsidian else JarvisIceWhite,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ভয়েস সিলেক্ট করুন (৩টি পুরুষ 👨 ও ৩টি নারী 👩):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisIceWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        REAL_VOICE_PERSONAS.forEach { persona ->
                            val selected = settings.selectedVoicePersonaId == persona.id
                            val emoji = if (persona.gender == VoiceGender.FEMALE) "👩" else "👨"
                            Surface(
                                color = if (selected) StudioGoldLight else StudioGlassFill,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selected) StudioGoldLight else StudioGlassBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectVoicePersona(persona.id) }
                                    .testTag("settings_voice_persona_${persona.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "$emoji ${persona.nameBn}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) JarvisObsidian else JarvisIceWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = persona.subtitleBn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) JarvisObsidian else StudioSubtleText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Switch 1: Continuous Background Mic
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "নন-স্টপ ব্যাকগ্রাউন্ড মাইক",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "JARVIS কথা বলার মাঝেও আপনার কথা শুনবে, মাইক বন্ধ হবে না।",
                                style = MaterialTheme.typography.bodySmall,
                                color = JarvisEmerald
                            )
                        }
                        Switch(
                            checked = settings.continuousBackgroundMic,
                            onCheckedChange = onUpdateContinuousBackgroundMic,
                            colors = SwitchDefaults.colors(checkedThumbColor = JarvisEmerald),
                            modifier = Modifier.testTag("settings_continuous_mic_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Switch 2: Auto-Speak Replies
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "মুখে উত্তর বলবে (Auto-Speak)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "বাংলা ও ইংরেজিতে অটোমেটিক ভয়েসে উত্তর দেবে।",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioSubtleText
                            )
                        }
                        Switch(
                            checked = settings.autoSpeakReplies,
                            onCheckedChange = onUpdateAutoSpeak,
                            colors = SwitchDefaults.colors(checkedThumbColor = StudioGoldLight)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Switch 3: Wake Word
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wake Word (\"Hey JARVIS\")",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "\"Hey JARVIS\" বা \"হেই জারভিস\" বললে সাথে সাথে কথা শুনবে।",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioSubtleText
                            )
                        }
                        Switch(
                            checked = settings.wakeWordEnabled,
                            onCheckedChange = onUpdateWakeWord,
                            colors = SwitchDefaults.colors(checkedThumbColor = JarvisEmerald),
                            modifier = Modifier.testTag("wake_word_switch")
                        )
                    }
                }
            }
        }

        // 3. SIMPLE ANDROID SYSTEM PERMISSIONS CARD
        item {
            Surface(
                color = StudioDarkCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StudioDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = StudioGoldLight, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "৩. ফোন পারমিশন",
                            style = MaterialTheme.typography.titleMedium,
                            color = JarvisIceWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    PermissionStatusRow(
                        name = "মাইক্রোফোন (Microphone)",
                        granted = telemetry.hasMicPermission,
                        actionLabel = "Allow",
                        onAction = onRequestMicPermission
                    )
                    PermissionStatusRow(
                        name = "ক্যামেরা ও ফ্ল্যাশলাইট",
                        granted = telemetry.hasCameraPermission,
                        actionLabel = "Allow",
                        onAction = onRequestCameraPermission
                    )
                    PermissionStatusRow(
                        name = "নোটিফিকেশন রিডার",
                        granted = telemetry.hasNotificationListenerAccess,
                        actionLabel = "Allow",
                        onAction = { onOpenSettingsSection("notification_access") }
                    )
                    PermissionStatusRow(
                        name = "স্ক্রিন ব্রাইটনেস কন্ট্রোল",
                        granted = telemetry.hasWriteSettingsAccess,
                        actionLabel = "Allow",
                        onAction = { onOpenSettingsSection("write_settings") }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    name: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (granted) JarvisEmerald else JarvisAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisIceWhite
            )
        }
        if (granted) {
            Text(
                text = "চালু আছে",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisEmerald,
                fontWeight = FontWeight.Bold
            )
        } else {
            OutlinedButton(
                onClick = onAction,
                border = BorderStroke(1.dp, JarvisCyan),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(actionLabel, color = JarvisCyan, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

