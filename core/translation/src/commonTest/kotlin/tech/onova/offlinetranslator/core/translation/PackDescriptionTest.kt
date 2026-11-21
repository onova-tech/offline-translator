package tech.onova.offlinetranslator.core.translation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** contracts/pack-format.md "Rules": what makes a pack usable, and what reads as damaged. */
class PackDescriptionTest {
    private val packJson =
        """
        {
          "format": 1, "id": "translate_pt", "language": "pt", "version": 1,
          "directions": [
            { "from": "en", "to": "pt", "folder": "en-pt",
              "model": "model.enpt.intgemm.alphas.bin", "shortlist": "lex.50.50.enpt.s2t.bin", "vocab": "vocab.enpt.spm",
              "sha256": { "model.enpt.intgemm.alphas.bin": "07892fd2" } },
            { "from": "pt", "to": "en", "folder": "pt-en",
              "model": "model.pten.intgemm.alphas.bin", "shortlist": "lex.50.50.pten.s2t.bin", "vocab": "vocab.pten.spm" }
          ],
          "source": "https://example.org/models/", "license": "MPL-2.0"
        }
        """.trimIndent()

    private val everyFile: (String) -> Long? = { 1_000L }

    @Test
    fun aCompletePackGivesItsDirectionsWithFullPaths() {
        val directions = PackDescription.read("/packs/translate_pt", packJson, everyFile)
        assertEquals(
            listOf(
                PackDirection(
                    name = "en-pt",
                    model = "/packs/translate_pt/en-pt/model.enpt.intgemm.alphas.bin",
                    shortlist = "/packs/translate_pt/en-pt/lex.50.50.enpt.s2t.bin",
                    sourceVocab = "/packs/translate_pt/en-pt/vocab.enpt.spm",
                    targetVocab = "/packs/translate_pt/en-pt/vocab.enpt.spm",
                ),
                PackDirection(
                    name = "pt-en",
                    model = "/packs/translate_pt/pt-en/model.pten.intgemm.alphas.bin",
                    shortlist = "/packs/translate_pt/pt-en/lex.50.50.pten.s2t.bin",
                    sourceVocab = "/packs/translate_pt/pt-en/vocab.pten.spm",
                    targetVocab = "/packs/translate_pt/pt-en/vocab.pten.spm",
                ),
            ),
            directions,
        )
    }

    /** Spec 080: English to Japanese, Korean and Chinese have a vocabulary for each side. */
    @Test
    fun aDirectionWithTwoVocabulariesGivesEach() {
        val split =
            """
            { "format": 1, "directions": [ { "folder": "en-ja", "model": "m.bin", "shortlist": "l.bin",
              "sourceVocab": "srcvocab.enja.spm", "targetVocab": "trgvocab.enja.spm" } ] }
            """.trimIndent()
        val direction = PackDescription.read("/p", split, everyFile)?.single()
        assertEquals("/p/en-ja/srcvocab.enja.spm", direction?.sourceVocab)
        assertEquals("/p/en-ja/trgvocab.enja.spm", direction?.targetVocab)
        assertNull(PackDescription.read("/p", split, { path -> if (path.endsWith("trgvocab.enja.spm")) null else 1L }))
        val none =
            split.replace(
                "\"sourceVocab\": \"srcvocab.enja.spm\", \"targetVocab\": \"trgvocab.enja.spm\"",
                "\"x\": 1",
            )
        assertNull(PackDescription.read("/p", none, everyFile), "a direction without a vocabulary is damaged")
    }

    @Test
    fun aMissingOrEmptyFileMakesThePackDamaged() {
        assertNull(PackDescription.read("/p", packJson) { path -> if (path.endsWith("vocab.pten.spm")) null else 1L })
        assertNull(
            PackDescription.read("/p", packJson) { path -> if (path.endsWith("lex.50.50.enpt.s2t.bin")) 0L else 1L },
        )
    }

    @Test
    fun anUnknownFormatOrUnreadableJsonIsDamaged() {
        assertNull(PackDescription.read("/p", packJson.replace("\"format\": 1", "\"format\": 2"), everyFile))
        assertNull(PackDescription.read("/p", "{ not json", everyFile))
        assertNull(PackDescription.read("/p", """{ "format": 1, "directions": [] }""", everyFile))
    }
}
