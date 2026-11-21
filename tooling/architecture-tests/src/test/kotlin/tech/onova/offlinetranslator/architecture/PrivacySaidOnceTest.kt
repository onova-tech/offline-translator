package tech.onova.offlinetranslator.architecture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Privacy is said once (owner, 2026-11-17, spec 078): in the first-launch notice, the privacy policy and the Play
 * listing, not again in every tool. "Repeating this over and over in every tool that needs permission seems more like a
 * threat." A tool's explanation or help never promises that nothing is sent, stored or kept off this phone; it says
 * what the tool does.
 */
class PrivacySaidOnceTest {
    private val string = Regex("""<string name="([a-z_0-9]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)

    /** Where privacy is said on purpose: the first-launch notice and Settings' policy row. */
    private val sayItHere = listOf("host_privacy_notice_", "privacy_policy")

    private val reassurances =
        listOf(
            // English
            "never sent",
            "nothing is sent",
            "is not sent",
            "isn't sent",
            "saved or sent",
            "recorded or sent",
            "never stored",
            "never kept",
            "never saved",
            "on this phone only",
            "stays on this phone",
            "stay on your phone",
            "leaves the phone",
            // Português (Brasil)
            "nunca é enviad",
            "nada é enviado",
            "nem enviado",
            "nunca é guardad",
            "nunca guardad",
            "nunca é salv",
            "só neste celular",
            "sai do celular",
            "ficam no seu celular",
            // Español
            "nunca se envía",
            "no se envía nada",
            "ni se envía",
            "nunca se guarda",
            "solo en este teléfono",
            "sale del teléfono",
            "se quedan en tu teléfono",
        )

    private val strings: List<Triple<String, String, String>> =
        listOf("core", "feature")
            .map { ProjectSources.root.resolve(it) }
            .flatMap { folder ->
                folder
                    .walkTopDown()
                    .onEnter { it.name != "build" }
                    .filter { it.extension == "xml" && it.name.startsWith("strings") }
                    .filter { it.parentFile.name.startsWith("values") }
                    .flatMap { file ->
                        string.findAll(file.readText()).map {
                            Triple(it.groupValues[1], file.parentFile.name, it.groupValues[2])
                        }
                    }.toList()
            }

    @Test
    fun noToolRepeatsThePrivacyPromise() {
        assertTrue(strings.size > MIN_STRINGS, "Found only ${strings.size} strings")
        val problems =
            strings
                .filterNot { (name, _, _) -> sayItHere.any(name::startsWith) }
                .mapNotNull { (name, language, text) ->
                    reassurances
                        .firstOrNull {
                            text.contains(
                                it,
                                ignoreCase = true,
                            )
                        }?.let { "$name ($language): \"$it\"" }
                }
        assertEquals(emptyList(), problems, "Say it in the first-launch notice instead (spec 078)")
    }

    private companion object {
        const val MIN_STRINGS = 400
    }
}
