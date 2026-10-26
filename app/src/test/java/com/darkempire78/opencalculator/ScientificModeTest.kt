package com.darkempire78.opencalculator

import com.darkempire78.opencalculator.calculator.CalcResult
import com.darkempire78.opencalculator.calculator.CalcError
import com.darkempire78.opencalculator.calculator.Calculator
import com.darkempire78.opencalculator.calculator.parser.Expression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scientific functions (Phase 3).
 *
 * Tolerance policy: trigonometric, logarithmic, exponential and non-integer power results go
 * through IEEE double (about 15-16 significant digits), so they are compared with an absolute
 * tolerance of [EPS] = 1e-9. Exact operations (integer powers, factorial of integers, sqrt of
 * perfect squares) are compared with tolerance 0 through BigDecimal.compareTo.
 */
class ScientificModeTest {

    private val expression = Expression()
    private val calculator = Calculator(10)

    private fun run(input: String, degrees: Boolean): CalcResult =
        calculator.evaluateSafe(expression.getCleanExpression(input, ".", ","), degrees)

    private fun value(input: String, degrees: Boolean = true): Double {
        val r = run(input, degrees)
        assertTrue("$input -> $r", r is CalcResult.Value)
        return (r as CalcResult.Value).value.toDouble()
    }

    private fun assertNear(expected: Double, input: String, degrees: Boolean = true, eps: Double = EPS) =
        assertEquals(input, expected, value(input, degrees), eps)

    private fun assertError(expected: CalcError, input: String, degrees: Boolean = true) {
        val r = run(input, degrees)
        assertEquals("$input -> $r", CalcResult.Error(expected), r)
    }

    // ---- trigonometry ----------------------------------------------------------------------
    @Test fun sinZero() = assertNear(0.0, "sin(0)")
    @Test fun cosZero() = assertNear(1.0, "cos(0)")
    @Test fun tanZero() = assertNear(0.0, "tan(0)")
    @Test fun sin90Degrees() = assertNear(1.0, "sin(90)", degrees = true)
    @Test fun sin30Degrees() = assertNear(0.5, "sin(30)", degrees = true)
    @Test fun cos60Degrees() = assertNear(0.5, "cos(60)", degrees = true)
    @Test fun sin180DegreesIsZeroNotRoundingNoise() = assertEquals(0.0, value("sin(180)"), 0.0)
    @Test fun cos90DegreesIsZeroNotRoundingNoise() = assertEquals(0.0, value("cos(90)"), 0.0)
    @Test fun sinNegative180DegreesIsZeroNotRoundingNoise() = assertEquals(0.0, value("sin(-180)"), 0.0)
    @Test fun tan45Degrees() = assertNear(1.0, "tan(45)")
    @Test fun sinHalfPiRadians() = assertNear(1.0, "sin(π÷2)", degrees = false)
    @Test fun cosPiRadians() = assertNear(-1.0, "cos(π)", degrees = false)
    @Test fun sameAngleInBothModes() {
        // 180 degrees == pi radians
        assertEquals(value("cos(180)", true), value("cos(π)", false), EPS)
    }
    @Test fun inverseSineDegrees() = assertNear(30.0, "sin⁻¹(0.5)", degrees = true)
    @Test fun inverseCosineRadians() = assertNear(Math.PI / 3, "cos⁻¹(0.5)", degrees = false)
    @Test fun inverseTangentDegrees() = assertNear(45.0, "tan⁻¹(1)", degrees = true)
    @Test fun inverseSineOutOfDomain() = assertError(CalcError.DOMAIN, "sin⁻¹(2)")
    @Test fun inverseCosineOutOfDomain() = assertError(CalcError.DOMAIN, "cos⁻¹(-1.5)")

    // ---- undefined tangent -------------------------------------------------------------------
    @Test fun tan90DegreesIsUndefined() = assertError(CalcError.DOMAIN, "tan(90)", degrees = true)
    @Test fun tan270DegreesIsUndefined() = assertError(CalcError.DOMAIN, "tan(270)", degrees = true)
    @Test fun tanMinus90DegreesIsUndefined() = assertError(CalcError.DOMAIN, "tan(-90)", degrees = true)
    @Test fun tanHalfPiRadiansIsUndefined() = assertError(CalcError.DOMAIN, "tan(π÷2)", degrees = false)
    @Test fun tan89DegreesIsDefined() = assertNear(57.28996163075943, "tan(89)", eps = 1e-6)

    // ---- logarithms and exponential ---------------------------------------------------------
    @Test fun log10Of100() = assertNear(2.0, "log(100)")
    @Test fun log10Of1000() = assertNear(3.0, "log(1000)")
    @Test fun lnOfE() = assertNear(1.0, "ln(e)")
    @Test fun lnOfOne() = assertNear(0.0, "ln(1)")
    @Test fun log2Of8() = assertNear(3.0, "log₂(8)")
    @Test fun expOfZero() = assertNear(1.0, "exp(0)")
    @Test fun expOfOne() = assertNear(Math.E, "exp(1)")
    @Test fun lnOfZeroIsDomainError() = assertError(CalcError.DOMAIN, "ln(0)")
    @Test fun logOfNegativeIsDomainError() = assertError(CalcError.DOMAIN, "log(-1)")

    // ---- roots, powers, factorial -----------------------------------------------------------
    @Test fun squareRootOf9IsExact() = assertEquals(0, java.math.BigDecimal(3).compareTo((run("√9", true) as CalcResult.Value).value))
    @Test fun squareRootOf2() = assertNear(1.4142135623730951, "√2", eps = 1e-12)
    @Test fun squareRootOfNegativeNeedsRealNumber() = assertError(CalcError.REQUIRE_REAL, "√(-4)")
    @Test fun powerTwoCubedIsExact() = assertEquals(0, java.math.BigDecimal(8).compareTo((run("2^3", true) as CalcResult.Value).value))
    @Test fun powerNegativeExponent() = assertNear(0.125, "2^-3")
    @Test fun powerFractionalExponent() = assertNear(Math.pow(2.0, 3.5), "2^3.5", eps = 1e-8)
    @Test fun powerNegativeBaseFractionalExponentNeedsRealNumber() = assertError(CalcError.REQUIRE_REAL, "(-8)^0.5")
    @Test fun factorialOf5IsExact() = assertEquals(0, java.math.BigDecimal(120).compareTo((run("5!", true) as CalcResult.Value).value))
    @Test fun factorialOfZeroIsOne() = assertNear(1.0, "0!")
    @Test fun factorialOfNegativeIsDomainError() = assertError(CalcError.DOMAIN, "(-3)!")
    @Test fun factorialOfHugeNumberIsInfinity() = assertError(CalcError.INFINITY, "5000!")
    @Test fun factorialOfHalfUsesGamma() = assertNear(Math.sqrt(Math.PI) / 2, "0.5!", eps = 1e-9)

    // ---- nth root, reciprocal, absolute value -----------------------------------------------
    @Test fun cubeRootOf27() = assertNear(3.0, "3ⁿ√27")
    @Test fun fourthRootOf16() = assertNear(2.0, "4ⁿ√16")
    @Test fun oddRootOfNegativeIsReal() = assertNear(-3.0, "3ⁿ√(-27)")
    @Test fun evenRootOfNegativeNeedsRealNumber() = assertError(CalcError.REQUIRE_REAL, "2ⁿ√(-4)")
    @Test fun zeroRootIndexIsDomainError() = assertError(CalcError.DOMAIN, "0ⁿ√8")
    @Test fun rootBindsTighterThanMultiplication() = assertNear(6.0, "2×3ⁿ√27")
    @Test fun rootOfExpression() = assertNear(3.0, "2ⁿ√(4+5)")
    @Test fun reciprocalViaPowerMinusOne() = assertNear(0.25, "4^(-1)")
    @Test fun absoluteValueOfNegative() = assertNear(5.0, "abs(-5)")
    @Test fun absoluteValueOfPositive() = assertNear(5.0, "abs(5)")
    @Test fun moduloRemainsAvailable() = assertNear(1.0, "7#3")

    // ---- precedence and propagation ---------------------------------------------------------
    @Test fun parenthesesChangePrecedence() = assertNear(98.0, "2×(3+4)^2")
    @Test fun withoutParenthesesPowerFirst() = assertNear(2 * 3 + 16.0, "2×3+4^2")
    @Test fun mixedFunctions() = assertNear(1.0, "sin(30)+cos(60)")
    @Test fun nestedFunctions() = assertNear(2.0, "log(√10000)")
    @Test fun constantsInExpression() = assertNear(2 * Math.PI, "2π", eps = 1e-12)
    @Test fun errorPropagatesThroughAddition() = assertError(CalcError.REQUIRE_REAL, "1+√(-1)")
    @Test fun errorPropagatesThroughMultiplication() = assertError(CalcError.DOMAIN, "5×ln(0)")
    @Test fun errorPropagatesInsideNestedFunction() = assertError(CalcError.DOMAIN, "sin(ln(-1))")
    @Test fun divisionByZeroInsideFunction() = assertError(CalcError.DIVISION_BY_ZERO, "sin(1÷0)")
    @Test fun syntaxErrorIsReported() = assertError(CalcError.SYNTAX, "1.2.3+sin(0)")

    @Test
    fun errorsDoNotLeakIntoTheNextEvaluation() {
        assertError(CalcError.DOMAIN, "ln(0)")
        assertNear(2.0, "1+1")
    }

    @Test
    fun unknownErrorTypesHaveStablePriority() {
        // When several flags are raised, the first in this order is reported:
        // syntax > domain > real-number > division by zero > infinity
        assertError(CalcError.SYNTAX, "ln(0)+1.2.3")
    }

    companion object {
        const val EPS = 1e-9
    }
}
