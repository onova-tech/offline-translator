package tech.onova.offlinetranslator.feature.settings.licenses

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 012 T056: the build's license list (AboutLibraries' JSON) as the page shows it. */
class OpenSourceLicensesTest {
    private val json =
        """
        {
          "metadata": {"generated": "2026-11-18"},
          "libraries": [
            {"uniqueId": "androidx.activity:activity", "name": "Activity", "website": "https://developer.android.com",
             "developers": [{"name": "The Android Open Source Project"}], "licenses": ["Apache-2.0"]},
            {"uniqueId": "androidx.activity:activity-ktx", "name": "Activity", "website": "https://developer.android.com",
             "developers": [{"name": "The Android Open Source Project"}], "licenses": ["Apache-2.0"]},
            {"uniqueId": "androidx.annotation:annotation", "name": "Annotation",
             "organization": {"name": "The Android Open Source Project"}, "licenses": ["Apache-2.0"]},
            {"uniqueId": "offlinetranslator.bergamot", "name": "Bergamot translator", "website": "https://github.com/mozilla/translations",
             "developers": [{"name": "Mozilla"}], "licenses": ["offlinetranslator-bergamot"]},
            {"uniqueId": "nobody:thing", "name": "Thing", "licenses": []}
          ],
          "licenses": {
            "Apache-2.0": {"name": "Apache License 2.0", "url": "https://www.apache.org/licenses/LICENSE-2.0",
                           "content": "Apache text", "hash": "Apache-2.0"},
            "offlinetranslator-bergamot": {"name": "Mozilla Public License 2.0", "url": "https://www.mozilla.org/MPL/2.0/",
                                   "content": "MPL text", "hash": "offlinetranslator-bergamot"}
          }
        }
        """.trimIndent()

    @Test
    fun projectsAreGroupedByAuthorSortedByNameAndArtifactsOfOneProjectAreOneRow() {
        val groups = OpenSourceLicenses.parse(json)
        assertEquals(listOf("Mozilla", "The Android Open Source Project", ""), groups.map { it.author })
        val android = groups.single { it.author == "The Android Open Source Project" }
        assertEquals(listOf("Activity", "Annotation"), android.projects.map { it.name })
        assertEquals(
            listOf("Apache License 2.0"),
            android.projects
                .first()
                .licenses
                .map { it.name },
        )
    }

    @Test
    fun aProjectCarriesItsLicensesTextAndSource() {
        val bergamot =
            OpenSourceLicenses
                .parse(json)
                .single { it.author == "Mozilla" }
                .projects
                .single()
        assertEquals("Bergamot translator", bergamot.name)
        assertEquals("https://github.com/mozilla/translations", bergamot.website)
        val license = bergamot.licenses.single()
        assertEquals("Mozilla Public License 2.0", license.name)
        assertEquals("MPL text", license.content)
    }

    @Test
    fun aProjectWithoutALicenseIsKeptWithNoneSoATestCanFindIt() {
        val thing =
            OpenSourceLicenses
                .parse(json)
                .single { it.author == "" }
                .projects
                .single()
        assertTrue(thing.licenses.isEmpty())
    }

    @Test
    fun aDamagedListIsEmpty() {
        assertEquals(emptyList(), OpenSourceLicenses.parse("not json"))
    }
}
