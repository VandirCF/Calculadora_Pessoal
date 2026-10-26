package com.darkempire78.opencalculator.finance

import java.math.BigDecimal
import java.math.RoundingMode

/** One row of an amortization schedule. Amounts are magnitudes (always >= 0 for a normal loan). */
data class AmortizationRow(
    val period: Int,
    val payment: BigDecimal,
    val interest: BigDecimal,
    val principal: BigDecimal,
    val balance: BigDecimal
)

data class AmortizationSummary(
    val rows: List<AmortizationRow>,
    val totalInterest: BigDecimal,
    val totalPrincipal: BigDecimal,
    /** Balance after the last row of the range. */
    val balance: BigDecimal
)

object Amortization {
    /**
     * Amortization of a loan of [loanAmount] with [paymentAmount] per period (both given as
     * positive magnitudes) at [ratePercent] per period.
     *
     * Each period: interest = balance * r rounded to [scale] (HALF_EVEN, as lenders do),
     * principal = payment - interest, balance = balance - principal.
     * With [beginMode] the first payment happens at time 0 and bears no interest.
     * Rows [from]..[to] (1-based, inclusive) are returned, but the balance is carried from period 1.
     */
    fun schedule(
        loanAmount: BigDecimal,
        paymentAmount: BigDecimal,
        ratePercent: BigDecimal,
        from: Int,
        to: Int,
        beginMode: Boolean = false,
        scale: Int = 2
    ): FinancialResult<AmortizationSummary> = financial {
        if (from < 1 || to < from) fail(FinancialError.INVALID_INPUT, "invalid period range $from..$to")
        if (to > 100000) fail(FinancialError.OUT_OF_RANGE, "too many periods")
        if (loanAmount.signum() < 0 || paymentAmount.signum() < 0) fail(FinancialError.INVALID_INPUT, "use positive magnitudes for loan and payment")
        val r = ratePercent.divide(BigDecimal(100), WORK_MC)
        if (r <= BigDecimal.ONE.negate()) fail(FinancialError.OUT_OF_RANGE, "rate must be greater than -100%")

        var balance = loanAmount
        val rows = ArrayList<AmortizationRow>()
        var totalInterest = BigDecimal.ZERO
        var totalPrincipal = BigDecimal.ZERO
        for (k in 1..to) {
            val interest = if (beginMode && k == 1) BigDecimal.ZERO
            else balance.multiply(r, WORK_MC).setScale(scale, RoundingMode.HALF_EVEN)
            val principal = paymentAmount.subtract(interest)
            balance = balance.subtract(principal)
            if (k >= from) {
                rows.add(AmortizationRow(k, paymentAmount, interest, principal, balance))
                totalInterest = totalInterest.add(interest)
                totalPrincipal = totalPrincipal.add(principal)
            }
        }
        AmortizationSummary(rows, totalInterest, totalPrincipal, balance)
    }
}

/** How often a rate compounds or is quoted. */
enum class Periodicity(val periodsPerYear: Int) {
    DAILY(365), WEEKLY(52), BIWEEKLY(26), MONTHLY(12), BIMONTHLY(6),
    QUARTERLY(4), SEMIANNUAL(2), ANNUAL(1)
}

enum class RateKind { NOMINAL, EFFECTIVE }

/** Full description of a rate conversion, so the UI can show source, target and method. */
data class RateConversion(
    val sourceRatePercent: BigDecimal,
    val sourcePeriodicity: Periodicity,
    val sourceKind: RateKind,
    val resultRatePercent: BigDecimal,
    val targetPeriodicity: Periodicity,
    val targetKind: RateKind,
    val method: Method
) {
    enum class Method {
        /** Effective annual rate = (1 + j/m)^m - 1. */
        NOMINAL_TO_EFFECTIVE,

        /** Nominal annual rate j = m ((1 + e)^(1/m) - 1). */
        EFFECTIVE_TO_NOMINAL,

        /** Compound equivalence: (1 + r_to) = (1 + r_from)^(P_from / P_to). */
        COMPOUND_EQUIVALENT
    }
}

object RateConversions {
    private val HUNDRED = BigDecimal(100)

    private fun fraction(percent: BigDecimal) = percent.divide(HUNDRED, WORK_MC)
    private fun checkRate(r: BigDecimal) {
        if (r <= BigDecimal.ONE.negate()) fail(FinancialError.OUT_OF_RANGE, "rate must be greater than -100%")
    }

    /**
     * Nominal annual rate [nominalPercent] compounded [compounding] times a year -> effective rate
     * for the same year. Note: nominal/12 is the monthly rate, NOT the effective annual rate.
     */
    fun nominalToEffectiveAnnual(nominalPercent: BigDecimal, compounding: Periodicity): FinancialResult<RateConversion> = financial {
        val m = BigDecimal(compounding.periodsPerYear)
        val periodic = fraction(nominalPercent).divide(m, WORK_MC)
        checkRate(periodic)
        val effective = BigMath.pow(BigDecimal.ONE.add(periodic), m).subtract(BigDecimal.ONE)
        RateConversion(nominalPercent, compounding, RateKind.NOMINAL, effective.multiply(HUNDRED).round(MC), Periodicity.ANNUAL, RateKind.EFFECTIVE, RateConversion.Method.NOMINAL_TO_EFFECTIVE)
    }

    /** Effective annual rate -> nominal annual rate compounded [compounding] times a year. */
    fun effectiveAnnualToNominal(effectivePercent: BigDecimal, compounding: Periodicity): FinancialResult<RateConversion> = financial {
        val m = BigDecimal(compounding.periodsPerYear)
        val e = fraction(effectivePercent)
        checkRate(e)
        val nominal = BigMath.pow(BigDecimal.ONE.add(e), BigDecimal.ONE.divide(m, WORK_MC)).subtract(BigDecimal.ONE).multiply(m, WORK_MC)
        RateConversion(effectivePercent, Periodicity.ANNUAL, RateKind.EFFECTIVE, nominal.multiply(HUNDRED).round(MC), compounding, RateKind.NOMINAL, RateConversion.Method.EFFECTIVE_TO_NOMINAL)
    }

    /** Equivalent effective rate for another periodicity under compound capitalization. */
    fun equivalent(effectivePercent: BigDecimal, from: Periodicity, to: Periodicity): FinancialResult<RateConversion> = financial {
        val r = fraction(effectivePercent)
        checkRate(r)
        val exponent = BigDecimal(from.periodsPerYear).divide(BigDecimal(to.periodsPerYear), WORK_MC)
        val converted = BigMath.pow(BigDecimal.ONE.add(r), exponent).subtract(BigDecimal.ONE)
        RateConversion(effectivePercent, from, RateKind.EFFECTIVE, converted.multiply(HUNDRED).round(MC), to, RateKind.EFFECTIVE, RateConversion.Method.COMPOUND_EQUIVALENT)
    }
}
