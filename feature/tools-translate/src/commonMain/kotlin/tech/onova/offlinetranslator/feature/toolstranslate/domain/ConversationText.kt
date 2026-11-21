package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode

/** The conversation as Copy and Share send it: the text the screen shows (spec 012 FR-027, ui-rules A6). */
internal object ConversationText {
    /** Each turn as "Language: heard", then "Other language: translation" once there is one. */
    fun format(
        turns: List<Turn>,
        nameOf: (LanguageCode) -> String,
    ): String =
        turns.joinToString("\n") { turn ->
            buildString {
                append(nameOf(turn.from)).append(": ").append(turn.heard)
                turn.translation?.let { append('\n').append(nameOf(turn.to)).append(": ").append(it) }
            }
        }
}
