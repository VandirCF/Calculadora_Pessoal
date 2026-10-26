package com.darkempire78.opencalculator.finance

import java.math.BigDecimal

/**
 * Time value of money registers.
 *
 * Sign convention: money received is positive, money paid is negative. Loans and investments
 * therefore have PV and PMT/FV with opposite signs. Signs are never altered silently.
 *
 * Equation (r = i / 100 per period, b = 1 when payments are at the beginning of the period):
 *
 *     PV (1+r)^n + PMT (1 + r b) ((1+r)^n - 1) / r + FV = 0          (r != 0)
 *     PV + PMT n + FV = 0                                             (r == 0)
 */
data class TvmRegisters(
    val n: BigDecimal? = null,
    /** Interest rate per period, in percent. */
    val i: BigDecimal? = null,
    val pv: BigDecimal? = null,
    val pmt: BigDecimal? = null,
    val fv: BigDecimal? = null,
    val beginMode: Boolean = false
)

object Tvm {
    private val HUNDRED = BigDecimal(100)

    /** Annuity growth factor ((1+r)^n - 1) / r and its limit n when r = 0. */
    private fun annuityFactor(r: BigDecimal, n: BigDecimal): BigDecimal =
        if (r.signum() == 0) n else BigMath.pow(BigDecimal.ONE.add(r), n).subtract(BigDecimal.ONE).divide(r, WORK_MC)

    private fun beginFactor(r: BigDecimal, begin: Boolean): BigDecimal =
        if (begin) BigDecimal.ONE.add(r) else BigDecimal.ONE

    private fun rate(registers: TvmRegisters): BigDecimal {
        val i = registers.i ?: fail(FinancialError.MISSING_REGISTER, "i")
        val r = i.divide(HUNDRED, WORK_MC)
        if (r <= BigDecimal.ONE.negate()) fail(FinancialError.OUT_OF_RANGE, "rate must be greater than -100%")
        return r
    }

    private fun periods(registers: TvmRegisters): BigDecimal {
        val n = registers.n ?: fail(FinancialError.MISSING_REGISTER, "n")
        if (n.signum() < 0) fail(FinancialError.OUT_OF_RANGE, "n must not be negative")
        return n
    }

    private fun need(value: BigDecimal?, name: String): BigDecimal =
        value ?: fail(FinancialError.MISSING_REGISTER, name)

    /** Value of PV(1+r)^n + PMT(1+rb)·annuity + FV; zero when the registers satisfy the equation. */
    private fun balance(r: BigDecimal, n: BigDecimal, pv: BigDecimal, pmt: BigDecimal, fv: BigDecimal, begin: Boolean): BigDecimal {
        val growth = BigMath.pow(BigDecimal.ONE.add(r), n)
        return pv.multiply(growth, WORK_MC)
            .add(pmt.multiply(beginFactor(r, begin), WORK_MC).multiply(annuityFactor(r, n), WORK_MC), WORK_MC)
            .add(fv, WORK_MC)
    }

    fun solvePmt(registers: TvmRegisters): FinancialResult<BigDecimal> = financial {
        val r = rate(registers)
        val n = periods(registers)
        val pv = need(registers.pv, "PV")
        val fv = need(registers.fv, "FV")
        val denominator = beginFactor(r, registers.beginMode).multiply(annuityFactor(r, n), WORK_MC)
        if (denominator.signum() == 0) fail(FinancialError.NO_SOLUTION, "n must be greater than zero")
        val growth = BigMath.pow(BigDecimal.ONE.add(r), n)
        pv.multiply(growth, WORK_MC).add(fv, WORK_MC).negate().divide(denominator, WORK_MC).round(MC)
    }

    fun solvePv(registers: TvmRegisters): FinancialResult<BigDecimal> = financial {
        val r = rate(registers)
        val n = periods(registers)
        val pmt = need(registers.pmt, "PMT")
        val fv = need(registers.fv, "FV")
        val growth = BigMath.pow(BigDecimal.ONE.add(r), n)
        val annuity = pmt.multiply(beginFactor(r, registers.beginMode), WORK_MC).multiply(annuityFactor(r, n), WORK_MC)
        fv.add(annuity, WORK_MC).negate().divide(growth, WORK_MC).round(MC)
    }

    fun solveFv(registers: TvmRegisters): FinancialResult<BigDecimal> = financial {
        val r = rate(registers)
        val n = periods(registers)
        val pv = need(registers.pv, "PV")
        val pmt = need(registers.pmt, "PMT")
        balance(r, n, pv, pmt, BigDecimal.ZERO, registers.beginMode).negate().round(MC)
    }

    fun solveN(registers: TvmRegisters): FinancialResult<BigDecimal> = financial {
        val r = rate(registers)
        val pv = need(registers.pv, "PV")
        val pmt = need(registers.pmt, "PMT")
        val fv = need(registers.fv, "FV")
        requireOppositeSigns(pv, pmt, fv)
        if (r.signum() == 0) {
            if (pmt.signum() == 0) fail(FinancialError.NO_SOLUTION, "PMT is zero and the rate is zero")
            val n = pv.add(fv).negate().divide(pmt, WORK_MC)
            if (n.signum() < 0) fail(FinancialError.INCONSISTENT_SIGNS, "no non-negative n satisfies the equation")
            return@financial n.round(MC)
        }
        val k = pmt.multiply(beginFactor(r, registers.beginMode), WORK_MC).divide(r, WORK_MC)
        val denominator = pv.add(k, WORK_MC)
        if (denominator.signum() == 0) fail(FinancialError.NO_SOLUTION, "the payments never amortize the balance")
        val ratio = k.subtract(fv, WORK_MC).divide(denominator, WORK_MC)
        if (ratio.signum() <= 0) fail(FinancialError.INCONSISTENT_SIGNS, "check the signs of PV, PMT and FV")
        val n = BigMath.ln(ratio).divide(BigMath.ln(BigDecimal.ONE.add(r)), WORK_MC)
        if (n.signum() < 0) fail(FinancialError.NO_SOLUTION, "no non-negative n satisfies the equation")
        n.round(MC)
    }

    /** Interest rate per period, in percent. */
    fun solveRate(registers: TvmRegisters): FinancialResult<BigDecimal> {
        return try {
            val n = periods(registers)
            val pv = need(registers.pv, "PV")
            val pmt = need(registers.pmt, "PMT")
            val fv = need(registers.fv, "FV")
            requireOppositeSigns(pv, pmt, fv)
            if (n.signum() == 0) fail(FinancialError.NO_SOLUTION, "n must be greater than zero")
            val roots = RootFinder.findRoots { r -> balance(r, n, pv, pmt, fv, registers.beginMode) }
            when {
                roots.isEmpty() -> FinancialResult.Failure(FinancialError.NO_SOLUTION, "no rate between -99% and 100000% satisfies the equation")
                roots.size > 1 -> FinancialResult.Failure(FinancialError.MULTIPLE_SOLUTIONS, "more than one rate satisfies the equation", roots.map { it.multiply(HUNDRED).round(MC) })
                else -> FinancialResult.Success(roots[0].multiply(HUNDRED).round(MC))
            }
        } catch (e: FinancialException) {
            FinancialResult.Failure(e.error, e.message.orEmpty())
        }
    }

    /** Opposite signs are needed: at least one inflow and one outflow among PV, PMT and FV. */
    private fun requireOppositeSigns(pv: BigDecimal, pmt: BigDecimal, fv: BigDecimal) {
        val signs = listOf(pv, pmt, fv).map { it.signum() }.filter { it != 0 }.toSet()
        if (signs.size < 2) fail(FinancialError.INCONSISTENT_SIGNS, "cash in and cash out must have opposite signs")
    }
}
