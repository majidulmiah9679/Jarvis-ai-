package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.JarvisBrainService
import com.example.data.REAL_VOICE_PERSONAS
import com.example.data.VoiceGender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("JARVIS AI", appName)
  }

  @Test
  fun `detect Google Gemini and Vertex AQ API keys accurately`() {
    val sampleVertexKey = "AQ." + "Ab8RN6K8U_9WskNO9HdbqbAkcSzUFqSz_i_qQgb2-UXoj1uFRA"
    assertTrue(JarvisBrainService.isGoogleGeminiOrVertexKey(sampleVertexKey))
    assertEquals(
      "GOOGLE GEMINI / VERTEX KEY (AQ.)",
      JarvisBrainService.detectKeyType(sampleVertexKey)
    )

    val sampleGeminiKey = "AIzaSySampleKey1234567890"
    assertTrue(JarvisBrainService.isGoogleGeminiOrVertexKey(sampleGeminiKey))
    assertEquals(
      "GEMINI API KEY (AIza)",
      JarvisBrainService.detectKeyType(sampleGeminiKey)
    )

    val sampleOpenAiKey = "sk-proj-123456"
    assertFalse(JarvisBrainService.isGoogleGeminiOrVertexKey(sampleOpenAiKey))
  }

  @Test
  fun `verify 6 real male and female voice personas are configured`() {
    assertEquals(6, REAL_VOICE_PERSONAS.size)
    assertEquals(3, REAL_VOICE_PERSONAS.count { it.gender == VoiceGender.FEMALE })
    assertEquals(3, REAL_VOICE_PERSONAS.count { it.gender == VoiceGender.MALE })
  }
}

