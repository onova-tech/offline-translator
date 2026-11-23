package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * The Conversation's Copy (spec 012): the text goes to the clipboard, and [effects] says "copied" when the system
 * shows no confirmation of its own (before Android 13).
 */
internal class CopyOutput(
    private val outputs: TranslatorOutputs,
) {
    private val channel = MutableSharedFlow<TranslatorEffect>(extraBufferCapacity = BUFFER)
    val effects: SharedFlow<TranslatorEffect> = channel.asSharedFlow()

    /** Copies [text], the conversation as the screen shows it (ConversationText); [label] names the clip. */
    fun copy(
        label: String,
        text: String,
    ) {
        if (!outputs.clipboard.copy(label, text)) channel.tryEmit(TranslatorEffect.Copied)
    }

    private companion object {
        const val BUFFER = 8
    }
}
