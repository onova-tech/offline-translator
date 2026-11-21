package tech.onova.offlinetranslator.core.translation

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** One direction of a pack, with its files as paths the engine can open (contracts/pack-format.md). */
public data class PackDirection(
    /** The folder's name, such as `pt-en`: the engine's name for the direction. */
    val name: String,
    val model: String,
    val shortlist: String,
    /** The source and target vocabularies: the same file for most directions, two for some (spec 080 R2). */
    val sourceVocab: String,
    val targetVocab: String,
)

/**
 * A pack's `pack.json`, read by the rules of contracts/pack-format.md: a format the app knows, and every file it names
 * present and not empty. Anything else reads as a damaged pack (null). The hashes are the build script's to check, not
 * every start's (75 MB to hash would miss the 2-second goal).
 */
public object PackDescription {
    public const val FORMAT: Int = 1

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * The directions of the pack in [folder], whose `pack.json` is [text]. [fileSize] gives a file's size in bytes, or
     * null when it doesn't exist, so the rules are tested without a disk.
     */
    public fun read(
        folder: String,
        text: String,
        fileSize: (String) -> Long?,
    ): List<PackDirection>? {
        val description = decode(text)?.takeIf { it.format == FORMAT && it.directions.isNotEmpty() } ?: return null
        val directions = description.directions.mapNotNull { d -> d.inFolder("$folder/${d.folder}") }
        val files = directions.flatMap { d -> listOf(d.model, d.shortlist, d.sourceVocab, d.targetVocab) }
        // A direction without a vocabulary, or a file missing or empty, makes the whole pack damaged.
        val whole = directions.size == description.directions.size && files.all { (fileSize(it) ?: 0) > 0 }
        return directions.takeIf { whole }
    }

    private fun decode(text: String): PackJson? =
        try {
            json.decodeFromString(PackJson.serializer(), text)
        } catch (ignored: SerializationException) {
            null
        } catch (ignored: IllegalArgumentException) {
            null
        }

    @Serializable
    private class PackJson(
        val format: Int,
        val directions: List<DirectionJson> = emptyList(),
    )

    /** One vocabulary for both sides ([vocab]), or one for each ([sourceVocab] and [targetVocab]). */
    @Serializable
    private class DirectionJson(
        val folder: String,
        val model: String,
        val shortlist: String,
        val vocab: String? = null,
        val sourceVocab: String? = null,
        val targetVocab: String? = null,
    ) {
        /** Its files under [base]; null when it names no vocabulary for a side. */
        fun inFolder(base: String): PackDirection? {
            val source = sourceVocab ?: vocab
            val target = targetVocab ?: vocab
            if (source == null || target == null) return null
            return PackDirection(folder, "$base/$model", "$base/$shortlist", "$base/$source", "$base/$target")
        }
    }
}
