package com.example

import com.example.util.DetectedScriptType
import com.example.util.JarvisLanguage
import com.example.util.LanguageDetectionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun detectUserLanguage_identifiesBengaliUnicodeScript() {
        val result = LanguageDetectionHelper.detectUserLanguage("ফ্ল্যাশলাইট চালু করো বস")
        assertTrue(result.isBangla)
        assertEquals(JarvisLanguage.BANGLA, result.language)
        assertEquals(DetectedScriptType.BENGALI_UNICODE, result.scriptType)
        assertEquals("bn-BD", result.ttsLocale.toLanguageTag())
        assertTrue(result.geminiPromptDirective.contains("BANGLA [bn-BD]"))
    }

    @Test
    fun detectUserLanguage_identifiesRomanizedBangla() {
        val result = LanguageDetectionHelper.detectUserLanguage("Api key kothay boshabo kivabe cholbe")
        assertTrue(result.isBangla)
        assertEquals(JarvisLanguage.BANGLA, result.language)
        assertEquals(DetectedScriptType.BANGLISH_ROMANIZED, result.scriptType)
        assertEquals("bn-BD", result.ttsLocale.toLanguageTag())
        assertTrue(result.geminiPromptDirective.contains("Romanized Bangla"))
    }

    @Test
    fun detectUserLanguage_identifiesEnglish() {
        val result = LanguageDetectionHelper.detectUserLanguage("Jarvis, turn on the flashlight and check battery status")
        assertFalse(result.isBangla)
        assertEquals(JarvisLanguage.ENGLISH, result.language)
        assertEquals(DetectedScriptType.ENGLISH_LATIN, result.scriptType)
        assertTrue(result.geminiPromptDirective.contains("ENGLISH [en-US]"))
    }

    @Test
    fun resolveDynamicTtsLocale_switchesBetweenBanglaAndEnglish() {
        val bnLocale = LanguageDetectionHelper.resolveDynamicTtsLocale("জি বস, এখন ব্যাটারি ৮৫% আছে।", "bn")
        assertEquals("bn-BD", bnLocale.toLanguageTag())

        val enLocale = LanguageDetectionHelper.resolveDynamicTtsLocale("Right away, Boss. Opening Android Settings.", "en")
        assertEquals("en-GB", enLocale.toLanguageTag())
    }
}
