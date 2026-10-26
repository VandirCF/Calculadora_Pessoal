package com.darkempire78.opencalculator.finance

import org.junit.Assert.assertEquals
import org.junit.Test

class BigMathTest {
    // Constants below are well known mathematical constants (30+ digits), not engine output.
    @Test fun expOne() = assertClose("2.7182818284590452353602874713527", BigMath.exp(bd("1")), "1E-30")
    @Test fun expZero() = assertEquals(0, bd("1").compareTo(BigMath.exp(bd("0"))))
    @Test fun expNegative() = assertClose("0.36787944117144232159552377016146", BigMath.exp(bd("-1")), "1E-30")
    @Test fun lnTwo() = assertClose("0.69314718055994530941723212145818", BigMath.ln(bd("2")), "1E-30")
    @Test fun lnTen() = assertClose("2.3025850929940456840179914546844", BigMath.ln(bd("10")), "1E-30")
    @Test fun lnOfSmallNumber() = assertClose("-6.9077552789821370520539743640531", BigMath.ln(bd("0.001")), "1E-29")
    @Test fun lnOfHugeNumber() = assertClose("230.25850929940456840179914546844", BigMath.ln(bd("1E100")), "1E-27")
    @Test fun lnRejectsNonPositive() {
        try { BigMath.ln(bd("0")); throw AssertionError("expected failure") } catch (e: FinancialException) { assertEquals(FinancialError.OUT_OF_RANGE, e.error) }
    }
    @Test fun squareRootOfTwoViaPower() = assertClose("1.4142135623730950488016887242097", BigMath.pow(bd("2"), bd("0.5")), "1E-30")
    @Test fun integerPowerIsExact() = assertEquals(0, bd("1024").compareTo(BigMath.pow(bd("2"), bd("10"))))
    @Test fun negativeIntegerPower() = assertClose("0.0009765625", BigMath.pow(bd("2"), bd("-10")), "1E-30")
    @Test fun zeroExponentIsOne() = assertEquals(0, bd("1").compareTo(BigMath.pow(bd("0"), bd("0"))))
    @Test fun expLnRoundTrip() = assertClose("123.456789", BigMath.exp(BigMath.ln(bd("123.456789"))), "1E-28")
    @Test fun powerOfNegativeBaseWithFractionIsRejected() {
        try { BigMath.pow(bd("-2"), bd("0.5")); throw AssertionError("expected failure") } catch (e: FinancialException) { assertEquals(FinancialError.OUT_OF_RANGE, e.error) }
    }
    @Test fun zeroToNegativePowerIsDivisionByZero() {
        try { BigMath.pow(bd("0"), bd("-1")); throw AssertionError("expected failure") } catch (e: FinancialException) { assertEquals(FinancialError.DIVISION_BY_ZERO, e.error) }
    }
}
