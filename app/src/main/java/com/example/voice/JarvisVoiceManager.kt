package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.JarvisSettings
import com.example.data.REAL_VOICE_PERSONAS
import com.example.data.RealVoicePersona
import com.example.data.VoiceGender
import com.example.data.findVoicePersonaById
import com.example.util.LanguageDetectionHelper
import com.example.util.LanguageDetectionResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
import org.json.JSONArray
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
    val installedMaleVoicesCount: Int = 0,
    val installedFemaleVoicesCount: Int = 0,
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
    private val onSpeechResult: (String) -> Unit,
    private val onWakeWordTriggered: ((matchedTrigger: String, isBangla: Boolean, trailingCommand: String?) -> Unit)? = null
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

    @Volatile
    private var isListeningForWakeWordOnly = false

    @Volatile
    private var isRecognizerActive = false

    @Volatile
    private var isContinuousMicSessionLocked = false

    private var pendingRetryRunnable: Runnable? = null

    private var preferredEnglishLocale: Locale = Locale.UK
    private var preferredBanglaLocale: Locale = Locale.forLanguageTag("bn-BD")
    private var preferredEnglishVoice: Voice? = null
    private var preferredBanglaVoice: Voice? = null
    private var allInstalledVoices: List<Voice> = emptyList()

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(28, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val _hudState = MutableStateFlow(VoiceHudState.IDLE)
    val hudState: StateFlow<VoiceHudState> = _hudState.asStateFlow()

    private val _audioLevel = MutableStateFlow(0.08f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _voiceStatusMessage = MutableStateFlow("মাইক্রোফোনে ট্যাপ করে কথা বলুন, Boss • বাংলা ও English সাপোর্টেড।")
    val voiceStatusMessage: StateFlow<String> = _voiceStatusMessage.asStateFlow()

    private val _activeVoiceEngineLabel = MutableStateFlow("REAL HUMAN VOICE // INITIALIZING")
    val activeVoiceEngineLabel: StateFlow<String> = _activeVoiceEngineLabel.asStateFlow()

    private val _ttsDiagnostics = MutableStateFlow(TtsLocaleDiagnostics())
    val ttsDiagnostics: StateFlow<TtsLocaleDiagnostics> = _ttsDiagnostics.asStateFlow()

    private val _lastDetectedLanguage = MutableStateFlow(
        LanguageDetectionHelper.detectUserLanguage("", "AUTO")
    )
    val lastDetectedLanguage: StateFlow<LanguageDetectionResult> = _lastDetectedLanguage.asStateFlow()

    private val _isLiveConversationActive = MutableStateFlow(false)
    val isLiveConversationActive: StateFlow<Boolean> = _isLiveConversationActive.asStateFlow()

    private val _isWakeWordStandbyActive = MutableStateFlow(false)
    val isWakeWordStandbyActive: StateFlow<Boolean> = _isWakeWordStandbyActive.asStateFlow()

    private val _lastWakeWordEvent = MutableStateFlow("")
    val lastWakeWordEvent: StateFlow<String> = _lastWakeWordEvent.asStateFlow()

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
                    if (utteranceId == null || utteranceId.endsWith("_FINAL")) {
                        finishSpeakingAndResumeMicIfNeeded()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    finishSpeakingAndResumeMicIfNeeded()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _voiceStatusMessage.value = "Android TTS playback error (code $errorCode)."
                    finishSpeakingAndResumeMicIfNeeded()
                }
            })

            val diag = _ttsDiagnostics.value
            val bnStatus = if (diag.isBanglaSupported) "বাংলা Ready (${diag.resolvedBanglaLocale})" else "বাংলা Voice Ready"
            val activePersona = findVoicePersonaById(currentSettings.selectedVoicePersonaId)
            _voiceStatusMessage.value =
                "Real Voice Ready: ${activePersona.titleBn} • English & $bnStatus"
            updateEngineBadge(currentSettings)

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

    private fun finishSpeakingAndResumeMicIfNeeded() {
        speakingWatchdogJob?.cancel()
        stopSpeakingWaveformAnimation()
        val cb = onSpeakingFinishedCallback
        onSpeakingFinishedCallback = null

        val shouldKeepListening = isContinuousMicSessionLocked ||
            _isLiveConversationActive.value ||
            currentSettings.handsFreeMode

        if (cb != null) {
            mainHandler.postDelayed({ cb.invoke() }, 350L)
        } else if (shouldKeepListening) {
            mainHandler.postDelayed({
                if (isContinuousMicSessionLocked || _isLiveConversationActive.value || currentSettings.handsFreeMode) {
                    startListening(
                        languageMode = currentSettings.languageMode,
                        wakeWordStandbyOnly = false
                    )
                } else {
                    _hudState.value = VoiceHudState.IDLE
                }
            }, 350L)
        } else {
            _hudState.value = VoiceHudState.IDLE
        }
    }

    /**
     * Probes the Android TextToSpeech engine for the best available English (UK/US/IN)
     * and Bangla (bn-BD / bn-IN / bn) locales and catalogs both Male and Female neural voices.
     */
    fun probeAndConfigureLocales() {
        val tts = textToSpeech ?: return
        val enginePkg = tts.defaultEngine ?: "System TTS Engine"
        val availableVoices = try {
            tts.voices?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        allInstalledVoices = availableVoices

        // 1. Resolve best English locale
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

        val activePersona = findVoicePersonaById(currentSettings.selectedVoicePersonaId)
        preferredEnglishVoice = selectBestVoiceForPersona(
            voices = availableVoices,
            langCode = "en",
            preferredCountry = resolvedEnLocale.country,
            persona = activePersona
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

        val matchedBnVoice = selectBestVoiceForPersona(
            voices = availableVoices,
            langCode = "bn",
            preferredCountry = resolvedBnLocale.country,
            persona = activePersona
        ) ?: selectBestVoiceForPersona(
            voices = availableVoices,
            langCode = "ben",
            preferredCountry = "BD",
            persona = activePersona
        )

        if (matchedBnVoice != null) {
            bnSupported = true
            resolvedBnLocale = matchedBnVoice.locale
        }

        preferredBanglaLocale = resolvedBnLocale
        preferredBanglaVoice = matchedBnVoice

        applyLocaleAndVoiceToTts(isBangla = false, settings = currentSettings)

        val maleCount = availableVoices.count { isVoiceLikelyMale(it) }
        val femaleCount = availableVoices.count { isVoiceLikelyFemale(it) }

        _ttsDiagnostics.value = TtsLocaleDiagnostics(
            isEngineInitialized = isTtsInitialized,
            enginePackageName = enginePkg,
            isEnglishSupported = enSupported,
            resolvedEnglishLocale = resolvedEnLocale.toLanguageTag(),
            activeEnglishVoiceName = preferredEnglishVoice?.name ?: "${activePersona.name} (${resolvedEnLocale.country})",
            isBanglaSupported = bnSupported,
            resolvedBanglaLocale = resolvedBnLocale.toLanguageTag(),
            activeBanglaVoiceName = preferredBanglaVoice?.name ?: "${activePersona.titleBn} (${resolvedBnLocale.toLanguageTag()})",
            installedMaleVoicesCount = maleCount,
            installedFemaleVoicesCount = femaleCount,
            lastFallbackReason = _ttsDiagnostics.value.lastFallbackReason
        )
    }

    private fun isVoiceLikelyFemale(voice: Voice): Boolean {
        val n = voice.name.lowercase()
        val features = try {
            voice.features?.map { it.lowercase() }.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        return n.contains("female") ||
            n.contains("#female") ||
            features.any { it.contains("female") } ||
            n.contains("-sfg") ||
            n.contains("-tpf") ||
            n.contains("-bnf") ||
            n.contains("-baf") ||
            n.contains("-gba") ||
            n.contains("-gbc") ||
            n.contains("-iob") ||
            n.contains("wavenet-a") ||
            n.contains("wavenet-c") ||
            n.contains("wavenet-f") ||
            n.contains("neural2-a") ||
            n.contains("neural2-c") ||
            n.contains("neural2-f")
    }

    private fun isVoiceLikelyMale(voice: Voice): Boolean {
        val n = voice.name.lowercase()
        val features = try {
            voice.features?.map { it.lowercase() }.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        return n.contains("male") && !n.contains("female") ||
            n.contains("#male") ||
            features.any { it.contains("male") && !it.contains("female") } ||
            n.contains("-iom") ||
            n.contains("-rjs") ||
            n.contains("-bnm") ||
            n.contains("-bam") ||
            n.contains("-tpd") ||
            n.contains("-gbb") ||
            n.contains("-gbd") ||
            n.contains("wavenet-b") ||
            n.contains("wavenet-d") ||
            n.contains("neural2-b") ||
            n.contains("neural2-d")
    }

    private fun selectBestVoiceForPersona(
        voices: List<Voice>,
        langCode: String,
        preferredCountry: String,
        persona: RealVoicePersona
    ): Voice? {
        val matchingLang = voices.filter { voice ->
            voice.locale.language.equals(langCode, ignoreCase = true) ||
                voice.name.startsWith("$langCode-", ignoreCase = true)
        }
        if (matchingLang.isEmpty()) return null

        val sortedByQuality = matchingLang.sortedWith(
            compareByDescending<Voice> {
                val genderMatches = when (persona.gender) {
                    VoiceGender.FEMALE -> isVoiceLikelyFemale(it)
                    VoiceGender.MALE -> isVoiceLikelyMale(it)
                }
                if (genderMatches) 2 else if (!isVoiceLikelyFemale(it) && !isVoiceLikelyMale(it)) 1 else 0
            }
                .thenByDescending { it.locale.country.equals(preferredCountry, ignoreCase = true) }
                .thenByDescending { !it.isNetworkConnectionRequired }
                .thenByDescending { it.quality }
                .thenBy { it.latency }
        )

        val genderMatchedList = sortedByQuality.filter {
            when (persona.gender) {
                VoiceGender.FEMALE -> isVoiceLikelyFemale(it)
                VoiceGender.MALE -> isVoiceLikelyMale(it)
            }
        }

        if (genderMatchedList.isNotEmpty()) {
            val personaVariantIndex = REAL_VOICE_PERSONAS
                .filter { it.gender == persona.gender }
                .indexOfFirst { it.id == persona.id }
                .coerceAtLeast(0)
            return genderMatchedList[personaVariantIndex % genderMatchedList.size]
        }

        return sortedByQuality.firstOrNull()
    }

    /**
     * Automatically detects whether the user's input is Bangla or English, updates
     * [lastDetectedLanguage], and dynamically pre-configures the Android TTS locale & voice persona.
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
                targetLocale = if (detection.isBangla) preferredBanglaLocale else preferredEnglishLocale,
                settings = currentSettings
            )
        }
        return detection
    }

    private fun applyLocaleAndVoiceToTts(
        isBangla: Boolean,
        targetLocale: Locale = if (isBangla) preferredBanglaLocale else preferredEnglishLocale,
        settings: JarvisSettings = currentSettings
    ): Boolean {
        val tts = textToSpeech ?: return false
        val persona = findVoicePersonaById(settings.selectedVoicePersonaId)
        return try {
            if (isBangla) {
                val bnVoice = selectBestVoiceForPersona(
                    voices = allInstalledVoices,
                    langCode = "bn",
                    preferredCountry = targetLocale.country.ifBlank { "BD" },
                    persona = persona
                ) ?: preferredBanglaVoice

                if (bnVoice != null) {
                    tts.voice = bnVoice
                    true
                } else {
                    val res = tts.setLanguage(targetLocale)
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
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
                val enVoice = selectBestVoiceForPersona(
                    voices = allInstalledVoices,
                    langCode = "en",
                    preferredCountry = targetLocale.country.ifBlank { "GB" },
                    persona = persona
                ) ?: preferredEnglishVoice

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
        val personaChanged = currentSettings.selectedVoicePersonaId != settings.selectedVoicePersonaId
        currentSettings = settings
        updateEngineBadge(settings)
        if (personaChanged && isTtsInitialized) {
            mainHandler.post {
                probeAndConfigureLocales()
            }
        }
    }

    private fun updateEngineBadge(settings: JarvisSettings) {
        val persona = findVoicePersonaById(settings.selectedVoicePersonaId)
        val hasElevenKey = resolveElevenLabsApiKey(settings).isNotBlank()
        val hasGeminiKey = settings.customGeminiApiKey.trim().startsWith("AIza") ||
            settings.secondaryAiApiKey.trim().startsWith("AIza")
        _activeVoiceEngineLabel.value = when {
            settings.ttsEngine == "ELEVENLABS" && hasElevenKey ->
                "ELEVENLABS • ${persona.name.uppercase()}"
            settings.ttsEngine == "GEMINI_TTS" && hasGeminiKey ->
                "GEMINI HD VOICE • ${persona.name.uppercase()}"
            else ->
                "${persona.titleBn} (${persona.gender.badgeEn}) // ONLINE"
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

    private fun resolveGeminiApiKeyForTts(settings: JarvisSettings = currentSettings): String {
        val primary = settings.customGeminiApiKey.trim().removePrefix("Bearer ").trim()
        if (primary.startsWith("AIza")) return primary
        val secondary = settings.secondaryAiApiKey.trim().removePrefix("Bearer ").trim()
        if (secondary.startsWith("AIza")) return secondary
        return ""
    }

    fun setProcessingState() {
        _hudState.value = VoiceHudState.PROCESSING
        _partialTranscript.value = ""
    }

    fun setIdleState() {
        val shouldKeepListening = isContinuousMicSessionLocked ||
            _isLiveConversationActive.value ||
            currentSettings.handsFreeMode
        if (shouldKeepListening) {
            startListening(currentSettings.languageMode, wakeWordStandbyOnly = false)
        } else if (_hudState.value == VoiceHudState.PROCESSING) {
            _hudState.value = VoiceHudState.IDLE
        }
    }

    private fun cancelPendingRetry() {
        pendingRetryRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingRetryRunnable = null
    }

    /**
     * Starts listening with the microphone.
     * When [wakeWordStandbyOnly] is false, locks in the continuous session if continuousBackgroundMic
     * or live conversation is active, so the microphone never drops out between utterances!
     */
    fun startListening(
        languageMode: String = currentSettings.languageMode,
        wakeWordStandbyOnly: Boolean = false
    ) {
        cancelPendingRetry()
        if (!wakeWordStandbyOnly) {
            haltPlaybackInternal()
            if (currentSettings.continuousBackgroundMic) {
                isContinuousMicSessionLocked = true
                _isLiveConversationActive.value = true
            }
        }
        isListeningForWakeWordOnly = wakeWordStandbyOnly

        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    isRecognizerActive = false
                    isContinuousMicSessionLocked = false
                    _isLiveConversationActive.value = false
                    _voiceStatusMessage.value =
                        "SpeechRecognizer service not available on this device. Use text input or install Google Speech Services."
                    _hudState.value = VoiceHudState.IDLE
                    return@post
                }

                // If recognizer is already actively listening, just keep it running smoothly without disturbance
                if (isRecognizerActive && speechRecognizer != null) {
                    if (!wakeWordStandbyOnly) {
                        _hudState.value = VoiceHudState.LISTENING
                        _voiceStatusMessage.value =
                            "মাইক সচল আছে • কথা বলুন, ${currentSettings.bossTitle} (অবিরাম লিসেনিং চালু)…"
                    }
                    return@post
                }

                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                    speechRecognizer?.setRecognitionListener(createRecognitionListener())
                }

                val localeTag = when (languageMode.uppercase()) {
                    "BN" -> "bn-BD"
                    "EN" -> "en-US"
                    else -> Locale.getDefault().toLanguageTag().ifBlank { "bn-BD" }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    // Extended silence windows so the mic listens patiently without cutting off mid-thought
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
                }

                _partialTranscript.value = ""
                isRecognizerActive = true
                if (wakeWordStandbyOnly) {
                    _hudState.value = VoiceHudState.IDLE
                    _voiceStatusMessage.value =
                        "Wake Word Ready • Say \"Hey JARVIS\" or tap the Mic button."
                } else {
                    _hudState.value = VoiceHudState.LISTENING
                    _voiceStatusMessage.value =
                        "মাইক চালু আছে ($localeTag) • কথা বলুন, ${currentSettings.bossTitle} (মাইক বন্ধ হবে না)…"
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting SpeechRecognizer", e)
                isRecognizerActive = false
                if (isContinuousMicSessionLocked) {
                    scheduleSilentContinuousRearm(delayMs = 800L, recreateRecognizer = true)
                } else {
                    _hudState.value = VoiceHudState.IDLE
                    _voiceStatusMessage.value = "Microphone error: ${e.localizedMessage}"
                }
            }
        }
    }

    /**
     * Silently re-arms the SpeechRecognizer without changing _hudState to IDLE,
     * preventing any UI jumping, flickering, or interruption while Continuous Mic is ON.
     */
    private fun scheduleSilentContinuousRearm(
        delayMs: Long = 350L,
        recreateRecognizer: Boolean = false
    ) {
        cancelPendingRetry()
        val runnable = Runnable {
            pendingRetryRunnable = null
            if (!isContinuousMicSessionLocked && !_isLiveConversationActive.value && !currentSettings.handsFreeMode) {
                return@Runnable
            }
            if (_hudState.value == VoiceHudState.SPEAKING || _hudState.value == VoiceHudState.PROCESSING) {
                return@Runnable
            }
            if (recreateRecognizer) {
                try {
                    speechRecognizer?.destroy()
                } catch (_: Exception) {
                }
                speechRecognizer = null
            }
            isRecognizerActive = false
            startListening(
                languageMode = currentSettings.languageMode,
                wakeWordStandbyOnly = false
            )
        }
        pendingRetryRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    fun startWakeWordStandby(languageMode: String = currentSettings.languageMode) {
        _isWakeWordStandbyActive.value = true
    }

    fun stopWakeWordStandby() {
        _isWakeWordStandbyActive.value = false
        cancelPendingRetry()
        if (isListeningForWakeWordOnly) {
            isListeningForWakeWordOnly = false
            stopListening()
        }
    }

    fun playWakeWordChime() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 140)
            mainHandler.postDelayed({
                try {
                    toneGen.release()
                } catch (_: Exception) {
                }
            }, 220L)
        } catch (_: Exception) {
        }
    }

    /**
     * Explicitly stops listening when the user taps the Stop button.
     */
    fun stopListening() {
        cancelPendingRetry()
        isListeningForWakeWordOnly = false
        isRecognizerActive = false
        isContinuousMicSessionLocked = false
        _isLiveConversationActive.value = false
        consecutiveSilenceRetries = 0
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
            _voiceStatusMessage.value = "মাইক্রোফোন থামানো হয়েছে। কথা বলতে আবার মাইকে ট্যাপ করুন।"
        }
    }

    fun setLiveConversationActive(active: Boolean) {
        _isLiveConversationActive.value = active
        isContinuousMicSessionLocked = active
        if (!active) {
            cancelPendingRetry()
            consecutiveSilenceRetries = 0
        }
    }

    /**
     * Speaks the AI response using the selected Real Human Voice Persona:
     * 1. ElevenLabs Neural Voice (if selected & key present)
     * 2. Gemini 2.5 Flash Native Audio TTS (if selected & Gemini key present)
     * 3. Android Neural Multi-Voice Phone TTS (with automatic Male/Female voice & natural pitch/rate tuning)
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

        // Stop any active recognizer while JARVIS speaks so it doesn't hear its own voice
        cancelPendingRetry()
        if (isRecognizerActive) {
            isRecognizerActive = false
            mainHandler.post {
                try {
                    speechRecognizer?.cancel()
                } catch (_: Exception) {
                }
            }
        }

        haltPlaybackInternal()
        onSpeakingFinishedCallback = onComplete

        val persona = findVoicePersonaById(settings.selectedVoicePersonaId)
        val elevenApiKey = resolveElevenLabsApiKey(settings)
        val geminiApiKey = resolveGeminiApiKeyForTts(settings)

        when {
            !forcePhoneTts && settings.ttsEngine == "ELEVENLABS" && elevenApiKey.isNotBlank() -> {
                scope.launch {
                    val success = speakWithElevenLabs(
                        text = cleanText,
                        languageHint = languageHint,
                        settings = settings,
                        voiceId = persona.elevenLabsVoiceId,
                        apiKey = elevenApiKey
                    )
                    if (!success) {
                        recordFallbackReason("ElevenLabs unavailable — switched to Real Voice (${persona.titleBn})")
                        speakWithPhoneTts(cleanText, languageHint, settings, isFallback = true)
                    }
                }
            }

            !forcePhoneTts && settings.ttsEngine == "GEMINI_TTS" && geminiApiKey.isNotBlank() -> {
                scope.launch {
                    val success = speakWithGeminiNativeTts(
                        text = cleanText,
                        persona = persona,
                        apiKey = geminiApiKey,
                        languageHint = languageHint,
                        settings = settings
                    )
                    if (!success) {
                        recordFallbackReason("Gemini Audio TTS unavailable — switched to Real Voice (${persona.titleBn})")
                        speakWithPhoneTts(cleanText, languageHint, settings, isFallback = true)
                    }
                }
            }

            else -> {
                speakWithPhoneTts(cleanText, languageHint, settings, isFallback = false)
            }
        }
    }

    private fun recordFallbackReason(reason: String) {
        _ttsDiagnostics.value = _ttsDiagnostics.value.copy(lastFallbackReason = reason)
    }

    /**
     * Synthesizes ultra-natural human speech using Gemini's `gemini-2.5-flash-preview-tts`
     * with prebuilt Male (`Charon`, `Fenrir`, `Puck`) and Female (`Aoede`, `Kore`, `Leda`) voices.
     */
    private suspend fun speakWithGeminiNativeTts(
        text: String,
        persona: RealVoicePersona,
        apiKey: String,
        languageHint: String,
        settings: JarvisSettings
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                _hudState.value = VoiceHudState.SPEAKING
                _voiceStatusMessage.value = "Synthesizing Gemini Real Voice (${persona.titleBn})…"
            }

            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=$apiKey"

            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", text))
                        )
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("responseModalities", JSONArray().put("AUDIO"))
                        put(
                            "speechConfig",
                            JSONObject().put(
                                "voiceConfig",
                                JSONObject().put(
                                    "prebuiltVoiceConfig",
                                    JSONObject().put("voiceName", persona.geminiVoiceName)
                                )
                            )
                        )
                    }
                )
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyStr = response.body?.string().orEmpty()
            response.close()

            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini TTS HTTP ${response.code}: ${bodyStr.take(120)}")
                return@withContext false
            }

            val root = JSONObject(bodyStr)
            val base64Audio = root.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optJSONObject("inlineData")
                ?.optString("data")
                .orEmpty()

            if (base64Audio.isBlank()) return@withContext false

            val pcmBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            if (pcmBytes.isEmpty()) return@withContext false

            val wavFile = File(context.cacheDir, "jarvis_gemini_real_voice.wav")
            writePcmToWavFile(pcmBytes, wavFile, sampleRate = 24000, channels = 1, bitsPerSample = 16)

            withContext(Dispatchers.Main) {
                playAudioFileWithFallback(
                    file = wavFile,
                    fallbackText = text,
                    languageHint = languageHint,
                    settings = settings,
                    engineBadge = "Gemini Real Voice (${persona.titleBn})"
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Native TTS error — falling back to Phone Neural TTS", e)
            false
        }
    }

    private fun writePcmToWavFile(
        pcmData: ByteArray,
        wavFile: File,
        sampleRate: Int = 24000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val totalDataLen = pcmData.size + 36

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalDataLen)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // Subchunk1Size for PCM
            putShort(1.toShort()) // AudioFormat 1 = PCM
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign)
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcmData.size)
        }.array()

        FileOutputStream(wavFile).use { fos ->
            fos.write(header)
            fos.write(pcmData)
        }
    }

    private suspend fun speakWithElevenLabs(
        text: String,
        languageHint: String,
        settings: JarvisSettings,
        voiceId: String,
        apiKey: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val persona = findVoicePersonaById(settings.selectedVoicePersonaId)
            withContext(Dispatchers.Main) {
                _hudState.value = VoiceHudState.SPEAKING
                _voiceStatusMessage.value = "Synthesizing ElevenLabs Real Voice (${persona.titleBn})…"
            }

            val safeVoiceId = voiceId.trim().ifBlank { persona.elevenLabsVoiceId }
            val url = "https://api.elevenlabs.io/v1/text-to-speech/$safeVoiceId?output_format=mp3_44100_128"

            val payload = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                put(
                    "voice_settings",
                    JSONObject().apply {
                        put("stability", 0.52)
                        put("similarity_boost", 0.82)
                        put("style", 0.28)
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
                playAudioFileWithFallback(
                    file = outFile,
                    fallbackText = text,
                    languageHint = languageHint,
                    settings = settings,
                    engineBadge = "ElevenLabs (${persona.titleBn})"
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "ElevenLabs synthesis failed — triggering Phone Neural TTS fallback", e)
            false
        }
    }

    private fun playAudioFileWithFallback(
        file: File,
        fallbackText: String,
        languageHint: String,
        settings: JarvisSettings,
        engineBadge: String
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
                    _voiceStatusMessage.value = "JARVIS কথা বলছে ($engineBadge)"
                    startSpeakingWaveformAnimation()
                    player.start()
                }
                setOnCompletionListener { player ->
                    player.release()
                    if (mediaPlayer == player) mediaPlayer = null
                    finishSpeakingAndResumeMicIfNeeded()
                }
                setOnErrorListener { player, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra — falling back to Phone TTS")
                    player.release()
                    if (mediaPlayer == player) mediaPlayer = null
                    recordFallbackReason("MediaPlayer stream error ($what) — switched to Android Neural TTS")
                    speakWithPhoneTts(fallbackText, languageHint, settings, isFallback = true)
                    true
                }
            }
            mediaPlayer = mp
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play synthesized audio — falling back to Phone TTS", e)
            recordFallbackReason("Audio player exception — switched to Android Neural TTS")
            speakWithPhoneTts(fallbackText, languageHint, settings, isFallback = true)
        }
    }

    /**
     * Speaks text using Android's Neural TextToSpeech engine, matching the selected
     * Male or Female [RealVoicePersona] (`jarvis_male_classic`, `friday_female_clear`, etc.)
     * and automatically switching between Bangla (`bn-BD`/`bn-IN`) and English (`en-GB`/`en-US`).
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

            val persona = findVoicePersonaById(settings.selectedVoicePersonaId)
            val resolvedLocale = LanguageDetectionHelper.resolveDynamicTtsLocale(
                textToSpeak = text,
                languageHint = languageHint,
                preferredBanglaLocale = preferredBanglaLocale,
                preferredEnglishLocale = preferredEnglishLocale
            )
            val containsBangla = LanguageDetectionHelper.isBanglaUtterance(text, languageHint)

            applyLocaleAndVoiceToTts(
                isBangla = containsBangla,
                targetLocale = resolvedLocale,
                settings = settings
            )

            // Apply the user's or persona's natural human rate & pitch
            val effectiveRate = settings.speechRate.coerceIn(0.5f, 2.0f)
            val effectivePitch = settings.speechPitch.coerceIn(0.5f, 2.0f)
            tts.setSpeechRate(effectiveRate)
            tts.setPitch(effectivePitch)

            val langBadge = if (containsBangla) "বাংলা" else "English"
            _hudState.value = VoiceHudState.SPEAKING
            _voiceStatusMessage.value =
                "JARVIS কথা বলছে • ${persona.titleBn} ($langBadge)"
            startSpeakingWaveformAnimation()

            val segments = splitIntoSafeTtsSegments(text)
            var hasError = false
            segments.forEachIndexed { index, segment ->
                val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                val suffix = if (index == segments.lastIndex) "_FINAL" else "_PART_$index"
                val utteranceId = "${UUID.randomUUID()}$suffix"

                val segmentIsBangla = LanguageDetectionHelper.isBanglaUtterance(segment, languageHint)
                val segmentLocale = LanguageDetectionHelper.resolveDynamicTtsLocale(
                    textToSpeak = segment,
                    languageHint = if (segmentIsBangla) "bn" else "en",
                    preferredBanglaLocale = preferredBanglaLocale,
                    preferredEnglishLocale = preferredEnglishLocale
                )
                applyLocaleAndVoiceToTts(
                    isBangla = segmentIsBangla,
                    targetLocale = segmentLocale,
                    settings = settings
                )
                tts.setSpeechRate(effectiveRate)
                tts.setPitch(effectivePitch)

                val params = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                val result = tts.speak(segment, queueMode, params, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    hasError = true
                }
            }

            if (hasError) {
                _voiceStatusMessage.value = "Android TTS playback encountered an engine error."
                finishSpeakingAndResumeMicIfNeeded()
            } else {
                speakingWatchdogJob?.cancel()
                val estimatedMs = (text.length * 80L).coerceIn(2600L, 16000L)
                speakingWatchdogJob = scope.launch {
                    delay(estimatedMs)
                    if (_hudState.value == VoiceHudState.SPEAKING) {
                        finishSpeakingAndResumeMicIfNeeded()
                    }
                }
            }
        }
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
                if (isContinuousMicSessionLocked || _isLiveConversationActive.value) {
                    scheduleSilentContinuousRearm(delayMs = 350L)
                } else {
                    _hudState.value = VoiceHudState.IDLE
                    _voiceStatusMessage.value = "ভয়েস আউটপুট থামানো হয়েছে।"
                }
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
                isRecognizerActive = true
                if (!isListeningForWakeWordOnly) {
                    _hudState.value = VoiceHudState.LISTENING
                }
            }

            override fun onBeginningOfSpeech() {
                if (!isListeningForWakeWordOnly) {
                    _hudState.value = VoiceHudState.LISTENING
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (!isListeningForWakeWordOnly && _hudState.value == VoiceHudState.LISTENING) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.08f, 1.0f)
                    _audioLevel.value = (_audioLevel.value * 0.65f + normalized * 0.35f).coerceIn(0.08f, 1.0f)
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isRecognizerActive = false
                _audioLevel.value = 0.08f
            }

            override fun onError(error: Int) {
                if (!isRecognizerActive && error == SpeechRecognizer.ERROR_CLIENT) {
                    return
                }
                isRecognizerActive = false
                _audioLevel.value = 0.08f

                // Fatal permission error cannot be retried until user grants permission
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    isContinuousMicSessionLocked = false
                    _isLiveConversationActive.value = false
                    _hudState.value = VoiceHudState.IDLE
                    _voiceStatusMessage.value = "মাইক্রোফোন পারমিশন প্রয়োজন (Microphone permission required)।"
                    return
                }

                val needsRecognizerReset = error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                    error == SpeechRecognizer.ERROR_CLIENT ||
                    error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED

                if (needsRecognizerReset) {
                    try {
                        speechRecognizer?.destroy()
                    } catch (_: Exception) {
                    }
                    speechRecognizer = null
                }

                // CRITICAL: If Continuous Mic / Live Conversation is active, NEVER drop to IDLE on silence!
                // Keep _hudState = LISTENING steady and smoothly re-arm listening so the mic never stops!
                val shouldKeepMicAlive = isContinuousMicSessionLocked ||
                    _isLiveConversationActive.value ||
                    currentSettings.handsFreeMode

                if (shouldKeepMicAlive &&
                    _hudState.value != VoiceHudState.SPEAKING &&
                    _hudState.value != VoiceHudState.PROCESSING
                ) {
                    _hudState.value = VoiceHudState.LISTENING
                    _voiceStatusMessage.value =
                        "মাইক সচল আছে • কথা বলুন, ${currentSettings.bossTitle} (অবিরাম লিসেনিং চালু)…"
                    val retryDelay = if (needsRecognizerReset) 650L else 350L
                    scheduleSilentContinuousRearm(
                        delayMs = retryDelay,
                        recreateRecognizer = needsRecognizerReset
                    )
                    return
                }

                isListeningForWakeWordOnly = false
                _hudState.value = VoiceHudState.IDLE
                val errorText = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "কোনো কথা শোনা যায়নি। কথা বলতে মাইকে ট্যাপ করুন, Boss।"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "মাইক বিরতিতে আছে। কথা বলতে মাইকে ট্যাপ করুন, Boss।"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "নেটওয়ার্ক সংযোগ চেক করুন (Speech recognition network error)।"
                    else -> "ভয়েস প্রস্তুত • কথা বলতে মাইকে ট্যাপ করুন।"
                }
                _voiceStatusMessage.value = errorText
            }

            override fun onResults(results: Bundle?) {
                isRecognizerActive = false
                _audioLevel.value = 0.08f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val primaryTranscript = matches.firstOrNull()?.trim().orEmpty()
                _partialTranscript.value = ""

                if (primaryTranscript.isBlank()) {
                    if (isContinuousMicSessionLocked || _isLiveConversationActive.value || currentSettings.handsFreeMode) {
                        _hudState.value = VoiceHudState.LISTENING
                        scheduleSilentContinuousRearm(delayMs = 300L)
                    } else {
                        _hudState.value = VoiceHudState.IDLE
                    }
                    return
                }

                var wakeMatch: WakeWordMatchResult = WakeWordMatchResult.NoWakeWord
                for (candidate in matches) {
                    val analyzed = WakeWordDetector.analyzeTranscript(candidate)
                    if (analyzed !is WakeWordMatchResult.NoWakeWord) {
                        wakeMatch = analyzed
                        break
                    }
                }

                when (val matched = wakeMatch) {
                    is WakeWordMatchResult.WakeWordOnly -> {
                        consecutiveSilenceRetries = 0
                        isListeningForWakeWordOnly = false
                        isContinuousMicSessionLocked = true
                        _isLiveConversationActive.value = true
                        _lastWakeWordEvent.value = matched.matchedTrigger
                        playWakeWordChime()
                        _voiceStatusMessage.value =
                            "Wake Word \"${matched.matchedTrigger}\" detected! Listening continuously, Boss…"
                        if (onWakeWordTriggered != null) {
                            onWakeWordTriggered.invoke(matched.matchedTrigger, matched.isBanglaTrigger, null)
                        } else {
                            startListening(currentSettings.languageMode, wakeWordStandbyOnly = false)
                        }
                    }

                    is WakeWordMatchResult.WakeWordWithCommand -> {
                        consecutiveSilenceRetries = 0
                        isListeningForWakeWordOnly = false
                        isContinuousMicSessionLocked = true
                        _isLiveConversationActive.value = true
                        _lastWakeWordEvent.value = matched.matchedTrigger
                        playWakeWordChime()
                        val detected = detectAndConfigureLanguage(matched.command, currentSettings.languageMode)
                        _voiceStatusMessage.value =
                            "Wake Word \"${matched.matchedTrigger}\" + ${detected.language.nativeLabel} command — processing…"
                        _hudState.value = VoiceHudState.PROCESSING
                        if (onWakeWordTriggered != null) {
                            onWakeWordTriggered.invoke(matched.matchedTrigger, matched.isBanglaTrigger, matched.command)
                        } else {
                            onSpeechResult(matched.command)
                        }
                    }

                    is WakeWordMatchResult.NoWakeWord -> {
                        consecutiveSilenceRetries = 0
                        isListeningForWakeWordOnly = false
                        val detected = detectAndConfigureLanguage(primaryTranscript, currentSettings.languageMode)
                        _voiceStatusMessage.value =
                            "শোনা হয়েছে (${detected.language.nativeLabel}): “$primaryTranscript” — উত্তর তৈরি হচ্ছে…"
                        _hudState.value = VoiceHudState.PROCESSING
                        onSpeechResult(primaryTranscript)
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partialList = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .orEmpty()
                val partial = partialList.firstOrNull()?.trim().orEmpty()
                if (partial.isBlank()) return

                _partialTranscript.value = partial
                detectAndConfigureLanguage(partial, currentSettings.languageMode)
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
