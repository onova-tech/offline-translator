package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.Route

/**
 * Which models a translation needs (spec 012 FR-004): every pack holds one language with English both ways, so a pair
 * without English goes through English with two packs.
 */
internal object Routes {
    private const val PACK_PREFIX = "translate_"

    fun between(
        from: LanguageCode,
        to: LanguageCode,
    ): Route =
        when {
            from == to -> Route.Same
            from == LanguageCode.ENGLISH -> Route.Direct(from, to, packOf(to))
            to == LanguageCode.ENGLISH -> Route.Direct(from, to, packOf(from))
            else -> Route.ThroughEnglish(from, to, first = packOf(from), second = packOf(to))
        }

    /** The packs [route] needs, in the order it uses them. */
    fun packsFor(route: Route): List<PackId> =
        when (route) {
            Route.Same -> emptyList()
            is Route.Direct -> listOf(route.pack)
            is Route.ThroughEnglish -> listOf(route.first, route.second)
        }

    fun packOf(language: LanguageCode): PackId = PackId(PACK_PREFIX + language.value)

    fun languageOf(pack: PackId): LanguageCode? =
        pack.value.takeIf { it.startsWith(PACK_PREFIX) }?.let { LanguageCode(it.removePrefix(PACK_PREFIX)) }
}
