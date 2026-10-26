package com.darkempire78.opencalculator.finance

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/** One cash flow repeated [count] consecutive periods. */
data class CashFlow(@SerializedName("amount") val amount: BigDecimal, @SerializedName("count") val count: Int = 1)

/**
 * Cash flow list: CF0 at time 0 followed by CF1..CFn, each repeated `count` times.
 * Temporal convention: CF0 is not discounted; the first repeated flow happens at the end of
 * period 1, the next at period 2, and so on. The rate is per period, in percent.
 */
data class CashFlows(val cf0: BigDecimal = BigDecimal.ZERO, val flows: List<CashFlow> = emptyList()) {

    /** Amount at each period, index 0 being CF0. */
    fun expanded(): List<BigDecimal> {
        val result = ArrayList<BigDecimal>()
        result.add(cf0)
        for (flow in flows) repeat(flow.count) { result.add(flow.amount) }
        return result
    }

    fun validate() {
        if (flows.isEmpty() && cf0.signum() == 0) fail(FinancialError.EMPTY_CASH_FLOWS, "no cash flows entered")
        if (flows.size > MAX_ENTRIES) fail(FinancialError.OUT_OF_RANGE, "at most $MAX_ENTRIES cash flow entries")
        for (flow in flows) {
            if (flow.count < 1 || flow.count > MAX_FREQUENCY) fail(FinancialError.INVALID_INPUT, "frequency must be between 1 and $MAX_FREQUENCY")
        }
    }

    companion object {
        const val MAX_ENTRIES = 80
        const val MAX_FREQUENCY = 99
    }
}

object Npv {
    private val HUNDRED = BigDecimal(100)

    /** Net present value at [ratePercent] per period. */
    fun compute(cashFlows: CashFlows, ratePercent: BigDecimal): FinancialResult<BigDecimal> = financial {
        cashFlows.validate()
        npvAt(cashFlows.expanded(), ratePercent.divide(HUNDRED, WORK_MC)).round(MC)
    }

    internal fun npvAt(amounts: List<BigDecimal>, rate: BigDecimal): BigDecimal {
        val base = BigDecimal.ONE.add(rate)
        if (base.signum() <= 0) fail(FinancialError.OUT_OF_RANGE, "rate must be greater than -100%")
        val discount = BigDecimal.ONE.divide(base, WORK_MC)
        var factor = BigDecimal.ONE
        var sum = BigDecimal.ZERO
        for (amount in amounts) {
            sum = sum.add(amount.multiply(factor, WORK_MC), WORK_MC)
            factor = factor.multiply(discount, WORK_MC)
        }
        return sum
    }
}

object Irr {
    private val HUNDRED = BigDecimal(100)

    /**
     * Internal rate of return, in percent per period.
     *
     * - No change of sign in the flows: NO_SIGN_CHANGE (IRR does not exist).
     * - One root in the search range: returned; with MULTIPLE_SIGN_CHANGES warning when the
     *   flows change sign more than once (other IRRs could exist outside the range).
     * - Several roots: MULTIPLE_SOLUTIONS with every candidate; no single value is guessed.
     * - Iteration limit reached: NO_CONVERGENCE.
     */
    fun compute(
        cashFlows: CashFlows,
        maxIterations: Int = RootFinder.DEFAULT_MAX_ITERATIONS,
        tolerance: BigDecimal = RootFinder.DEFAULT_TOLERANCE
    ): FinancialResult<BigDecimal> {
        return try {
            cashFlows.validate()
            val amounts = cashFlows.expanded()
            val signChanges = countSignChanges(amounts)
            if (signChanges == 0) fail(FinancialError.NO_SIGN_CHANGE, "cash flows never change sign")
            val roots = RootFinder.findRoots(maxIterations, tolerance) { r -> Npv.npvAt(amounts, r) }
            when {
                roots.isEmpty() -> FinancialResult.Failure(FinancialError.NO_SOLUTION, "no IRR between -99% and 100000%")
                roots.size > 1 -> FinancialResult.Failure(FinancialError.MULTIPLE_SOLUTIONS, "more than one IRR exists", roots.map { percent(it) })
                else -> FinancialResult.Success(
                    percent(roots[0]),
                    if (signChanges > 1) listOf(FinancialWarning.MULTIPLE_SIGN_CHANGES) else emptyList()
                )
            }
        } catch (e: FinancialException) {
            FinancialResult.Failure(e.error, e.message.orEmpty())
        }
    }

    private fun percent(rate: BigDecimal): BigDecimal = rate.multiply(HUNDRED).setScale(12, java.math.RoundingMode.HALF_EVEN)

    internal fun countSignChanges(amounts: List<BigDecimal>): Int {
        var changes = 0
        var previous = 0
        for (a in amounts) {
            val s = a.signum()
            if (s == 0) continue
            if (previous != 0 && s != previous) changes++
            previous = s
        }
        return changes
    }
}
