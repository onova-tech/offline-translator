package tech.onova.offlinetranslator.core.data.datastore

/** Maps a stored enum name back to a value, tolerating unknown names from other versions. */
public inline fun <reified E : Enum<E>> enumOrDefault(
    name: String?,
    default: E,
): E = enumValues<E>().firstOrNull { it.name == name } ?: default

public inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? = enumValues<E>().firstOrNull { it.name == name }
