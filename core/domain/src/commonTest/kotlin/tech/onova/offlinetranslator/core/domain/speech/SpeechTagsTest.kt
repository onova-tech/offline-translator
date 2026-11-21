package tech.onova.offlinetranslator.core.domain.speech

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Spec 082: from the speech tag the Translator uses to the app's language and Whisper's code. */
class SpeechTagsTest {
    @Test
    fun theLanguageSubtagNamesTheLanguage() {
        assertEquals(LanguageCode("pt"), SpeechTags.languageOf("pt-BR"))
        assertEquals(LanguageCode("pt"), SpeechTags.languageOf("pt-PT"))
        assertEquals(LanguageCode.ENGLISH, SpeechTags.languageOf("en-US"))
    }

    @Test
    fun chineseNorwegianAndFilipinoHaveTheirOwnCodes() {
        assertEquals(LanguageCode("zh"), SpeechTags.languageOf("zh-CN"))
        assertEquals(LanguageCode("zh_hant"), SpeechTags.languageOf("zh-TW"))
        assertEquals(LanguageCode("zh_hant"), SpeechTags.languageOf("zh-Hant-HK"))
        assertEquals(LanguageCode("nb"), SpeechTags.languageOf("nb-NO"))
        assertEquals(LanguageCode("tl"), SpeechTags.languageOf("fil-PH"))
    }

    @Test
    fun aLanguageTheAppDoesntOfferHasNone() {
        assertNull(SpeechTags.languageOf("xx-YY"))
    }

    @Test
    fun whisperCodesFollow() {
        assertEquals("ar", SpeechTags.whisperCodeOf("ar-SA"))
        assertEquals("no", SpeechTags.whisperCodeOf("nb-NO"))
        assertEquals("zh", SpeechTags.whisperCodeOf("zh-TW"))
        assertNull(SpeechTags.whisperCodeOf("ug-CN"))
    }
}
