package tech.onova.offlinetranslator.core.domain.speech

import tech.onova.offlinetranslator.core.domain.ports.PackId

/** Every speech pack (spec 082): Whisper, espeak-ng's data and each voice, as Google Play delivers them. */
public val SpeechCatalog.packs: List<PackId>
    get() = listOf(recognitionPack, voiceDataPack) + voices.values.map { it.pack }
