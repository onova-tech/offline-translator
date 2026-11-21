package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import org.jetbrains.compose.resources.stringResource
import tech.onova.offlinetranslator.core.designsystem.components.IconAction
import tech.onova.offlinetranslator.core.designsystem.components.Notice
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.feature.toolstranslate.resources.Res
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_listening
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_listening_failed
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_manage_languages
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_microphone_busy
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_microphone_denied
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_no_recognizer
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_no_speech_engine
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_no_voice
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_nothing_heard
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_ok
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speak
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_can_download
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_downloading
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_not_installed
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_unsupported
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_stop_listening

/** The voice's state and what its controls do; [permissionGate] is the microphone's, shown above the text. */
internal class SpeechControls(
    val ui: SpeechUi,
    val onMicrophone: () -> Unit,
    val onReadAloud: () -> Unit,
    /** A message about the language: the Manage languages page, at that language (owner, 2026-11-20). */
    val onFix: () -> Unit,
    val onDismiss: () -> Unit,
    val permissionGate: (@Composable () -> Unit)?,
) {
    /**
     * Below Android 12 the microphone isn't shown (spec edge case); a phone without an on-device recognizer shows it,
     * and a tap says why it can't listen (FR-021).
     */
    val canSpeak: Boolean get() = ui.recognition != RecognitionSupport.SYSTEM_TOO_OLD
}

/** The text field's trailing action: Speak, or Stop listening while it listens (and says so to TalkBack). */
@Composable
internal fun MicrophoneAction(speech: SpeechControls) {
    val listening = speech.ui.listening
    val state = stringResource(Res.string.translator_listening)
    IconAction(
        icon = PocketIcons.Microphone,
        label = stringResource(if (listening) Res.string.translator_stop_listening else Res.string.translator_speak),
        onClick = speech.onMicrophone,
        tinted = listening,
        modifier = if (listening) Modifier.semantics { stateDescription = state } else Modifier,
    )
}

/**
 * Why the phone can't listen or read aloud, in the content: what the phone's own speech can't do, and OK; one about the
 * language (its speech or voice) also has "Manage languages", where it's got (owner, 2026-11-20).
 */
@Composable
internal fun SpeechNotice(
    problem: SpeechProblem,
    source: String,
    target: String,
    speech: SpeechControls,
    modifier: Modifier = Modifier,
) {
    val fix =
        when (problem) {
            SpeechProblem.CAN_DOWNLOAD,
            SpeechProblem.DOWNLOADING,
            SpeechProblem.NOT_INSTALLED,
            SpeechProblem.UNSUPPORTED,
            SpeechProblem.LANGUAGE_MISSING,
            SpeechProblem.NO_VOICE,
            -> stringResource(Res.string.translator_manage_languages)

            else -> null
        }
    Notice(text = problemText(problem, source, target), modifier = modifier) {
        TextButton(onClick = speech.onDismiss) { Text(stringResource(Res.string.translator_ok)) }
        if (fix != null) TextButton(onClick = speech.onFix) { Text(fix) }
    }
}

@Composable
private fun problemText(
    problem: SpeechProblem,
    source: String,
    target: String,
): String =
    when (problem) {
        SpeechProblem.NOTHING_HEARD -> {
            stringResource(Res.string.translator_nothing_heard)
        }

        SpeechProblem.CAN_DOWNLOAD -> {
            stringResource(Res.string.translator_speech_can_download, source)
        }

        SpeechProblem.DOWNLOADING -> {
            stringResource(Res.string.translator_speech_downloading, source)
        }

        SpeechProblem.NOT_INSTALLED -> {
            stringResource(Res.string.translator_speech_not_installed, source)
        }

        SpeechProblem.UNSUPPORTED, SpeechProblem.LANGUAGE_MISSING -> {
            stringResource(Res.string.translator_speech_unsupported, source)
        }

        SpeechProblem.NO_RECOGNIZER -> {
            stringResource(Res.string.translator_no_recognizer)
        }

        SpeechProblem.BUSY -> {
            stringResource(Res.string.translator_microphone_busy)
        }

        SpeechProblem.NO_PERMISSION -> {
            stringResource(Res.string.translator_microphone_denied)
        }

        SpeechProblem.LISTENING_FAILED -> {
            stringResource(Res.string.translator_listening_failed)
        }

        SpeechProblem.NO_VOICE -> {
            stringResource(Res.string.translator_no_voice, target)
        }

        SpeechProblem.NO_ENGINE -> {
            stringResource(Res.string.translator_no_speech_engine)
        }
    }
