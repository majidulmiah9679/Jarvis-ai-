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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.actions.DeviceTelemetry
import com.example.ai.JarvisBrainService
import com.example.data.JarvisSettings
import com.example.data.MemoryEntity
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
    onTestAiBrain: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    val detectedKeyLabel = JarvisBrainService.detectKeyType(primaryKeyInput)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings & API Keys",
                style = MaterialTheme.typography.headlineMedium,
                color = JarvisIceWhite,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Gemini, Groq, OpenAI, OpenRouter, ElevenLabs সহ যেকোনো API Key বা Token এখানে সহজেই বসান।",
                style = MaterialTheme.typography.bodyMedium,
                color = StudioSubtleText
            )
        }

        // 1. UNIVERSAL AI API KEY & TOKEN STUDIO CARD
        item {
            Surface(
                color = StudioDarkCard,
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, StudioDarkCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_api_key_setup_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Key, contentDescription = null, tint = StudioGoldLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI API Keys & Tokens",
                                style = MaterialTheme.typography.titleMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = StudioGlassFill,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, StudioGlassBorder)
                        ) {
                            Text(
                                text = detectedKeyLabel.substringBefore(" ("),
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioGoldLight,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Provider Mode Pills
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
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, if (selected) StudioGoldLight else StudioGlassBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { providerModeInput = code }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) JarvisObsidian else JarvisIceWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Main API Key / Token Field
                    OutlinedTextField(
                        value = primaryKeyInput,
                        onValueChange = {
                            primaryKeyInput = it
                            statusConfirmation = ""
                        },
                        label = { Text("Primary API Key / Token (AIza..., gsk_..., sk-...)") },
                        placeholder = { Text("Paste any Gemini, Groq, OpenAI, or Bearer token") },
                        singleLine = true,
                        visualTransformation = if (showPrimaryKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
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
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioGoldLight,
                            unfocusedBorderColor = StudioGlassBorder
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_gemini_api_key_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Backup / Secondary API Key Field
                    OutlinedTextField(
                        value = secondaryKeyInput,
                        onValueChange = {
                            secondaryKeyInput = it
                            statusConfirmation = ""
                        },
                        label = { Text("Backup / 2nd API Key (Optional Auto-Failover)") },
                        placeholder = { Text("Second key if primary key hits quota limit") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioVioletAccent,
                            unfocusedBorderColor = StudioGlassBorder
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("secondary_ai_key_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // ElevenLabs Voice API Key
                    OutlinedTextField(
                        value = elevenLabsKeyInput,
                        onValueChange = {
                            elevenLabsKeyInput = it
                            statusConfirmation = ""
                        },
                        label = { Text("ElevenLabs Voice Key (Optional — Phone TTS Built-in)") },
                        placeholder = { Text("Paste ElevenLabs xi-api-key") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisEmerald,
                            unfocusedBorderColor = StudioGlassBorder
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_elevenlabs_key_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Token Limit Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Max Tokens:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisIceWhite
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(512, 1024, 2048, 4096).forEach { tokenVal ->
                                val selected = maxTokensInput == tokenVal
                                Surface(
                                    color = if (selected) StudioGoldLight else StudioGlassFill,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, if (selected) StudioGoldLight else StudioGlassBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { maxTokensInput = tokenVal }
                                ) {
                                    Text(
                                        text = "$tokenVal",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) JarvisObsidian else JarvisIceWhite,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Expandable Advanced Model Name & Custom Base URL
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
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
                            text = "Advanced: Custom Model Name & Base URL",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioGoldLight
                        )
                    }

                    AnimatedVisibility(visible = showAdvancedOptions) {
                        Column(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customModelInput,
                                onValueChange = { customModelInput = it },
                                label = { Text("Custom Model ID (e.g. gemini-2.5-flash, gpt-4o-mini)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = customBaseUrlInput,
                                onValueChange = { customBaseUrlInput = it },
                                label = { Text("Custom Endpoint URL") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (statusConfirmation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = statusConfirmation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                statusConfirmation = "✅ সব API Key ও Token সেভ হয়েছে!"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioGoldWarm,
                                contentColor = JarvisObsidian
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_gemini_api_key_button")
                        ) {
                            Text("Save All Keys", fontWeight = FontWeight.Bold)
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
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_ai_brain_button")
                        ) {
                            Text("Test AI Brain", color = JarvisIceWhite, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 2. SIMPLE LANGUAGE & VOICE CARD
        item {
            Surface(
                color = StudioDarkCard,
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, StudioDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = StudioGoldLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Language & Voice",
                            style = MaterialTheme.typography.titleMedium,
                            color = JarvisIceWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "AUTO" to "Auto (EN + বাংলা)",
                            "BN" to "বাংলা",
                            "EN" to "English"
                        ).forEach { (code, label) ->
                            val selected = settings.languageMode == code
                            Surface(
                                color = if (selected) StudioGoldLight else StudioGlassFill,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, if (selected) StudioGoldLight else StudioGlassBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onUpdateLanguageMode(code) }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) JarvisObsidian else JarvisIceWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Speak Replies",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Speaks replies automatically in বাংলা & English.",
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
                }
            }
        }

        // 3. SIMPLE ANDROID PERMISSIONS CARD
        item {
            Surface(
                color = StudioDarkCard,
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, StudioDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = StudioGoldLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Phone Permissions",
                            style = MaterialTheme.typography.titleMedium,
                            color = JarvisIceWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionStatusRow(
                        name = "Microphone (Voice Chat)",
                        granted = telemetry.hasMicPermission,
                        actionLabel = "Allow",
                        onAction = onRequestMicPermission
                    )
                    PermissionStatusRow(
                        name = "Camera & Flashlight",
                        granted = telemetry.hasCameraPermission,
                        actionLabel = "Allow",
                        onAction = onRequestCameraPermission
                    )
                    PermissionStatusRow(
                        name = "Notification Reader",
                        granted = telemetry.hasNotificationListenerAccess,
                        actionLabel = "Allow",
                        onAction = { onOpenSettingsSection("notification_access") }
                    )
                    PermissionStatusRow(
                        name = "Screen Brightness Control",
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
                text = "GRANTED",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisEmerald,
                fontWeight = FontWeight.Bold
            )
        } else {
            OutlinedButton(
                onClick = onAction,
                border = BorderStroke(1.dp, JarvisCyan),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(actionLabel, color = JarvisCyan, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
