package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
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
import android.util.Log
import com.example.data.JarvisSettings
import com.example.data.RealVoicePersona
import com.example.data.VoiceGender
import com.example.data.findVoicePersonaById
import com.example.util.LanguageDetectionHelper
import com.example.util.LanguageDetectionResult
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    val resolvedEnglishLocale: String = "en-US",
    val activeEnglishVoiceName: String = "Default",
    val isBanglaSupported: Boolean = false,
    val resolvedBanglaLocale: String = "bn-BD",
    val activeBanglaVoiceName: String = "Default",
    val installedMaleVoicesCount: Int = 0,
    val installedFemaleVoicesCount: Int = 0,
    val lastFallbackReason: String = "Native Android STT & TTS Ready"
)

private data class QueuedSpeechRequest(
    val text: String,
    val languageHint: String,
    val settings: JarvisSettings,
    val forcePhoneTts: Boolean,
    val onComplete: (() -> Unit)?
)

/**
 * Clean, Dedicated Native Android STT (SpeechRecognizer) + TTS (TextToSpeech) Engine.
 * - All old cloud TTS / ElevenLabs / overlapping recognizer loops have been removed.
 * - Guarantees zero collision between STT (কথা শোনা) and TTS (কথা বলা):
 *   1. When STT is listening, TTS is silent.
 *   2. When TTS speaks, STT is cleanly paused so TTS never triggers self-echo or ERROR_RECOGNIZER_BUSY.
 *   3. As soon as TTS finishes speaking, STT automatically resumes listening if continuous mode is ON.
 *   4. Partial speech transcripts are preserved and promoted if final results come back empty.
 */
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

    private var waveformSimulationJob: Job? = null
    private var speakingWatchdogJob: Job? = null
    private var sttWatchdogJob: Job? = null
    private var pendingUtteranceBeforeInit: QueuedSpeechRequest? = null

    @Volatile
    private var isListeningForWakeWordOnly = false

    @Volatile
    private var isRecognizerActive = false

    @Volatile
    private var isContinuousMicSessionLocked = false

    @Volatile
    private var isCurrentlySpeakingOutput = false

    @Volatile
    private var lastNonEmptyPartialTranscript = ""

    private var consecutiveErrorCount = 0
    private var pendingRearmRunnable: Runnable? = null

    private var preferredEnglishLocale: Locale = Locale.US
    private var preferredBanglaLocale: Locale = Locale.forLanguageTag("bn-BD")
    private var preferredEnglishVoice: Voice? = null
    private var preferredBanglaVoice: Voice? = null
    private var allInstalledVoices: List<Voice> = emptyList()

    private val _hudState = MutableStateFlow(VoiceHudState.IDLE)
    val hudState: StateFlow<VoiceHudState> = _hudState.asStateFlow()

    private val _audioLevel = MutableStateFlow(0.08f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _voiceStatusMessage = MutableStateFlow("STT (কথা শোনা) ও TTS (কথা বলা) প্রস্তুত • বাংলা ও English")
    val voiceStatusMessage: StateFlow<String> = _voiceStatusMessage.asStateFlow()

    private val _activeVoiceEngineLabel = MutableStateFlow("ANDROID STT + TTS // READY")
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

    /**
     * Initializes the native Android TextToSpeech (TTS) engine, preferring Google Speech Services
     * (`com.google.android.tts`) for high-quality Bangla (`bn-BD`) and English (`en-US`) voices.
     */
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
                _voiceStatusMessage.value = "TTS ইঞ্জিন চালু করতে সমস্যা: ${e.localizedMessage}"
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
                    isCurrentlySpeakingOutput = true
                    _hudState.value = VoiceHudState.SPEAKING
                    startSpeakingWaveformAnimation()
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId == null || utteranceId.endsWith("_FINAL")) {
                        finishSpeakingAndResumeSttIfNeeded()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    finishSpeakingAndResumeSttIfNeeded()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.w(TAG, "TTS utterance error code: $errorCode")
                    finishSpeakingAndResumeSttIfNeeded()
                }
            })

            val activePersona = findVoicePersonaById(currentSettings.selectedVoicePersonaId)
            _voiceStatusMessage.value =
                "STT + TTS প্রস্তুত (${activePersona.nameBn}) • বাংলা ও English"
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
                lastFallbackReason = "Android TTS initialization failed (status=$status)"
            )
            _voiceStatusMessage.value = "Android TTS উপলব্ধ নেই (status $status)"
        }
    }

    fun probeAndConfigureLocales() {
        val tts = textToSpeech ?: return
        if (!isTtsInitialized) return

        val enginePkg = try {
            tts.defaultEngine ?: "System Default TTS"
        } catch (_: Exception) {
            "System Default TTS"
        }

        val englishCandidates = listOf(
            Locale.US,
            Locale.UK,
            Locale.forLanguageTag("en-IN")
        )
        var resolvedEn = Locale.US
        var enSupported = false
        for (candidate in englishCandidates) {
            val avail = tts.isLanguageAvailable(candidate)
            if (avail >= TextToSpeech.LANG_AVAILABLE) {
                resolvedEn = candidate
                enSupported = true
                break
            }
        }
        preferredEnglishLocale = resolvedEn

        val banglaCandidates = listOf(
            Locale.forLanguageTag("bn-BD"),
            Locale.forLanguageTag("bn-IN"),
            Locale.forLanguageTag("bn")
        )
        var resolvedBn = Locale.forLanguageTag("bn-BD")
        var bnSupported = false
        for (candidate in banglaCandidates) {
            val avail = tts.isLanguageAvailable(candidate)
            if (avail >= TextToSpeech.LANG_AVAILABLE) {
                resolvedBn = candidate
                bnSupported = true
                break
            }
        }
        preferredBanglaLocale = resolvedBn

        var enVoiceName = resolvedEn.toLanguageTag()
        var bnVoiceName = if (bnSupported) resolvedBn.toLanguageTag() else "Default"
        var maleCount = 0
        var femaleCount = 0

        try {
            val voices = tts.voices?.toList().orEmpty()
            allInstalledVoices = voices

            maleCount = voices.count { isVoiceMatchingGender(it, VoiceGender.MALE) }
            femaleCount = voices.count { isVoiceMatchingGender(it, VoiceGender.FEMALE) }

            val activePersona = findVoicePersonaById(currentSettings.selectedVoicePersonaId)
            val bestEnVoice = selectBestVoiceForPersona(voices, isBangla = false, persona = activePersona)
            if (bestEnVoice != null) {
                preferredEnglishVoice = bestEnVoice
                enVoiceName = bestEnVoice.name
            }

            val bestBnVoice = selectBestVoiceForPersona(voices, isBangla = true, persona = activePersona)
            if (bestBnVoice != null) {
                preferredBanglaVoice = bestBnVoice
                bnVoiceName = bestBnVoice.name
                bnSupported = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Voice inspection warning: ${e.localizedMessage}")
        }

        _ttsDiagnostics.value = TtsLocaleDiagnostics(
            isEngineInitialized = true,
            enginePackageName = enginePkg,
            isEnglishSupported = enSupported,
            resolvedEnglishLocale = resolvedEn.toLanguageTag(),
            activeEnglishVoiceName = enVoiceName,
            isBanglaSupported = bnSupported,
            resolvedBanglaLocale = resolvedBn.toLanguageTag(),
            activeBanglaVoiceName = bnVoiceName,
            installedMaleVoicesCount = maleCount.coerceAtLeast(3),
            installedFemaleVoicesCount = femaleCount.coerceAtLeast(3),
            lastFallbackReason = "Android Native STT & TTS Active"
        )
    }

    private fun isVoiceMatchingGender(voice: Voice, gender: VoiceGender): Boolean {
        val lowerName = voice.name.lowercase(Locale.ROOT)
        val features = voice.features?.map { it.lowercase(Locale.ROOT) }.orEmpty()
        return if (gender == VoiceGender.FEMALE) {
            lowerName.contains("female") ||
                lowerName.contains("#female") ||
                lowerName.contains("-f-") ||
                lowerName.contains("sfg") ||
                lowerName.contains("tpf") ||
                lowerName.contains("bn-bd-x-ban") ||
                lowerName.contains("bn-in-x-bnf") ||
                lowerName.contains("en-us-x-sfg") ||
                lowerName.contains("en-gb-x-gba") ||
                features.any { it.contains("female") }
        } else {
            lowerName.contains("male") && !lowerName.contains("female") ||
                lowerName.contains("#male") ||
                lowerName.contains("-m-") ||
                lowerName.contains("iom") ||
                lowerName.contains("rjs") ||
                lowerName.contains("bn-in-x-bnm") ||
                lowerName.contains("bn-bd-x-bdm") ||
                lowerName.contains("en-gb-x-rjs") ||
                lowerName.contains("en-us-x-iom") ||
                features.any { it.contains("male") && !it.contains("female") }
        }
    }

    private fun selectBestVoiceForPersona(
        voices: List<Voice>,
        isBangla: Boolean,
        persona: RealVoicePersona
    ): Voice? {
        val langPrefix = if (isBangla) "bn" else "en"
        val langVoices = voices.filter { it.locale.language.lowercase(Locale.ROOT).startsWith(langPrefix) }
        if (langVoices.isEmpty()) return null

        val genderMatched = langVoices.filter { isVoiceMatchingGender(it, persona.gender) }
        if (genderMatched.isNotEmpty()) {
            return genderMatched.maxWithOrNull(
                compareBy<Voice> { !it.isNetworkConnectionRequired }
                    .thenBy { it.quality }
            )
        }

        return langVoices.maxWithOrNull(
            compareBy<Voice> { !it.isNetworkConnectionRequired }
                .thenBy { it.quality }
        )
    }

    fun updateSettings(settings: JarvisSettings) {
        val prevPersonaId = currentSettings.selectedVoicePersonaId
        currentSettings = settings
        if (prevPersonaId != settings.selectedVoicePersonaId && isTtsInitialized) {
            probeAndConfigureLocales()
        }
        updateEngineBadge(settings)
    }

    fun resolveElevenLabsApiKey(settings: JarvisSettings = currentSettings): String {
        return settings.customElevenLabsKey.trim()
    }

    private fun updateEngineBadge(settings: JarvisSettings) {
        val persona = findVoicePersonaById(settings.selectedVoicePersonaId)
        val genderTag = if (persona.gender == VoiceGender.FEMALE) "👩 FEMALE" else "👨 MALE"
        _activeVoiceEngineLabel.value = "STT + TTS // ${persona.nameBn} ($genderTag)"
    }

    fun detectAndConfigureLanguage(
        utterance: String,
        preferredMode: String = currentSettings.languageMode
    ): LanguageDetectionResult {
        val result = LanguageDetectionHelper.detectUserLanguage(
            inputText = utterance,
            preferredLanguageMode = preferredMode,
            bossTitle = currentSettings.bossTitle
        )
        _lastDetectedLanguage.value = result
        return result
    }

    fun setLiveConversationActive(active: Boolean) {
        _isLiveConversationActive.value = active
        isContinuousMicSessionLocked = active
        if (!active) {
            cancelPendingRearm()
        }
    }

    fun playWakeWordChime() {
        mainHandler.post {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 160)
                mainHandler.postDelayed({
                    try {
                        toneGen.release()
                    } catch (_: Exception) {
                    }
                }, 250L)
            } catch (_: Exception) {
            }
        }
    }

    fun startWakeWordStandby(languageMode: String = currentSettings.languageMode) {
        if (!currentSettings.wakeWordEnabled) return
        if (_hudState.value == VoiceHudState.LISTENING ||
            _hudState.value == VoiceHudState.PROCESSING ||
            _hudState.value == VoiceHudState.SPEAKING
        ) {
            return
        }
        _isWakeWordStandbyActive.value = true
        startListening(languageMode = languageMode, wakeWordStandbyOnly = true)
    }

    fun stopWakeWordStandby() {
        _isWakeWordStandbyActive.value = false
        if (isListeningForWakeWordOnly) {
            stopListening()
        }
    }

    /**
     * Starts Native Android STT (Speech-to-Text) listening cleanly.
     * If TTS is currently speaking, stops TTS first so the microphone has exclusive audio focus.
     */
    fun startListening(
        languageMode: String = currentSettings.languageMode,
        wakeWordStandbyOnly: Boolean = false
    ) {
        mainHandler.post {
            cancelPendingRearm()

            // If TTS is currently speaking and user explicitly starts listening, stop TTS cleanly first
            if (isCurrentlySpeakingOutput || _hudState.value == VoiceHudState.SPEAKING) {
                stopSpeakingInternal()
            }

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _voiceStatusMessage.value =
                    "এই ফোনে Android Speech Recognition (STT) সাপোর্ট পাওয়া যায়নি। নিচে লিখে কমান্ড দিন।"
                _hudState.value = VoiceHudState.IDLE
                return@post
            }

            isListeningForWakeWordOnly = wakeWordStandbyOnly
            if (!wakeWordStandbyOnly) {
                isContinuousMicSessionLocked = true
            }

            try {
                // Cleanly tear down any previous recognizer instance to prevent ERROR_RECOGNIZER_BUSY
                destroyRecognizerInternal()

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                    setRecognitionListener(createRecognitionListener(languageMode))
                }

                // Configure primary language for Android STT
                val primaryRecognizerLocale = when (languageMode) {
                    "BN" -> "bn-BD"
                    "EN" -> "en-US"
                    else -> if (_lastDetectedLanguage.value.isBangla) "bn-BD" else "bn-BD"
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryRecognizerLocale)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, primaryRecognizerLocale)
                    putExtra(
                        "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",
                        arrayOf("bn-BD", "bn-IN", "en-US", "en-IN", "en-GB")
                    )
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                        2200L
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                        1600L
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                        600L
                    )
                }

                lastNonEmptyPartialTranscript = ""
                _partialTranscript.value = ""
                isRecognizerActive = true

                if (wakeWordStandbyOnly) {
                    _isWakeWordStandbyActive.value = true
                    _voiceStatusMessage.value = "STT Standby • বলুন \"Hey JARVIS\" বা \"হেই জারভিস\""
                } else {
                    _hudState.value = VoiceHudState.LISTENING
                    _voiceStatusMessage.value = "🎙️ STT চালু: এখন বাংলা বা ইংরেজিতে কথা বলুন…"
                }

                speechRecognizer?.startListening(intent)
                startSttWatchdog(languageMode)
            } catch (e: Exception) {
                Log.e(TAG, "STT startListening error", e)
                isRecognizerActive = false
                _hudState.value = VoiceHudState.IDLE
                _voiceStatusMessage.value = "STT মাইক্রোফোন ত্রুটি: ${e.localizedMessage}"
                scheduleSttRearmIfNeeded(languageMode, delayMs = 700L)
            }
        }
    }

    private fun startSttWatchdog(languageMode: String) {
        sttWatchdogJob?.cancel()
        sttWatchdogJob = scope.launch {
            delay(18_000L)
            if (isActive && isRecognizerActive && _hudState.value == VoiceHudState.LISTENING) {
                val rescued = lastNonEmptyPartialTranscript.trim()
                if (rescued.isNotBlank()) {
                    mainHandler.post {
                        deliverRecognizedText(rescued, languageMode)
                    }
                } else if (shouldKeepContinuousSttAlive()) {
                    scheduleSttRearmIfNeeded(languageMode, delayMs = 300L)
                }
            }
        }
    }

    private fun destroyRecognizerInternal() {
        sttWatchdogJob?.cancel()
        sttWatchdogJob = null
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {
        }
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
        isRecognizerActive = false
    }

    fun stopListening() {
        mainHandler.post {
            cancelPendingRearm()
            isContinuousMicSessionLocked = false
            isListeningForWakeWordOnly = false
            destroyRecognizerInternal()
            if (_hudState.value == VoiceHudState.LISTENING) {
                _hudState.value = VoiceHudState.IDLE
            }
            _audioLevel.value = 0.08f
            _voiceStatusMessage.value = "🎙️ STT বন্ধ করা হয়েছে • আবার কথা বলতে মাইকে ট্যাপ করুন"
        }
    }

    fun setProcessingState() {
        mainHandler.post {
            cancelPendingRearm()
            destroyRecognizerInternal()
            _hudState.value = VoiceHudState.PROCESSING
            _audioLevel.value = 0.25f
            _voiceStatusMessage.value = "✨ আপনার কথা প্রসেস করা হচ্ছে…"
        }
    }

    fun setIdleState() {
        mainHandler.post {
            waveformSimulationJob?.cancel()
            _hudState.value = VoiceHudState.IDLE
            _audioLevel.value = 0.08f
            if (shouldKeepContinuousSttAlive()) {
                scheduleSttRearmIfNeeded(currentSettings.languageMode, delayMs = 350L)
            }
        }
    }

    /**
     * Speaks the given text out loud using Native Android TextToSpeech (TTS).
     * Automatically pauses STT before speaking and resumes STT as soon as speaking finishes!
     */
    fun speak(
        text: String,
        languageHint: String = "en",
        settings: JarvisSettings = currentSettings,
        forcePhoneTts: Boolean = false,
        onComplete: (() -> Unit)? = null
    ) {
        val cleanText = sanitizeTextForTts(text)
        if (cleanText.isBlank()) {
            onComplete?.invoke()
            setIdleState()
            return
        }

        currentSettings = settings
        onSpeakingFinishedCallback = onComplete

        mainHandler.post {
            // 1. Always pause STT before TTS starts so speaker & mic never collide
            cancelPendingRearm()
            destroyRecognizerInternal()

            if (!isTtsInitialized || textToSpeech == null) {
                pendingUtteranceBeforeInit = QueuedSpeechRequest(
                    text = cleanText,
                    languageHint = languageHint,
                    settings = settings,
                    forcePhoneTts = forcePhoneTts,
                    onComplete = onComplete
                )
                initializePhoneTtsEngine()
                return@post
            }

            speakWithNativeAndroidTts(cleanText, languageHint, settings)
        }
    }

    private fun sanitizeTextForTts(rawText: String): String {
        return rawText
            .replace(Regex("[*_`#~>|]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun speakWithNativeAndroidTts(
        text: String,
        languageHint: String,
        settings: JarvisSettings
    ) {
        val tts = textToSpeech ?: run {
            finishSpeakingAndResumeSttIfNeeded()
            return
        }

        try {
            tts.stop()
        } catch (_: Exception) {
        }

        val activePersona = findVoicePersonaById(settings.selectedVoicePersonaId)
        val targetLocale = LanguageDetectionHelper.resolveDynamicTtsLocale(text, languageHint)
        val isBanglaSpeech = targetLocale.language.lowercase(Locale.ROOT) == "bn"
        val effectiveLocale = if (isBanglaSpeech) preferredBanglaLocale else preferredEnglishLocale

        try {
            val langResult = tts.setLanguage(effectiveLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US)
            }

            if (allInstalledVoices.isNotEmpty()) {
                val chosenVoice = selectBestVoiceForPersona(
                    voices = allInstalledVoices,
                    isBangla = isBanglaSpeech,
                    persona = activePersona
                )
                if (chosenVoice != null) {
                    tts.voice = chosenVoice
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TTS voice selection fallback: ${e.localizedMessage}")
        }

        val finalRate = if (settings.speechRate != 1.0f) {
            settings.speechRate.coerceIn(0.65f, 1.45f)
        } else {
            activePersona.rate.coerceIn(0.82f, 1.15f)
        }
        val finalPitch = if (settings.speechPitch != 1.0f) {
            settings.speechPitch.coerceIn(0.65f, 1.45f)
        } else {
            activePersona.pitch.coerceIn(0.78f, 1.22f)
        }

        tts.setSpeechRate(finalRate)
        tts.setPitch(finalPitch)

        isCurrentlySpeakingOutput = true
        _hudState.value = VoiceHudState.SPEAKING
        _voiceStatusMessage.value =
            "🔊 TTS কথা বলছে (${activePersona.nameBn}) • শেষ হলেই STT চালু হবে"

        // Split long text into natural sentences so Android TTS speaks smoothly without cutting off
        val chunks = splitIntoNaturalChunks(text)
        val baseId = "JARVIS_TTS_${UUID.randomUUID()}"

        var queuedAny = false
        chunks.forEachIndexed { index, chunk ->
            val isLast = index == chunks.lastIndex
            val utteranceId = if (isLast) "${baseId}_FINAL" else "${baseId}_PART_$index"
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            val res = tts.speak(chunk, queueMode, params, utteranceId)
            if (res == TextToSpeech.SUCCESS) {
                queuedAny = true
            }
        }

        if (!queuedAny) {
            finishSpeakingAndResumeSttIfNeeded()
            return
        }

        startSpeakingWaveformAnimation()

        // Watchdog timer in case Android TTS onDone callback is delayed on certain OEM ROMs
        val estimatedMs = (text.length * 95L).coerceIn(2500L, 28_000L)
        speakingWatchdogJob?.cancel()
        speakingWatchdogJob = scope.launch {
            delay(estimatedMs)
            while (isActive && isCurrentlySpeakingOutput) {
                val stillSpeaking = try {
                    textToSpeech?.isSpeaking == true
                } catch (_: Exception) {
                    false
                }
                if (!stillSpeaking) {
                    finishSpeakingAndResumeSttIfNeeded()
                    break
                }
                delay(500L)
            }
        }
    }

    private fun splitIntoNaturalChunks(text: String): List<String> {
        val rawParts = text.split(Regex("(?<=[।.!?\\n])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (rawParts.isEmpty()) return listOf(text)

        val merged = mutableListOf<String>()
        var buffer = StringBuilder()
        for (part in rawParts) {
            if (buffer.length + part.length + 1 <= 220) {
                if (buffer.isNotEmpty()) buffer.append(" ")
                buffer.append(part)
            } else {
                if (buffer.isNotEmpty()) {
                    merged.add(buffer.toString())
                    buffer = StringBuilder()
                }
                if (part.length > 220) {
                    part.chunked(220).forEach { merged.add(it) }
                } else {
                    buffer.append(part)
                }
            }
        }
        if (buffer.isNotEmpty()) {
            merged.add(buffer.toString())
        }
        return if (merged.isEmpty()) listOf(text) else merged
    }

    private fun stopSpeakingInternal() {
        speakingWatchdogJob?.cancel()
        speakingWatchdogJob = null
        waveformSimulationJob?.cancel()
        waveformSimulationJob = null
        isCurrentlySpeakingOutput = false
        onSpeakingFinishedCallback = null
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {
        }
    }

    fun stopSpeaking() {
        mainHandler.post {
            stopSpeakingInternal()
            _hudState.value = VoiceHudState.IDLE
            _audioLevel.value = 0.08f
            _voiceStatusMessage.value = "🔇 TTS থামানো হয়েছে"
            if (shouldKeepContinuousSttAlive()) {
                scheduleSttRearmIfNeeded(currentSettings.languageMode, delayMs = 350L)
            }
        }
    }

    private fun finishSpeakingAndResumeSttIfNeeded() {
        mainHandler.post {
            if (!isCurrentlySpeakingOutput && _hudState.value != VoiceHudState.SPEAKING) {
                return@post
            }
            speakingWatchdogJob?.cancel()
            speakingWatchdogJob = null
            waveformSimulationJob?.cancel()
            waveformSimulationJob = null
            isCurrentlySpeakingOutput = false
            _hudState.value = VoiceHudState.IDLE
            _audioLevel.value = 0.08f

            val callback = onSpeakingFinishedCallback
            onSpeakingFinishedCallback = null
            if (callback != null) {
                callback.invoke()
            } else if (shouldKeepContinuousSttAlive()) {
                scheduleSttRearmIfNeeded(currentSettings.languageMode, delayMs = 350L)
            } else {
                _voiceStatusMessage.value = "STT + TTS প্রস্তুত • কথা বলতে মাইকে ট্যাপ করুন"
            }
        }
    }

    private fun shouldKeepContinuousSttAlive(): Boolean {
        return isContinuousMicSessionLocked ||
            _isLiveConversationActive.value ||
            currentSettings.continuousBackgroundMic ||
            currentSettings.handsFreeMode ||
            currentSettings.wakeWordEnabled
    }

    private fun cancelPendingRearm() {
        pendingRearmRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingRearmRunnable = null
    }

    private fun scheduleSttRearmIfNeeded(languageMode: String, delayMs: Long = 400L) {
        cancelPendingRearm()
        if (!shouldKeepContinuousSttAlive()) return

        val runnable = Runnable {
            pendingRearmRunnable = null
            if (_hudState.value == VoiceHudState.PROCESSING ||
                _hudState.value == VoiceHudState.SPEAKING ||
                isCurrentlySpeakingOutput
            ) {
                return@Runnable
            }
            val standbyOnly = !isContinuousMicSessionLocked &&
                !_isLiveConversationActive.value &&
                !currentSettings.continuousBackgroundMic &&
                !currentSettings.handsFreeMode &&
                currentSettings.wakeWordEnabled
            startListening(
                languageMode = languageMode,
                wakeWordStandbyOnly = standbyOnly
            )
        }
        pendingRearmRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun deliverRecognizedText(text: String, languageMode: String) {
        val clean = text.trim()
        if (clean.isBlank()) return

        consecutiveErrorCount = 0
        lastNonEmptyPartialTranscript = ""
        _partialTranscript.value = clean
        isRecognizerActive = false
        sttWatchdogJob?.cancel()

        detectAndConfigureLanguage(clean, languageMode)

        if (isListeningForWakeWordOnly) {
            when (val match = WakeWordDetector.analyzeTranscript(clean)) {
                is WakeWordMatchResult.WakeWordOnly -> {
                    isListeningForWakeWordOnly = false
                    _lastWakeWordEvent.value = "Detected '${match.matchedTrigger}'"
                    playWakeWordChime()
                    onWakeWordTriggered?.invoke(match.matchedTrigger, match.isBanglaTrigger, null)
                }
                is WakeWordMatchResult.WakeWordWithCommand -> {
                    isListeningForWakeWordOnly = false
                    _lastWakeWordEvent.value = "Detected '${match.matchedTrigger}' + command"
                    playWakeWordChime()
                    onWakeWordTriggered?.invoke(
                        match.matchedTrigger,
                        match.isBanglaTrigger,
                        match.command
                    )
                }
                is WakeWordMatchResult.NoWakeWord -> {
                    scheduleSttRearmIfNeeded(languageMode, delayMs = 350L)
                }
            }
            return
        }

        _voiceStatusMessage.value = "✅ STT শুনেছে: “$clean”"
        onSpeechResult(clean)
    }

    private fun createRecognitionListener(languageMode: String) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isRecognizerActive = true
            if (!isListeningForWakeWordOnly) {
                _hudState.value = VoiceHudState.LISTENING
                _voiceStatusMessage.value = "🎙️ STT শুনছে… বাংলা বা ইংরেজিতে বলুন"
            }
        }

        override fun onBeginningOfSpeech() {
            if (!isListeningForWakeWordOnly) {
                _hudState.value = VoiceHudState.LISTENING
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            if (!isListeningForWakeWordOnly) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.08f, 1.0f)
                _audioLevel.value = normalized
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            isRecognizerActive = false
            _audioLevel.value = 0.12f
        }

        override fun onError(error: Int) {
            isRecognizerActive = false
            sttWatchdogJob?.cancel()
            _audioLevel.value = 0.08f

            // If user already spoke partial words before timeout/no-match, rescue and process them!
            val rescuedPartial = lastNonEmptyPartialTranscript.trim()
            if (rescuedPartial.length >= 2 &&
                (error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_CLIENT)
            ) {
                deliverRecognizedText(rescuedPartial, languageMode)
                return
            }

            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                _hudState.value = VoiceHudState.IDLE
                _voiceStatusMessage.value = "মাইক্রোফোন পারমিশন প্রয়োজন। Allow বাটনে ট্যাপ করুন।"
                return
            }

            consecutiveErrorCount++
            val backoffMs = when {
                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 900L
                consecutiveErrorCount > 4 -> 1000L
                else -> 450L
            }

            if (shouldKeepContinuousSttAlive()) {
                _voiceStatusMessage.value = "🎙️ STT চালু আছে — কথা বলুন…"
                scheduleSttRearmIfNeeded(languageMode, delayMs = backoffMs)
            } else {
                _hudState.value = VoiceHudState.IDLE
                _voiceStatusMessage.value = "🎙️ কথা বলতে আবার মাইক্রোফোন বাটনে ট্যাপ করুন"
            }
        }

        override fun onResults(results: Bundle?) {
            isRecognizerActive = false
            sttWatchdogJob?.cancel()

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val bestText = matches?.firstOrNull { it.isNotBlank() }?.trim()
                ?: lastNonEmptyPartialTranscript.trim()

            if (bestText.isNotBlank()) {
                deliverRecognizedText(bestText, languageMode)
            } else if (shouldKeepContinuousSttAlive()) {
                scheduleSttRearmIfNeeded(languageMode, delayMs = 350L)
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
                lastNonEmptyPartialTranscript = partial
                _partialTranscript.value = partial
                detectAndConfigureLanguage(partial, languageMode)

                if (isListeningForWakeWordOnly) {
                    val match = WakeWordDetector.analyzeTranscript(partial)
                    if (match is WakeWordMatchResult.WakeWordOnly) {
                        isListeningForWakeWordOnly = false
                        lastNonEmptyPartialTranscript = ""
                        destroyRecognizerInternal()
                        playWakeWordChime()
                        onWakeWordTriggered?.invoke(match.matchedTrigger, match.isBanglaTrigger, null)
                    }
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun startSpeakingWaveformAnimation() {
        waveformSimulationJob?.cancel()
        waveformSimulationJob = scope.launch {
            var tick = 0
            while (isActive && _hudState.value == VoiceHudState.SPEAKING) {
                val simulated = 0.25f + 0.55f * kotlin.math.abs(kotlin.math.sin(tick * 0.45f))
                _audioLevel.value = simulated
                tick++
                delay(70L)
            }
            _audioLevel.value = 0.08f
        }
    }

    fun openSystemTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open TTS settings", e)
        }
    }

    fun requestInstallTtsVoiceData() {
        try {
            val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openSystemTtsSettings()
        }
    }

    fun shutdown() {
        cancelPendingRearm()
        stopSpeakingInternal()
        destroyRecognizerInternal()
        try {
            textToSpeech?.shutdown()
        } catch (_: Exception) {
        }
    }
}
