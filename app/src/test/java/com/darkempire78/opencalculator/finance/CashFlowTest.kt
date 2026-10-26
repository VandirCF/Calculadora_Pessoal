package com.darkempire78.opencalculator.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CashFlowTest {

    private fun flows(cf0: String, vararg amounts: Pair<String, Int>) =
        CashFlows(bd(cf0), amounts.map { CashFlow(bd(it.first), it.second) })

    private val projectA = flows("-1000", "300" to 1, "400" to 1, "500" to 1)
    private val projectB = flows("-5000", "1500" to 4)

    // ---- NPV -------------------------------------------------------------------------------
    @Test fun npvWithPositiveRate() =
        assertClose("-21.03681442524417731029", Npv.compute(projectA, bd("10")).success(), "1E-12")

    @Test fun npvWithZeroRateIsThePlainSum() =
        assertClose("200", Npv.compute(projectA, bd("0")).success())

    @Test fun npvRepeatedFlowsViaFrequency() =
        assertClose("-31.80973993350155520556", Npv.compute(projectB, bd("8")).success(), "1E-12")

    @Test fun frequencyIsEquivalentToRepeatingTheEntry() {
        val repeated = flows("-5000", "1500" to 1, "1500" to 1, "1500" to 1, "1500" to 1)
        assertEquals(
            Npv.compute(projectB, bd("8")).success().compareTo(Npv.compute(repeated, bd("8")).success()), 0
        )
    }

    @Test fun npvOfOnlyCf0IsCf0() =
        assertClose("-1000", Npv.compute(flows("-1000"), bd("12")).success())

    @Test fun npvWithNegativeRateAboveMinusHundred() {
        // -50%: each period doubles the value: -1000 + 300*2 + 400*4 + 500*8 = 5200
        assertClose("5200", Npv.compute(projectA, bd("-50")).success(), "1E-10")
    }

    @Test fun npvRejectsRateOfMinusHundredPercent() =
        assertFails(FinancialError.OUT_OF_RANGE, Npv.compute(projectA, bd("-100")))

    @Test fun npvEmptyListIsRejected() =
        assertFails(FinancialError.EMPTY_CASH_FLOWS, Npv.compute(CashFlows(), bd("10")))

    @Test fun npvInvalidFrequencyIsRejected() {
        assertFails(FinancialError.INVALID_INPUT, Npv.compute(flows("-10", "5" to 0), bd("10")))
        assertFails(FinancialError.INVALID_INPUT, Npv.compute(flows("-10", "5" to 100), bd("10")))
    }

    @Test fun npvTooManyEntriesIsRejected() {
        val many = CashFlows(bd("-1"), List(CashFlows.MAX_ENTRIES + 1) { CashFlow(bd("1"), 1) })
        assertFails(FinancialError.OUT_OF_RANGE, Npv.compute(many, bd("5")))
    }

    // ---- IRR -------------------------------------------------------------------------------
    @Test fun irrConventionalFlowHasOneRoot() =
        assertClose("8.896339469335", Irr.compute(projectA).success(), "1E-9")

    @Test fun irrWithFrequency() =
        assertClose("7.713847295208", Irr.compute(projectB).success(), "1E-9")

    @Test fun irrMakesNpvZero() {
        val irr = Irr.compute(projectA).success()
        val npv = Npv.compute(projectA, irr).success()
        assertClose("0", npv, "1E-9")
    }

    @Test fun irrWithoutSignChangeDoesNotExist() {
        assertFails(FinancialError.NO_SIGN_CHANGE, Irr.compute(flows("100", "50" to 1, "20" to 1)))
        assertFails(FinancialError.NO_SIGN_CHANGE, Irr.compute(flows("-100", "-50" to 1)))
    }

    @Test fun irrWithTwoRootsIsAmbiguousAndListsCandidates() {
        val failure = Irr.compute(flows("-100", "230" to 1, "-132" to 1)).failure()
        assertEquals(FinancialError.MULTIPLE_SOLUTIONS, failure.error)
        assertEquals(2, failure.candidates.size)
        assertClose("10", failure.candidates[0], "1E-9")
        assertClose("20", failure.candidates[1], "1E-9")
    }

    @Test fun irrWithTwoSignChangesButOneRootInRangeWarns() {
        // -1000, +1050, -0.5: second root is at -99.95% (outside the supported range). Reference: quadratic formula.
        val result = Irr.compute(flows("-1000", "1050" to 1, "-0.5" to 1))
        assertClose("4.9523593368305979007", result.success(), "1E-9")
        assertEquals(listOf(FinancialWarning.MULTIPLE_SIGN_CHANGES), (result as FinancialResult.Success).warnings)
    }

    @Test fun irrWithTwoSignChangesAndANegativeRateRootIsAmbiguous() {
        // NPV(r) = -1000 + 1500/(1+r) - 100/(1+r)^2 has roots at about -93% and +43.5%
        val failure = Irr.compute(flows("-1000", "1500" to 1, "-100" to 1)).failure()
        assertEquals(FinancialError.MULTIPLE_SOLUTIONS, failure.error)
        assertEquals(2, failure.candidates.size)
        assertClose("43.007352543677", failure.candidates[1], "1E-9")
        assertTrue(failure.candidates[0] < bd("-90"))
    }

    @Test fun irrConventionalFlowHasNoWarning() =
        assertTrue((Irr.compute(projectA) as FinancialResult.Success).warnings.isEmpty())

    @Test fun irrStopsAtIterationLimitWithoutLooping() =
        assertFails(FinancialError.NO_CONVERGENCE, Irr.compute(projectA, maxIterations = 5))

    @Test fun irrEmptyListIsRejected() =
        assertFails(FinancialError.EMPTY_CASH_FLOWS, Irr.compute(CashFlows()))

    @Test fun irrWhenAllFlowsAfterCf0AreZeroHasNoSignChange() =
        assertFails(FinancialError.NO_SIGN_CHANGE, Irr.compute(flows("-100", "0" to 3)))

    @Test fun irrOfHighReturnIsFound() {
        // -100 then 1100 next period: 1000%
        assertClose("1000", Irr.compute(flows("-100", "1100" to 1)).success(), "1E-8")
    }

    @Test fun irrOfNegativeReturnIsFound() {
        // -100 then 50: -50%
        assertClose("-50", Irr.compute(flows("-100", "50" to 1)).success(), "1E-8")
    }
}
