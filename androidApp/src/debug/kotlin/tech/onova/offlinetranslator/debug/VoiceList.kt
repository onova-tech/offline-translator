package tech.onova.offlinetranslator.debug

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Spec 012 research R2, in detail: every text-to-speech engine on the phone and, for en, pt and es, the voices it lists
 * and what each says about itself, since the app's own check reads only the default engine. The lines go to the page
 * and every voice to logcat (`adb logcat -s PhoneProbe`).
 */
internal suspend fun voiceLines(context: Context): List<Pair<String, String>> {
    val first = startEngine(context, null) ?: return listOf("Engines" to "none started")
    val engines = first.engines.map { it.name }
    val default = first.defaultEngine
    first.shutdown()
    val lines = mutableListOf("Engines" to engines.joinToString().ifEmpty { "none listed" })
    for (name in engines) {
        val tts = startEngine(context, name)
        if (tts == null) {
            lines += name to "failed to start"
            continue
        }
        lines += (if (name == default) "$name (default)" else name) to ""
        val voices = tts.voices
        if (voices == null) lines += "Voices" to "null"
        for (language in LANGUAGES) {
            val iso3 = Locale.forLanguageTag(language).isO3Language
            val ofLanguage = voices.orEmpty().filter { it.locale.language == language || it.locale.language == iso3 }
            ofLanguage.forEach { Log.i(TAG, "$name ${describe(it)}") }
            val offline = ofLanguage.filter { it.isOffline() }
            lines += "$language: ${offline.size} offline of ${ofLanguage.size}" to
                offline
                    .map { it.locale.toString() }
                    .distinct()
                    .joinToString()
                    .ifEmpty { "—" }
            lines += "$language: language codes" to
                ofLanguage
                    .map { it.locale.language }
                    .distinct()
                    .joinToString()
                    .ifEmpty { "—" }
        }
        for (tag in TAGS) {
            lines += "$tag isLanguageAvailable" to availability(tts.isLanguageAvailable(Locale.forLanguageTag(tag)))
        }
        tts.shutdown()
    }
    return lines
}

private suspend fun startEngine(
    context: Context,
    engine: String?,
): TextToSpeech? {
    val started = CompletableDeferred<Boolean>()
    val tts =
        withContext(Dispatchers.Main) {
            TextToSpeech(context.applicationContext, { started.complete(it == TextToSpeech.SUCCESS) }, engine)
        }
    if (!started.await()) {
        tts.shutdown()
        return null
    }
    return tts
}

/** `SpeechMapping.offlineVoice`'s test, without the language; older engines still offer network synthesis. */
@Suppress("DEPRECATION")
private fun Voice.isOffline(): Boolean =
    !isNetworkConnectionRequired &&
        TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS !in features.orEmpty() &&
        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in features.orEmpty()

private fun describe(voice: Voice): String =
    "${voice.name} locale=${voice.locale} language=${voice.locale.language} country=${voice.locale.country} " +
        "network=${voice.isNetworkConnectionRequired} quality=${voice.quality} features=${voice.features}"

private fun availability(code: Int): String =
    when (code) {
        TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> "country and variant"
        TextToSpeech.LANG_COUNTRY_AVAILABLE -> "country"
        TextToSpeech.LANG_AVAILABLE -> "language only"
        TextToSpeech.LANG_MISSING_DATA -> "missing data"
        TextToSpeech.LANG_NOT_SUPPORTED -> "not supported"
        else -> "code $code"
    }

private const val TAG = "PhoneProbe"
private val LANGUAGES = listOf("en", "pt", "es")
private val TAGS = listOf("en-US", "pt-BR", "es-ES", "es-US")
