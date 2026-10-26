package com.darkempire78.opencalculator

import com.darkempire78.opencalculator.calculator.Calculator
import com.darkempire78.opencalculator.calculator.parser.Expression
import com.darkempire78.opencalculator.calculator.parser.NumberFormatter
import com.darkempire78.opencalculator.finance.RpnFormatter
import com.darkempire78.opencalculator.finance.rpn.RpnEngine
import com.darkempire78.opencalculator.financial.FinancialStateStore
import com.darkempire78.opencalculator.util.DecimalSeparatorSetting
import com.darkempire78.opencalculator.util.GroupingSetting
import com.darkempire78.opencalculator.util.PastedNumber
import com.darkempire78.opencalculator.util.SeparatorSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

/**
 * Brazilian format on screen (decimal "," and thousands "."), plain format inside.
 *
 * What the user types and reads is localized; what the engines compute and store never is:
 * BigDecimal values, expressions with "." as the only decimal mark, JSON with "." numbers.
 */
class FormatoBrasileiroTest {

    private val decimal = ","
    private val grouping = "."
    private val originalLocale: Locale = Locale.getDefault()

    @Before
    fun useBrazilianLocale() = Locale.setDefault(Locale("pt", "BR"))

    @After
    fun restoreLocale() = Locale.setDefault(originalLocale)

    private fun evaluate(typed: String): BigDecimal {
        val clean = Expression().getCleanExpression(typed, decimal, grouping)
        return Calculator(10).evaluateSafe(clean, true).let {
            assertTrue("$typed -> $it", it is com.darkempire78.opencalculator.calculator.CalcResult.Value)
            (it as com.darkempire78.opencalculator.calculator.CalcResult.Value).value
        }
    }

    // ---- input: what the user types ----------------------------------------------------------------
    @Test fun decimalCommaIsUnderstood() = assertEquals(0, BigDecimal("4").compareTo(evaluate("1,5+2,5")))
    @Test fun thousandsDotIsIgnored() = assertEquals(0, BigDecimal("1500").compareTo(evaluate("1.000+500")))
    @Test fun thousandsAndDecimalTogether() = assertEquals(0, BigDecimal("1234567.89").compareTo(evaluate("1.234.567,89")))
    @Test fun percentWithDecimalComma() = assertEquals(0, BigDecimal("21").compareTo(evaluate("200×10,5%")))
    @Test fun percentAlone() = assertEquals(0, BigDecimal("0.105").compareTo(evaluate("10,5%")))
    @Test fun negativeWithSeparators() = assertEquals(0, BigDecimal("-99000").compareTo(evaluate("-100.000+1.000")))

    // ---- output: what the user reads -----------------------------------------------------------------
    @Test fun expressionIsGroupedForDisplay() =
        assertEquals("1.234.567,89", NumberFormatter.format("1234567,89", decimal, grouping))

    @Test fun expressionWithOperatorsIsGroupedNumberByNumber() =
        assertEquals("1.000+500", NumberFormatter.format("1000+500", decimal, grouping))

    @Test fun financialValueUsesBrazilianFormat() {
        assertEquals("-100.000,00", RpnFormatter.format(BigDecimal("-100000"), 2, decimal, grouping))
        assertEquals("88,85", RpnFormatter.format(BigDecimal("88.848788678341707"), 2, decimal, grouping))
        assertEquals("1.234.567,89", RpnFormatter.format(BigDecimal("1234567.891"), 2, decimal, grouping))
    }

    @Test fun typedFinancialNumberKeepsTheTrailingComma() =
        assertEquals("1.234,", RpnFormatter.formatEntry("1234.", decimal, grouping))

    // ---- paste: valid Brazilian numbers become display text, anything else is refused -----------------------
    @Test fun pasteOfPvWithSeparators() = assertEquals("-100000,00", PastedNumber.parse("-100.000,00", decimal, grouping))
    @Test fun pasteOfDecimalPercentValue() = assertEquals("10,5", PastedNumber.parse("10,5", decimal, grouping))
    @Test fun pasteOfAmericanFormatIsNotMisread() {
        // "1,234.5" in Brazilian reading is not a number (two decimal marks): refused, never silently wrong
        assertEquals(null, PastedNumber.parse("1,234.5", decimal, grouping))
    }

    // ---- default separators ----------------------------------------------------------------------------
    @Test
    fun brazilianLocaleResolvesToCommaAndDot() {
        val symbols = java.text.DecimalFormatSymbols.getInstance(Locale("pt", "BR"))
        val (dec, grp) = SeparatorSettings.resolve(
            DecimalSeparatorSetting.AUTO, GroupingSetting.AUTO, symbols.decimalSeparator.toString(), symbols.groupingSeparator.toString()
        )
        assertEquals("," to ".", dec to grp)
    }

    // ---- internal representation never uses the localized text ------------------------------------------------
    @Test
    fun engineTypedWithBrazilianValuesStoresPlainDecimals() {
        val engine = RpnEngine()
        engine.keys("100000 CHS PV 12 n 1 i 0 FV PMT")
        assertEquals(0, BigDecimal("-100000").compareTo(engine.tvm.pv))
        assertEquals("-100000", engine.tvm.pv!!.toPlainString())
        assertFalse("internal values must not contain a decimal comma", engine.x.toPlainString().contains(","))
    }

    @Test
    fun persistedJsonUsesDotsEvenWithABrazilianLocale() {
        val engine = RpnEngine().also { it.keys("1000 CHS PV 12 n 1 i 0 FV PMT") }
        val json = FinancialStateStore.encode(engine)
        assertTrue(json, json.contains("88.8487886783417"))
        assertFalse(json.contains("88,848"))
    }

    @Test
    fun calculatorResultsUsePlainDecimals() {
        val result = evaluate("1,5+2,25")
        assertEquals("3.75", result.toPlainString())
    }
}
