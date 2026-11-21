package tech.onova.offlinetranslator.feature.toolstranslate.fakes

import tech.onova.offlinetranslator.core.domain.ports.LanguageNames

/** Names a test can read: "Name(ja)" and "Own(ja)" (spec 080). */
class FakeLanguageNames : LanguageNames {
    override fun nameOf(
        tag: String,
        appLanguage: String,
    ): String = "Name($tag)"

    override fun ownName(tag: String): String = "Own($tag)"
}
