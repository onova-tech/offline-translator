package tech.onova.offlinetranslator.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import okio.Path

/**
 * Creates the app's typed JSON stores inside one app-private [directory] (research.md R4).
 * Create each file at most once per process; DataStore forbids two stores on the same file.
 */
public class JsonDataStoreFactory(
    private val fileSystem: FileSystem,
    private val directory: Path,
    private val scope: CoroutineScope,
) {
    public fun <T> create(
        fileName: String,
        serializer: JsonDocumentSerializer<T>,
    ): DataStore<T> {
        fileSystem.createDirectories(directory)
        return DataStoreFactory.create(
            storage =
                OkioStorage(
                    fileSystem = fileSystem,
                    serializer = serializer,
                    producePath = { directory / fileName },
                ),
            corruptionHandler = ReplaceFileCorruptionHandler { serializer.defaultValue },
            scope = scope,
        )
    }
}
