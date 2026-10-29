package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.jarvisDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jarvis_preferences"
)

data class ElevenLabsVoicePreset(
    val id: String,
    val name: String,
    val accent: String,
    val description: String
)

val ELEVENLABS_VOICE_PRESETS = listOf(
    ElevenLabsVoicePreset(
        id = "JBFqnCBsd6RMkjVDRZzb",
        name = "George (JARVIS Classic)",
        accent = "British English + Multilingual v2",
        description = "Warm, intelligent, composed British butler cadence"
    ),
    ElevenLabsVoicePreset(
        id = "onwK4e9ZLuTAKqWW03F9",
        name = "Daniel (Tactical HUD)",
        accent = "British Authoritative + Multilingual",
        description = "Crisp, authoritative telemetry and mission briefing voice"
    ),
    ElevenLabsVoicePreset(
        id = "pNInz6obpgDQGcFmaJgB",
        name = "Adam (Deep Neural)",
        accent = "American Deep + Multilingual",
        description = "Deep resonant AI assistant voice"
    ),
    ElevenLabsVoicePreset(
        id = "ErXwobaYiN019PkySvjV",
        name = "Antoni (Conversational)",
        accent = "Natural Expressive + Bangla/English",
        description = "Smooth, natural bilingual conversational voice"
    ),
    ElevenLabsVoicePreset(
        id = "21m00Tcm4TlvDq8ikWAM",
        name = "Rachel (F.R.I.D.A.Y. Style)",
        accent = "Calm Clear + Multilingual",
        description = "Crystal-clear female AI assistant voice"
    )
)

data class JarvisSettings(
    val bossTitle: String = "Boss",
    val languageMode: String = "AUTO", // "AUTO", "EN", "BN"
    val ttsEngine: String = "PHONE_TTS", // "PHONE_TTS" or "ELEVENLABS"
    val elevenLabsVoiceId: String = "JBFqnCBsd6RMkjVDRZzb",
    val customElevenLabsKey: String = "",
    val customGeminiApiKey: String = "",
    val secondaryAiApiKey: String = "", // Extra backup key (OpenAI / Groq / OpenRouter / second Gemini key)
    val aiProviderMode: String = "AUTO", // "AUTO", "FIREBASE", "GEMINI", "OPENAI", "GROQ", "OPENROUTER", "CUSTOM"
    val customBaseUrl: String = "",
    val customModelName: String = "",
    val maxOutputTokens: Int = 1024,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 0.95f,
    val autoSpeakReplies: Boolean = true,
    val handsFreeMode: Boolean = false,
    val wakeWordEnabled: Boolean = true,
    val confirmSensitiveActions: Boolean = true,
    val geminiModel: String = "gemini-3.8-flash"
)

class JarvisPreferencesRepository(private val context: Context) {

    private object Keys {
        val BOSS_TITLE = stringPreferencesKey("boss_title")
        val LANGUAGE_MODE = stringPreferencesKey("language_mode")
        val TTS_ENGINE = stringPreferencesKey("tts_engine")
        val ELEVENLABS_VOICE_ID = stringPreferencesKey("elevenlabs_voice_id")
        val CUSTOM_ELEVENLABS_KEY = stringPreferencesKey("custom_elevenlabs_key")
        val CUSTOM_GEMINI_KEY = stringPreferencesKey("custom_gemini_key")
        val SECONDARY_AI_KEY = stringPreferencesKey("secondary_ai_key")
        val AI_PROVIDER_MODE = stringPreferencesKey("ai_provider_mode")
        val CUSTOM_BASE_URL = stringPreferencesKey("custom_base_url")
        val CUSTOM_MODEL_NAME = stringPreferencesKey("custom_model_name")
        val MAX_OUTPUT_TOKENS = intPreferencesKey("max_output_tokens")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val HANDS_FREE = booleanPreferencesKey("hands_free")
        val WAKE_WORD = booleanPreferencesKey("wake_word")
        val CONFIRM_SENSITIVE = booleanPreferencesKey("confirm_sensitive")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
    }

    val settingsFlow: Flow<JarvisSettings> = context.jarvisDataStore.data.map { prefs ->
        JarvisSettings(
            bossTitle = prefs[Keys.BOSS_TITLE] ?: "Boss",
            languageMode = prefs[Keys.LANGUAGE_MODE] ?: "AUTO",
            ttsEngine = prefs[Keys.TTS_ENGINE] ?: "PHONE_TTS",
            elevenLabsVoiceId = prefs[Keys.ELEVENLABS_VOICE_ID] ?: "JBFqnCBsd6RMkjVDRZzb",
            customElevenLabsKey = prefs[Keys.CUSTOM_ELEVENLABS_KEY] ?: "",
            customGeminiApiKey = prefs[Keys.CUSTOM_GEMINI_KEY] ?: "",
            secondaryAiApiKey = prefs[Keys.SECONDARY_AI_KEY] ?: "",
            aiProviderMode = prefs[Keys.AI_PROVIDER_MODE] ?: "AUTO",
            customBaseUrl = prefs[Keys.CUSTOM_BASE_URL] ?: "",
            customModelName = prefs[Keys.CUSTOM_MODEL_NAME] ?: "",
            maxOutputTokens = prefs[Keys.MAX_OUTPUT_TOKENS] ?: 1024,
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
            speechPitch = prefs[Keys.SPEECH_PITCH] ?: 0.95f,
            autoSpeakReplies = prefs[Keys.AUTO_SPEAK] ?: true,
            handsFreeMode = prefs[Keys.HANDS_FREE] ?: false,
            wakeWordEnabled = prefs[Keys.WAKE_WORD] ?: true,
            confirmSensitiveActions = prefs[Keys.CONFIRM_SENSITIVE] ?: true,
            geminiModel = prefs[Keys.GEMINI_MODEL] ?: "gemini-3.8-flash"
        )
    }

    suspend fun updateBossTitle(title: String) {
        context.jarvisDataStore.edit { it[Keys.BOSS_TITLE] = title.ifBlank { "Boss" } }
    }

    suspend fun updateLanguageMode(mode: String) {
        context.jarvisDataStore.edit { it[Keys.LANGUAGE_MODE] = mode }
    }

    suspend fun updateTtsEngine(engine: String) {
        context.jarvisDataStore.edit { it[Keys.TTS_ENGINE] = engine }
    }

    suspend fun updateElevenLabsVoiceId(voiceId: String) {
        context.jarvisDataStore.edit {
            it[Keys.ELEVENLABS_VOICE_ID] = voiceId.trim().ifBlank { "JBFqnCBsd6RMkjVDRZzb" }
        }
    }

    suspend fun updateCustomElevenLabsKey(apiKey: String) {
        context.jarvisDataStore.edit { it[Keys.CUSTOM_ELEVENLABS_KEY] = apiKey.trim() }
    }

    suspend fun updateCustomGeminiApiKey(apiKey: String) {
        context.jarvisDataStore.edit { it[Keys.CUSTOM_GEMINI_KEY] = apiKey.trim() }
    }

    suspend fun updateUniversalAiConfig(
        primaryKey: String,
        secondaryKey: String,
        providerMode: String,
        customBaseUrl: String,
        customModelName: String,
        maxTokens: Int,
        elevenLabsKey: String
    ) {
        context.jarvisDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_GEMINI_KEY] = primaryKey.trim()
            prefs[Keys.SECONDARY_AI_KEY] = secondaryKey.trim()
            prefs[Keys.AI_PROVIDER_MODE] = providerMode.trim().ifBlank { "AUTO" }
            prefs[Keys.CUSTOM_BASE_URL] = customBaseUrl.trim()
            prefs[Keys.CUSTOM_MODEL_NAME] = customModelName.trim()
            prefs[Keys.MAX_OUTPUT_TOKENS] = maxTokens.coerceIn(256, 8192)
            prefs[Keys.CUSTOM_ELEVENLABS_KEY] = elevenLabsKey.trim()
        }
    }

    suspend fun updateSpeechRate(rate: Float) {
        context.jarvisDataStore.edit { it[Keys.SPEECH_RATE] = rate.coerceIn(0.5f, 2.0f) }
    }

    suspend fun updateSpeechPitch(pitch: Float) {
        context.jarvisDataStore.edit { it[Keys.SPEECH_PITCH] = pitch.coerceIn(0.5f, 2.0f) }
    }

    suspend fun updateAutoSpeak(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.AUTO_SPEAK] = enabled }
    }

    suspend fun updateHandsFreeMode(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.HANDS_FREE] = enabled }
    }

    suspend fun updateWakeWordEnabled(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.WAKE_WORD] = enabled }
    }

    suspend fun updateConfirmSensitiveActions(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.CONFIRM_SENSITIVE] = enabled }
    }

    suspend fun updateGeminiModel(model: String) {
        context.jarvisDataStore.edit { it[Keys.GEMINI_MODEL] = model }
    }
}
