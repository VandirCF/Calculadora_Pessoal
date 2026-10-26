package com.darkempire78.opencalculator.finance

import java.math.BigDecimal

/**
 * Root search for functions of a rate r > -1 (TVM rate and IRR).
 *
 * The domain is scanned on a fixed grid; every sign change brackets one root, refined by
 * bisection. Bisection always converges inside a bracket, so the only failure mode is the
 * iteration limit `maxIterations` (reported as NO_CONVERGENCE, never an infinite loop).
 */
object RootFinder {
    const val DEFAULT_MAX_ITERATIONS = 100

    /** Absolute tolerance on the rate (1E-15 = 1E-13 percentage points). */
    val DEFAULT_TOLERANCE: BigDecimal = BigDecimal("1E-15")

    /** Rates scanned: -99%..0 (step 1%), 0..100% (step 0.5%), 100%..1000% (step 10%), then geometric. */
    val grid: List<BigDecimal> by lazy {
        val points = ArrayList<BigDecimal>()
        for (k in -99..-1) points.add(BigDecimal(k).movePointLeft(2))
        for (k in 0..200) points.add(BigDecimal(k).movePointLeft(3).multiply(BigDecimal(5)))
        for (k in 1..90) points.add(BigDecimal.ONE.add(BigDecimal(k).movePointLeft(1)))
        var r = BigDecimal(10)
        repeat(30) {
            r = r.multiply(BigDecimal("1.25"))
            points.add(r)
        }
        points
    }

    /** Returns every root found in the grid range, or fails with NO_CONVERGENCE. */
    fun findRoots(
        maxIterations: Int = DEFAULT_MAX_ITERATIONS,
        tolerance: BigDecimal = DEFAULT_TOLERANCE,
        f: (BigDecimal) -> BigDecimal
    ): List<BigDecimal> {
        val roots = ArrayList<BigDecimal>()
        var previousRate: BigDecimal? = null
        var previousValue: BigDecimal? = null
        for (rate in grid) {
            val value = try {
                f(rate)
            } catch (e: FinancialException) {
                null
            }
            if (value == null) {
                previousRate = null
                previousValue = null
                continue
            }
            if (value.signum() == 0) {
                roots.add(rate)
            } else if (previousValue != null && previousValue.signum() != 0 && previousValue.signum() != value.signum()) {
                roots.add(bisect(previousRate!!, rate, previousValue, maxIterations, tolerance, f))
            }
            previousRate = rate
            previousValue = value
        }
        return roots
    }

    private fun bisect(
        lowStart: BigDecimal,
        highStart: BigDecimal,
        lowValue: BigDecimal,
        maxIterations: Int,
        tolerance: BigDecimal,
        f: (BigDecimal) -> BigDecimal
    ): BigDecimal {
        var low = lowStart
        var high = highStart
        val lowSign = lowValue.signum()
        var iterations = 0
        while (high.subtract(low).abs() > tolerance) {
            if (iterations++ >= maxIterations) fail(FinancialError.NO_CONVERGENCE, "no convergence in $maxIterations iterations")
            val mid = low.add(high).divide(BigDecimal(2), WORK_MC)
            val v = f(mid)
            if (v.signum() == 0) return mid
            if (v.signum() == lowSign) low = mid else high = mid
        }
        return low.add(high).divide(BigDecimal(2), WORK_MC)
    }
}
