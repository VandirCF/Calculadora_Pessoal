package com.darkempire78.opencalculator.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmortizationRatesTest {

    // Loan 10000, 12 months, 1% per month, payment 888.49 (rounded), interest rounded to cents.
    private val loan = bd("10000")
    private val payment = bd("888.49")
    private val rate = bd("1")

    @Test fun firstRowsMatchIndependentSchedule() {
        val rows = Amortization.schedule(loan, payment, rate, 1, 3).success().rows
        assertEquals(bd("100.00"), rows[0].interest)
        assertEquals(bd("788.49"), rows[0].principal)
        assertEquals(bd("9211.51"), rows[0].balance)
        assertEquals(bd("92.12"), rows[1].interest)
        assertEquals(bd("796.37"), rows[1].principal)
        assertEquals(bd("8415.14"), rows[1].balance)
        assertEquals(bd("84.15"), rows[2].interest)
        assertEquals(bd("804.34"), rows[2].principal)
        assertEquals(bd("7610.80"), rows[2].balance)
    }

    @Test fun rangeSummaryOfFirstSixPeriods() {
        val summary = Amortization.schedule(loan, payment, rate, 1, 6).success()
        assertEquals(bd("480.14"), summary.totalInterest)
        assertEquals(bd("4850.80"), summary.totalPrincipal)
        assertEquals(bd("5149.20"), summary.balance)
    }

    @Test fun interestPlusPrincipalEqualsPayments() {
        val summary = Amortization.schedule(loan, payment, rate, 1, 12).success()
        assertEquals(0, (payment.multiply(bd("12"))).compareTo(summary.totalInterest.add(summary.totalPrincipal)))
    }

    @Test fun finalBalanceIsWithinRoundingOfZero() {
        val balance = Amortization.schedule(loan, payment, rate, 1, 12).success().balance
        assertEquals(bd("-0.02"), balance) // independent schedule ends at -0.02 because the payment was rounded
    }

    @Test fun rangeNotStartingAtOneCarriesTheBalance() {
        val rows = Amortization.schedule(loan, payment, rate, 3, 3).success().rows
        assertEquals(1, rows.size)
        assertEquals(3, rows[0].period)
        assertEquals(bd("7610.80"), rows[0].balance)
    }

    @Test fun beginModeFirstPaymentBearsNoInterest() {
        val rows = Amortization.schedule(loan, payment, rate, 1, 2, beginMode = true).success().rows
        assertEquals(bd("0.00"), rows[0].interest.setScale(2))
        assertEquals(payment, rows[0].principal)
        assertEquals(bd("9111.51"), rows[0].balance)
        assertEquals(bd("91.12"), rows[1].interest)
    }

    @Test fun zeroRateHasNoInterest() {
        val summary = Amortization.schedule(bd("1200"), bd("100"), bd("0"), 1, 12).success()
        assertEquals(0, summary.totalInterest.compareTo(bd("0")))
        assertEquals(0, summary.balance.compareTo(bd("0")))
    }

    @Test fun invalidRangesAreRejected() {
        assertFails(FinancialError.INVALID_INPUT, Amortization.schedule(loan, payment, rate, 0, 3))
        assertFails(FinancialError.INVALID_INPUT, Amortization.schedule(loan, payment, rate, 5, 3))
        assertFails(FinancialError.INVALID_INPUT, Amortization.schedule(bd("-1"), payment, rate, 1, 3))
        assertFails(FinancialError.OUT_OF_RANGE, Amortization.schedule(loan, payment, bd("-100"), 1, 3))
    }

    // ---- rate conversions ------------------------------------------------------------------
    @Test fun nominalMonthlyToEffectiveAnnual() {
        val c = RateConversions.nominalToEffectiveAnnual(bd("12"), Periodicity.MONTHLY).success()
        assertClose("12.682503013196972066", c.resultRatePercent, "1E-15")
        assertEquals(RateKind.NOMINAL, c.sourceKind)
        assertEquals(RateKind.EFFECTIVE, c.targetKind)
        assertEquals(Periodicity.MONTHLY, c.sourcePeriodicity)
        assertEquals(Periodicity.ANNUAL, c.targetPeriodicity)
        assertEquals(RateConversion.Method.NOMINAL_TO_EFFECTIVE, c.method)
    }

    @Test fun nominalQuarterlyToEffectiveAnnual() =
        assertClose("26.247696", RateConversions.nominalToEffectiveAnnual(bd("24"), Periodicity.QUARTERLY).success().resultRatePercent, "1E-15")

    @Test fun effectiveAnnualToNominalMonthly() =
        assertClose("9.568968514684489279", RateConversions.effectiveAnnualToNominal(bd("10"), Periodicity.MONTHLY).success().resultRatePercent, "1E-15")

    @Test fun effectiveAnnualToMonthlyEquivalent() {
        val c = RateConversions.equivalent(bd("10"), Periodicity.ANNUAL, Periodicity.MONTHLY).success()
        assertClose("0.797414042890374107", c.resultRatePercent, "1E-15")
        assertEquals(RateConversion.Method.COMPOUND_EQUIVALENT, c.method)
    }

    @Test fun monthlyToAnnualEquivalent() =
        assertClose("12.682503013196972066", RateConversions.equivalent(bd("1"), Periodicity.MONTHLY, Periodicity.ANNUAL).success().resultRatePercent, "1E-15")

    @Test fun dailyToAnnualEquivalent() =
        assertClose("20.015941067771088857", RateConversions.equivalent(bd("0.05"), Periodicity.DAILY, Periodicity.ANNUAL).success().resultRatePercent, "1E-14")

    @Test fun annualToQuarterlyEquivalent() =
        assertClose("2.873734472208028043", RateConversions.equivalent(bd("12"), Periodicity.ANNUAL, Periodicity.QUARTERLY).success().resultRatePercent, "1E-15")

    @Test fun annualDividedByTwelveIsNotTheEffectiveMonthlyRate() {
        val equivalentMonthly = RateConversions.equivalent(bd("12"), Periodicity.ANNUAL, Periodicity.MONTHLY).success().resultRatePercent
        assertTrue("simple division (1%) differs from the compound equivalent", equivalentMonthly.compareTo(bd("1")) != 0)
        assertClose("0.948879293", equivalentMonthly, "1E-8")
    }

    @Test fun conversionsRoundTrip() {
        val monthly = RateConversions.equivalent(bd("7.5"), Periodicity.ANNUAL, Periodicity.MONTHLY).success().resultRatePercent
        val back = RateConversions.equivalent(monthly, Periodicity.MONTHLY, Periodicity.ANNUAL).success().resultRatePercent
        assertClose("7.5", back, "1E-25")
    }

    @Test fun zeroRateConvertsToZero() =
        assertClose("0", RateConversions.equivalent(bd("0"), Periodicity.ANNUAL, Periodicity.MONTHLY).success().resultRatePercent)

    @Test fun rateAtOrBelowMinusHundredIsRejected() {
        assertFails(FinancialError.OUT_OF_RANGE, RateConversions.equivalent(bd("-100"), Periodicity.ANNUAL, Periodicity.MONTHLY))
        assertFails(FinancialError.OUT_OF_RANGE, RateConversions.effectiveAnnualToNominal(bd("-150"), Periodicity.MONTHLY))
    }
}
