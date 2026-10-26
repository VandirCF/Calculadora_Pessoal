package com.darkempire78.opencalculator.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvmTest {

    private fun regs(
        n: String? = null, i: String? = null, pv: String? = null, pmt: String? = null, fv: String? = null,
        begin: Boolean = false
    ) = TvmRegisters(n?.let(::bd), i?.let(::bd), pv?.let(::bd), pmt?.let(::bd), fv?.let(::bd), begin)

    // ---- PMT -------------------------------------------------------------------------------
    @Test fun loanPaymentEndOfPeriod() =
        assertClose("-599.55052515275239459146", Tvm.solvePmt(regs(n = "360", i = "0.5", pv = "100000", fv = "0")).success())

    @Test fun loanPaymentBeginningOfPeriod() =
        assertClose("-596.56768671915661153379", Tvm.solvePmt(regs(n = "360", i = "0.5", pv = "100000", fv = "0", begin = true)).success())

    @Test fun paymentWithZeroRateIsLinear() =
        assertClose("100", Tvm.solvePmt(regs(n = "12", i = "0", pv = "-1200", fv = "0")).success())

    @Test fun paymentSignsAreNotChangedSilently() {
        // PV negative (money out) => PMT positive (money in), exactly the opposite sign of the loan above
        val pmt = Tvm.solvePmt(regs(n = "360", i = "0.5", pv = "-100000", fv = "0")).success()
        assertClose("599.55052515275239459146", pmt)
    }

    // ---- FV / PV ---------------------------------------------------------------------------
    @Test fun futureValueOfMonthlySavings() =
        assertClose("38702.85541666115129651715", Tvm.solveFv(regs(n = "120", i = "0.75", pv = "0", pmt = "-200")).success())

    @Test fun futureValueWithFractionalPeriods() =
        assertClose("1269.05870628588337185966", Tvm.solveFv(regs(n = "2.5", i = "10", pv = "-1000", pmt = "0")).success(), "1E-10")

    @Test fun presentValueOfAnnuity() =
        assertClose("22477.51920311200096972380", Tvm.solvePv(regs(n = "60", i = "1", pmt = "-500", fv = "0")).success())

    // ---- n ---------------------------------------------------------------------------------
    @Test fun periodsToDoubleMoney() =
        assertClose("69.66071689357488922414", Tvm.solveN(regs(i = "1", pv = "-1000", pmt = "0", fv = "2000")).success(), "1E-10")

    @Test fun periodsToPayOffLoan() =
        assertClose("40.74890715609405773008", Tvm.solveN(regs(i = "1", pv = "10000", pmt = "-300", fv = "0")).success(), "1E-10")

    @Test fun periodsWithZeroRate() =
        assertClose("12", Tvm.solveN(regs(i = "0", pv = "-1200", pmt = "100", fv = "0")).success())

    @Test fun periodsAllSameSignIsInconsistent() =
        assertFails(FinancialError.INCONSISTENT_SIGNS, Tvm.solveN(regs(i = "1", pv = "1000", pmt = "100", fv = "500")))

    @Test fun periodsPaymentTooSmallNeverAmortizes() {
        // interest alone is 100 per period; a payment of 50 can never pay the loan off
        assertTrue(Tvm.solveN(regs(i = "1", pv = "10000", pmt = "-50", fv = "0")) is FinancialResult.Failure)
    }

    // ---- rate ------------------------------------------------------------------------------
    @Test fun rateOfTwoPeriodGrowth() =
        assertClose("10", Tvm.solveRate(regs(n = "2", pv = "-1000", pmt = "0", fv = "1210")).success(), "1E-9")

    @Test fun rateOfLoan() =
        assertClose("0.49999931931192", Tvm.solveRate(regs(n = "360", pv = "100000", pmt = "-599.55", fv = "0")).success(), "1E-9")

    @Test fun rateWithSameSignsIsInconsistent() =
        assertFails(FinancialError.INCONSISTENT_SIGNS, Tvm.solveRate(regs(n = "10", pv = "1000", pmt = "100", fv = "0")))

    @Test fun rateWithZeroPeriodsHasNoSolution() =
        assertFails(FinancialError.NO_SOLUTION, Tvm.solveRate(regs(n = "0", pv = "-1000", pmt = "0", fv = "1210")))

    @Test fun rateWithoutPositiveSolutionInRange() {
        // -1000 now, +1 later: the rate would be -99.9%, outside the supported -99%..
        assertFails(FinancialError.NO_SOLUTION, Tvm.solveRate(regs(n = "1", pv = "-1000", pmt = "0", fv = "1")))
    }

    // ---- validation ------------------------------------------------------------------------
    @Test fun missingRegisterIsReported() =
        assertFails(FinancialError.MISSING_REGISTER, Tvm.solvePmt(regs(n = "12", i = "1", pv = "100")))

    @Test fun negativePeriodsAreRejected() =
        assertFails(FinancialError.OUT_OF_RANGE, Tvm.solvePmt(regs(n = "-1", i = "1", pv = "100", fv = "0")))

    @Test fun rateAtOrBelowMinusHundredPercentIsRejected() =
        assertFails(FinancialError.OUT_OF_RANGE, Tvm.solvePmt(regs(n = "12", i = "-100", pv = "100", fv = "0")))

    @Test fun zeroPeriodsPaymentIsNotComputable() =
        assertFails(FinancialError.NO_SOLUTION, Tvm.solvePmt(regs(n = "0", i = "1", pv = "100", fv = "0")))

    // ---- round trips: solving a register and feeding it back ---------------------------------
    @Test fun solvedPaymentSatisfiesTheEquation() {
        val base = regs(n = "48", i = "1.2", pv = "25000", fv = "-3000")
        val pmt = Tvm.solvePmt(base).success()
        val fv = Tvm.solveFv(base.copy(pmt = pmt)).success()
        assertClose("-3000", fv, "1E-20")
    }

    @Test fun solvedPeriodsRoundTrip() {
        val base = regs(i = "0.8", pv = "-5000", pmt = "150", fv = "0")
        val n = Tvm.solveN(base).success()
        val pmt = Tvm.solvePmt(base.copy(n = n)).success()
        assertClose("150", pmt, "1E-18")
    }

    @Test fun solvedRateRoundTrip() {
        val base = regs(n = "36", pv = "-12000", pmt = "400", fv = "0")
        val i = Tvm.solveRate(base).success()
        val pmt = Tvm.solvePmt(base.copy(i = i)).success()
        assertClose("400", pmt, "1E-8")
    }
}
