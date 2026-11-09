package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.JarvisSettings
import com.example.data.REAL_VOICE_PERSONAS
import com.example.data.VoiceGender
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisIceWhite
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.StudioDarkCard
import com.example.ui.theme.StudioDarkCardBorder
import com.example.ui.theme.StudioGlassBorder
import com.example.ui.theme.StudioGlassFill
import com.example.ui.theme.StudioGoldLight
import com.example.ui.theme.StudioSubtleText
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
    partialTranscript: String = "",
    lastHeardTranscript: String = "",
    onStartSttListening: () -> Unit = {},
    onStopSttListening: () -> Unit = {},
    onUpdateTtsEngine: (String) -> Unit,
    onSelectVoicePersona: (String) -> Unit = {},
    onUpdateContinuousBackgroundMic: (Boolean) -> Unit = {},
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
    var localSpeechRate by rememberSaveable(settings.speechRate) {
        mutableFloatStateOf(settings.speechRate)
    }
    var localSpeechPitch by rememberSaveable(settings.speechPitch) {
        mutableFloatStateOf(settings.speechPitch)
    }
    var testSpeechInput by rememberSaveable {
        mutableStateOf("জি বস, আমি আপনার কথা পরিষ্কার শুনতে পাচ্ছি এবং বাংলা ও ইংরেজিতে উত্তর দিতে প্রস্তুত।")
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
                text = "STT (কথা শোনা) ও TTS (কথা বলা)",
                style = MaterialTheme.typography.headlineSmall,
                color = JarvisIceWhite,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Android Native Speech-to-Text (STT) এবং Text-to-Speech (TTS) কন্ট্রোল ও লাইভ টেস্ট",
                style = MaterialTheme.typography.bodySmall,
                color = StudioSubtleText
            )
        }

        // 1. STT (Speech-to-Text — কথা শোনা) Configuration & Live Test Card
        item {
            Surface(
                color = StudioDarkCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StudioDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = null,
                                tint = JarvisEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "১. STT ইঞ্জিন (Speech-to-Text • কথা শোনা)",
                                style = MaterialTheme.typography.titleMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (hudState == VoiceHudState.LISTENING) "শুনছে…" else "প্রস্তুত",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = voiceStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioSubtleText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live STT Transcript Box
                    Surface(
                        color = StudioGlassFill,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StudioGlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val sttText = when {
                            partialTranscript.isNotBlank() -> "শুনছি: “$partialTranscript”"
                            lastHeardTranscript.isNotBlank() -> "শেষ শোনা কথা: “$lastHeardTranscript”"
                            else -> "নিচের 'STT টেস্ট (কথা বলুন)' বাটনে ট্যাপ করে কথা বলুন"
                        }
                        Text(
                            text = sttText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisIceWhite,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isSttActive = hudState == VoiceHudState.LISTENING
                        Button(
                            onClick = {
                                if (isSttActive) onStopSttListening() else onStartSttListening()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSttActive) JarvisCrimson else JarvisEmerald,
                                contentColor = if (isSttActive) JarvisIceWhite else JarvisObsidian
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stt_live_test_button")
                        ) {
                            Icon(
                                imageVector = if (isSttActive) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSttActive) "STT শোনা বন্ধ করুন" else "🎙️ STT টেস্ট (এখন কথা বলুন)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "STT ভাষা (কোন ভাষায় কথা শুনবে):",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisIceWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "AUTO" to "বাংলা + English",
                            "BN" to "বাংলা (bn-BD)",
                            "EN" to "English (en-US)"
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
                    HorizontalDivider(color = StudioDarkCardBorder)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "অটো-কন্টিনিউয়াস STT (নন-স্টপ শোনা)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "TTS কথা বলা শেষ হলেই অটোমেটিক আবার STT কথা শোনা শুরু করবে।",
                                style = MaterialTheme.typography.bodySmall,
                                color = JarvisEmerald
                            )
                        }
                        Switch(
                            checked = settings.continuousBackgroundMic,
                            onCheckedChange = onUpdateContinuousBackgroundMic,
                            colors = SwitchDefaults.colors(checkedThumbColor = JarvisEmerald)
                        )
                    }
                }
            }
        }

        // 2. TTS (Text-to-Speech — কথা বলা) Live Test & Voice Selection Card
        item {
            Surface(
                color = StudioDarkCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StudioDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = StudioGoldLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "২. TTS ইঞ্জিন (Text-to-Speech • কথা বলা)",
                                style = MaterialTheme.typography.titleMedium,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (ttsDiagnostics.isEngineInitialized) "সক্রিয়" else "প্রস্তুত হচ্ছে",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = testSpeechInput,
                        onValueChange = { testSpeechInput = it },
                        label = { Text("TTS টেস্ট টেক্সট (বাংলা / English)") },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioGoldLight,
                            unfocusedBorderColor = StudioGlassBorder
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
                            onClick = { onTestSpeak(testSpeechInput, "bn") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioGoldLight,
                                contentColor = JarvisObsidian
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_custom_voice_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TTS বাজান", fontWeight = FontWeight.Bold)
                        }

                        if (hudState == VoiceHudState.SPEAKING) {
                            OutlinedButton(
                                onClick = onStopSpeaking,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, JarvisCrimson),
                                modifier = Modifier.testTag("voice_lab_stop_button")
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null, tint = JarvisCrimson)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("থামান", color = JarvisCrimson)
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
                                    "জি বস, আমার বাংলা টিটিএস ভয়েস একদম পরিষ্কারভাবে কাজ করছে।",
                                    "bn"
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StudioGlassBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_bangla_voice_button")
                        ) {
                            Text("বাংলা TTS টেস্ট", color = JarvisIceWhite, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                onTestSpeak(
                                    "Hello Boss, Android native Text to Speech is online and speaking clearly.",
                                    "en"
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StudioGlassBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_english_voice_button")
                        ) {
                            Text("English TTS টেস্ট", color = JarvisIceWhite, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = StudioDarkCardBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 6 Real Male & Female Voices for TTS
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.RecordVoiceOver,
                            contentDescription = null,
                            tint = StudioGoldLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TTS কণ্ঠস্বর (৩টি পুরুষ 👨 ও ৩টি নারী 👩):",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisIceWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

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
                                    1.dp,
                                    if (selected) StudioGoldLight else StudioGlassBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectVoicePersona(persona.id) }
                                    .testTag("voicelab_persona_${persona.id}")
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Speech Speed & Pitch Sliders
                    Text(
                        text = "TTS কথা বলার গতি (Speed): ${"%.2f".format(localSpeechRate)}x",
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisIceWhite
                    )
                    Slider(
                        value = localSpeechRate,
                        onValueChange = {
                            localSpeechRate = it
                            onUpdateSpeechRate(it)
                        },
                        valueRange = 0.7f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = StudioGoldLight,
                            activeTrackColor = StudioGoldLight
                        )
                    )

                    Text(
                        text = "TTS কণ্ঠের পিচ (Pitch): ${"%.2f".format(localSpeechPitch)}x",
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisIceWhite
                    )
                    Slider(
                        value = localSpeechPitch,
                        onValueChange = {
                            localSpeechPitch = it
                            onUpdateSpeechPitch(it)
                        },
                        valueRange = 0.7f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisEmerald,
                            activeTrackColor = JarvisEmerald
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenTtsSettings,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StudioGlassBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = JarvisCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ফোন TTS সেটিংস", color = JarvisIceWhite, style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = onRefreshTtsLocales,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StudioGlassBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = JarvisEmerald)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ভয়েস রিফ্রেশ", color = JarvisIceWhite, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
