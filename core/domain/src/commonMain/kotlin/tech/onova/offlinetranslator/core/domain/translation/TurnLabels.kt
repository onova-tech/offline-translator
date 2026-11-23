package tech.onova.offlinetranslator.core.domain.translation

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode

/** What a Conversation half puts before a turn: [me] for its own speaker's, [you] for the other person's. */
public data class TurnLabel(
    val me: String,
    val you: String,
)

/**
 * "Me:" and "You:" in each language of the catalog (spec 082 story 5, FR-014): a half names who spoke in its own
 * language, not the app's, so these words can't be the app's resources (`docs/ui-rules.md`, Exceptions, X1). Written
 * by hand (owner, 2026-11-22), each with its own punctuation. "You" is the polite form a stranger is addressed with;
 * where a bare "you" sounds rude as a label (Japanese, Korean), the word for "the other person". A language the
 * catalog gains needs its pair here: `TurnLabelsTest` fails until it has one.
 */
public object TurnLabels {
    private val ENGLISH = TurnLabel("Me:", "You:")

    private val byCode: Map<String, TurnLabel> =
        mapOf(
            "en" to ENGLISH,
            "af" to TurnLabel("Ek:", "U:"),
            "ar" to TurnLabel("أنا:", "أنت:"),
            "be" to TurnLabel("Я:", "Вы:"),
            "bg" to TurnLabel("Аз:", "Вие:"),
            "bn" to TurnLabel("আমি:", "আপনি:"),
            "ca" to TurnLabel("Jo:", "Vostè:"),
            "cs" to TurnLabel("Já:", "Vy:"),
            "da" to TurnLabel("Jeg:", "Du:"),
            "de" to TurnLabel("Ich:", "Sie:"),
            "el" to TurnLabel("Εγώ:", "Εσείς:"),
            "es" to TurnLabel("Yo:", "Usted:"),
            "et" to TurnLabel("Mina:", "Teie:"),
            "eu" to TurnLabel("Ni:", "Zu:"),
            "fa" to TurnLabel("من:", "شما:"),
            "fi" to TurnLabel("Minä:", "Te:"),
            "fr" to TurnLabel("Moi :", "Vous :"),
            "gl" to TurnLabel("Eu:", "Vostede:"),
            "gu" to TurnLabel("હું:", "તમે:"),
            "he" to TurnLabel("אני:", "את/ה:"),
            "hi" to TurnLabel("मैं:", "आप:"),
            "hu" to TurnLabel("Én:", "Ön:"),
            "id" to TurnLabel("Saya:", "Anda:"),
            "is" to TurnLabel("Ég:", "Þú:"),
            "it" to TurnLabel("Io:", "Lei:"),
            "ja" to TurnLabel("私：", "相手："),
            "kn" to TurnLabel("ನಾನು:", "ನೀವು:"),
            "ko" to TurnLabel("나:", "상대:"),
            "lt" to TurnLabel("Aš:", "Jūs:"),
            "lv" to TurnLabel("Es:", "Jūs:"),
            "ml" to TurnLabel("ഞാൻ:", "നിങ്ങൾ:"),
            "mr" to TurnLabel("मी:", "तुम्ही:"),
            "ms" to TurnLabel("Saya:", "Anda:"),
            "nb" to TurnLabel("Jeg:", "Du:"),
            "nl" to TurnLabel("Ik:", "U:"),
            "pl" to TurnLabel("Ja:", "Pan/Pani:"),
            "pt" to TurnLabel("Eu:", "Você:"),
            "ro" to TurnLabel("Eu:", "Dumneavoastră:"),
            "ru" to TurnLabel("Я:", "Вы:"),
            "sk" to TurnLabel("Ja:", "Vy:"),
            "sl" to TurnLabel("Jaz:", "Vi:"),
            "sq" to TurnLabel("Unë:", "Ju:"),
            "sv" to TurnLabel("Jag:", "Du:"),
            "ta" to TurnLabel("நான்:", "நீங்கள்:"),
            "te" to TurnLabel("నేను:", "మీరు:"),
            "th" to TurnLabel("ฉัน:", "คุณ:"),
            "tl" to TurnLabel("Ako:", "Kayo:"),
            "tr" to TurnLabel("Ben:", "Siz:"),
            "ug" to TurnLabel("مەن:", "سىز:"),
            "uk" to TurnLabel("Я:", "Ви:"),
            "ur" to TurnLabel("میں:", "آپ:"),
            "vi" to TurnLabel("Tôi:", "Bạn:"),
            "zh" to TurnLabel("我：", "您："),
            "zh_hant" to TurnLabel("我：", "您："),
        )

    /** [language]'s words; English for a language the catalog doesn't have. */
    public fun of(language: LanguageCode): TurnLabel = byCode[language.value] ?: ENGLISH
}
