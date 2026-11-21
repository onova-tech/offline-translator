package tech.onova.offlinetranslator.core.translation

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import tech.onova.offlinetranslator.core.domain.ports.EngineOpen
import tech.onova.offlinetranslator.core.domain.ports.EngineUnavailable
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.Route
import tech.onova.offlinetranslator.core.domain.ports.TranslationEngine
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Bergamot on Android (spec 012 research R3): every call runs on one engine thread, in order, since the engine isn't
 * thread-safe; dropping a stale result is the caller's job (newest wins). A model loads the first time its direction
 * is used. A phone whose processor has no `libbergamot.so` (32-bit) gets `UNSUPPORTED_DEVICE`. No network.
 */
public class BergamotEngine : TranslationEngine {
    private val thread = Executors.newSingleThreadExecutor { Thread(it, "bergamot") }
    private val dispatcher = thread.asCoroutineDispatcher()
    private var handle = 0L

    private val libraryLoaded: Boolean by lazy {
        try {
            System.loadLibrary(LIBRARY)
            true
        } catch (ignored: UnsatisfiedLinkError) {
            false
        }
    }

    override suspend fun open(packs: Map<PackId, String>): EngineOpen =
        withContext(dispatcher) {
            if (!libraryLoaded) return@withContext EngineOpen.Unavailable(EngineUnavailable.UNSUPPORTED_DEVICE)
            closeNow()
            // A pack whose description or files can't be read is left out and named; the others open.
            val read = packs.mapValues { (_, folder) -> read(folder) }
            val damaged = read.filterValues { it == null }.keys
            val directions = read.values.filterNotNull().flatten()
            val opened = if (damaged.isEmpty()) EngineOpen.Ready else EngineOpen.Damaged(damaged)
            if (directions.isEmpty()) return@withContext opened
            try {
                handle =
                    BergamotNative.open(
                        names = directions.map { it.name }.toTypedArray(),
                        models = directions.map { it.model }.toTypedArray(),
                        shortlists = directions.map { it.shortlist }.toTypedArray(),
                        sourceVocabs = directions.map { it.sourceVocab }.toTypedArray(),
                        targetVocabs = directions.map { it.targetVocab }.toTypedArray(),
                    )
                opened
            } catch (ignored: IllegalStateException) {
                EngineOpen.Unavailable(EngineUnavailable.DAMAGED_PACK)
            }
        }

    /** Throws [IllegalStateException] when the engine isn't open or a model fails; the caller shows it as failed. */
    override suspend fun translate(
        text: String,
        route: Route,
    ): String {
        if (route == Route.Same) return text
        return withContext(dispatcher) {
            check(handle != 0L) { "The engine isn't open" }
            val (first, second) =
                when (route) {
                    is Route.Direct -> {
                        name(route.from, route.to) to null
                    }

                    is Route.ThroughEnglish -> {
                        name(route.from, LanguageCode.ENGLISH) to name(LanguageCode.ENGLISH, route.to)
                    }

                    Route.Same -> {
                        error("Unreachable")
                    }
                }
            BergamotNative.translate(handle, first, second, text.encodeToByteArray()).decodeToString()
        }
    }

    /** Frees the models on the engine thread, after any translation still running there. */
    override fun close() {
        thread.execute { closeNow() }
    }

    private fun closeNow() {
        if (handle != 0L) BergamotNative.close(handle)
        handle = 0L
    }

    private fun read(folder: String): List<PackDirection>? {
        val text =
            try {
                File(folder, PACK_JSON).readText()
            } catch (ignored: IOException) {
                return null
            }
        return PackDescription.read(folder, text) { path -> File(path).takeIf { it.isFile }?.length() }
    }

    private fun name(
        from: LanguageCode,
        to: LanguageCode,
    ) = "${from.value}-${to.value}"

    private companion object {
        const val LIBRARY = "bergamot"
        const val PACK_JSON = "pack.json"
    }
}
