package tech.onova.offlinetranslator.core.domain.speech

import tech.onova.offlinetranslator.core.domain.ports.PackId

/**
 * One of the app's own voices (spec 082): a Piper voice in its [pack], its [voice] name and [quality] in Piper's
 * catalogue, and its dataset's [licence] as its model card states it (research R4).
 */
public data class AppVoice(
    val pack: PackId,
    val voice: String,
    val quality: String,
    val licence: String,
)
