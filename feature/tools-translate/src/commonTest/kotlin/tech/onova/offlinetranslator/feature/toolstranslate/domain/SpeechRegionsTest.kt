package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import kotlin.test.Test
import kotlin.test.assertEquals

/** Spec 012 research R9: speech needs a region; the phone's own one wins when it speaks that language. */
class SpeechRegionsTest {
    private val cases =
        listOf(
            Case("pt", listOf("pt-BR"), "pt-BR"),
            Case("pt", listOf("pt-PT"), "pt-PT"),
            Case("es", listOf("es-MX", "en-US"), "es-MX"),
            Case("es", listOf("pt-BR"), "es-ES"),
            Case("en", listOf("pt-BR", "en-GB"), "en-GB"),
            Case("en", emptyList(), "en-US"),
            Case("pt", listOf("en-US"), "pt-BR"),
            Case("es", listOf("es"), "es-ES"),
            Case("fr", listOf("en-US"), "fr-FR"),
            Case("fr", listOf("fr-CA"), "fr-CA"),
            Case("zh_hant", listOf("zh-CN"), "zh-TW"),
            Case("zh", listOf("en-US"), "zh-CN"),
            Case("tl", emptyList(), "fil-PH"),
            Case("xx", emptyList(), "xx"),
        )

    @Test
    fun eachLanguageGetsTheRightRegion() {
        cases.forEach { case ->
            assertEquals(
                case.expected,
                SpeechRegions.tagFor(LanguageCode(case.language), case.phoneLocales),
                "${case.language} on a phone in ${case.phoneLocales}",
            )
        }
    }

    private class Case(
        val language: String,
        val phoneLocales: List<String>,
        val expected: String,
    )
}
