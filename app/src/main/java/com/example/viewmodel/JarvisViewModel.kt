package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.actions.ActionExecutionResult
import com.example.actions.DeviceActionExecutor
import com.example.actions.DeviceTelemetry
import com.example.actions.InstalledAppInfo
import com.example.actions.PendingConfirmationAction
import com.example.ai.JarvisBrainService
import com.example.data.ActionLogEntity
import com.example.data.ChatMessageEntity
import com.example.data.JarvisDatabase
import com.example.data.JarvisPreferencesRepository
import com.example.data.JarvisRepository
import com.example.data.JarvisSettings
import com.example.data.MemoryEntity
import com.example.service.JarvisVoiceForegroundService
import com.example.util.LanguageDetectionResult
import com.example.voice.JarvisVoiceManager
import com.example.voice.TtsLocaleDiagnostics
import com.example.voice.VoiceHudState
import com.example.voice.WakeWordDetector
import com.example.voice.WakeWordMatchResult
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val database = JarvisDatabase.getInstance(application)
    private val repository = JarvisRepository(database.jarvisDao())
    private val preferencesRepository = JarvisPreferencesRepository(application)
    private val actionExecutor = DeviceActionExecutor(application)
    private val brainService = JarvisBrainService()

    val voiceManager = JarvisVoiceManager(
        context = application,
        scope = viewModelScope,
        onSpeechResult = { recognizedText ->
            handleRecognizedSpeech(recognizedText)
        },
        onWakeWordTriggered = { matchedTrigger, isBangla, trailingCommand ->
            handleWakeWordTriggered(matchedTrigger, isBangla, trailingCommand)
        }
    )

    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val memories: StateFlow<List<MemoryEntity>> = repository.allMemories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val actionLogs: StateFlow<List<ActionLogEntity>> = repository.allActionLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val settings: StateFlow<JarvisSettings> = preferencesRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JarvisSettings()
    )

    val hudState: StateFlow<VoiceHudState> = voiceManager.hudState
    val audioLevel: StateFlow<Float> = voiceManager.audioLevel
    val partialTranscript: StateFlow<String> = voiceManager.partialTranscript
    val voiceStatusMessage: StateFlow<String> = voiceManager.voiceStatusMessage
    val activeVoiceEngineLabel: StateFlow<String> = voiceManager.activeVoiceEngineLabel
    val ttsDiagnostics: StateFlow<TtsLocaleDiagnostics> = voiceManager.ttsDiagnostics
    val lastDetectedLanguage: StateFlow<LanguageDetectionResult> = voiceManager.lastDetectedLanguage
    val isLiveConversationActive: StateFlow<Boolean> = voiceManager.isLiveConversationActive
    val isWakeWordStandbyActive: StateFlow<Boolean> = voiceManager.isWakeWordStandbyActive
    val lastWakeWordEvent: StateFlow<String> = voiceManager.lastWakeWordEvent
    val isForegroundServiceRunning: StateFlow<Boolean> = JarvisVoiceForegroundService.isRunning

    private val _wakeWordTriggeredUiPulse = MutableStateFlow(0L)
    val wakeWordTriggeredUiPulse: StateFlow<Long> = _wakeWordTriggeredUiPulse.asStateFlow()

    private val _telemetry = MutableStateFlow(actionExecutor.getLiveTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmationAction?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmationAction?> = _pendingConfirmation.asStateFlow()

    private val _attachedDocumentName = MutableStateFlow<String?>(null)
    val attachedDocumentName: StateFlow<String?> = _attachedDocumentName.asStateFlow()

    private val _attachedDocumentContent = MutableStateFlow<String?>(null)

    private val _followUpSuggestions = MutableStateFlow(
        listOf(
            "Hey JARVIS",
            "Status report, Jarvis",
            "ফ্ল্যাশলাইট চালু করো",
            "Set alarm for 07:00",
            "ব্যাটারি কত শতাংশ আছে?"
        )
    )
    val followUpSuggestions: StateFlow<List<String>> = _followUpSuggestions.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    init {
        viewModelScope.launch {
            settings.collect { current ->
                voiceManager.updateSettings(current)
                if (current.wakeWordEnabled && _telemetry.value.hasMicPermission && voiceManager.hudState.value == VoiceHudState.IDLE) {
                    voiceManager.startWakeWordStandby(current.languageMode)
                } else if (!current.wakeWordEnabled) {
                    voiceManager.stopWakeWordStandby()
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = actionExecutor.getInstalledApps()
            val recent = repository.getRecentMessages(1)
            if (recent.isEmpty()) {
                repository.addMessage(
                    ChatMessageEntity(
                        role = "jarvis",
                        text = "Good day, Boss. JARVIS online. Say \"Hey JARVIS\" or \"হেই জারভিস\" anytime to trigger AI listening mode, or speak/type in English or বাংলা.",
                        detailedText = "Try saying: 'Hey JARVIS', 'Hey JARVIS turn on flashlight', 'Status report', or 'ফ্ল্যাশলাইট চালু করো'.",
                        language = "en",
                        actionName = "status_report",
                        actionStatus = "ONLINE"
                    )
                )
            }
        }

        // Periodic live hardware telemetry refresh
        viewModelScope.launch {
            while (isActive) {
                _telemetry.value = actionExecutor.getLiveTelemetry()
                delay(4000L)
            }
        }
    }

    fun refreshTelemetryNow() {
        val currentTelemetry = actionExecutor.getLiveTelemetry()
        _telemetry.value = currentTelemetry
        if (currentTelemetry.hasMicPermission &&
            settings.value.wakeWordEnabled &&
            voiceManager.hudState.value == VoiceHudState.IDLE
        ) {
            voiceManager.startWakeWordStandby(settings.value.languageMode)
        }
    }

    fun dismissErrorBanner() {
        _errorBanner.value = null
    }

    private fun handleWakeWordTriggered(
        matchedTrigger: String,
        isBangla: Boolean,
        trailingCommand: String?
    ) {
        _errorBanner.value = null
        _wakeWordTriggeredUiPulse.value = System.currentTimeMillis()
        voiceManager.setLiveConversationActive(true)

        viewModelScope.launch(Dispatchers.IO) {
            repository.logAction(
                toolName = "wake_word_detected",
                target = matchedTrigger,
                parametersSummary = trailingCommand ?: "Wake Word Triggered Active Listening",
                status = "ACTIVE",
                details = "Wake word '$matchedTrigger' activated JARVIS AI listening mode"
            )
        }

        if (!trailingCommand.isNullOrBlank()) {
            sendCommand(trailingCommand)
            return
        }

        // User said "Hey JARVIS" / "হেই জারভিস" to wake up the assistant:
        // Immediately enter active AI listening mode (with a quick acknowledgement if autoSpeak is on)
        val current = settings.value
        val useBangla = isBangla || current.languageMode == "BN" || lastDetectedLanguage.value.isBangla
        val wakeReply = if (useBangla) {
            "জি বস, বলুন আমি শুনছি।"
        } else {
            "Yes, ${current.bossTitle}? I'm listening."
        }

        if (current.autoSpeakReplies) {
            voiceManager.speak(
                text = wakeReply,
                languageHint = if (useBangla) "bn" else "en",
                settings = current,
                onComplete = {
                    voiceManager.startListening(
                        languageMode = settings.value.languageMode,
                        wakeWordStandbyOnly = false
                    )
                }
            )
        } else {
            voiceManager.startListening(
                languageMode = current.languageMode,
                wakeWordStandbyOnly = false
            )
        }
    }

    private fun handleRecognizedSpeech(transcript: String) {
        val trimmed = transcript.trim()
        if (trimmed.isBlank()) return

        when (val wakeAnalysis = WakeWordDetector.analyzeTranscript(trimmed)) {
            is WakeWordMatchResult.WakeWordOnly -> {
                handleWakeWordTriggered(
                    matchedTrigger = wakeAnalysis.matchedTrigger,
                    isBangla = wakeAnalysis.isBanglaTrigger,
                    trailingCommand = null
                )
            }
            is WakeWordMatchResult.WakeWordWithCommand -> {
                _wakeWordTriggeredUiPulse.value = System.currentTimeMillis()
                sendCommand(wakeAnalysis.command)
            }
            is WakeWordMatchResult.NoWakeWord -> {
                sendCommand(trimmed)
            }
        }
    }

    fun startVoiceListening() {
        _errorBanner.value = null
        refreshTelemetryNow()
        voiceManager.setLiveConversationActive(true)
        voiceManager.startListening(settings.value.languageMode, wakeWordStandbyOnly = false)
    }

    fun startLiveVoiceGreetingAndListen() {
        _errorBanner.value = null
        refreshTelemetryNow()
        voiceManager.setLiveConversationActive(true)
        val current = settings.value
        val useBangla = current.languageMode == "BN" || lastDetectedLanguage.value.isBangla
        val greeting = if (useBangla) {
            "জি বস, আমি লাইভে আছি এবং আপনার কথা শুনছি। বলুন!"
        } else {
            "Live voice connected, ${current.bossTitle}. I am listening—speak naturally in Bangla or English."
        }
        voiceManager.speak(
            text = greeting,
            languageHint = if (useBangla) "bn" else "en",
            settings = current,
            onComplete = {
                if (voiceManager.isLiveConversationActive.value) {
                    voiceManager.startListening(settings.value.languageMode, wakeWordStandbyOnly = false)
                }
            }
        )
    }

    fun setLiveConversationMode(enabled: Boolean) {
        voiceManager.setLiveConversationActive(enabled)
        if (!enabled) {
            voiceManager.stopListening()
            voiceManager.stopSpeaking()
            if (settings.value.wakeWordEnabled && _telemetry.value.hasMicPermission) {
                voiceManager.startWakeWordStandby(settings.value.languageMode)
            }
        }
    }

    fun stopVoiceListening() {
        voiceManager.stopListening()
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun speakCustomText(text: String, languageHint: String = "en") {
        voiceManager.speak(
            text = text,
            languageHint = languageHint,
            settings = settings.value
        )
    }

    fun speakWithAndroidTtsFallback(text: String, languageHint: String = "en") {
        voiceManager.speak(
            text = text,
            languageHint = languageHint,
            settings = settings.value,
            forcePhoneTts = true
        )
    }

    fun openAndroidTtsSettings() {
        voiceManager.openSystemTtsSettings()
    }

    fun installAndroidTtsVoiceData() {
        voiceManager.requestInstallTtsVoiceData()
    }

    fun refreshTtsLocales() {
        voiceManager.probeAndConfigureLocales()
    }

    fun attachDocumentFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<Application>().contentResolver
                var displayName = "Selected_Document.txt"
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            displayName = cursor.getString(idx) ?: displayName
                        }
                    }
                }

                val textBuilder = StringBuilder()
                resolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        var charsRead = 0
                        while (line != null && charsRead < 10_000) {
                            textBuilder.append(line).append("\n")
                            charsRead += line.length
                            line = reader.readLine()
                        }
                    }
                }

                val extracted = textBuilder.toString().trim()
                if (extracted.isNotBlank()) {
                    _attachedDocumentName.value = displayName
                    _attachedDocumentContent.value = extracted
                    repository.logAction(
                        toolName = "pick_document",
                        target = displayName,
                        parametersSummary = "${extracted.length} chars loaded",
                        status = "EXECUTED",
                        details = "Loaded document '$displayName' via Android Document Picker"
                    )
                } else {
                    _attachedDocumentName.value = displayName
                    _attachedDocumentContent.value = "[Binary or non-plain-text file: $displayName]"
                }
            } catch (e: Exception) {
                _errorBanner.value = "Could not read selected file: ${e.localizedMessage}"
            }
        }
    }

    fun clearAttachedDocument() {
        _attachedDocumentName.value = null
        _attachedDocumentContent.value = null
    }

    fun sendCommand(rawCommand: String) {
        val rawTrimmed = rawCommand.trim()
        if (rawTrimmed.isBlank()) return

        // If the user typed or tapped "Hey JARVIS" by itself, trigger AI listening mode directly
        val wakeCheck = WakeWordDetector.analyzeTranscript(rawTrimmed)
        if (wakeCheck is WakeWordMatchResult.WakeWordOnly && _attachedDocumentName.value == null) {
            voiceManager.playWakeWordChime()
            handleWakeWordTriggered(
                matchedTrigger = wakeCheck.matchedTrigger,
                isBangla = wakeCheck.isBanglaTrigger,
                trailingCommand = null
            )
            return
        }

        val prompt = when (wakeCheck) {
            is WakeWordMatchResult.WakeWordWithCommand -> wakeCheck.command
            else -> rawTrimmed
        }

        _errorBanner.value = null
        voiceManager.setProcessingState()

        viewModelScope.launch {
            val currentSettings = settings.value
            val docName = _attachedDocumentName.value
            val docText = _attachedDocumentContent.value

            val userDisplayPrompt = if (docName != null) {
                "📎 [$docName] $prompt"
            } else {
                prompt
            }

            val languageDetection = voiceManager.detectAndConfigureLanguage(
                utterance = prompt,
                preferredMode = currentSettings.languageMode
            )
            val isBanglaInput = languageDetection.isBangla
            repository.addMessage(
                ChatMessageEntity(
                    role = "user",
                    text = userDisplayPrompt,
                    language = languageDetection.language.shortCode
                )
            )

            clearAttachedDocument()

            val recentHistory = withContext(Dispatchers.IO) {
                repository.getRecentMessages(12)
            }
            val savedMemories = withContext(Dispatchers.IO) {
                repository.getMemoriesSnapshot()
            }
            val liveTelemetry = actionExecutor.getLiveTelemetry()
            _telemetry.value = liveTelemetry

            val result = brainService.processCommand(
                userPrompt = prompt,
                recentHistory = recentHistory,
                savedMemories = savedMemories,
                telemetry = liveTelemetry,
                installedApps = _installedApps.value,
                settings = currentSettings,
                attachedDocumentText = docText
            )

            result.fold(
                onSuccess = { decision ->
                    _followUpSuggestions.value = decision.followUpSuggestions

                    var actionStatusBadge: String? = null
                    var actionExtraNote = decision.detailedText

                    if (decision.actionName != "none" && decision.actionName.isNotBlank()) {
                        if (decision.actionName == "save_memory") {
                            val category = decision.actionTarget.ifBlank { "FACT" }.uppercase()
                            val fact = decision.actionSecondary.ifBlank { prompt }
                            repository.addMemory(category = category, fact = fact)
                            repository.logAction(
                                toolName = "save_memory",
                                target = category,
                                parametersSummary = fact,
                                status = "EXECUTED",
                                details = "Saved to persistent Room Memory Core"
                            )
                            actionStatusBadge = "MEMORY SAVED"
                        } else {
                            val execResult = actionExecutor.executeValidatedToolCall(
                                actionName = decision.actionName,
                                target = decision.actionTarget,
                                secondaryParam = decision.actionSecondary,
                                requireSensitiveConfirmation = currentSettings.confirmSensitiveActions,
                                language = decision.detectedLanguage
                            )
                            actionStatusBadge = execResult.status
                            if (execResult.pendingConfirmation != null) {
                                _pendingConfirmation.value = execResult.pendingConfirmation
                            }
                            if (actionExtraNote.isBlank()) {
                                actionExtraNote = execResult.summary
                            } else {
                                actionExtraNote = "${decision.detailedText}\n\n[Android System]: ${execResult.summary}"
                            }
                            repository.logAction(
                                toolName = decision.actionName,
                                target = decision.actionTarget,
                                parametersSummary = decision.actionSecondary,
                                status = execResult.status,
                                details = execResult.summary
                            )
                            _telemetry.value = actionExecutor.getLiveTelemetry()
                        }
                    }

                    repository.addMessage(
                        ChatMessageEntity(
                            role = "jarvis",
                            text = decision.spokenReply,
                            detailedText = actionExtraNote,
                            language = decision.detectedLanguage,
                            actionName = decision.actionName.takeIf { it != "none" },
                            actionStatus = actionStatusBadge
                        )
                    )

                    if (currentSettings.autoSpeakReplies) {
                        voiceManager.speak(
                            text = decision.spokenReply,
                            languageHint = decision.detectedLanguage,
                            settings = currentSettings,
                            onComplete = {
                                if (voiceManager.isLiveConversationActive.value || settings.value.handsFreeMode) {
                                    voiceManager.startListening(settings.value.languageMode)
                                }
                            }
                        )
                    } else {
                        voiceManager.setIdleState()
                    }
                },
                onFailure = { err ->
                    val errMsg = err.localizedMessage ?: "Unknown Gemini API error"
                    _errorBanner.value = errMsg
                    val fallbackText = if (isBanglaInput) {
                        "দুঃখিত বস, জেমিনি নিউরাল নেটওয়ার্কে সংযোগ করতে সমস্যা হয়েছে: $errMsg"
                    } else {
                        "Apologies, ${currentSettings.bossTitle}. I encountered a neural link error: $errMsg"
                    }
                    repository.addMessage(
                        ChatMessageEntity(
                            role = "jarvis",
                            text = fallbackText,
                            language = if (isBanglaInput) "bn" else "en",
                            actionName = "error",
                            actionStatus = "API_ERROR"
                        )
                    )
                    voiceManager.setIdleState()
                }
            )
        }
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch {
            val result: ActionExecutionResult = when (pending.actionName) {
                "prepare_call" -> actionExecutor.executeConfirmedCall(pending.target)
                "prepare_sms" -> actionExecutor.executeConfirmedSms(pending.target, pending.secondaryValue)
                else -> ActionExecutionResult(false, "FAILED", "Unsupported confirmation action")
            }
            repository.logAction(
                toolName = pending.actionName,
                target = pending.target,
                parametersSummary = pending.secondaryValue,
                status = result.status,
                details = result.summary
            )
            val confirmMsg = if (pending.language == "bn") {
                "নিশ্চিত করার জন্য ধন্যবাদ বস। ${result.summary}"
            } else {
                "Confirmed, ${settings.value.bossTitle}. ${result.summary}"
            }
            repository.addMessage(
                ChatMessageEntity(
                    role = "jarvis",
                    text = confirmMsg,
                    language = pending.language,
                    actionName = pending.actionName,
                    actionStatus = result.status
                )
            )
            if (settings.value.autoSpeakReplies) {
                voiceManager.speak(confirmMsg, pending.language, settings.value)
            }
        }
    }

    fun cancelPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch {
            repository.logAction(
                toolName = pending.actionName,
                target = pending.target,
                parametersSummary = pending.secondaryValue,
                status = "CANCELLED",
                details = "Cancelled by Boss before execution"
            )
            val cancelMsg = if (pending.language == "bn") {
                "ঠিক আছে বস, আমি ${pending.actionName} কমান্ডটি বাতিল করেছি।"
            } else {
                "Understood, ${settings.value.bossTitle}. I have aborted the ${pending.actionName} request."
            }
            repository.addMessage(
                ChatMessageEntity(
                    role = "jarvis",
                    text = cancelMsg,
                    language = pending.language,
                    actionName = pending.actionName,
                    actionStatus = "CANCELLED"
                )
            )
        }
    }

    // Direct hardware control triggers from Control Deck UI
    fun triggerDirectAction(actionName: String, target: String, secondary: String = "") {
        viewModelScope.launch {
            val res = actionExecutor.executeValidatedToolCall(
                actionName = actionName,
                target = target,
                secondaryParam = secondary,
                requireSensitiveConfirmation = settings.value.confirmSensitiveActions,
                language = "en"
            )
            if (res.pendingConfirmation != null) {
                _pendingConfirmation.value = res.pendingConfirmation
            }
            repository.logAction(
                toolName = actionName,
                target = target,
                parametersSummary = secondary,
                status = res.status,
                details = res.summary
            )
            _telemetry.value = actionExecutor.getLiveTelemetry()
        }
    }

    // Memory & Privacy Management
    fun addManualMemory(category: String, fact: String) {
        if (fact.isBlank()) return
        viewModelScope.launch {
            repository.addMemory(category.uppercase(), fact.trim())
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun clearConversationHistory() {
        viewModelScope.launch {
            repository.clearConversationHistory()
        }
    }

    fun clearActionLogs() {
        viewModelScope.launch {
            repository.clearActionLogs()
        }
    }

    fun wipeAllStoredData() {
        viewModelScope.launch {
            repository.wipeAllData()
        }
    }

    // Settings updates
    fun updateBossTitle(title: String) = viewModelScope.launch {
        preferencesRepository.updateBossTitle(title)
    }

    fun updateLanguageMode(mode: String) = viewModelScope.launch {
        preferencesRepository.updateLanguageMode(mode)
    }

    fun updateTtsEngine(engine: String) = viewModelScope.launch {
        preferencesRepository.updateTtsEngine(engine)
    }

    fun updateElevenLabsVoiceId(voiceId: String) = viewModelScope.launch {
        preferencesRepository.updateElevenLabsVoiceId(voiceId)
    }

    fun updateCustomElevenLabsKey(apiKey: String) = viewModelScope.launch {
        preferencesRepository.updateCustomElevenLabsKey(apiKey)
    }

    fun updateCustomGeminiApiKey(apiKey: String) = viewModelScope.launch {
        preferencesRepository.updateCustomGeminiApiKey(apiKey)
    }

    fun saveUniversalAiConfig(
        primaryKey: String,
        secondaryKey: String,
        providerMode: String,
        customBaseUrl: String,
        customModelName: String,
        maxTokens: Int,
        elevenLabsKey: String
    ) = viewModelScope.launch {
        preferencesRepository.updateUniversalAiConfig(
            primaryKey = primaryKey,
            secondaryKey = secondaryKey,
            providerMode = providerMode,
            customBaseUrl = customBaseUrl,
            customModelName = customModelName,
            maxTokens = maxTokens,
            elevenLabsKey = elevenLabsKey
        )
    }

    fun updateSpeechRate(rate: Float) = viewModelScope.launch {
        preferencesRepository.updateSpeechRate(rate)
    }

    fun updateSpeechPitch(pitch: Float) = viewModelScope.launch {
        preferencesRepository.updateSpeechPitch(pitch)
    }

    fun updateAutoSpeak(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.updateAutoSpeak(enabled)
    }

    fun updateWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateWakeWordEnabled(enabled)
            if (enabled) {
                if (_telemetry.value.hasMicPermission) {
                    voiceManager.startWakeWordStandby(settings.value.languageMode)
                }
            } else {
                voiceManager.stopWakeWordStandby()
            }
        }
    }

    fun toggleHandsFreeService(enable: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateHandsFreeMode(enable)
            val app = getApplication<Application>()
            if (enable) {
                JarvisVoiceForegroundService.start(
                    app,
                    "Wake Word \"Hey JARVIS\" & Hands-Free Voice Link Active"
                )
                voiceManager.startListening(settings.value.languageMode, wakeWordStandbyOnly = false)
            } else {
                JarvisVoiceForegroundService.stop(app)
                voiceManager.stopListening()
                if (settings.value.wakeWordEnabled && _telemetry.value.hasMicPermission) {
                    voiceManager.startWakeWordStandby(settings.value.languageMode)
                }
            }
        }
    }

    fun updateConfirmSensitiveActions(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.updateConfirmSensitiveActions(enabled)
    }

    fun updateGeminiModel(model: String) = viewModelScope.launch {
        preferencesRepository.updateGeminiModel(model)
    }

    override fun onCleared() {
        voiceManager.shutdown()
        super.onCleared()
    }
}
