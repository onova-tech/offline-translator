package tech.onova.offlinetranslator.core.domain.format

/** Decimal and digit-grouping separators of the app language (spec 004 research.md R12). */
public data class NumberSymbols(
    val decimal: Char,
    val grouping: Char,
) {
    public companion object {
        public val ENGLISH: NumberSymbols = NumberSymbols(decimal = '.', grouping = ',')
    }
}
