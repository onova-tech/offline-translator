package tech.onova.offlinetranslator.core.domain.speech

import tech.onova.offlinetranslator.core.domain.translation.LanguageCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Spec 082 T009: what the generated catalog must keep (research R4). */
class SpeechCatalogTest {
    private val languages = LanguageCatalog.all.map { it.code }.toSet()

    @Test
    fun everyLanguageItSpeaksOrHearsIsOneTheAppOffers() {
        assertTrue(languages.containsAll(SpeechCatalog.voices.keys), "voices: ${SpeechCatalog.voices.keys - languages}")
        assertTrue(languages.containsAll(SpeechCatalog.recognition.keys))
    }

    @Test
    fun whisperHearsAlmostEveryLanguage() {
        // All but Uyghur, which Whisper doesn't know (research R4).
        assertEquals(languages.size - 1, SpeechCatalog.recognition.size)
        assertTrue(SpeechCatalog.recognition.values.all { it.isNotBlank() })
    }

    @Test
    fun everyVoiceHasItsOwnPackAndAFreeLicence() {
        val packs = SpeechCatalog.packs.map { it.value }
        assertEquals(packs.distinct(), packs)
        SpeechCatalog.voices.forEach { (language, voice) ->
            assertEquals("voice_${language.value}", voice.pack.value)
            assertTrue(FREE.containsMatchIn(voice.licence), "$language: ${voice.licence}")
            assertFalse(NOT_FREE.containsMatchIn(voice.licence), "$language: ${voice.licence}")
        }
    }

    @Test
    fun noVoiceComesFromResearchOnlyData() {
        // Arabic's and Turkish's only Piper voices were tuned from "lessac", licensed for research only (research R4).
        assertFalse(SpeechCatalog.voices.values.any { "lessac" in it.voice })
        assertTrue(SpeechCatalog.voices.keys.none { it.value in setOf("ar", "tr", "ja", "ko") })
    }

    private companion object {
        val FREE =
            Regex(
                """cc0|public domain|cc[- ]?by|creativecommons\.org/licenses/by|attribution|\bmit\b""",
                RegexOption.IGNORE_CASE,
            )
        val NOT_FREE = Regex("""by-?nc|non-?commercial|research""", RegexOption.IGNORE_CASE)
    }
}
