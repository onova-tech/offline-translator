package tech.onova.offlinetranslator.core.domain.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DecimalFormatterTest {
    private val english = NumberSymbols.ENGLISH
    private val portuguese = NumberSymbols(decimal = ',', grouping = '.')

    private fun format(
        plain: String,
        digits: Int = 12,
        symbols: NumberSymbols = english,
    ) = DecimalFormatter.format(plain, digits, symbols)

    @Test
    fun removesTrailingZerosAndKeepsExactDecimals() {
        assertEquals("0.3", format("0.3000000000000000000000000000000000"))
        assertEquals("24", format("24"))
        assertEquals("24", format("24.000"))
    }

    @Test
    fun roundsToTwelveSignificantDigits() {
        assertEquals("0.333333333333", format("0.3333333333333333333333333333333333"))
        assertEquals("2", format("1.999999999999999999999999999999998"))
        assertEquals("6.28318530718", format("6.283185307179586476925286766559"))
    }

    @Test
    fun roundsHalfAwayFromZero() {
        assertEquals("0.123456789013", format("0.1234567890125"))
        assertEquals("−3", format("-2.5", digits = 1))
        assertEquals("2", format("2.4", digits = 1))
    }

    @Test
    fun groupsTheIntegerPartWithTheLanguageSymbols() {
        assertEquals("1,234,567.891", format("1234567.891"))
        assertEquals("1.234.567,891", format("1234567.891", symbols = portuguese))
        assertEquals("−1.234,5", format("-1234.5", symbols = portuguese))
        assertEquals("999,999,999,999", format("999999999999"))
    }

    @Test
    fun usesTheMinusSignAndHidesNegativeZero() {
        assertEquals("−4", format("-4"))
        assertEquals("0", format("-0.000"))
        assertEquals("0", format("0"))
    }

    @Test
    fun switchesToScientificNotationOutsideThePlainRange() {
        assertEquals("1E+12", format("1000000000000"))
        assertEquals("1E+12", format("999999999999.5"))
        assertEquals("1.2345E+20", format("123450000000000000000"))
        assertEquals("1,2345E+20", format("123450000000000000000", symbols = portuguese))
        assertEquals("0.000001", format("0.000001"))
        assertEquals("0.00000123456", format("0.00000123456"))
        assertEquals("1E−7", format("0.0000001"))
        assertEquals("−2.5E−9", format("-0.0000000025"))
    }

    @Test
    fun formatsVeryLargeExactValues() {
        val factorial170 = "7257415615307998967" + "0".repeat(288)
        assertEquals("7.25741561531E+306", format(factorial170))
    }

    @Test
    fun honoursOtherPrecisions() {
        assertEquals("176.6666667", format("176.66666666666666666666", digits = 10))
        assertEquals("8.04672", format("8.04672", digits = 10))
    }

    @Test
    fun rejectsInvalidInput() {
        assertFailsWith<IllegalArgumentException> { format("1e5") }
        assertFailsWith<IllegalArgumentException> { format("12", digits = 0) }
    }

    @Test
    fun roundsPlainNumbersCarryingIntoANewDigit() {
        assertEquals("1", DecimalFormatter.round("0.99999", 3))
        assertEquals("1000", DecimalFormatter.round("999.96", 4))
        assertEquals("-10", DecimalFormatter.round("-9.996", 3))
        assertEquals("0.00123", DecimalFormatter.round("0.0012345", 3))
        assertEquals("120000", DecimalFormatter.round("123456", 2))
        assertEquals("0.333", DecimalFormatter.round("0.33333", 3))
        assertEquals("0", DecimalFormatter.round("-0.000", 3))
        assertEquals("42", DecimalFormatter.round("42", 34))
    }

    @Test
    fun groupsIntegers() {
        assertEquals("1", DecimalFormatter.groupInteger("1", ','))
        assertEquals("12.345", DecimalFormatter.groupInteger("12345", '.'))
    }
}
