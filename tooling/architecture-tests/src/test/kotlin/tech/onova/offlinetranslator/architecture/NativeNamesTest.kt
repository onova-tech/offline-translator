package tech.onova.offlinetranslator.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The engine's JNI functions are found by the Kotlin class's full name, so a package rename must rebuild
 * `libbergamot.so` too (spec 081, 2026-11-21: the renamed app crashed on its first translation). Both the C++ source and
 * each committed library must export the names of the class as it is now.
 */
class NativeNamesTest {
    private val translation = File(ProjectSources.root, "core/translation")
    private val kotlinClass: File =
        translation
            .walkTopDown()
            .single { it.name == "BergamotNative.kt" }

    private val expected: String by lazy {
        val pkg =
            Regex("""^package ([\w.]+)""", RegexOption.MULTILINE)
                .find(kotlinClass.readText())
                ?.groupValues
                ?.get(1)
                ?: error("no package in $kotlinClass")
        "Java_" + pkg.replace('.', '_') + "_BergamotNative_"
    }

    @Test
    fun theJniSourceNamesTheClassAsItIs() {
        val source = File(translation, "native/bergamot_jni.cpp").readText()
        val names = Regex("""Java_\w+?_BergamotNative_\w+""").findAll(source).map { it.value }.toList()
        assertEquals(FUNCTIONS, names.size, "JNI functions in bergamot_jni.cpp: $names")
        assertTrue(names.all { it.startsWith(expected) }, "expected $expected…, found $names")
    }

    @Test
    fun everyCommittedLibraryExportsThoseNames() {
        val libraries =
            File(translation, "src/androidMain/jniLibs")
                .walkTopDown()
                .filter { it.name == "libbergamot.so" }
                .toList()
        assertEquals(ABIS, libraries.size, "libbergamot.so per ABI: $libraries")
        libraries.forEach { library ->
            val text = String(library.readBytes(), Charsets.ISO_8859_1)
            assertEquals(FUNCTIONS, Regex(Regex.escape(expected) + """\w+""").findAll(text).count(), "$library")
        }
    }

    private companion object {
        const val FUNCTIONS = 3
        const val ABIS = 2
    }
}
