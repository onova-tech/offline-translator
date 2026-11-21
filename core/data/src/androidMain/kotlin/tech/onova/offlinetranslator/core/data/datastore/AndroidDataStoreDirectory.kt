package tech.onova.offlinetranslator.core.data.datastore

import android.content.Context
import okio.Path
import okio.Path.Companion.toPath

/** App-private location of the JSON stores: `filesDir/datastore/` (never backed up, Principle II). */
public fun androidDataStoreDirectory(context: Context): Path =
    context.filesDir
        .resolve("datastore")
        .absolutePath
        .toPath()
