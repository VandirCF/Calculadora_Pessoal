package com.darkempire78.opencalculator

import com.darkempire78.opencalculator.calculator.Calculator
import com.darkempire78.opencalculator.calculator.division_by_0
import com.darkempire78.opencalculator.calculator.domain_error
import com.darkempire78.opencalculator.calculator.is_infinity
import com.darkempire78.opencalculator.calculator.parser.Expression
import com.darkempire78.opencalculator.calculator.parser.NumberFormatter
import com.darkempire78.opencalculator.calculator.parser.NumberingSystem
import com.darkempire78.opencalculator.calculator.require_real_number
import com.darkempire78.opencalculator.calculator.syntax_error
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Behaviour of the basic calculator (Phase 2). Documented in docs/BASIC_MODE.md.
 *
 * Inputs use the on-screen symbols with "." as decimal separator and "," as grouping
 * separator (fixed, so results never depend on the machine locale).
 * Exact comparisons use [BigDecimal.compareTo] (scale-insensitive); no floating point
 * equality is involved in the basic operations.
 */
class BasicModeTest {

    private val expression = Expression()
    private val calculator = Calculator(10)

    @Before
    fun resetErrorFlags() {
        division_by_0 = false
        domain_error = false
        syntax_error = false
        is_infinity = false
        require_real_number = false
    }

    private fun eval(input: String): BigDecimal =
        calculator.evaluate(expression.getCleanExpression(input, ".", ","), true)

    private fun assertValue(expected: String, input: String) {
        val actual = eval(input)
        assertNoError(input)
        assertTrue("$input = $actual, expected $expected", BigDecimal(expected).compareTo(actual) == 0)
    }

    private fun assertNoError(input: String) {
        assertFalse("division_by_0 for $input", division_by_0)
        assertFalse("domain_error for $input", domain_error)
        assertFalse("syntax_error for $input", syntax_error)
        assertFalse("is_infinity for $input", is_infinity)
        assertFalse("require_real_number for $input", require_real_number)
    }

    // ---- the four operations -------------------------------------------------------------

    @Test fun addition() = assertValue("5", "2+3")
    @Test fun subtraction() = assertValue("-1", "2-3")
    @Test fun multiplication() = assertValue("6", "2×3")
    @Test fun division() = assertValue("2.5", "5÷2")

    @Test
    fun divisionWithNonTerminatingExpansionIsRoundedToConfiguredPrecision() {
        // Calculator(10): scale 10, HALF_DOWN
        assertValue("0.3333333333", "1÷3")
        assertValue("0.6666666667", "2÷3")
    }

    // ---- precedence and chaining ------------------------------------------------------------

    @Test fun multiplicationBeforeAddition() = assertValue("14", "2+3×4")
    @Test fun divisionBeforeSubtraction() = assertValue("8", "10-4÷2")
    @Test fun chainedOperationsLeftToRight() = assertValue("1", "10-4-5")
    @Test fun chainedDivisionLeftToRight() = assertValue("2", "20÷5÷2")
    @Test fun parenthesesOverridePrecedence() = assertValue("20", "(2+3)×4")
    @Test fun implicitMultiplicationBeforeParenthesis() = assertValue("20", "4(2+3)")
    @Test fun implicitMultiplicationAfterParenthesis() = assertValue("20", "(2+3)4")
    @Test fun unclosedParenthesisIsAutoClosed() = assertValue("5", "(2+3")

    // ---- negative numbers -------------------------------------------------------------------

    @Test fun leadingNegativeNumber() = assertValue("-2", "-5+3")
    @Test fun negativeOperandAfterMultiplication() = assertValue("-6", "2×-3")
    @Test fun negativeTimesNegative() = assertValue("6", "-2×-3")
    @Test fun negativeInParentheses() = assertValue("-1", "2+(-3)")
    @Test fun doubleNegativeViaSubtraction() = assertValue("5", "2--3")

    // ---- decimals, leading zeros ------------------------------------------------------------

    @Test fun decimalAddition() = assertValue("0.3", "0.1+0.2") // exact: no binary float error
    @Test fun leadingDecimalPoint() = assertValue("0.5", ".5")
    @Test fun leadingZerosAreIgnored() = assertValue("7", "007")
    @Test fun trailingZerosInDecimal() = assertValue("1.5", "1.50")

    @Test
    fun duplicatedDecimalSeparatorIsSyntaxError() {
        eval("1.2.3")
        assertTrue(syntax_error)
    }

    @Test
    fun lonelyDecimalPointIsSyntaxError() {
        eval(".")
        assertTrue(syntax_error)
    }

    // ---- division by zero -------------------------------------------------------------------

    @Test
    fun divisionByZeroIsReported() {
        eval("5÷0")
        assertTrue(division_by_0)
    }

    @Test
    fun divisionByZeroDecimalIsReported() {
        eval("5÷0.0")
        assertTrue(division_by_0)
    }

    @Test
    fun divisionByZeroInsideLongerExpressionIsReported() {
        eval("1+2÷(3-3)")
        assertTrue(division_by_0)
    }

    @Test
    fun zeroDividedByZeroIsReported() {
        eval("0÷0")
        assertTrue(division_by_0)
    }

    @Test
    fun errorFlagsAreIndependentAcrossCalculations() {
        eval("5÷0")
        assertTrue(division_by_0)
        resetErrorFlags()
        assertValue("2", "4÷2")
    }

    // ---- percentage -------------------------------------------------------------------------

    @Test fun percentAlone() = assertValue("0.5", "50%")
    @Test fun percentOfNumberViaMultiplication() = assertValue("20", "200×10%")
    @Test fun addPercentIsRelativeToBase() = assertValue("110", "100+10%")
    @Test fun subtractPercentIsRelativeToBase() = assertValue("90", "100-10%")
    @Test fun dividePercentIsPlainFraction() = assertValue("1000", "100÷10%")
    @Test fun percentOfParenthesisIsRelativeToBase() = assertValue("55", "50+(5+5)%")

    // ---- large and small values -------------------------------------------------------------

    @Test
    fun veryLargeProductIsExact() {
        assertValue("999999999890000000001", "99999999999×9999999999")
    }

    @Test
    fun veryLargeAdditionIsExact() {
        assertValue("100000000000000000000000000000000000000000", "99999999999999999999999999999999999999999+1")
    }

    @Test
    fun verySmallDecimalIsExact() = assertValue("0.00000000000000000001", "0.00000000000000000001")

    @Test
    fun verySmallQuotientIsRoundedToConfiguredScale() {
        // 1÷3000000000000 = 3.33e-13; with scale 10 and HALF_DOWN, it rounds to 0
        assertValue("0", "1÷3000000000000")
    }

    // ---- formatting (display) ---------------------------------------------------------------

    @Test
    fun formatGroupsThousandsWithInternationalSystem() {
        assertEquals("1,234,567.89", NumberFormatter.format("1234567.89", ".", ",", NumberingSystem.INTERNATIONAL))
    }

    @Test
    fun formatKeepsNegativeSign() {
        assertEquals("-1,000", NumberFormatter.format("-1000", ".", ",", NumberingSystem.INTERNATIONAL))
    }

    @Test
    fun formatUsesConfiguredSeparators() {
        assertEquals("1.234,5", NumberFormatter.format("1234,5", ",", ".", NumberingSystem.INTERNATIONAL))
    }

    @Test
    fun formatLeavesOperatorsUntouched() {
        assertEquals("1,000+2,000", NumberFormatter.format("1000+2000", ".", ",", NumberingSystem.INTERNATIONAL))
    }

    @Test
    fun noErrorOnPlainNumber() {
        assertValue("42", "42")
    }
}
