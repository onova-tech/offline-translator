package tech.onova.offlinetranslator.core.platform.speech

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.core.domain.ports.SpeechFailure
import tech.onova.offlinetranslator.core.domain.ports.SpeechInput
import kotlin.coroutines.resume
import android.speech.RecognitionSupport as PhoneSupport

/**
 * The phone's own recognizer, on the device only (spec 012 research R1): always `createOnDeviceSpeechRecognizer`,
 * never the plain one, which may send audio to a server. Below Android 12 there is none. Every call to the recognizer
 * runs on the main thread, as Android requires.
 */
public class AndroidSpeechInput(
    context: Context,
) : SpeechInput {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    override suspend fun support(speechTag: String): RecognitionSupport =
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> RecognitionSupport.SYSTEM_TOO_OLD

            !SpeechRecognizer.isOnDeviceRecognitionAvailable(context) -> RecognitionSupport.NO_RECOGNIZER

            // Android 12 and 12L can't tell before trying (research R1).
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> RecognitionSupport.UNKNOWN

            else -> withContext(Dispatchers.Main) { checkSupport(speechTag) }
        }

    override fun requestLanguage(speechTag: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) return false
        main.post {
            val recognizer = create() ?: return@post
            // The phone's own speech service downloads it, with its own notice; the app opens no connection.
            recognizer.triggerModelDownload(intent(speechTag))
            // The request waits for the service to connect, and destroying the recognizer drops it: wait a little.
            main.postDelayed({ recognizer.destroy() }, HANDOFF_MILLIS)
        }
        return true
    }

    override fun canUpdateService(): Boolean = servicePackage() != null

    override fun openServiceUpdate() {
        val service = servicePackage() ?: return
        val pages = listOf(STORE_APP + service, STORE_WEB + service)
        pages.firstOrNull { page ->
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(page)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                true
            } catch (ignored: ActivityNotFoundException) {
                false
            }
        }
    }

    /**
     * The on-device speech service most Android phones have, Android System Intelligence, when the phone has it: the
     * one that downloads languages for the on-device recognizer (the owner's Samsung, 2026-11-20). Android names no
     * other, so another phone gets the message without the button.
     */
    private fun servicePackage(): String? =
        context.packageManager
            .queryIntentServices(Intent(RecognitionService.SERVICE_INTERFACE), 0)
            .map { it.serviceInfo.packageName }
            .firstOrNull { it == SYSTEM_INTELLIGENCE }

    override fun listen(speechTag: String): Flow<SpeechEvent> =
        callbackFlow {
            val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) create() else null
            if (recognizer == null) {
                trySend(SpeechEvent.Failed(SpeechFailure.OTHER))
                close()
                awaitClose()
                return@callbackFlow
            }
            recognizer.setRecognitionListener(
                object : RecognitionListener {
                    override fun onPartialResults(partialResults: Bundle?) {
                        firstText(partialResults)?.let { trySend(SpeechEvent.Partial(it)) }
                    }

                    override fun onResults(results: Bundle?) {
                        trySend(SpeechEvent.Final(firstText(results).orEmpty()))
                        close()
                    }

                    override fun onError(error: Int) {
                        trySend(SpeechEvent.Failed(SpeechMapping.failure(error)))
                        close()
                    }

                    override fun onReadyForSpeech(params: Bundle?) = Unit

                    override fun onBeginningOfSpeech() = Unit

                    override fun onRmsChanged(rmsdB: Float) = Unit

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() = Unit

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?,
                    ) = Unit
                },
            )
            recognizer.startListening(intent(speechTag))
            awaitClose {
                recognizer.cancel()
                recognizer.destroy()
            }
        }.flowOn(Dispatchers.Main)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun checkSupport(speechTag: String): RecognitionSupport {
        val recognizer = create() ?: return RecognitionSupport.NO_RECOGNIZER
        // Android queues the question until its service connects. Destroyed before it answers, the recognizer has no
        // connection when Android gets to the question, and Android's own code crashes the app (the owner's phone,
        // 2026-11-22, when Manage languages asked again). So it is destroyed once it answered, even unheard.
        return suspendCancellableCoroutine { answer ->
            recognizer.checkRecognitionSupport(
                intent(speechTag),
                context.mainExecutor,
                object : RecognitionSupportCallback {
                    override fun onSupportResult(recognitionSupport: PhoneSupport) {
                        recognizer.destroy()
                        val result =
                            SpeechMapping.support(
                                speechTag = speechTag,
                                installed = recognitionSupport.installedOnDeviceLanguages,
                                pending = recognitionSupport.pendingOnDeviceLanguages,
                                supported = recognitionSupport.supportedOnDeviceLanguages,
                            )
                        if (answer.isActive) answer.resume(result)
                    }

                    override fun onError(error: Int) {
                        recognizer.destroy()
                        if (answer.isActive) answer.resume(RecognitionSupport.UNKNOWN)
                    }
                },
            )
        }
    }

    /** The on-device recognizer, or null: some phones throw when no on-device service exists (research R1). */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun create(): SpeechRecognizer? =
        try {
            if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                null
            }
        } catch (ignored: RuntimeException) {
            null
        }

    private fun intent(speechTag: String) =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechTag)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            // A hint the service may ignore; the tool also ends a turn after 2 s with nothing new (FR-023).
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_MILLIS)

    private fun firstText(results: Bundle?): String? =
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }

    private companion object {
        const val SILENCE_MILLIS = 2_000L
        const val HANDOFF_MILLIS = 10_000L
        const val SYSTEM_INTELLIGENCE = "com.google.android.as"
        const val STORE_APP = "market://details?id="
        const val STORE_WEB = "https://play.google.com/store/apps/details?id="
    }
}
