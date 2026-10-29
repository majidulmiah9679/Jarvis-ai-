package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.BuildConfig
import com.example.data.JarvisSettings
import com.example.util.LanguageDetectionHelper
import com.example.util.LanguageDetectionResult
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private const val TAG = "JarvisVoiceManager"

enum class VoiceHudState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

data class TtsLocaleDiagnostics(
    val isEngineInitialized: Boolean = false,
    val enginePackageName: String = "Initializing…",
    val isEnglishSupported: Boolean = false,
    val resolvedEnglishLocale: String = "en-GB",
    val activeEnglishVoiceName: String = "Default",
    val isBanglaSupported: Boolean = false,
    val resolvedBanglaLocale: String = "bn-BD",
    val activeBanglaVoiceName: String = "Default",
    val lastFallbackReason: String = "Ready as primary or automatic fallback"
)

private data class QueuedSpeechRequest(
    val text: String,
    val languageHint: String,
    val settings: JarvisSettings,
    val forcePhoneTts: Boolean,
    val onComplete: (() -> Unit)?
)

class JarvisVoiceManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSpeechResult: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var mediaPlayer: MediaPlayer? = null
    private var waveformSimulationJob: Job? = null
    private var speakingWatchdogJob: Job? = null
    private var pendingUtteranceBeforeInit: QueuedSpeechRequest? = null
    private var consecutiveSilenceRetries = 0

    private var preferredEnglishLocale: Locale = Locale.UK
    private var preferredBanglaLocale: Locale = Locale.forLanguageTag("bn-BD")
    private var preferredEnglishVoice: Voice? = null
    private var preferredBanglaVoice: Voice? = null

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val _hudState = MutableStateFlow(VoiceHudState.IDLE)
    val hudState: StateFlow<VoiceHudState> = _hudState.asStateFlow()

    private val _audioLevel = MutableStateFlow(0.08f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _voiceStatusMessage = MutableStateFlow("Initializing Android TTS (English & বাংলা) + Neural Voice Link…")
    val voiceStatusMessage: StateFlow<String> = _voiceStatusMessage.asStateFlow()

    private val _activeVoiceEngineLabel = MutableStateFlow("PHONE TTS // INITIALIZING")
    val activeVoiceEngineLabel: StateFlow<String> = _activeVoiceEngineLabel.asStateFlow()

    private val _ttsDiagnostics = MutableStateFlow(TtsLocaleDiagnostics())
    val ttsDiagnostics: StateFlow<TtsLocaleDiagnostics> = _ttsDiagnostics.asStateFlow()

    private val _lastDetectedLanguage = MutableStateFlow(
        LanguageDetectionHelper.detectUserLanguage("", "AUTO")
    )
    val lastDetectedLanguage: StateFlow<LanguageDetectionResult> = _lastDetectedLanguage.asStateFlow()

    private val _isLiveConversationActive = MutableStateFlow(true)
    val isLiveConversationActive: StateFlow<Boolean> = _isLiveConversationActive.asStateFlow()

    private var currentSettings = JarvisSettings()
    @Volatile
    private var onSpeakingFinishedCallback: (() -> Unit)? = null

    init {
        initializePhoneTtsEngine()
    }

    fun initializePhoneTtsEngine() {
        mainHandler.post {
            try {
                textToSpeech?.shutdown()
                isTtsInitialized = false
                // Prefer Google TTS engine if installed for best English & Bangla support, else system default
                val googleTtsPkg = "com.google.android.tts"
                val hasGoogleTts = try {
                    context.packageManager.getPackageInfo(googleTtsPkg, 0)
                    true
                } catch (_: Exception) {
                    false
                }

                textToSpeech = if (hasGoogleTts) {
                    TextToSpeech(context.applicationContext, this, googleTtsPkg)
                } else {
                    TextToSpeech(context.applicationContext, this)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize TextToSpeech", e)
                _voiceStatusMessage.value = "Phone TTS unavailable: ${e.localizedMessage}"
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            val tts = textToSpeech
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )

            probeAndConfigureLocales()

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _hudState.value = VoiceHudState.SPEAKING
                    startSpeakingWaveformAnimation()
                }

                override fun onDone(utteranceId: String?) {
                    // Only transition to IDLE when the final segment finishes
                    if (utteranceId == null || utteranceId.endsWith("_FINAL")) {
                        speakingWatchdogJob?.cancel()
                        stopSpeakingWaveformAnimation()
                        _hudState.value = VoiceHudState.IDLE
                        val cb = onSpeakingFinishedCallback
                        onSpeakingFinishedCallback = null
                        if (cb != null) {
                            mainHandler.postDelayed({ cb.invoke() }, 420L)
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    speakingWatchdogJob?.cancel()
                    stopSpeakingWaveformAnimation()
                    _hudState.value = VoiceHudState.IDLE
                    val cb = onSpeakingFinishedCallback
                    onSpeakingFinishedCallback = null
                    if (cb != null) {
                        mainHandler.postDelayed({ cb.invoke() }, 420L)
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    speakingWatchdogJob?.cancel()
                    stopSpeakingWaveformAnimation()
                    _hudState.value = VoiceHudState.IDLE
                    _voiceStatusMessage.value = "Android TTS playback error (code $errorCode)."
                    val cb = onSpeakingFinishedCallback
                    onSpeakingFinishedCallback = null
                    if (cb != null) {
                        mainHandler.postDelayed({ cb.invoke() }, 420L)
                    }
                }
            })

            val diag = _ttsDiagnostics.value
            val bnStatus = if (diag.isBanglaSupported) "বাংলা Ready (${diag.resolvedBanglaLocale})" else "বাংলা Voice Pack Optional"
            _voiceStatusMessage.value =
                "Android TTS Ready • English (${diag.resolvedEnglishLocale}) & $bnStatus"
            updateEngineBadge(currentSettings)

            // If an utterance was requested while TTS was still initializing, speak it now
            val pending = pendingUtteranceBeforeInit
            if (pending != null) {
                pendingUtteranceBeforeInit = null
                speak(
                    text = pending.text,
                    languageHint = pending.languageHint,
                    settings = pending.settings,
                    forcePhoneTts = pending.forcePhoneTts,
                    onComplete = pending.onComplete
                )
            }
        } else {
            isTtsInitialized = false
            _ttsDiagnostics.value = _ttsDiagnostics.value.copy(
                isEngineInitialized = false,
                enginePackageName = "Initialization Failed (Status $status)"
            )
            _voiceStatusMessage.value = "Android TTS initialization failed with status $status"
        }
    }

    /**
     * Probes the Android TextToSpeech engine for the best available English (UK/US/IN)
     * and Bangla (bn-BD / bn-IN / bn) locales and high-quality voices.
     */
    fun probeAndConfigureLocales() {
        val tts = textToSpeech ?: return
        val enginePkg = tts.defaultEngine ?: "System TTS Engine"
        val availableVoices = try {
            tts.voices?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

        // 1. Resolve best English locale (Prefer UK for authentic JARVIS cadence, fallback to US -> IN -> EN)
        val englishCandidates = listOf(
            Locale.UK,
            Locale.US,
            Locale.forLanguageTag("en-IN"),
            Locale.ENGLISH
        )
        var resolvedEnLocale: Locale = Locale.US
        var enSupported = false
        for (candidate in englishCandidates) {
            val avail = tts.isLanguageAvailable(candidate)
            if (avail >= TextToSpeech.LANG_AVAILABLE) {
                resolvedEnLocale = candidate
                enSupported = true
                break
            }
        }
        preferredEnglishLocale = resolvedEnLocale
        preferredEnglishVoice = selectBestVoiceForLanguage(
            voices = availableVoices,
            langCode = "en",
            preferredCountry = resolvedEnLocale.country
        )

        // 2. Resolve best Bangla locale (Prefer bn-BD -> bn-IN -> bn)
        val banglaCandidates = listOf(
            Locale.forLanguageTag("bn-BD"),
            Locale.forLanguageTag("bn-IN"),
            Locale.forLanguageTag("bn"),
            Locale.forLanguageTag("ben-BD"),
            Locale.forLanguageTag("ben-IN")
        )
        var resolvedBnLocale: Locale = Locale.forLanguageTag("bn-BD")
        var bnSupported = false
        for (candidate in banglaCandidates) {
            val avail = tts.isLanguageAvailable(candidate)
            if (avail >= TextToSpeech.LANG_AVAILABLE) {
                resolvedBnLocale = candidate
                bnSupported = true
                break
            }
        }

        // Also check if any installed voice explicitly supports Bengali ('bn' or 'ben')
        val matchedBnVoice = selectBestVoiceForLanguage(
            voices = availableVoices,
            langCode = "bn",
            preferredCountry = resolvedBnLocale.country
        ) ?: selectBestVoiceForLanguage(
            voices = availableVoices,
            langCode = "ben",
            preferredCountry = "BD"
        )

        if (matchedBnVoice != null) {
            bnSupported = true
            resolvedBnLocale = matchedBnVoice.locale
        }

        preferredBanglaLocale = resolvedBnLocale
        preferredBanglaVoice = matchedBnVoice

        // Default initial locale to English
        applyLocaleAndVoiceToTts(isBangla = false)

        _ttsDiagnostics.value = TtsLocaleDiagnostics(
            isEngineInitialized = isTtsInitialized,
            enginePackageName = enginePkg,
            isEnglishSupported = enSupported,
            resolvedEnglishLocale = resolvedEnLocale.toLanguageTag(),
            activeEnglishVoiceName = preferredEnglishVoice?.name ?: "${resolvedEnLocale.displayLanguage} (${resolvedEnLocale.country})",
            isBanglaSupported = bnSupported,
            resolvedBanglaLocale = resolvedBnLocale.toLanguageTag(),
            activeBanglaVoiceName = preferredBanglaVoice?.name ?: if (bnSupported) {
                "বাংলা (${resolvedBnLocale.toLanguageTag()})"
            } else {
                "System Default / Voice Pack Needed"
            },
            lastFallbackReason = _ttsDiagnostics.value.lastFallbackReason
        )
    }

    private fun selectBestVoiceForLanguage(
        voices: List<Voice>,
        langCode: String,
        preferredCountry: String
    ): Voice? {
        val matchingLang = voices.filter { voice ->
            voice.locale.language.equals(langCode, ignoreCase = true) ||
                voice.name.startsWith("$langCode-", ignoreCase = true)
        }
        if (matchingLang.isEmpty()) return null

        return matchingLang.sortedWith(
            compareByDescending<Voice> { it.locale.country.equals(preferredCountry, ignoreCase = true) }
                .thenByDescending { !it.isNetworkConnectionRequired }
                .thenByDescending { it.quality }
                .thenBy { it.latency }
        ).firstOrNull()
    }

    /**
     * Automatically detects whether the user's input is Bangla or English, updates
     * [lastDetectedLanguage], and dynamically pre-configures the Android TTS locale.
     */
    fun detectAndConfigureLanguage(
        utterance: String,
        preferredMode: String = currentSettings.languageMode
    ): LanguageDetectionResult {
        val detection = LanguageDetectionHelper.detectUserLanguage(
            inputText = utterance,
            preferredLanguageMode = preferredMode,
            bossTitle = currentSettings.bossTitle
        )
        _lastDetectedLanguage.value = detection
        mainHandler.post {
            applyLocaleAndVoiceToTts(
                isBangla = detection.isBangla,
                targetLocale = if (detection.isBangla) preferredBanglaLocale else preferredEnglishLocale
            )
        }
        return detection
    }

    private fun applyLocaleAndVoiceToTts(
        isBangla: Boolean,
        targetLocale: Locale = if (isBangla) preferredBanglaLocale else preferredEnglishLocale
    ): Boolean {
        val tts = textToSpeech ?: return false
        return try {
            if (isBangla) {
                val bnVoice = preferredBanglaVoice
                if (bnVoice != null) {
                    tts.voice = bnVoice
                    true
                } else {
                    val res = tts.setLanguage(targetLocale)
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Try generic "bn" or "bn-IN" before falling back
                        val altRes = tts.setLanguage(Locale.forLanguageTag("bn-IN"))
                        if (altRes == TextToSpeech.LANG_MISSING_DATA || altRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                            tts.setLanguage(preferredEnglishLocale)
                            false
                        } else {
                            true
                        }
                    } else {
                        true
                    }
                }
            } else {
                val enVoice = preferredEnglishVoice
                if (enVoice != null) {
                    tts.voice = enVoice
                } else {
                    tts.setLanguage(targetLocale)
                }
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply TTS locale (isBangla=$isBangla)", e)
            false
        }
    }

    fun openSystemTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
            }
        }
    }

    fun requestInstallTtsVoiceData() {
        try {
            val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (_: Exception) {
            openSystemTtsSettings()
        }
    }

    fun updateSettings(settings: JarvisSettings) {
        currentSettings = settings
        updateEngineBadge(settings)
    }

    private fun updateEngineBadge(settings: JarvisSettings) {
        val hasElevenKey = resolveElevenLabsApiKey(settings).isNotBlank()
        _activeVoiceEngineLabel.value = if (settings.ttsEngine == "ELEVENLABS" && hasElevenKey) {
            "ELEVENLABS + ANDROID TTS FALLBACK"
        } else if (settings.ttsEngine == "ELEVENLABS") {
            "ANDROID TTS FALLBACK // ACTIVE"
        } else {
            "ANDROID TTS (EN / বাংলা) // ONLINE"
        }
    }

    fun resolveElevenLabsApiKey(settings: JarvisSettings = currentSettings): String {
        if (settings.customElevenLabsKey.isNotBlank()) {
            return settings.customElevenLabsKey.trim()
        }
        val buildConfigKey = try {
            BuildConfig.ELEVENLABS_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        return if (buildConfigKey.isNotBlank() &&
            buildConfigKey != "YOUR_ELEVENLABS_API_KEY" &&
            buildConfigKey != "null"
        ) {
            buildConfigKey
        } else {
            ""
        }
    }

    fun setProcessingState() {
        _hudState.value = VoiceHudState.PROCESSING
        _partialTranscript.value = ""
    }

    fun setIdleState() {
        if (_hudState.value == VoiceHudState.PROCESSING) {
            _hudState.value = VoiceHudState.IDLE
        }
    }

    fun startListening(languageMode: String = currentSettings.languageMode) {
        stopSpeaking()
        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    _voiceStatusMessage.value =
                        "SpeechRecognizer service not available on this device/emulator. Use text input or install Google Speech Services."
                    _hudState.value = VoiceHudState.IDLE
                    return@post
                }

                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                    speechRecognizer?.setRecognitionListener(createRecognitionListener())
                }

                val localeTag = when (languageMode.uppercase()) {
                    "BN" -> "bn-BD"
                    "EN" -> "en-US"
                    else -> Locale.getDefault().toLanguageTag().ifBlank { "en-US" }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                _partialTranscript.value = ""
                _hudState.value = VoiceHudState.LISTENING
                _voiceStatusMessage.value = "Listening ($localeTag)… Speak your command, Boss."
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting SpeechRecognizer", e)
                _hudState.value = VoiceHudState.IDLE
                _voiceStatusMessage.value = "Microphone error: ${e.localizedMessage}"
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (_: Exception) {
            }
            if (_hudState.value == VoiceHudState.LISTENING) {
                _hudState.value = VoiceHudState.IDLE
            }
            _audioLevel.value = 0.08f
        }
    }

    fun setLiveConversationActive(active: Boolean) {
        _isLiveConversationActive.value = active
        if (!active) {
            consecutiveSilenceRetries = 0
        }
    }

    /**
     * Speaks the AI response using the primary voice engine, or seamlessly falls back
     * to the Android TextToSpeech (TTS) engine with English (`en-GB`/`en-US`) and
     * Bangla (`bn-BD`/`bn-IN`) locale support.
     */
    fun speak(
        text: String,
        languageHint: String = "en",
        settings: JarvisSettings = currentSettings,
        forcePhoneTts: Boolean = false,
        onComplete: (() -> Unit)? = null
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            onComplete?.invoke()
            return
        }

        // Stop any previous playback WITHOUT clearing the newly registered onComplete callback
        haltPlaybackInternal()
        onSpeakingFinishedCallback = onComplete

        val apiKey = resolveElevenLabsApiKey(settings)
        if (!forcePhoneTts && settings.ttsEngine == "ELEVENLABS" && apiKey.isNotBlank()) {
            scope.launch {
                val success = speakWithElevenLabs(
                    text = cleanText,
                    languageHint = languageHint,
                    settings = settings,
                    voiceId = settings.elevenLabsVoiceId,
                    apiKey = apiKey
                )
                if (!success) {
                    recordFallbackReason("ElevenLabs unavailable or failed — switched to Android Phone TTS fallback")
                    speakWithPhoneTts(cleanText, languageHint, settings, isFallback = true)
                }
            }
        } else {
            val isFallback = !forcePhoneTts && settings.ttsEngine == "ELEVENLABS" && apiKey.isBlank()
            if (isFallback) {
                recordFallbackReason("ElevenLabs API key not set — using Android Phone TTS fallback")
            }
            speakWithPhoneTts(cleanText, languageHint, settings, isFallback = isFallback)
        }
    }

    private fun recordFallbackReason(reason: String) {
        _ttsDiagnostics.value = _ttsDiagnostics.value.copy(lastFallbackReason = reason)
    }

    private suspend fun speakWithElevenLabs(
        text: String,
        languageHint: String,
        settings: JarvisSettings,
        voiceId: String,
        apiKey: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                _hudState.value = VoiceHudState.SPEAKING
                _voiceStatusMessage.value = "Synthesizing ElevenLabs Neural Voice ($voiceId)…"
            }

            val safeVoiceId = voiceId.trim().ifBlank { "JBFqnCBsd6RMkjVDRZzb" }
            val url = "https://api.elevenlabs.io/v1/text-to-speech/$safeVoiceId?output_format=mp3_44100_128"

            val payload = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                put(
                    "voice_settings",
                    JSONObject().apply {
                        put("stability", 0.55)
                        put("similarity_boost", 0.80)
                        put("style", 0.25)
                        put("use_speaker_boost", true)
                    }
                )
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("xi-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "audio/mpeg")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string()?.take(140).orEmpty()
                Log.w(TAG, "ElevenLabs HTTP ${response.code}: $errBody")
                response.close()
                return@withContext false
            }

            val bytes = response.body?.bytes()
            response.close()
            if (bytes == null || bytes.isEmpty()) {
                return@withContext false
            }

            val outFile = File(context.cacheDir, "jarvis_elevenlabs_tts.mp3")
            FileOutputStream(outFile).use { it.write(bytes) }

            withContext(Dispatchers.Main) {
                playMp3FileWithFallback(
                    file = outFile,
                    fallbackText = text,
                    languageHint = languageHint,
                    settings = settings
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "ElevenLabs synthesis failed — triggering Android TTS fallback", e)
            false
        }
    }

    private fun playMp3FileWithFallback(
        file: File,
        fallbackText: String,
        languageHint: String,
        settings: JarvisSettings
    ) {
        try {
            mediaPlayer?.release()
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnPreparedListener { player ->
                    _hudState.value = VoiceHudState.SPEAKING
                    _voiceStatusMessage.value = "JARVIS Speaking (ElevenLabs Neural Audio)"
                    startSpeakingWaveformAnimation()
                    player.start()
                }
                setOnCompletionListener { player ->
                    stopSpeakingWaveformAnimation()
                    _hudState.value = VoiceHudState.IDLE
                    _voiceStatusMessage.value = "ElevenLabs voice transmission complete."
                    player.release()
                    if (mediaPlayer == player) mediaPlayer = null
                    onSpeakingFinishedCallback?.invoke()
                }
                setOnErrorListener { player, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra — falling back to Phone TTS")
                    player.release()
                    if (mediaPlayer == player) mediaPlayer = null
                    recordFallbackReason("MediaPlayer stream error ($what) — switched to Android Phone TTS")
                    speakWithPhoneTts(fallbackText, languageHint, settings, isFallback = true)
                    true
                }
            }
            mediaPlayer = mp
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play ElevenLabs MP3 — falling back to Phone TTS", e)
            recordFallbackReason("Audio player exception — switched to Android Phone TTS")
            speakWithPhoneTts(fallbackText, languageHint, settings, isFallback = true)
        }
    }

    /**
     * Speaks text using Android's native TextToSpeech engine, automatically detecting
     * Bangla Unicode script (`\u0980`..`\u09FF`) or language hint (`"bn"` vs `"en"`),
     * applying the appropriate locale (`bn-BD`/`bn-IN` or `en-GB`/`en-US`), and chunking
     * long responses safely.
     */
    private fun speakWithPhoneTts(
        text: String,
        languageHint: String,
        settings: JarvisSettings,
        isFallback: Boolean = false
    ) {
        mainHandler.post {
            val tts = textToSpeech
            if (!isTtsInitialized || tts == null) {
                // Queue utterance until onInit completes
                pendingUtteranceBeforeInit = QueuedSpeechRequest(
                    text = text,
                    languageHint = languageHint,
                    settings = settings,
                    forcePhoneTts = true,
                    onComplete = onSpeakingFinishedCallback
                )
                _voiceStatusMessage.value = "Android TTS initializing — queued response…"
                initializePhoneTtsEngine()
                return@post
            }

            val resolvedLocale = LanguageDetectionHelper.resolveDynamicTtsLocale(
                textToSpeak = text,
                languageHint = languageHint,
                preferredBanglaLocale = preferredBanglaLocale,
                preferredEnglishLocale = preferredEnglishLocale
            )
            val containsBangla = LanguageDetectionHelper.isBanglaUtterance(text, languageHint)

            val localeApplied = applyLocaleAndVoiceToTts(
                isBangla = containsBangla,
                targetLocale = resolvedLocale
            )
            tts.setSpeechRate(settings.speechRate.coerceIn(0.5f, 2.0f))
            tts.setPitch(settings.speechPitch.coerceIn(0.5f, 2.0f))

            val modeLabel = if (isFallback) "Android TTS Fallback" else "Android Phone TTS"
            val langBadge = if (containsBangla) {
                if (localeApplied) "বাংলা (${resolvedLocale.toLanguageTag()})" else "বাংলা (Default Voice)"
            } else {
                "English (${resolvedLocale.toLanguageTag()})"
            }

            _hudState.value = VoiceHudState.SPEAKING
            _voiceStatusMessage.value = "JARVIS Speaking ($modeLabel • $langBadge)"
            startSpeakingWaveformAnimation()

            val segments = splitIntoSafeTtsSegments(text)
            var hasError = false
            segments.forEachIndexed { index, segment ->
                val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                val suffix = if (index == segments.lastIndex) "_FINAL" else "_PART_$index"
                val utteranceId = "${UUID.randomUUID()}$suffix"

                // If mixed bilingual sentences exist, switch locale per sentence dynamically
                val segmentIsBangla = LanguageDetectionHelper.isBanglaUtterance(segment, languageHint)
                val segmentLocale = LanguageDetectionHelper.resolveDynamicTtsLocale(
                    textToSpeak = segment,
                    languageHint = if (segmentIsBangla) "bn" else "en",
                    preferredBanglaLocale = preferredBanglaLocale,
                    preferredEnglishLocale = preferredEnglishLocale
                )
                applyLocaleAndVoiceToTts(
                    isBangla = segmentIsBangla,
                    targetLocale = segmentLocale
                )

                val params = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                val result = tts.speak(segment, queueMode, params, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    hasError = true
                }
            }

            if (hasError) {
                speakingWatchdogJob?.cancel()
                stopSpeakingWaveformAnimation()
                _hudState.value = VoiceHudState.IDLE
                _voiceStatusMessage.value = "Android TTS playback encountered an engine error."
                val cb = onSpeakingFinishedCallback
                onSpeakingFinishedCallback = null
                if (cb != null) {
                    mainHandler.postDelayed({ cb.invoke() }, 450L)
                }
            } else {
                // Safety watchdog in case the device/emulator TTS engine finishes without firing onDone
                speakingWatchdogJob?.cancel()
                val estimatedMs = (text.length * 75L).coerceIn(2400L, 14000L)
                speakingWatchdogJob = scope.launch {
                    delay(estimatedMs)
                    if (_hudState.value == VoiceHudState.SPEAKING) {
                        stopSpeakingWaveformAnimation()
                        _hudState.value = VoiceHudState.IDLE
                        val cb = onSpeakingFinishedCallback
                        onSpeakingFinishedCallback = null
                        if (cb != null) {
                            mainHandler.post { cb.invoke() }
                        }
                    }
                }
            }
        }
    }

    private fun containsBanglaScript(input: String): Boolean {
        return LanguageDetectionHelper.isBanglaUtterance(input)
    }

    private fun splitIntoSafeTtsSegments(text: String): List<String> {
        val maxLen = try {
            TextToSpeech.getMaxSpeechInputLength().coerceAtMost(1500)
        } catch (_: Exception) {
            1000
        }
        val cleaned = text.replace(Regex("[*#_`~]+"), "").trim()
        if (cleaned.length <= maxLen && !cleaned.contains("।")) {
            return listOf(cleaned)
        }

        // Split by English and Bangla sentence terminators ('.', '!', '?', '।', '\n')
        val rawSentences = cleaned.split(Regex("(?<=[.!?।\n])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (rawSentences.isEmpty()) {
            return cleaned.chunked(maxLen)
        }
        return rawSentences
    }

    private fun haltPlaybackInternal() {
        speakingWatchdogJob?.cancel()
        speakingWatchdogJob = null
        stopSpeakingWaveformAnimation()
        mainHandler.post {
            try {
                textToSpeech?.stop()
            } catch (_: Exception) {
            }
            try {
                mediaPlayer?.let {
                    if (it.isPlaying) it.stop()
                    it.release()
                }
                mediaPlayer = null
            } catch (_: Exception) {
            }
        }
    }

    fun stopSpeaking() {
        pendingUtteranceBeforeInit = null
        onSpeakingFinishedCallback = null
        speakingWatchdogJob?.cancel()
        speakingWatchdogJob = null
        mainHandler.post {
            stopSpeakingWaveformAnimation()
            try {
                textToSpeech?.stop()
            } catch (_: Exception) {
            }
            try {
                mediaPlayer?.let {
                    if (it.isPlaying) it.stop()
                    it.release()
                }
                mediaPlayer = null
            } catch (_: Exception) {
            }
            if (_hudState.value == VoiceHudState.SPEAKING) {
                _hudState.value = VoiceHudState.IDLE
                _voiceStatusMessage.value = "Voice output halted by Boss."
            }
            _audioLevel.value = 0.08f
        }
    }

    private fun startSpeakingWaveformAnimation() {
        waveformSimulationJob?.cancel()
        waveformSimulationJob = scope.launch {
            var tick = 0f
            while (isActive && _hudState.value == VoiceHudState.SPEAKING) {
                tick += 0.35f
                val pulse = 0.35f + 0.45f * kotlin.math.abs(kotlin.math.sin(tick)) +
                    0.18f * kotlin.math.abs(kotlin.math.cos(tick * 2.3f))
                _audioLevel.value = pulse.coerceIn(0.15f, 0.98f)
                delay(55L)
            }
            _audioLevel.value = 0.08f
        }
    }

    private fun stopSpeakingWaveformAnimation() {
        waveformSimulationJob?.cancel()
        waveformSimulationJob = null
        _audioLevel.value = 0.08f
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _hudState.value = VoiceHudState.LISTENING
            }

            override fun onBeginningOfSpeech() {
                _hudState.value = VoiceHudState.LISTENING
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.08f, 1.0f)
                _audioLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _audioLevel.value = 0.1f
            }

            override fun onError(error: Int) {
                _audioLevel.value = 0.08f
                _hudState.value = VoiceHudState.IDLE
                val errorText = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Listening for your voice, Boss… (কথা বলুন)"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waiting for your voice command, Boss…"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice commands."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Speech recognition network error. Check connectivity."
                    else -> "Voice recognition ready (status code $error)."
                }
                _voiceStatusMessage.value = errorText

                // If Live Conversation Mode is active and it was just a pause/silence, keep listening automatically
                if (_isLiveConversationActive.value &&
                    (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) &&
                    consecutiveSilenceRetries < 3
                ) {
                    consecutiveSilenceRetries++
                    mainHandler.postDelayed({
                        if (_isLiveConversationActive.value && _hudState.value == VoiceHudState.IDLE) {
                            startListening(currentSettings.languageMode)
                        }
                    }, 750L)
                }
            }

            override fun onResults(results: Bundle?) {
                _audioLevel.value = 0.08f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val transcript = matches?.firstOrNull()?.trim().orEmpty()
                _partialTranscript.value = ""
                if (transcript.isNotBlank()) {
                    consecutiveSilenceRetries = 0
                    val detected = detectAndConfigureLanguage(transcript, currentSettings.languageMode)
                    _voiceStatusMessage.value =
                        "Detected ${detected.language.nativeLabel} (${detected.ttsLocale.toLanguageTag()}) — processing…"
                    _hudState.value = VoiceHudState.PROCESSING
                    onSpeechResult(transcript)
                } else {
                    _hudState.value = VoiceHudState.IDLE
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (partial.isNotBlank()) {
                    _partialTranscript.value = partial
                    detectAndConfigureLanguage(partial, currentSettings.languageMode)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun shutdown() {
        stopListening()
        stopSpeaking()
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {
            }
            try {
                textToSpeech?.shutdown()
                textToSpeech = null
            } catch (_: Exception) {
            }
        }
    }
}
