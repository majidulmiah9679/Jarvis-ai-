package com.example.ai

import android.util.Log
import com.example.actions.DeviceTelemetry
import com.example.actions.InstalledAppInfo
import com.example.data.ChatMessageEntity
import com.example.data.JarvisSettings
import com.example.data.MemoryEntity
import com.example.util.LanguageDetectionHelper
import com.example.util.LanguageDetectionResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "JarvisBrainService"

data class JarvisBrainDecision(
    val spokenReply: String,
    val detailedText: String,
    val detectedLanguage: String, // "en" or "bn"
    val actionName: String,
    val actionTarget: String,
    val actionSecondary: String,
    val followUpSuggestions: List<String>,
    val providerUsed: String = "FIREBASE_AI_LOGIC"
)

class JarvisBrainService {

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val jarvisResponseSchema = Schema.obj(
        properties = mapOf(
            "spokenReply" to Schema.string(
                description = "Concise, natural spoken reply addressing the user respectfully as Boss (or their chosen title). Reply in Bangla if the user spoke Bangla, or in English if the user spoke English."
            ),
            "detailedText" to Schema.string(
                description = "Optional extra details, task breakdown, code, or explanation if the user asked for an explanation, plan, or summary. Can be empty string for simple commands."
            ),
            "detectedLanguage" to Schema.enumeration(
                values = listOf("en", "bn"),
                description = "Language of the user's message and JARVIS's response ('en' for English, 'bn' for Bangla)."
            ),
            "actionName" to Schema.enumeration(
                values = listOf(
                    "none",
                    "get_device_telemetry",
                    "toggle_flashlight",
                    "set_volume",
                    "set_brightness",
                    "set_alarm",
                    "set_timer",
                    "launch_app",
                    "open_website",
                    "open_settings",
                    "launch_camera",
                    "open_navigation",
                    "prepare_call",
                    "prepare_sms",
                    "share_text",
                    "read_notifications",
                    "save_memory"
                ),
                description = "Validated Android native action to execute, or 'none' if purely conversational."
            ),
            "actionTarget" to Schema.string(
                description = "Primary target for the tool call: e.g., 'on'/'off' for flashlight; stream name ('media'/'ring'/'alarm') for set_volume; 'HH:mm' (24h) for set_alarm; seconds for set_timer; app name for launch_app; URL or search query for open_website; setting section ('wifi','bluetooth','location','sound','display','battery','notification_access') for open_settings; 'photo' or 'video' for launch_camera; destination for open_navigation; phone number for prepare_call/prepare_sms; memory category ('PREFERENCE','FACT','CONTACT','ROUTINE') for save_memory."
            ),
            "actionSecondary" to Schema.string(
                description = "Secondary parameter for the tool call: e.g., volume/brightness percentage '0'-'100', alarm/timer label, SMS body text, share text content, or the exact memory fact to save."
            ),
            "followUpSuggestions" to Schema.array(
                items = Schema.string(),
                description = "3 short contextual follow-up voice commands the user might want to say next."
            )
        )
    )

    private val structuredConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = jarvisResponseSchema
        temperature = 0.4f
    }

    // Lazy initialization per Firebase AI Logic mandate
    private val flashModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = structuredConfig
        )
    }

    private val flashLiteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = structuredConfig
        )
    }

    companion object {
        /**
         * Automatically detects what kind of API key or token the user pasted.
         */
        fun detectKeyType(rawKey: String): String {
            val k = rawKey.trim().removePrefix("Bearer ").trim()
            return when {
                k.isBlank() -> "BUILT-IN FIREBASE AI (NO KEY NEEDED)"
                k.startsWith("AIza") -> "GOOGLE GEMINI API KEY"
                k.startsWith("gsk_") -> "GROQ CLOUD API KEY (LLAMA-3.3)"
                k.startsWith("sk-or-") -> "OPENROUTER API KEY"
                k.startsWith("sk-") -> "OPENAI / COMPATIBLE BEARER TOKEN"
                else -> "UNIVERSAL API TOKEN"
            }
        }
    }

    suspend fun processCommand(
        userPrompt: String,
        recentHistory: List<ChatMessageEntity>,
        savedMemories: List<MemoryEntity>,
        telemetry: DeviceTelemetry,
        installedApps: List<InstalledAppInfo>,
        settings: JarvisSettings,
        attachedDocumentText: String? = null
    ): Result<JarvisBrainDecision> {
        val languageDetection = LanguageDetectionHelper.detectUserLanguage(
            inputText = userPrompt,
            preferredLanguageMode = settings.languageMode,
            bossTitle = settings.bossTitle
        )

        val systemPrompt = buildSystemPrompt(
            userPrompt = userPrompt,
            languageDetection = languageDetection,
            recentHistory = recentHistory,
            savedMemories = savedMemories,
            telemetry = telemetry,
            installedApps = installedApps,
            settings = settings,
            attachedDocumentText = attachedDocumentText
        )

        // Collect all candidate keys entered by the user (Primary key + Secondary backup key)
        val candidateKeys = listOf(
            settings.customGeminiApiKey.trim(),
            settings.secondaryAiApiKey.trim()
        ).filter { it.isNotBlank() }

        // 1. If user explicitly chose a non-Firebase key or entered any API key/token, try their keys in order
        if (settings.aiProviderMode != "FIREBASE" && candidateKeys.isNotEmpty()) {
            for (rawKey in candidateKeys) {
                val cleanKey = rawKey.removePrefix("Bearer ").trim()
                val result = routeRequestByKeyOrProvider(
                    systemPrompt = systemPrompt,
                    apiKey = cleanKey,
                    settings = settings,
                    fallbackLanguageCode = languageDetection.language.shortCode
                )
                if (result.isSuccess) {
                    return result
                }
                Log.w(TAG, "Key attempt failed (${result.exceptionOrNull()?.message}), trying next backup…")
            }
        }

        // 2. Try Built-in Provisioned Firebase AI Logic (works out-of-the-box in AI Studio without any API key)
        try {
            val activeModel = if (settings.geminiModel == "gemini-3.5-flash-lite") {
                flashLiteModel
            } else {
                flashModel
            }

            val response = withTimeout(60_000L) {
                activeModel.generateContent(
                    content {
                        text(systemPrompt)
                    }
                )
            }

            val rawJson = response.text
                ?: throw IllegalStateException("Gemini returned an empty response.")

            return Result.success(
                parseDecisionJson(
                    rawJson = rawJson,
                    bossTitle = settings.bossTitle,
                    providerUsed = "FIREBASE AI (${settings.geminiModel})",
                    defaultLanguageCode = languageDetection.language.shortCode
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini inference timed out", e)
            return Result.success(
                executeLocalSmartFallback(
                    userPrompt = userPrompt,
                    languageDetection = languageDetection,
                    telemetry = telemetry,
                    bossTitle = settings.bossTitle,
                    fallbackReason = "Cloud AI timed out — executed via JARVIS Local Reflex Core"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic error, falling back to Local Reflex Core", e)
            return Result.success(
                executeLocalSmartFallback(
                    userPrompt = userPrompt,
                    languageDetection = languageDetection,
                    telemetry = telemetry,
                    bossTitle = settings.bossTitle,
                    fallbackReason = "Offline/Local Reflex Mode active"
                )
            )
        }
    }

    private suspend fun routeRequestByKeyOrProvider(
        systemPrompt: String,
        apiKey: String,
        settings: JarvisSettings,
        fallbackLanguageCode: String
    ): Result<JarvisBrainDecision> {
        val mode = settings.aiProviderMode.uppercase()
        val isGeminiKey = mode == "GEMINI" || (mode == "AUTO" && apiKey.startsWith("AIza"))
        val isGroqKey = mode == "GROQ" || (mode == "AUTO" && apiKey.startsWith("gsk_"))
        val isOpenRouterKey = mode == "OPENROUTER" || (mode == "AUTO" && apiKey.startsWith("sk-or-"))
        val isOpenAiKey = mode == "OPENAI" || (mode == "AUTO" && apiKey.startsWith("sk-"))

        return when {
            isGeminiKey -> callDirectGeminiMultiModelApi(
                systemPrompt = systemPrompt,
                apiKey = apiKey,
                customModel = settings.customModelName,
                maxTokens = settings.maxOutputTokens,
                bossTitle = settings.bossTitle,
                defaultLanguageCode = fallbackLanguageCode
            )

            isGroqKey -> callOpenAiCompatibleApi(
                endpointUrl = "https://api.groq.com/openai/v1/chat/completions",
                apiKey = apiKey,
                modelName = settings.customModelName.ifBlank { "llama-3.3-70b-versatile" },
                maxTokens = settings.maxOutputTokens,
                systemPrompt = systemPrompt,
                bossTitle = settings.bossTitle,
                providerLabel = "GROQ AI",
                defaultLanguageCode = fallbackLanguageCode
            )

            isOpenRouterKey -> callOpenAiCompatibleApi(
                endpointUrl = "https://openrouter.ai/api/v1/chat/completions",
                apiKey = apiKey,
                modelName = settings.customModelName.ifBlank { "google/gemini-2.5-flash" },
                maxTokens = settings.maxOutputTokens,
                systemPrompt = systemPrompt,
                bossTitle = settings.bossTitle,
                providerLabel = "OPENROUTER AI",
                defaultLanguageCode = fallbackLanguageCode
            )

            isOpenAiKey -> callOpenAiCompatibleApi(
                endpointUrl = settings.customBaseUrl.ifBlank { "https://api.openai.com/v1/chat/completions" },
                apiKey = apiKey,
                modelName = settings.customModelName.ifBlank { "gpt-4o-mini" },
                maxTokens = settings.maxOutputTokens,
                systemPrompt = systemPrompt,
                bossTitle = settings.bossTitle,
                providerLabel = "OPENAI API",
                defaultLanguageCode = fallbackLanguageCode
            )

            settings.customBaseUrl.isNotBlank() -> callOpenAiCompatibleApi(
                endpointUrl = settings.customBaseUrl.trim(),
                apiKey = apiKey,
                modelName = settings.customModelName.ifBlank { "gpt-4o-mini" },
                maxTokens = settings.maxOutputTokens,
                systemPrompt = systemPrompt,
                bossTitle = settings.bossTitle,
                providerLabel = "CUSTOM AI ENDPOINT",
                defaultLanguageCode = fallbackLanguageCode
            )

            else -> {
                // Try Gemini API first, then OpenAI-compatible if user pasted an unknown token format
                val geminiAttempt = callDirectGeminiMultiModelApi(
                    systemPrompt = systemPrompt,
                    apiKey = apiKey,
                    customModel = settings.customModelName,
                    maxTokens = settings.maxOutputTokens,
                    bossTitle = settings.bossTitle,
                    defaultLanguageCode = fallbackLanguageCode
                )
                if (geminiAttempt.isSuccess) {
                    geminiAttempt
                } else {
                    callOpenAiCompatibleApi(
                        endpointUrl = "https://api.openai.com/v1/chat/completions",
                        apiKey = apiKey,
                        modelName = settings.customModelName.ifBlank { "gpt-4o-mini" },
                        maxTokens = settings.maxOutputTokens,
                        systemPrompt = systemPrompt,
                        bossTitle = settings.bossTitle,
                        providerLabel = "BEARER TOKEN AI",
                        defaultLanguageCode = fallbackLanguageCode
                    )
                }
            }
        }
    }

    /**
     * Tries multiple Gemini model endpoints automatically so that whether the user's Gemini API key
     * supports gemini-2.5-flash, gemini-2.0-flash, or gemini-1.5-flash (or a custom model name),
     * it succeeds automatically.
     */
    private suspend fun callDirectGeminiMultiModelApi(
        systemPrompt: String,
        apiKey: String,
        customModel: String,
        maxTokens: Int,
        bossTitle: String,
        defaultLanguageCode: String
    ): Result<JarvisBrainDecision> = withContext(Dispatchers.IO) {
        val candidateModels = buildList {
            if (customModel.isNotBlank()) add(customModel.trim())
            add("gemini-2.5-flash")
            add("gemini-2.0-flash")
            add("gemini-1.5-flash")
        }.distinct()

        var lastError: Exception? = null
        for (modelName in candidateModels) {
            try {
                val url =
                    "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

                val jsonInstruction = buildRawJsonInstruction(systemPrompt)

                val requestBodyJson = JSONObject().apply {
                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put(
                                "parts",
                                JSONArray().put(JSONObject().put("text", jsonInstruction))
                            )
                        )
                    )
                    put(
                        "generationConfig",
                        JSONObject().apply {
                            put("responseMimeType", "application/json")
                            put("temperature", 0.4)
                            put("maxOutputTokens", maxTokens.coerceIn(256, 8192))
                        }
                    )
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val bodyStr = response.body?.string().orEmpty()
                response.close()

                if (!response.isSuccessful) {
                    lastError = Exception("Gemini ($modelName) HTTP ${response.code}: ${bodyStr.take(120)}")
                    continue
                }

                val root = JSONObject(bodyStr)
                val textOut = root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.trim()
                    ?: throw IllegalStateException("Empty Gemini response")

                val cleanedJson = stripMarkdownFences(textOut)
                return@withContext Result.success(
                    parseDecisionJson(
                        rawJson = cleanedJson,
                        bossTitle = bossTitle,
                        providerUsed = "GEMINI KEY ($modelName)",
                        defaultLanguageCode = defaultLanguageCode
                    )
                )
            } catch (e: Exception) {
                lastError = e
            }
        }

        Result.failure(lastError ?: Exception("All Gemini model endpoints failed for provided key"))
    }

    /**
     * Supports OpenAI (`sk-...`), Groq (`gsk_...`), OpenRouter (`sk-or-...`), DeepSeek,
     * and any custom OpenAI-compatible Bearer token endpoint.
     */
    private suspend fun callOpenAiCompatibleApi(
        endpointUrl: String,
        apiKey: String,
        modelName: String,
        maxTokens: Int,
        systemPrompt: String,
        bossTitle: String,
        providerLabel: String,
        defaultLanguageCode: String
    ): Result<JarvisBrainDecision> = withContext(Dispatchers.IO) {
        try {
            val jsonInstruction = buildRawJsonInstruction(systemPrompt)

            val messagesArray = JSONArray().apply {
                put(
                    JSONObject().apply {
                        put("role", "system")
                        put(
                            "content",
                            "You are JARVIS AI. Always respond ONLY with a valid raw JSON object matching the requested schema."
                        )
                    }
                )
                put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", jsonInstruction)
                    }
                )
            }

            val payload = JSONObject().apply {
                put("model", modelName)
                put("messages", messagesArray)
                put("temperature", 0.4)
                put("max_tokens", maxTokens.coerceIn(256, 8192))
            }

            val request = Request.Builder()
                .url(endpointUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyStr = response.body?.string().orEmpty()
            response.close()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("$providerLabel HTTP ${response.code}: ${bodyStr.take(120)}")
                )
            }

            val root = JSONObject(bodyStr)
            val contentText = root.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
                ?: throw IllegalStateException("Empty response from $providerLabel")

            val cleanedJson = stripMarkdownFences(contentText)
            Result.success(
                parseDecisionJson(
                    rawJson = cleanedJson,
                    bossTitle = bossTitle,
                    providerUsed = "$providerLabel ($modelName)",
                    defaultLanguageCode = defaultLanguageCode
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildRawJsonInstruction(systemPrompt: String): String {
        return """
            $systemPrompt
            
            IMPORTANT: Return ONLY valid raw JSON (no markdown code fences) with these exact keys:
            {
              "spokenReply": "string",
              "detailedText": "string",
              "detectedLanguage": "en" or "bn",
              "actionName": "none" | "get_device_telemetry" | "toggle_flashlight" | "set_volume" | "set_brightness" | "set_alarm" | "set_timer" | "launch_app" | "open_website" | "open_settings" | "launch_camera" | "open_navigation" | "prepare_call" | "prepare_sms" | "share_text" | "read_notifications" | "save_memory",
              "actionTarget": "string",
              "actionSecondary": "string",
              "followUpSuggestions": ["string", "string", "string"]
            }
        """.trimIndent()
    }

    private fun stripMarkdownFences(raw: String): String {
        val trimmed = raw.trim()
        val startBrace = trimmed.indexOf('{')
        val endBrace = trimmed.lastIndexOf('}')
        return if (startBrace >= 0 && endBrace > startBrace) {
            trimmed.substring(startBrace, endBrace + 1)
        } else {
            trimmed
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        }
    }

    private fun parseDecisionJson(
        rawJson: String,
        bossTitle: String,
        providerUsed: String,
        defaultLanguageCode: String = "en"
    ): JarvisBrainDecision {
        val json = JSONObject(rawJson)
        val spokenReply = json.optString("spokenReply", "At your service, $bossTitle.")
        val detailedText = json.optString("detailedText", "")
        val replyDetection = LanguageDetectionHelper.detectUserLanguage(
            inputText = spokenReply,
            preferredLanguageMode = defaultLanguageCode,
            bossTitle = bossTitle
        )
        val rawLang = json.optString("detectedLanguage", defaultLanguageCode)
        val detectedLang = if (replyDetection.isBangla || rawLang.equals("bn", ignoreCase = true)) {
            "bn"
        } else {
            "en"
        }
        val actionName = json.optString("actionName", "none")
        val actionTarget = json.optString("actionTarget", "")
        val actionSecondary = json.optString("actionSecondary", "")

        val suggestionsArray = json.optJSONArray("followUpSuggestions")
        val suggestions = mutableListOf<String>()
        if (suggestionsArray != null) {
            for (i in 0 until suggestionsArray.length()) {
                val item = suggestionsArray.optString(i).trim()
                if (item.isNotBlank()) suggestions.add(item)
            }
        }

        return JarvisBrainDecision(
            spokenReply = spokenReply,
            detailedText = detailedText,
            detectedLanguage = detectedLang,
            actionName = actionName,
            actionTarget = actionTarget,
            actionSecondary = actionSecondary,
            followUpSuggestions = suggestions.ifEmpty {
                if (detectedLang == "bn") {
                    listOf("🔦 ফ্ল্যাশলাইট চালু করো", "🔋 ব্যাটারি কত?", "📷 ক্যামেরা ওপেন করো")
                } else {
                    listOf("Turn on flashlight", "Battery status", "Open Camera")
                }
            },
            providerUsed = providerUsed
        )
    }

    private fun executeLocalSmartFallback(
        userPrompt: String,
        languageDetection: LanguageDetectionResult,
        telemetry: DeviceTelemetry,
        bossTitle: String,
        fallbackReason: String
    ): JarvisBrainDecision {
        val lower = userPrompt.lowercase()
        val isBangla = languageDetection.isBangla
        val lang = languageDetection.language.shortCode

        return when {
            lower.contains("flashlight") || lower.contains("torch") || lower.contains("ফ্ল্যাশলাইট") || lower.contains("লাইট") -> {
                val turnOff = lower.contains("off") || lower.contains("বন্ধ") || lower.contains("bondho")
                val target = if (turnOff) "off" else "on"
                val reply = if (isBangla) {
                    if (turnOff) "জি বস, ফ্ল্যাশলাইট বন্ধ করে দিচ্ছি।" else "জি বস, ফ্ল্যাশলাইট চালু করে দিচ্ছি।"
                } else {
                    if (turnOff) "Right away, $bossTitle. Turning off the flashlight torch." else "Right away, $bossTitle. Turning on the flashlight torch."
                }
                JarvisBrainDecision(
                    spokenReply = reply,
                    detailedText = fallbackReason,
                    detectedLanguage = lang,
                    actionName = "toggle_flashlight",
                    actionTarget = target,
                    actionSecondary = "",
                    followUpSuggestions = listOf("Turn off flashlight", "Status report", "Open Camera")
                )
            }

            lower.contains("battery") || lower.contains("status") || lower.contains("ব্যাটারি") || lower.contains("চার্জ") -> {
                val chargingBn = if (telemetry.isCharging) "চার্জ হচ্ছে" else "চার্জ হচ্ছে না"
                val reply = if (isBangla) {
                    "বস, আপনার ফোনের ব্যাটারি এখন ${telemetry.batteryPercent}% ($chargingBn), তাপমাত্রা ${telemetry.batteryTempCelsius}°C এবং নেটওয়ার্ক ${telemetry.networkType}।"
                } else {
                    "$bossTitle, battery is at ${telemetry.batteryPercent}% (Charging: ${telemetry.isCharging}, ${telemetry.batteryTempCelsius}°C), network is ${telemetry.networkType}, and ${telemetry.availableRamMb} MB RAM is free."
                }
                JarvisBrainDecision(
                    spokenReply = reply,
                    detailedText = fallbackReason,
                    detectedLanguage = lang,
                    actionName = "get_device_telemetry",
                    actionTarget = "status",
                    actionSecondary = "",
                    followUpSuggestions = listOf("Turn on flashlight", "Open Settings", "Set timer for 2 minutes")
                )
            }

            lower.contains("camera") || lower.contains("ক্যামেরা") -> {
                val reply = if (isBangla) "জি বস, ক্যামেরা চালু করছি।" else "Launching the camera for you, $bossTitle."
                JarvisBrainDecision(
                    spokenReply = reply,
                    detailedText = fallbackReason,
                    detectedLanguage = lang,
                    actionName = "launch_camera",
                    actionTarget = "photo",
                    actionSecondary = "",
                    followUpSuggestions = listOf("Status report", "Open YouTube", "Open Settings")
                )
            }

            lower.contains("youtube") || lower.contains("ইউটিউব") -> {
                val reply = if (isBangla) "জি বস, ইউটিউব ওপেন করছি।" else "Opening YouTube now, $bossTitle."
                JarvisBrainDecision(
                    spokenReply = reply,
                    detailedText = fallbackReason,
                    detectedLanguage = lang,
                    actionName = "launch_app",
                    actionTarget = "YouTube",
                    actionSecondary = "",
                    followUpSuggestions = listOf("Status report", "Open Settings", "Turn on flashlight")
                )
            }

            lower.contains("setting") || lower.contains("সেটিংস") -> {
                val reply = if (isBangla) "জি বস, অ্যান্ড্রয়েড সেটিংস ওপেন করছি।" else "Opening Android Settings, $bossTitle."
                JarvisBrainDecision(
                    spokenReply = reply,
                    detailedText = fallbackReason,
                    detectedLanguage = lang,
                    actionName = "open_settings",
                    actionTarget = "general",
                    actionSecondary = "",
                    followUpSuggestions = listOf("Status report", "Check battery", "Open Camera")
                )
            }

            else -> {
                val reply = if (isBangla) {
                    "হ্যালো বস! আমি জারভিস। আপনার ফোনের ব্যাটারি এখন ${telemetry.batteryPercent}%। আপনি Settings-এ গিয়ে যেকোনো Gemini, Groq, OpenAI বা Custom API Key বসাতে পারবেন।"
                } else {
                    "At your service, $bossTitle. Battery is at ${telemetry.batteryPercent}%. You can configure any Gemini, Groq, OpenAI, or Custom API key in Settings."
                }
                JarvisBrainDecision(
                    spokenReply = reply,
                    detailedText = fallbackReason,
                    detectedLanguage = lang,
                    actionName = "none",
                    actionTarget = "",
                    actionSecondary = "",
                    followUpSuggestions = listOf("Status report", "ফ্ল্যাশলাইট চালু করো", "ব্যাটারি কত শতাংশ?")
                )
            }
        }
    }

    private fun buildSystemPrompt(
        userPrompt: String,
        languageDetection: LanguageDetectionResult,
        recentHistory: List<ChatMessageEntity>,
        savedMemories: List<MemoryEntity>,
        telemetry: DeviceTelemetry,
        installedApps: List<InstalledAppInfo>,
        settings: JarvisSettings,
        attachedDocumentText: String?
    ): String {
        val historyBlock = if (recentHistory.isEmpty()) {
            "No prior messages in this session."
        } else {
            recentHistory.takeLast(10).joinToString("\n") { msg ->
                val speaker = if (msg.role == "user") settings.bossTitle else "JARVIS"
                val actionInfo = if (!msg.actionName.isNullOrBlank() && msg.actionName != "none") {
                    " [Action: ${msg.actionName} -> ${msg.actionStatus}]"
                } else ""
                "$speaker: ${msg.text}$actionInfo"
            }
        }

        val memoriesBlock = if (savedMemories.isEmpty()) {
            "None saved yet."
        } else {
            savedMemories.take(15).joinToString("\n") { "- [${it.category}] ${it.fact}" }
        }

        val sampleApps = installedApps.take(30).joinToString(", ") { it.appName }

        val documentContext = if (!attachedDocumentText.isNullOrBlank()) {
            "\n--- ATTACHED USER DOCUMENT CONTENT ---\n${attachedDocumentText.take(8000)}\n--------------------------------------\n"
        } else {
            ""
        }

        return """
            You are JARVIS (Just A Rather Very Intelligent System), the advanced personal smart AI assistant inspired by Iron Man's JARVIS.
            
            ${languageDetection.geminiPromptDirective}
            
            IDENTITY & PERSONALITY RULES:
            1. Always address the user respectfully as "${settings.bossTitle}" (or "বস" when speaking Bangla).
            2. Keep `spokenReply` concise (1 to 3 sentences) so it sounds natural when spoken aloud via Text-to-Speech. Put longer lists, code, step-by-step plans, or deep explanations in `detailedText`.
            3. HONESTY & ANDROID SECURITY:
               - Never claim an action is already completed if it requires a permission that is currently false or if it is a sensitive action (`prepare_call`, `prepare_sms`) that requires Boss's confirmation.
               - If a command is ambiguous, ask a polite clarifying question and set `actionName = "none"`.
               - If the user says "Remember that..." or shares a personal preference/fact to remember, set `actionName = "save_memory"`, `actionTarget = "PREFERENCE"`, and `actionSecondary` to the clear fact.
            
            LIVE ANDROID DEVICE TELEMETRY:
            - Battery: ${telemetry.batteryPercent}% (Charging: ${telemetry.isCharging}, Temp: ${telemetry.batteryTempCelsius}°C)
            - Network: ${telemetry.networkType} (Online: ${telemetry.isOnline})
            - Volumes: Media=${telemetry.mediaVolumePercent}%, Ring=${telemetry.ringVolumePercent}%, Alarm=${telemetry.alarmVolumePercent}%
            - Brightness: ${telemetry.brightnessPercent}% (Write Settings Granted: ${telemetry.hasWriteSettingsAccess})
            - Flashlight Torch Currently On: ${telemetry.isFlashlightOn}
            - Available RAM: ${telemetry.availableRamMb} MB / ${telemetry.totalRamMb} MB
            - Permissions: Mic=${telemetry.hasMicPermission}, Camera=${telemetry.hasCameraPermission}, NotificationAccess=${telemetry.hasNotificationListenerAccess}
            - Installed Launchable Apps Sample: $sampleApps
            
            PERSISTENT MEMORIES ABOUT ${settings.bossTitle.uppercase()}:
            $memoriesBlock
            
            RECENT CONVERSATION CONTEXT:
            $historyBlock
            $documentContext
            
            CURRENT COMMAND FROM ${settings.bossTitle.uppercase()} [Detected Script: ${languageDetection.scriptType.label}]:
            "$userPrompt"
        """.trimIndent()
    }
}
