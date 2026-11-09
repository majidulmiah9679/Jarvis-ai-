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

enum class VoiceGender(val badgeBn: String, val badgeEn: String) {
    MALE("পুরুষ কণ্ঠ", "Male"),
    FEMALE("নারী কণ্ঠ", "Female")
}

data class RealVoicePersona(
    val id: String,
    val name: String,
    val titleBn: String,
    val gender: VoiceGender,
    val accent: String,
    val description: String,
    val geminiVoiceName: String,
    val elevenLabsVoiceId: String,
    val naturalPitch: Float,
    val naturalRate: Float,
    val sampleBangla: String,
    val sampleEnglish: String
) {
    val nameBn: String get() = titleBn
    val nameEn: String get() = name
    val subtitleBn: String get() = "${gender.badgeBn} • $accent"
    val pitch: Float get() = naturalPitch
    val rate: Float get() = naturalRate
    val sampleGreetingBn: String get() = sampleBangla
    val sampleGreetingEn: String get() = sampleEnglish
}

val REAL_VOICE_PERSONAS = listOf(
    RealVoicePersona(
        id = "jarvis_male_classic",
        name = "JARVIS Classic (Male 1)",
        titleBn = "জারভিস ক্লাসিক (পুরুষ ১)",
        gender = VoiceGender.MALE,
        accent = "British / প্রমিত বাংলা • Composed Male",
        description = "শান্ত, বুদ্ধিদীপ্ত ও স্বাভাবিক পুরুষ কণ্ঠ (Default JARVIS Male)",
        geminiVoiceName = "Charon",
        elevenLabsVoiceId = "JBFqnCBsd6RMkjVDRZzb",
        naturalPitch = 0.90f,
        naturalRate = 0.98f,
        sampleBangla = "হ্যালো বস, আমি জারভিস। আমার ক্লাসিক পুরুষ কণ্ঠ এখন সচল এবং আমি অবিরাম আপনার কথা শুনছি।",
        sampleEnglish = "Hello Boss, I am JARVIS. My classic male voice is online and ready for continuous conversation."
    ),
    RealVoicePersona(
        id = "friday_female_clear",
        name = "F.R.I.D.A.Y. Clear (Female 1)",
        titleBn = "ফ্রাইডে ক্লিয়ার (ফিমেল ১)",
        gender = VoiceGender.FEMALE,
        accent = "Natural Expressive • সুললিত নারী কণ্ঠ",
        description = "স্পষ্ট, স্মার্ট ও মানুষের মতো স্বাভাবিক ফিমেল এআই ভয়েস",
        geminiVoiceName = "Aoede",
        elevenLabsVoiceId = "21m00Tcm4TlvDq8ikWAM",
        naturalPitch = 1.18f,
        naturalRate = 1.00f,
        sampleBangla = "হ্যালো বস, আমি ফ্রাইডে। আমার ফিমেল ভয়েস এখন চালু হয়েছে। আপনি যা বলবেন আমি শুনে সাথে সাথে জবাব দেব।",
        sampleEnglish = "Hello Boss, F.R.I.D.A.Y. online. My natural female voice is active and listening continuously."
    ),
    RealVoicePersona(
        id = "aryan_male_deep",
        name = "Aryan Deep (Male 2)",
        titleBn = "আরিয়ান ডিপ (পুরুষ ২)",
        gender = VoiceGender.MALE,
        accent = "Deep Studio Baritone • গভীর পুরুষ কণ্ঠ",
        description = "গভীর, গম্ভীর ও কমান্ডিং রিয়েল পুরুষ কণ্ঠ",
        geminiVoiceName = "Fenrir",
        elevenLabsVoiceId = "pNInz6obpgDQGcFmaJgB",
        naturalPitch = 0.78f,
        naturalRate = 0.94f,
        sampleBangla = "বস, এটি আরিয়ান ডিপ পুরুষ কণ্ঠ। আপনার সব সিস্টেম ও ব্যাকগ্রাউন্ড মাইক এখন সম্পূর্ণ প্রস্তুত।",
        sampleEnglish = "Boss, this is Aryan with a deep studio male voice. All systems are standing by."
    ),
    RealVoicePersona(
        id = "nusrat_female_sweet",
        name = "Nusrat Natural (Female 2)",
        titleBn = "নুসরাত ন্যাচারাল (ফিমেল ২)",
        gender = VoiceGender.FEMALE,
        accent = "Warm Conversational • মিষ্টি নারী কণ্ঠ",
        description = "কোমল, প্রাণবন্ত ও মানুষের মতো ঘরোয়া বাংলা-ইংরেজি নারী কণ্ঠ",
        geminiVoiceName = "Kore",
        elevenLabsVoiceId = "EXAVITQu4vr4xnSDxMaL",
        naturalPitch = 1.28f,
        naturalRate = 0.96f,
        sampleBangla = "আসসালামু আলাইকুম বস! আমি নুসরাত। আপনি কথা বলুন, আমি মনোযোগ দিয়ে শুনছি এবং উত্তর দিচ্ছি।",
        sampleEnglish = "Hi Boss, this is Nusrat speaking in a warm, lifelike female voice."
    ),
    RealVoicePersona(
        id = "rafi_male_friendly",
        name = "Rafi Friendly (Male 3)",
        titleBn = "রাফি ফ্রেন্ডলি (পুরুষ ৩)",
        gender = VoiceGender.MALE,
        accent = "Dynamic Conversational • প্রাণবন্ত পুরুষ কণ্ঠ",
        description = "বন্ধুসুলভ, স্পষ্ট ও দ্রুত কথোপকথনের উপযোগী পুরুষ কণ্ঠ",
        geminiVoiceName = "Puck",
        elevenLabsVoiceId = "ErXwobaYiN019PkySvjV",
        naturalPitch = 1.02f,
        naturalRate = 1.04f,
        sampleBangla = "হ্যালো বস! আমি রাফি। আপনার যেকোনো প্রশ্ন বা ফোনের কাজ আমাকে বলুন, আমি সাথে সাথে করে দিচ্ছি।",
        sampleEnglish = "Hey Boss! This is Rafi with a friendly, energetic male voice."
    ),
    RealVoicePersona(
        id = "zara_female_studio",
        name = "Zara Studio HD (Female 3)",
        titleBn = "জারা স্টুডিও (ফিমেল ৩)",
        gender = VoiceGender.FEMALE,
        accent = "Crisp Broadcast HD • স্টুডিও নারী কণ্ঠ",
        description = "আধুনিক, প্রফেশনাল ও স্পষ্ট স্টুডিও ফিমেল কণ্ঠ",
        geminiVoiceName = "Leda",
        elevenLabsVoiceId = "AZnzlk1XvdvUeBnXmlld",
        naturalPitch = 1.12f,
        naturalRate = 1.03f,
        sampleBangla = "হ্যালো বস, জারা স্টুডিও ভয়েস সচল রয়েছে। ব্যাকগ্রাউন্ডে মাইক বন্ধ হবে না, আপনি নিশ্চিন্তে কথা বলতে পারেন।",
        sampleEnglish = "Hello Boss, Zara Studio HD female voice is locked in for uninterrupted hands-free chat."
    )
)

fun findVoicePersonaById(id: String): RealVoicePersona {
    return REAL_VOICE_PERSONAS.firstOrNull { it.id == id }
        ?: REAL_VOICE_PERSONAS.firstOrNull { it.elevenLabsVoiceId == id }
        ?: REAL_VOICE_PERSONAS.first()
}

data class ElevenLabsVoicePreset(
    val id: String,
    val name: String,
    val accent: String,
    val description: String
)

val ELEVENLABS_VOICE_PRESETS = REAL_VOICE_PERSONAS.map { persona ->
    ElevenLabsVoicePreset(
        id = persona.elevenLabsVoiceId,
        name = "${persona.name} • ${persona.titleBn}",
        accent = "${persona.gender.badgeBn} (${persona.gender.badgeEn}) • ${persona.accent}",
        description = persona.description
    )
}

data class JarvisSettings(
    val bossTitle: String = "Boss",
    val languageMode: String = "AUTO", // "AUTO", "EN", "BN"
    val ttsEngine: String = "PHONE_TTS", // "PHONE_TTS", "GEMINI_TTS", or "ELEVENLABS"
    val selectedVoicePersonaId: String = "jarvis_male_classic",
    val elevenLabsVoiceId: String = "JBFqnCBsd6RMkjVDRZzb",
    val customElevenLabsKey: String = "",
    val customGeminiApiKey: String = "",
    val secondaryAiApiKey: String = "", // Extra backup key (OpenAI / Groq / OpenRouter / second Gemini key)
    val aiProviderMode: String = "AUTO", // "AUTO", "FIREBASE", "GEMINI", "OPENAI", "GROQ", "OPENROUTER", "CUSTOM"
    val customBaseUrl: String = "",
    val customModelName: String = "",
    val maxOutputTokens: Int = 1024,
    val speechRate: Float = 0.98f,
    val speechPitch: Float = 0.90f,
    val autoSpeakReplies: Boolean = true,
    val continuousBackgroundMic: Boolean = true,
    val handsFreeMode: Boolean = false,
    val wakeWordEnabled: Boolean = false,
    val confirmSensitiveActions: Boolean = true,
    val geminiModel: String = "gemini-3.8-flash"
)

class JarvisPreferencesRepository(private val context: Context) {

    private object Keys {
        val BOSS_TITLE = stringPreferencesKey("boss_title")
        val LANGUAGE_MODE = stringPreferencesKey("language_mode")
        val TTS_ENGINE = stringPreferencesKey("tts_engine")
        val SELECTED_VOICE_PERSONA_ID = stringPreferencesKey("selected_voice_persona_id")
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
        val CONTINUOUS_BG_MIC = booleanPreferencesKey("continuous_bg_mic")
        val HANDS_FREE = booleanPreferencesKey("hands_free")
        val WAKE_WORD = booleanPreferencesKey("wake_word")
        val CONFIRM_SENSITIVE = booleanPreferencesKey("confirm_sensitive")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
    }

    val settingsFlow: Flow<JarvisSettings> = context.jarvisDataStore.data.map { prefs ->
        val personaId = prefs[Keys.SELECTED_VOICE_PERSONA_ID] ?: "jarvis_male_classic"
        val persona = findVoicePersonaById(personaId)
        JarvisSettings(
            bossTitle = prefs[Keys.BOSS_TITLE] ?: "Boss",
            languageMode = prefs[Keys.LANGUAGE_MODE] ?: "AUTO",
            ttsEngine = prefs[Keys.TTS_ENGINE] ?: "PHONE_TTS",
            selectedVoicePersonaId = persona.id,
            elevenLabsVoiceId = prefs[Keys.ELEVENLABS_VOICE_ID] ?: persona.elevenLabsVoiceId,
            customElevenLabsKey = prefs[Keys.CUSTOM_ELEVENLABS_KEY] ?: "",
            customGeminiApiKey = prefs[Keys.CUSTOM_GEMINI_KEY] ?: "",
            secondaryAiApiKey = prefs[Keys.SECONDARY_AI_KEY] ?: "",
            aiProviderMode = prefs[Keys.AI_PROVIDER_MODE] ?: "AUTO",
            customBaseUrl = prefs[Keys.CUSTOM_BASE_URL] ?: "",
            customModelName = prefs[Keys.CUSTOM_MODEL_NAME] ?: "",
            maxOutputTokens = prefs[Keys.MAX_OUTPUT_TOKENS] ?: 1024,
            speechRate = prefs[Keys.SPEECH_RATE] ?: persona.naturalRate,
            speechPitch = prefs[Keys.SPEECH_PITCH] ?: persona.naturalPitch,
            autoSpeakReplies = prefs[Keys.AUTO_SPEAK] ?: true,
            continuousBackgroundMic = prefs[Keys.CONTINUOUS_BG_MIC] ?: true,
            handsFreeMode = prefs[Keys.HANDS_FREE] ?: false,
            wakeWordEnabled = prefs[Keys.WAKE_WORD] ?: false,
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

    suspend fun updateSelectedVoicePersona(personaId: String) {
        val persona = findVoicePersonaById(personaId)
        context.jarvisDataStore.edit { prefs ->
            prefs[Keys.SELECTED_VOICE_PERSONA_ID] = persona.id
            prefs[Keys.ELEVENLABS_VOICE_ID] = persona.elevenLabsVoiceId
            prefs[Keys.SPEECH_PITCH] = persona.naturalPitch
            prefs[Keys.SPEECH_RATE] = persona.naturalRate
        }
    }

    suspend fun updateElevenLabsVoiceId(voiceId: String) {
        val matchedPersona = REAL_VOICE_PERSONAS.firstOrNull {
            it.elevenLabsVoiceId == voiceId.trim() || it.id == voiceId.trim()
        }
        context.jarvisDataStore.edit { prefs ->
            prefs[Keys.ELEVENLABS_VOICE_ID] = voiceId.trim().ifBlank { "JBFqnCBsd6RMkjVDRZzb" }
            if (matchedPersona != null) {
                prefs[Keys.SELECTED_VOICE_PERSONA_ID] = matchedPersona.id
                prefs[Keys.SPEECH_PITCH] = matchedPersona.naturalPitch
                prefs[Keys.SPEECH_RATE] = matchedPersona.naturalRate
            }
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

    suspend fun updateContinuousBackgroundMic(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.CONTINUOUS_BG_MIC] = enabled }
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
