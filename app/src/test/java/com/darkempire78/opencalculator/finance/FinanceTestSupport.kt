package com.darkempire78.opencalculator.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.math.BigDecimal

/**
 * Expected values in the finance tests come from an independent computation: closed-form
 * formulas and plain bisection written in Python with the decimal module (60 digits), see
 * docs/FINANCIAL_TEST_CASES.md. They are NOT produced by the code under test.
 */
fun bd(text: String) = BigDecimal(text)

fun assertClose(expected: String, actual: BigDecimal?, tolerance: String = "1E-12", message: String = "") {
    assertTrue("$message expected $expected but result was null", actual != null)
    val diff = bd(expected).subtract(actual).abs()
    assertTrue("$message expected $expected but was $actual (diff $diff, tolerance $tolerance)", diff <= bd(tolerance))
}

fun <T> FinancialResult<T>.success(): T {
    assertTrue("expected success but was $this", this is FinancialResult.Success)
    return (this as FinancialResult.Success).value
}

fun FinancialResult<*>.failure(): FinancialResult.Failure {
    assertTrue("expected failure but was $this", this is FinancialResult.Failure)
    return this as FinancialResult.Failure
}

fun assertFails(expected: FinancialError, result: FinancialResult<*>) =
    assertEquals(expected, result.failure().error)
