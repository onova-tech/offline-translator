package tech.onova.offlinetranslator.core.domain.format

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Display text for a decimal number with a fixed count of significant digits (spec 004
 * contracts/calculator-contract.md "Display format"). Works on plain decimal strings, so it is
 * independent of the arithmetic library: plain notation for 1E−6 ≤ |x| < 1E+12, scientific
 * otherwise, rounding half away from zero, trailing zeros removed, grouping in the integer part.
 */
public object DecimalFormatter {
    /** The minus sign shown to users; screen readers read it as "minus". */
    public const val MINUS: Char = '−'

    /**
     * [value] with exactly [decimals] decimals and grouped thousands: 1,234.5 or 1.234,5. A text field's value is not
     * [grouped], so it reads back as typed.
     */
    public fun fixed(
        value: Double,
        decimals: Int,
        symbols: NumberSymbols,
        grouped: Boolean = true,
    ): String {
        var scale = 1L
        repeat(decimals) { scale *= TEN }
        val scaled = (abs(value) * scale).roundToLong()
        val digits = (scaled / scale).toString()
        val whole = if (grouped) groupInteger(digits, symbols.grouping) else digits
        val sign = if (value < 0 && scaled != 0L) MINUS.toString() else ""
        if (decimals == 0) return "$sign$whole"
        return "$sign$whole${symbols.decimal}${(scaled % scale).toString().padStart(decimals, '0')}"
    }

    private const val GROUP_SIZE = 3
    private const val SMALLEST_PLAIN_EXPONENT = -6
    private const val LARGEST_PLAIN_EXPONENT = 11

    /** Formats [plain], a decimal string such as `-1234.5678` with no exponent. */
    public fun format(
        plain: String,
        significantDigits: Int,
        symbols: NumberSymbols,
    ): String {
        require(significantDigits > 0) { "significantDigits must be positive" }
        val number = SignificantDigits.parse(plain)?.roundedTo(significantDigits) ?: return "0"
        val sign = if (number.negative) MINUS.toString() else ""
        val body =
            if (number.exponent in SMALLEST_PLAIN_EXPONENT..LARGEST_PLAIN_EXPONENT) {
                plainText(number, symbols)
            } else {
                scientificText(number, symbols)
            }
        return sign + body
    }

    /**
     * Rounds [plain] to [significantDigits], half away from zero, and returns a plain decimal
     * string again, e.g. `0.99999` with 3 digits → `1`. Arithmetic code uses it instead of
     * library rounding, which can lose a power of ten when the rounding carries.
     */
    public fun round(
        plain: String,
        significantDigits: Int,
    ): String {
        require(significantDigits > 0) { "significantDigits must be positive" }
        val number = SignificantDigits.parse(plain)?.roundedTo(significantDigits) ?: return "0"
        val sign = if (number.negative) "-" else ""
        val integerLength = number.exponent + 1
        val body =
            if (integerLength > 0) {
                val padded = number.digits.padEnd(integerLength, '0')
                val fraction = padded.drop(integerLength)
                if (fraction.isEmpty()) padded else padded.take(integerLength) + "." + fraction
            } else {
                "0." + "0".repeat(-integerLength) + number.digits
            }
        return sign + body
    }

    /** Groups the digits of a non-negative integer, e.g. `1234567` → `1,234,567`. */
    public fun groupInteger(
        digits: String,
        grouping: Char,
    ): String =
        digits
            .reversed()
            .chunked(GROUP_SIZE)
            .joinToString(grouping.toString())
            .reversed()

    private fun plainText(
        number: SignificantDigits,
        symbols: NumberSymbols,
    ): String {
        val digits = number.digits
        val integerLength = number.exponent + 1
        val (integerPart, fraction) =
            if (integerLength > 0) {
                val padded = digits.padEnd(integerLength, '0')
                padded.take(integerLength) to padded.drop(integerLength)
            } else {
                "0" to "0".repeat(-integerLength) + digits
            }
        val grouped = groupInteger(integerPart, symbols.grouping)
        return if (fraction.isEmpty()) grouped else grouped + symbols.decimal + fraction
    }

    private fun scientificText(
        number: SignificantDigits,
        symbols: NumberSymbols,
    ): String {
        val digits = number.digits
        val mantissa = if (digits.length == 1) digits else digits.first() + symbols.decimal.toString() + digits.drop(1)
        val exponentSign = if (number.exponent < 0) MINUS else '+'
        return "${mantissa}E$exponentSign${kotlin.math.abs(number.exponent)}"
    }
}

/**
 * A non-zero number as its significant digits (first digit non-zero, no trailing zeros) and the
 * power of ten of the first digit: 123.45 is `12345` with exponent 2; 0.007 is `7` with exponent −3.
 */
internal data class SignificantDigits(
    val negative: Boolean,
    val digits: String,
    val exponent: Int,
) {
    fun roundedTo(count: Int): SignificantDigits {
        if (digits.length <= count) return this
        val kept = digits.take(count)
        if (digits[count] < ROUND_UP_FROM) return copy(digits = kept.trimEnd('0'))
        val incremented = incrementDecimal(kept)
        return if (incremented.length > kept.length) {
            copy(digits = incremented.take(count).trimEnd('0'), exponent = exponent + 1)
        } else {
            copy(digits = incremented.trimEnd('0'))
        }
    }

    companion object {
        private const val ROUND_UP_FROM = '5'

        /** Parses `-12.50`, `0.0007` or `42`; returns null for zero. */
        fun parse(plain: String): SignificantDigits? {
            val trimmed = plain.trim()
            val negative = trimmed.startsWith('-')
            val unsigned = trimmed.removePrefix("-").removePrefix("+")
            val integerPart = unsigned.substringBefore('.')
            val fraction = unsigned.substringAfter('.', missingDelimiterValue = "")
            require((integerPart + fraction).all { it.isDigit() }) { "Not a plain decimal: '$plain'" }
            val all = integerPart + fraction
            val first = all.indexOfFirst { it != '0' }
            if (first < 0) return null
            return SignificantDigits(
                negative = negative,
                digits = all.substring(first).trimEnd('0'),
                exponent = integerPart.length - 1 - first,
            )
        }

        /** Adds one to a string of digits, e.g. `199` → `200`, `999` → `1000`. */
        private fun incrementDecimal(digits: String): String {
            val chars = digits.toCharArray()
            var index = chars.lastIndex
            while (index >= 0) {
                if (chars[index] == '9') {
                    chars[index] = '0'
                    index--
                } else {
                    chars[index] = chars[index] + 1
                    return chars.concatToString()
                }
            }
            return "1" + chars.concatToString()
        }
    }
}

private const val TEN = 10L
