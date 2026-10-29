package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.ELEVENLABS_VOICE_PRESETS
import com.example.data.JarvisSettings
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisIceWhite
import com.example.ui.theme.JarvisMutedCyan
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisSurfaceCard
import com.example.voice.TtsLocaleDiagnostics
import com.example.voice.VoiceHudState

@Composable
fun VoiceLabScreen(
    settings: JarvisSettings,
    hudState: VoiceHudState,
    voiceStatusMessage: String,
    activeVoiceEngineLabel: String,
    ttsDiagnostics: TtsLocaleDiagnostics,
    hasConfiguredElevenLabsKey: Boolean,
    isForegroundServiceRunning: Boolean,
    onUpdateTtsEngine: (String) -> Unit,
    onUpdateElevenLabsVoiceId: (String) -> Unit,
    onUpdateCustomElevenLabsKey: (String) -> Unit,
    onUpdateSpeechRate: (Float) -> Unit,
    onUpdateSpeechPitch: (Float) -> Unit,
    onUpdateLanguageMode: (String) -> Unit,
    onUpdateAutoSpeak: (Boolean) -> Unit,
    onToggleHandsFreeService: (Boolean) -> Unit,
    onTestSpeak: (String, String) -> Unit,
    onTestAndroidTtsFallback: (String, String) -> Unit,
    onOpenTtsSettings: () -> Unit,
    onInstallTtsData: () -> Unit,
    onRefreshTtsLocales: () -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customVoiceIdInput by rememberSaveable(settings.elevenLabsVoiceId) {
        mutableStateOf(settings.elevenLabsVoiceId)
    }
    var customApiKeyInput by rememberSaveable(settings.customElevenLabsKey) {
        mutableStateOf(settings.customElevenLabsKey)
    }
    var testSpeechInput by rememberSaveable {
        mutableStateOf("Good evening, Boss. All JARVIS neural and voice systems are online and ready for your command.")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "NATURAL AI VOICE & TTS STUDIO",
                style = MaterialTheme.typography.headlineMedium,
                color = JarvisCyan
            )
            Text(
                text = "Configure Android Phone TTS, ElevenLabs Neural Voice ID & API, Bilingual Bangla/English speech, and test JARVIS voice output live.",
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisMutedCyan
            )
        }

        // 1. Live Voice Synthesis Status & Testing Console
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, JarvisCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = JarvisCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "JARVIS VOICE TESTING CONSOLE",
                                style = MaterialTheme.typography.labelLarge,
                                color = JarvisCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = activeVoiceEngineLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Status: $voiceStatusMessage",
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisMutedCyan
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = testSpeechInput,
                        onValueChange = { testSpeechInput = it },
                        label = { Text("Custom Speech Synthesis Test (English / বাংলা)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisCyan.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_test_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onTestSpeak(testSpeechInput, "en") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = JarvisObsidian
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_custom_voice_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SPEAK TEXT", fontWeight = FontWeight.Bold)
                        }

                        if (hudState == VoiceHudState.SPEAKING) {
                            OutlinedButton(
                                onClick = onStopSpeaking,
                                border = BorderStroke(1.dp, JarvisCrimson),
                                modifier = Modifier.testTag("voice_lab_stop_button")
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null, tint = JarvisCrimson)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("STOP", color = JarvisCrimson)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onTestSpeak(
                                    "Welcome back, Boss. Arc Reactor telemetry is nominal and all Android systems are at your command.",
                                    "en"
                                )
                            },
                            border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_english_voice_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test English Voice", color = JarvisIceWhite, style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                onTestSpeak(
                                    "হ্যালো বস, আমি জারভিস। আপনার ফোনের সব সিস্টেম এখন সম্পূর্ণ সচল এবং প্রস্তুত।",
                                    "bn"
                                )
                            },
                            border = BorderStroke(1.dp, JarvisEmerald.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_bangla_voice_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = JarvisEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("বাংলা ভয়েস টেস্ট", color = JarvisIceWhite, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 2. Voice Engine Selector (Phone Native TTS vs ElevenLabs Neural Voice)
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = JarvisCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SELECT VOICE SYNTHESIS ENGINE",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    EngineOptionRow(
                        selected = settings.ttsEngine == "PHONE_TTS",
                        title = "Android Phone Native TTS (Zero Latency & Offline Ready)",
                        subtitle = "Uses built-in Android TextToSpeech with automatic British/US English & বাংলা locale switching.",
                        onClick = { onUpdateTtsEngine("PHONE_TTS") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    EngineOptionRow(
                        selected = settings.ttsEngine == "ELEVENLABS",
                        title = "ElevenLabs Neural AI Voice (eleven_multilingual_v2)",
                        subtitle = "Ultra-realistic human-like JARVIS intonation in English & Bangla with automatic Phone TTS fallback.",
                        onClick = { onUpdateTtsEngine("ELEVENLABS") }
                    )
                }
            }
        }

        // 2b. Android TextToSpeech (TTS) Fallback & Bilingual Locale Diagnostics Card
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, JarvisEmerald.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("android_tts_fallback_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (ttsDiagnostics.isEngineInitialized) JarvisEmerald else JarvisAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ANDROID TTS FALLBACK ENGINE (EN & বাংলা)",
                                style = MaterialTheme.typography.labelLarge,
                                color = JarvisEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = onRefreshTtsLocales,
                            border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("RE-PROBE", color = JarvisCyan, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Engine: ${ttsDiagnostics.enginePackageName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisIceWhite
                    )
                    Text(
                        text = "• English Locale (${ttsDiagnostics.resolvedEnglishLocale}): ${if (ttsDiagnostics.isEnglishSupported) "SUPPORTED" else "DEFAULT"} • Voice: ${ttsDiagnostics.activeEnglishVoiceName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (ttsDiagnostics.isEnglishSupported) JarvisEmerald else JarvisAmber
                    )
                    Text(
                        text = "• Bangla Locale (${ttsDiagnostics.resolvedBanglaLocale}): ${if (ttsDiagnostics.isBanglaSupported) "SUPPORTED" else "VOICE PACK OPTIONAL"} • Voice: ${ttsDiagnostics.activeBanglaVoiceName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (ttsDiagnostics.isBanglaSupported) JarvisEmerald else JarvisAmber
                    )
                    Text(
                        text = "• Fallback Status: ${ttsDiagnostics.lastFallbackReason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisMutedCyan
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onTestAndroidTtsFallback(
                                    "Boss, Android native Text To Speech fallback is active and verified for English.",
                                    "en"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = JarvisObsidian
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("force_fallback_en_button")
                        ) {
                            Text("Fallback EN Test", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                onTestAndroidTtsFallback(
                                    "বস, অ্যান্ড্রয়েড টেক্সট টু স্পিচ বাংলা ভয়েস ফলব্যাক সচল রয়েছে।",
                                    "bn"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisEmerald,
                                contentColor = JarvisObsidian
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("force_fallback_bn_button")
                        ) {
                            Text("Fallback বাংলা Test", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onInstallTtsData,
                            border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("install_tts_data_button")
                        ) {
                            Text("Install Voice Data", color = JarvisCyan, style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = onOpenTtsSettings,
                            border = BorderStroke(1.dp, JarvisElectricBlue),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_tts_settings_button")
                        ) {
                            Text("Android TTS Settings", color = JarvisIceWhite, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 3. ElevenLabs Voice ID & API Configuration Card
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, JarvisElectricBlue.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Key, contentDescription = null, tint = JarvisElectricBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ELEVENLABS VOICE ID & API CONFIG",
                                style = MaterialTheme.typography.labelLarge,
                                color = JarvisCyan
                            )
                        }
                        Surface(
                            color = if (hasConfiguredElevenLabsKey) {
                                JarvisEmerald.copy(alpha = 0.2f)
                            } else {
                                JarvisAmber.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (hasConfiguredElevenLabsKey) "KEY CONFIGURED" else "KEY OPTIONAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasConfiguredElevenLabsKey) JarvisEmerald else JarvisAmber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select a preset ElevenLabs Voice ID or enter your custom cloned Voice ID:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisMutedCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ELEVENLABS_VOICE_PRESETS.forEach { preset ->
                        val isSelected = settings.elevenLabsVoiceId == preset.id
                        Surface(
                            color = if (isSelected) JarvisElectricBlue.copy(alpha = 0.22f) else JarvisObsidian,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) JarvisCyan else JarvisCyan.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    customVoiceIdInput = preset.id
                                    onUpdateElevenLabsVoiceId(preset.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        customVoiceIdInput = preset.id
                                        onUpdateElevenLabsVoiceId(preset.id)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = JarvisCyan)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${preset.name} (${preset.id.take(8)}…)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = JarvisIceWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${preset.accent} • ${preset.description}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = JarvisMutedCyan
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customVoiceIdInput,
                            onValueChange = { customVoiceIdInput = it },
                            label = { Text("ElevenLabs Voice ID") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("elevenlabs_voice_id_input")
                        )
                        Button(
                            onClick = { onUpdateElevenLabsVoiceId(customVoiceIdInput) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = JarvisObsidian
                            )
                        ) {
                            Text("SAVE ID", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customApiKeyInput,
                            onValueChange = { customApiKeyInput = it },
                            label = { Text("Optional Runtime ElevenLabs API Key (or set in AI Studio Secrets)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("elevenlabs_api_key_input")
                        )
                        Button(
                            onClick = { onUpdateCustomElevenLabsKey(customApiKeyInput) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisElectricBlue,
                                contentColor = JarvisIceWhite
                            )
                        ) {
                            Text("APPLY", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Phone TTS Tuning, Bilingual Language Mode & Hands-Free Foreground Service
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PHONE TTS MODULATION & BILINGUAL SETTINGS",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Speech Rate: ${String.format(java.util.Locale.US, "%.2fx", settings.speechRate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisIceWhite
                    )
                    Slider(
                        value = settings.speechRate,
                        onValueChange = { onUpdateSpeechRate(it) },
                        valueRange = 0.6f..1.6f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisCyan,
                            activeTrackColor = JarvisCyan
                        )
                    )

                    Text(
                        text = "Voice Pitch (JARVIS Resonance): ${String.format(java.util.Locale.US, "%.2fx", settings.speechPitch)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisIceWhite
                    )
                    Slider(
                        value = settings.speechPitch,
                        onValueChange = { onUpdateSpeechPitch(it) },
                        valueRange = 0.6f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisElectricBlue,
                            activeTrackColor = JarvisElectricBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Voice Recognition & Response Language:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisIceWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "AUTO" to "Auto (EN + বাংলা)",
                            "EN" to "English Only",
                            "BN" to "বাংলা (Bangla)"
                        ).forEach { (code, label) ->
                            val selected = settings.languageMode == code
                            Surface(
                                color = if (selected) JarvisCyan else JarvisObsidian,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, JarvisCyan),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onUpdateLanguageMode(code) }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) JarvisObsidian else JarvisIceWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
                                text = "Auto-Speak Every JARVIS Reply",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automatically convert Gemini responses to natural speech.",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisMutedCyan
                            )
                        }
                        Switch(
                            checked = settings.autoSpeakReplies,
                            onCheckedChange = onUpdateAutoSpeak,
                            colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hands-Free Continuous Mode (Foreground Service)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isForegroundServiceRunning) {
                                    "ACTIVE: Persistent notification running • Listening after replies."
                                } else {
                                    "User-controlled foreground service with persistent mic status notification."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isForegroundServiceRunning) JarvisEmerald else JarvisMutedCyan
                            )
                        }
                        Switch(
                            checked = settings.handsFreeMode,
                            onCheckedChange = onToggleHandsFreeService,
                            colors = SwitchDefaults.colors(checkedThumbColor = JarvisEmerald),
                            modifier = Modifier.testTag("hands_free_service_switch")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineOptionRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) JarvisElectricBlue.copy(alpha = 0.22f) else JarvisObsidian,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) JarvisCyan else JarvisCyan.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (selected) JarvisCyan else JarvisMutedCyan.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisIceWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisMutedCyan
                )
            }
        }
    }
}
