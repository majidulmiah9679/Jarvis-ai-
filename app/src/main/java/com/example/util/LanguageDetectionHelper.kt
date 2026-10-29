package com.example.util

import java.util.Locale

enum class JarvisLanguage(
    val shortCode: String,
    val bcp47Tag: String,
    val displayLabel: String,
    val nativeLabel: String
) {
    ENGLISH(
        shortCode = "en",
        bcp47Tag = "en-US",
        displayLabel = "English",
        nativeLabel = "English (EN)"
    ),
    BANGLA(
        shortCode = "bn",
        bcp47Tag = "bn-BD",
        displayLabel = "Bangla",
        nativeLabel = "বাংলা (BN)"
    )
}

enum class DetectedScriptType(val label: String) {
    BENGALI_UNICODE("বাংলা Unicode"),
    BANGLISH_ROMANIZED("Banglish (Romanized বাংলা)"),
    ENGLISH_LATIN("English (Latin)")
}

data class LanguageDetectionResult(
    val language: JarvisLanguage,
    val isBangla: Boolean,
    val scriptType: DetectedScriptType,
    val confidence: Float,
    val ttsLocale: Locale,
    val fallbackTtsLocale: Locale,
    val speechRecognizerLocaleTag: String,
    val geminiPromptDirective: String
)

/**
 * Helper utility that automatically detects whether the user is speaking or typing in
 * Bangla (including Bengali Unicode script and Romanized Bangla / Banglish) or English,
 * and dynamically configures the Gemini model prompt instructions and Android TTS Locale.
 */
object LanguageDetectionHelper {

    private val BANGLA_PRIMARY_LOCALE: Locale = Locale.forLanguageTag("bn-BD")
    private val BANGLA_FALLBACK_LOCALE: Locale = Locale.forLanguageTag("bn-IN")
    private val ENGLISH_PRIMARY_LOCALE: Locale = Locale.UK
    private val ENGLISH_FALLBACK_LOCALE: Locale = Locale.US

    /**
     * Common Romanized Bangla (Banglish) words frequently spoken or typed by Bangladeshi
     * and West Bengal users when interacting with a voice assistant.
     */
    private val ROMANIZED_BANGLA_KEYWORDS = setOf(
        "ami", "tumi", "apni", "amar", "tomar", "apnar", "amake", "tomake",
        "kemon", "kmn", "acho", "achen", "achi", "bhalo", "valo", "kharap",
        "koto", "kothay", "koi", "kokhon", "kobe", "keno", "kivabe", "ki",
        "korbo", "koro", "koren", "korun", "korchi", "kore", "kora", "korle",
        "daw", "dao", "dekhaw", "dekhao", "dekho", "dekhun", "bolun", "bolo",
        "bol", "bolen", "shuno", "shunun", "shomoy", "somoy", "baje", "ekhon",
        "chalu", "bondho", "kholo", "khulun", "boshabo", "bosabo", "cholbe",
        "chole", "hoy", "hobe", "hocche", "ache", "ase", "nai", "nei", "na",
        "haa", "ji", "bos", "ভাই", "dhonnobad", "shukriya", "alo", "bati",
        "komao", "barao", "banaw", "banao", "pathao", "likho", "lekho",
        "jeno", "amon", "emon", "sob", "shob", "aro", "ektu", "onek", "khub",
        "ajke", "kalker", "kalke", "abohawa", "bristi", "gorom", "thanda",
        "khobor", "gan", "gaan", "baja", "bajao", "taka", "hisab", "alarm",
        "charj", "ortho", "mane", "bujhiye", "bistarito", "sahajjo"
    )

    /**
     * Multi-word Banglish phrases that strongly indicate Bangla intent even when mixed
     * with English technical words (e.g., "flashlight on koro", "battery koto ache").
     */
    private val ROMANIZED_BANGLA_PHRASES = listOf(
        "on koro", "off koro", "chalu koro", "bondho koro", "open koro",
        "koto ache", "koto শতাংশ", "somoy koto", "shomoy koto", "koita baje",
        "kemon acho", "kemon achen", "ki khobor", "ki vabe", "kivabe cholbe",
        "kothay boshabo", "baniye daw", "baniye dao", "bole daw", "bole dao"
    )

    /**
     * Automatically detects whether the user's spoken or typed utterance is Bangla or English,
     * and returns a complete [LanguageDetectionResult] containing the dynamic Gemini prompt
     * directive and target TTS [Locale].
     */
    fun detectUserLanguage(
        inputText: String,
        preferredLanguageMode: String = "AUTO",
        bossTitle: String = "Boss"
    ): LanguageDetectionResult {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) {
            val defaultBangla = preferredLanguageMode.equals("BN", ignoreCase = true)
            return createResult(
                isBangla = defaultBangla,
                scriptType = if (defaultBangla) DetectedScriptType.BENGALI_UNICODE else DetectedScriptType.ENGLISH_LATIN,
                confidence = 0.5f,
                bossTitle = bossTitle
            )
        }

        // 1. Check for Bengali Unicode characters (\u0980..\u09FF) or Bengali Dari ('।')
        val bengaliCharCount = trimmed.count { it in '\u0980'..'\u09FF' || it == '।' }
        val latinCharCount = trimmed.count { it in 'a'..'z' || it in 'A'..'Z' }

        if (bengaliCharCount > 0) {
            val totalLetters = (bengaliCharCount + latinCharCount).coerceAtLeast(1)
            val ratio = bengaliCharCount.toFloat() / totalLetters.toFloat()
            val confidence = (0.75f + (ratio * 0.25f)).coerceIn(0.75f, 1.0f)
            return createResult(
                isBangla = true,
                scriptType = DetectedScriptType.BENGALI_UNICODE,
                confidence = confidence,
                bossTitle = bossTitle
            )
        }

        // 2. Check for Romanized Bangla (Banglish) phrases and tokens
        val lower = trimmed.lowercase(Locale.ROOT)
        val matchedPhrase = ROMANIZED_BANGLA_PHRASES.any { lower.contains(it) }
        val words = lower
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() }

        val banglishWordMatches = words.count { it in ROMANIZED_BANGLA_KEYWORDS }

        if (matchedPhrase || banglishWordMatches >= 2 || (words.size in 1..3 && banglishWordMatches >= 1)) {
            val confidence = when {
                matchedPhrase -> 0.92f
                words.isNotEmpty() -> (0.65f + (banglishWordMatches.toFloat() / words.size.toFloat()) * 0.30f)
                    .coerceIn(0.68f, 0.95f)
                else -> 0.75f
            }
            return createResult(
                isBangla = true,
                scriptType = DetectedScriptType.BANGLISH_ROMANIZED,
                confidence = confidence,
                bossTitle = bossTitle
            )
        }

        // 3. If user explicitly locked BN mode and input is a neutral noun (e.g., an app name), respect BN mode
        if (preferredLanguageMode.equals("BN", ignoreCase = true) && words.size <= 2) {
            return createResult(
                isBangla = true,
                scriptType = DetectedScriptType.BANGLISH_ROMANIZED,
                confidence = 0.65f,
                bossTitle = bossTitle
            )
        }

        // 4. Default to English
        return createResult(
            isBangla = false,
            scriptType = DetectedScriptType.ENGLISH_LATIN,
            confidence = 0.95f,
            bossTitle = bossTitle
        )
    }

    /**
     * Dynamically constructs the Gemini model system prompt instructions based on the
     * detected user language so that Gemini responds in the matching language and script.
     */
    fun buildDynamicGeminiLanguagePrompt(
        detection: LanguageDetectionResult,
        bossTitle: String = "Boss"
    ): String {
        return if (detection.isBangla) {
            val scriptNote = if (detection.scriptType == DetectedScriptType.BANGLISH_ROMANIZED) {
                "The user spoke/typed in Romanized Bangla (Banglish). Understand the Bangla meaning accurately and reply in natural, fluent বাংলা (Bengali Unicode script) so the Android Bangla TextToSpeech engine pronounces it clearly."
            } else {
                "The user spoke/typed in বাংলা (Bengali script). Reply in natural, polite, and intelligent বাংলা (Bengali script)."
            }
            """
            DYNAMIC LANGUAGE DIRECTIVE (AUTO-DETECTED: BANGLA [bn-BD], Confidence: ${(detection.confidence * 100).toInt()}%):
            - $scriptNote
            - Address the user respectfully as "বস" (or "$bossTitle").
            - Set `"detectedLanguage": "bn"` in the JSON output.
            - Keep `spokenReply` concise, warm, and natural for Bangla voice synthesis (1–3 sentences).
            - Provide `followUpSuggestions` in বাংলা so the user can tap or speak them easily.
            """.trimIndent()
        } else {
            """
            DYNAMIC LANGUAGE DIRECTIVE (AUTO-DETECTED: ENGLISH [en-US], Confidence: ${(detection.confidence * 100).toInt()}%):
            - The user spoke/typed in English. Reply in crisp, polite, and intelligent English inspired by Iron Man's JARVIS.
            - Address the user respectfully as "$bossTitle".
            - Set `"detectedLanguage": "en"` in the JSON output.
            - Keep `spokenReply` concise and natural for English voice synthesis (1–3 sentences).
            - Provide `followUpSuggestions` in English.
            """.trimIndent()
        }
    }

    /**
     * Dynamically resolves the best Android [TextToSpeech] [Locale] for a given reply text
     * and language hint (`"bn"` or `"en"`), verifying against available TTS locales when provided.
     */
    fun resolveDynamicTtsLocale(
        textToSpeak: String,
        languageHint: String = "AUTO",
        preferredBanglaLocale: Locale = BANGLA_PRIMARY_LOCALE,
        preferredEnglishLocale: Locale = ENGLISH_PRIMARY_LOCALE
    ): Locale {
        val hasBanglaUnicode = textToSpeak.any { it in '\u0980'..'\u09FF' }
        val hintIsBangla = languageHint.equals("bn", ignoreCase = true) ||
            languageHint.startsWith("bn-", ignoreCase = true)

        val isBangla = if (hasBanglaUnicode) {
            true
        } else if (hintIsBangla) {
            // If hint is Bangla and text is not purely English sentences, or if detection says Bangla
            val detection = detectUserLanguage(textToSpeak)
            detection.isBangla || !textToSpeak.any { it in 'a'..'z' || it in 'A'..'Z' }
        } else {
            detectUserLanguage(textToSpeak).isBangla
        }

        return if (isBangla) {
            preferredBanglaLocale
        } else {
            preferredEnglishLocale
        }
    }

    /**
     * Quick check if a text segment should be spoken using the Bangla TTS voice/locale.
     */
    fun isBanglaUtterance(text: String, languageHint: String = "AUTO"): Boolean {
        if (text.any { it in '\u0980'..'\u09FF' }) return true
        val detected = detectUserLanguage(text)
        if (detected.isBangla) return true
        return languageHint.equals("bn", ignoreCase = true) &&
            !text.any { it in 'a'..'z' || it in 'A'..'Z' }
    }

    private fun createResult(
        isBangla: Boolean,
        scriptType: DetectedScriptType,
        confidence: Float,
        bossTitle: String
    ): LanguageDetectionResult {
        val language = if (isBangla) JarvisLanguage.BANGLA else JarvisLanguage.ENGLISH
        val ttsLocale = if (isBangla) BANGLA_PRIMARY_LOCALE else ENGLISH_PRIMARY_LOCALE
        val fallbackLocale = if (isBangla) BANGLA_FALLBACK_LOCALE else ENGLISH_FALLBACK_LOCALE
        val recognizerTag = if (isBangla) "bn-BD" else "en-US"

        val tempResult = LanguageDetectionResult(
            language = language,
            isBangla = isBangla,
            scriptType = scriptType,
            confidence = confidence,
            ttsLocale = ttsLocale,
            fallbackTtsLocale = fallbackLocale,
            speechRecognizerLocaleTag = recognizerTag,
            geminiPromptDirective = ""
        )

        return tempResult.copy(
            geminiPromptDirective = buildDynamicGeminiLanguagePrompt(tempResult, bossTitle)
        )
    }
}
