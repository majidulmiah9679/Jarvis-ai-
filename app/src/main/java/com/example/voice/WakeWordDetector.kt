package com.example.voice

import com.example.util.LanguageDetectionHelper

/**
 * Result of analyzing a partial or final speech transcript for the "Hey JARVIS" wake word.
 */
sealed interface WakeWordMatchResult {
    /**
     * The utterance does not contain a wake word.
     */
    data object NoWakeWord : WakeWordMatchResult

    /**
     * The user spoke the wake word by itself (e.g., "Hey JARVIS", "OK JARVIS", "হেই জারভিস")
     * to trigger the AI active listening mode.
     */
    data class WakeWordOnly(
        val matchedTrigger: String,
        val isBanglaTrigger: Boolean
    ) : WakeWordMatchResult

    /**
     * The user spoke the wake word followed immediately by a command in the same utterance
     * (e.g., "Hey JARVIS turn on the flashlight" or "হেই জারভিস ফ্ল্যাশলাইট চালু করো").
     */
    data class WakeWordWithCommand(
        val matchedTrigger: String,
        val command: String,
        val isBanglaTrigger: Boolean
    ) : WakeWordMatchResult
}

/**
 * Dedicated Wake Word Detection Engine for JARVIS AI.
 *
 * Recognizes "Hey JARVIS" (plus English & Bangla variations and common Android SpeechRecognizer
 * phonetic near-matches) in both partial real-time transcripts and final speech results,
 * triggering the AI listening mode immediately.
 */
object WakeWordDetector {

    /**
     * Primary English wake-word phrases ordered from most specific (multi-word) to single-word,
     * including common SpeechRecognizer phonetic transcriptions for "Hey JARVIS".
     */
    private val englishWakePhrases = listOf(
        "hey jarvis",
        "hi jarvis",
        "hello jarvis",
        "ok jarvis",
        "okay jarvis",
        "yo jarvis",
        "wake up jarvis",
        "listen jarvis",
        "hey jervis",
        "hey jarviz",
        "hey charvis",
        "hey harvis",
        "hey service",
        "ok jervis",
        "hello jervis",
        "jarvis"
    )

    /**
     * Primary Bangla wake-word phrases (both Bengali Unicode script and Romanized Banglish).
     */
    private val banglaWakePhrases = listOf(
        "হেই জারভিস",
        "হে জারভিস",
        "হ্যালো জারভিস",
        "ওকে জারভিস",
        "এই জারভিস",
        "শোনো জারভিস",
        "শুনছো জারভিস",
        "জারভিস শোনো",
        "জারভিস",
        "hei jarvis",
        "hey jarvis bolo",
        "shono jarvis",
        "ei jarvis",
        "valo jarvis"
    )

    /**
     * Polite filler words that often immediately follow "Hey JARVIS" without being a real command
     * (e.g. "Hey Jarvis boss", "Hey Jarvis are you there", "হেই জারভিস বস").
     */
    private val wakeOnlyFillers = setOf(
        "boss",
        "sir",
        "please",
        "bro",
        "man",
        "buddy",
        "বস",
        "স্যার",
        "ভাই",
        "শুনছো",
        "আছো",
        "বলো"
    )

    /**
     * Analyzes a speech transcript (partial or final) to determine if the user said
     * "Hey JARVIS" (or a supported English/Bangla wake phrase).
     */
    fun analyzeTranscript(rawTranscript: String): WakeWordMatchResult {
        val cleaned = rawTranscript.trim()
        if (cleaned.isBlank()) return WakeWordMatchResult.NoWakeWord

        val normalized = normalizeForWakeCheck(cleaned)
        if (normalized.isBlank()) return WakeWordMatchResult.NoWakeWord

        // 1. Check Bangla wake phrases first (Unicode or Romanized)
        for (phrase in banglaWakePhrases) {
            val match = matchWakePhrase(cleaned, normalized, phrase, isBanglaPhrase = true)
            if (match != null) return match
        }

        // 2. Check English wake phrases
        for (phrase in englishWakePhrases) {
            val match = matchWakePhrase(cleaned, normalized, phrase, isBanglaPhrase = false)
            if (match != null) return match
        }

        return WakeWordMatchResult.NoWakeWord
    }

    /**
     * Returns true if the transcript contains any supported wake word ("Hey JARVIS", etc.).
     */
    fun containsWakeWord(rawTranscript: String): Boolean {
        return analyzeTranscript(rawTranscript) !is WakeWordMatchResult.NoWakeWord
    }

    /**
     * Strips any leading wake word ("Hey JARVIS", "হেই জারভিস", etc.) from a command string.
     * If the string only contained the wake word, returns an empty string.
     */
    fun stripWakeWord(rawTranscript: String): String {
        return when (val res = analyzeTranscript(rawTranscript)) {
            is WakeWordMatchResult.WakeWordOnly -> ""
            is WakeWordMatchResult.WakeWordWithCommand -> res.command
            is WakeWordMatchResult.NoWakeWord -> rawTranscript.trim()
        }
    }

    private fun matchWakePhrase(
        originalText: String,
        normalizedText: String,
        wakePhrase: String,
        isBanglaPhrase: Boolean
    ): WakeWordMatchResult? {
        // Match wake phrase at start of utterance or after a short greeting/interjection
        val escapedPhrase = Regex.escape(wakePhrase)
        val regex = Regex(
            pattern = "(?:^|\\b|\\s)($escapedPhrase)(?:\\b|\\s|$|[,.!?।:;\\-]*)(.*)",
            option = RegexOption.IGNORE_CASE
        )

        val matchResult = regex.find(originalText) ?: regex.find(normalizedText) ?: return null
        val matchedTrigger = matchResult.groupValues.getOrNull(1)?.trim().orEmpty().ifBlank { wakePhrase }
        val rawRemainder = matchResult.groupValues.getOrNull(2)?.trim().orEmpty()
            .trimStart(',', '.', '!', '?', '।', ':', ';', '-', ' ')
            .trim()

        val isBangla = isBanglaPhrase || LanguageDetectionHelper.isBanglaUtterance(originalText)

        // Check if the remainder is empty or just a short vocative ("Boss", "বস")
        val normalizedRemainder = normalizeForWakeCheck(rawRemainder)
        if (normalizedRemainder.isBlank() || normalizedRemainder in wakeOnlyFillers) {
            return WakeWordMatchResult.WakeWordOnly(
                matchedTrigger = matchedTrigger,
                isBanglaTrigger = isBangla
            )
        }

        return WakeWordMatchResult.WakeWordWithCommand(
            matchedTrigger = matchedTrigger,
            command = rawRemainder,
            isBanglaTrigger = isBangla
        )
    }

    private fun normalizeForWakeCheck(input: String): String {
        return input
            .lowercase()
            .replace(Regex("[,.!?।:;\"'`~()\\[\\]{}\\-_]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
