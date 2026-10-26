package com.darkempire78.opencalculator.finance

import org.junit.Assert.assertEquals
import org.junit.Test

class RpnFormatterTest {
    private fun f(v: String, scale: Int = 2, dec: String = ".", grp: String = ",", grouping: Boolean = true) =
        RpnFormatter.format(bd(v), scale, dec, grp, grouping)

    @Test fun groupsThousands() = assertEquals("1,234,567.89", f("1234567.891"))
    @Test fun roundsHalfEven() { assertEquals("0.12", f("0.125")); assertEquals("0.14", f("0.135")) }
    @Test fun negativeNumbers() = assertEquals("-1,000.50", f("-1000.5"))
    @Test fun smallNumbersNeedNoGrouping() = assertEquals("999.00", f("999"))
    @Test fun zeroScale() = assertEquals("1,235", f("1234.6", scale = 0))
    @Test fun brazilianSeparators() = assertEquals("1.234.567,89", f("1234567.89", dec = ",", grp = "."))
    @Test fun groupingCanBeDisabled() = assertEquals("1234567.89", f("1234567.89", grouping = false))
    @Test fun tinyNegativeNumberIsNeverShownAsMinusZero() = assertEquals("-1.000000E-03", f("-0.001"))
    @Test fun tinyNonZeroUsesScientificNotation() = assertEquals("1.000000E-05", f("0.00001"))
    @Test fun hugeNumbersUseScientificNotation() = assertEquals("1.234568E+20", f("123456789012345678901"))
    @Test fun scientificUsesLocaleDecimalSeparator() = assertEquals("1,000000E-05", f("0.00001", dec = ","))
    @Test fun entryKeepsTrailingDecimalPoint() = assertEquals("12.", RpnFormatter.formatEntry("12.", ".", ","))
    @Test fun entryKeepsTrailingZeros() = assertEquals("1,000.50", RpnFormatter.formatEntry("1000.50", ".", ","))
    @Test fun entryNegative() = assertEquals("-1,234", RpnFormatter.formatEntry("-1234", ".", ","))
    @Test fun entryWithLocaleSeparators() = assertEquals("1.234,5", RpnFormatter.formatEntry("1234.5", ",", "."))
}
