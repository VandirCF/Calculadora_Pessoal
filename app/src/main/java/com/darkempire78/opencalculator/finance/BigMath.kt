package com.darkempire78.opencalculator.finance

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Numeric policy of the financial engine (single place, documented in docs/FINANCIAL_FUNCTIONS.md).
 *
 * - Results use [MC]: 34 significant digits, HALF_EVEN (IEEE 754 decimal128).
 * - Transcendental functions (exp, ln, non-integer powers) are computed in BigDecimal with
 *   [WORK_MC] (50 digits) and rounded to [MC]. No Double is involved in the engine, except for
 *   the initial guess of ln, which is then refined by Newton iteration to full precision.
 * - Display rounding is a presentation concern and is applied by the UI.
 */
val MC: MathContext = MathContext(34, RoundingMode.HALF_EVEN)
val WORK_MC: MathContext = MathContext(50, RoundingMode.HALF_EVEN)

object BigMath {
    private val TWO = BigDecimal(2)
    private val HALF = BigDecimal("0.5")
    private val EXP_EPSILON = BigDecimal.ONE.movePointLeft(55)

    fun exp(x: BigDecimal): BigDecimal {
        if (x.signum() == 0) return BigDecimal.ONE
        if (x.abs() > BigDecimal(10000)) fail(FinancialError.OVERFLOW, "exp argument out of range")
        var reduced = x
        var halvings = 0
        while (reduced.abs() > HALF) {
            reduced = reduced.divide(TWO, WORK_MC)
            halvings++
        }
        var term = BigDecimal.ONE
        var sum = BigDecimal.ONE
        var n = 1
        while (n < 500) {
            term = term.multiply(reduced, WORK_MC).divide(BigDecimal(n), WORK_MC)
            sum = sum.add(term, WORK_MC)
            if (term.abs() < EXP_EPSILON) break
            n++
        }
        repeat(halvings) { sum = sum.multiply(sum, WORK_MC) }
        return sum.round(MC)
    }

    /** Natural logarithm of a strictly positive number (Newton/Halley iteration on exp). */
    fun ln(x: BigDecimal): BigDecimal {
        if (x.signum() <= 0) fail(FinancialError.OUT_OF_RANGE, "ln of a non-positive number")
        if (x.compareTo(BigDecimal.ONE) == 0) return BigDecimal.ZERO
        // Scale to [0.1, 1) * 10^k so that the seed is always finite
        val exponent = x.precision() - x.scale()
        val mantissa = x.movePointLeft(exponent)
        val seed = Math.log(mantissa.toDouble()) + exponent * Math.log(10.0)
        var y = BigDecimal(seed, WORK_MC)
        repeat(8) {
            val ey = exp(y).round(WORK_MC)
            y = y.add(TWO.multiply(x.subtract(ey, WORK_MC), WORK_MC).divide(x.add(ey, WORK_MC), WORK_MC), WORK_MC)
        }
        return y.round(MC)
    }

    /** base^exponent. Integer exponents are exact; others require base > 0. */
    fun pow(base: BigDecimal, exponent: BigDecimal): BigDecimal {
        if (exponent.signum() == 0) return BigDecimal.ONE
        val isInteger = exponent.stripTrailingZeros().scale() <= 0
        if (isInteger && exponent.abs() <= BigDecimal(100000)) {
            val e = exponent.toBigInteger().toInt()
            if (e >= 0) return base.pow(e, WORK_MC).round(MC)
            if (base.signum() == 0) fail(FinancialError.DIVISION_BY_ZERO, "zero to a negative power")
            return BigDecimal.ONE.divide(base.pow(-e, WORK_MC), WORK_MC).round(MC)
        }
        if (base.signum() <= 0) fail(FinancialError.OUT_OF_RANGE, "non-integer power of a non-positive number")
        return exp(exponent.multiply(ln(base), WORK_MC))
    }

    fun sqrt(x: BigDecimal): BigDecimal {
        if (x.signum() < 0) fail(FinancialError.OUT_OF_RANGE, "square root of a negative number")
        if (x.signum() == 0) return BigDecimal.ZERO
        // BigDecimal.sqrt needs API 33 and the app supports API 21: Newton iteration instead
        val seed = Math.sqrt(x.toDouble())
        var guess = if (seed.isFinite() && seed > 0.0) BigDecimal(seed, WORK_MC) else BigDecimal.ONE
        val tolerance = BigDecimal.ONE.movePointLeft(45)
        for (iteration in 0 until 500) {
            val next = guess.add(x.divide(guess, WORK_MC), WORK_MC).divide(TWO, WORK_MC)
            val converged = next.subtract(guess).abs() <= next.multiply(tolerance, WORK_MC)
            guess = next
            if (converged) break
        }
        return guess.round(MC)
    }
}
