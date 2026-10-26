package com.darkempire78.opencalculator.finance

import com.darkempire78.opencalculator.finance.rpn.RpnEngine
import com.darkempire78.opencalculator.finance.rpn.RpnKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class RpnEngineTest {

    private fun run(sequence: String) = RpnEngine().also { it.keys(sequence) }

    private fun assertX(expected: String, e: RpnEngine, tolerance: String = "0") =
        assertClose(expected, e.x, tolerance, "X:")

    private fun stack(e: RpnEngine) = listOf(e.x, e.y, e.z, e.t).map { it.stripTrailingZeros().toPlainString() }

    // ---- basic sequences from the specification ---------------------------------------------
    @Test fun twoEnterThreePlus() = assertX("5", run("2 ENTER 3 +"))
    @Test fun tenEnterFourMinus() = assertX("6", run("10 ENTER 4 -"))
    @Test fun multiplication() = assertX("12", run("3 ENTER 4 *"))
    @Test fun divisionIsYOverX() = assertX("2.5", run("5 ENTER 2 /"))
    @Test fun chainedWithoutIntermediateEnter() = assertX("20", run("2 ENTER 3 + 4 *")) // (2+3)*4
    @Test fun classicExpression() = assertX("14", run("2 ENTER 3 ENTER 4 * +")) // 2+3*4

    // ---- percent rule: X <- Y*X/100, Y kept ------------------------------------------------
    @Test fun hundredEnterTenPercent() {
        val e = run("100 ENTER 10 %")
        assertX("10", e)
        assertClose("100", e.y)
    }

    @Test fun percentThenAddGivesIncrease() = assertX("110", run("100 ENTER 10 % +"))
    @Test fun percentThenSubtractGivesDiscount() = assertX("90", run("100 ENTER 10 % -"))
    @Test fun percentOfTotal() = assertX("25", run("200 ENTER 50 PERCENT_TOTAL"))
    @Test fun percentOfTotalKeepsY() {
        val e = run("200 ENTER 50 PERCENT_TOTAL")
        assertEquals("200", e.y.stripTrailingZeros().toPlainString())
    }
    @Test fun percentOfZeroTotalIsAnError() {
        val e = run("0 ENTER 5 PERCENT_TOTAL")
        assertEquals(FinancialError.DIVISION_BY_ZERO, e.error)
    }
    @Test fun deltaPercent() = assertX("25", run("80 ENTER 100 DELTA_PERCENT"))
    @Test fun deltaPercentWithZeroBaseIsError() {
        val e = run("0 ENTER 5 DELTA_PERCENT")
        assertEquals(FinancialError.DIVISION_BY_ZERO, e.error)
    }

    // ---- stack mechanics ----------------------------------------------------------------------
    @Test fun enterDuplicatesX() = assertEquals(listOf("7", "7", "0", "0"), stack(run("7 ENTER")))

    @Test fun numberAfterEnterOverwritesX() = assertEquals(listOf("8", "7", "0", "0"), stack(run("7 ENTER 8")))

    @Test fun numberAfterResultLiftsTheStack() = assertEquals(listOf("4", "5", "0", "0"), stack(run("2 ENTER 3 + 4")))

    @Test fun stackHoldsFourLevelsAndLosesTheFifth() {
        val e = run("1 ENTER 2 ENTER 3 ENTER 4 ENTER 5")
        assertEquals(listOf("5", "4", "3", "2"), stack(e)) // the 1 fell off the top
    }

    @Test fun binaryOperationDropsTheStack() {
        val e = run("1 ENTER 2 ENTER 3 ENTER 4 +")
        assertEquals(listOf("7", "2", "1", "1"), stack(e)) // T is duplicated downwards
    }

    @Test fun swapExchangesXAndY() = assertEquals(listOf("1", "2", "0", "0"), stack(run("1 ENTER 2 SWAP")))

    @Test fun rollDownRotatesTheStack() {
        val e = run("1 ENTER 2 ENTER 3 ENTER 4 ROLL_DOWN")
        assertEquals(listOf("3", "2", "1", "4"), stack(e))
    }

    @Test fun rollUpIsTheInverseOfRollDown() {
        val e = run("1 ENTER 2 ENTER 3 ENTER 4 ROLL_DOWN ROLL_UP")
        assertEquals(listOf("4", "3", "2", "1"), stack(e))
    }

    @Test fun fourRollsReturnToTheStartingStack() {
        val e = run("1 ENTER 2 ENTER 3 ENTER 4 ROLL_DOWN ROLL_DOWN ROLL_DOWN ROLL_DOWN")
        assertEquals(listOf("4", "3", "2", "1"), stack(e))
    }

    @Test fun clxClearsOnlyXAndDisablesLift() {
        val e = run("5 ENTER 6 CLX 9")
        assertEquals(listOf("9", "5", "0", "0"), stack(e)) // 9 replaced the cleared X without lifting
    }

    @Test fun clearStackClearsAllLevelsButNotRegisters() {
        val e = run("5 ENTER 6 ENTER 7 STO3 CLEAR_STACK")
        assertEquals(listOf("0", "0", "0", "0"), stack(e))
        assertClose("7", e.memory[3])
    }

    // ---- last X --------------------------------------------------------------------------------
    @Test fun lastXRecoversTheOperandOfTheLastOperation() {
        val e = run("2 ENTER 3 + LAST_X")
        assertEquals(listOf("3", "5", "0", "0"), stack(e))
    }

    @Test fun lastXAllowsRepeatingAnOperand() = assertX("17", run("5 ENTER 6 + LAST_X +")) // 11, then 11 + 6

    @Test fun lastXOfUnaryOperation() {
        val e = run("16 SQRT LAST_X")
        assertEquals(listOf("16", "4", "0", "0"), stack(e))
    }

    // ---- number entry ----------------------------------------------------------------------------
    @Test fun decimalEntry() = assertX("12.5", run("12.5"))
    @Test fun leadingDecimalPoint() = assertX("0.5", run(". 5"))
    @Test fun changeSignWhileTyping() = assertX("-12", run("12 CHS"))
    @Test fun changeSignAfterResultKeepsStackLiftState() = assertEquals(listOf("-5", "0", "0", "0"), stack(run("2 ENTER 3 + CHS")))
    @Test fun leadingZerosCollapse() = assertX("7", run("007"))
    @Test fun backspaceRemovesLastDigit() = assertX("12", run("123 BACKSPACE"))
    @Test fun backspaceOnLastDigitClearsX() = assertX("0", run("5 BACKSPACE"))
    @Test fun twentyDigitLimit() {
        val e = RpnEngine()
        repeat(25) { e.press(RpnKey.D9) }
        assertEquals(20, e.entry!!.length)
    }

    // ---- registers --------------------------------------------------------------------------------
    @Test fun storeAndRecall() {
        val e = run("42 STO5 CLEAR_STACK RCL5")
        assertX("42", e)
    }

    @Test fun recallLiftsTheStack() {
        val e = run("42 STO5 7 ENTER 8 RCL5")
        assertEquals(listOf("42", "8", "7", "42"), stack(e)) // old Z (42) moves up into T
    }

    @Test fun storeDoesNotChangeX() = assertX("42", run("42 STO0"))

    // ---- errors ---------------------------------------------------------------------------------------
    @Test fun divisionByZeroSetsErrorAndKeepsStack() {
        val e = run("5 ENTER 0 /")
        assertEquals(FinancialError.DIVISION_BY_ZERO, e.error)
        assertEquals(listOf("0", "5", "0", "0"), stack(e))
    }

    @Test fun nextKeyClearsErrorAndRunsNormally() {
        val e = run("5 ENTER 0 / CLX 3")
        assertNull(e.error)
        assertX("3", e)
    }

    @Test fun squareRootOfNegativeIsError() = assertEquals(FinancialError.OUT_OF_RANGE, run("4 CHS SQRT").error)
    @Test fun reciprocalOfZeroIsError() = assertEquals(FinancialError.DIVISION_BY_ZERO, run("0 RECIPROCAL").error)
    @Test fun errorDoesNotCorruptLastX() {
        val e = run("2 ENTER 3 + 0 RECIPROCAL")
        assertEquals(FinancialError.DIVISION_BY_ZERO, e.error)
        assertClose("3", e.lastX)
    }

    @Test fun powerOperation() = assertX("1024", run("2 ENTER 10 POW"))
    @Test fun fractionalPower() = assertX("3", run("9 ENTER 0.5 POW"), "1E-30")
    @Test fun negativeBaseFractionalPowerIsError() = assertEquals(FinancialError.OUT_OF_RANGE, run("4 CHS ENTER 0.5 POW").error)
    @Test fun reciprocal() = assertX("0.25", run("4 RECIPROCAL"))

    // ---- financial keys through the keyboard (TVM) ------------------------------------------------
    @Test fun tvmPaymentFromKeys() {
        // 1000 CHS PV, 12 n, 1 i, 0 FV, PMT  (loan of 1000 paid in 12 monthly installments at 1%)
        val e = run("1000 CHS PV 12 n 1 i 0 FV PMT")
        assertClose("88.848788678341707339987831227886", e.x, "1E-25") // independent: 1000*0.01/(1-1.01^-12)
        assertClose("-1000", e.tvm.pv)
        assertClose("12", e.tvm.n)
        assertClose("1", e.tvm.i)
    }

    @Test fun pressingAFinancialKeyAfterATypedNumberStores() {
        val e = run("500 PMT")
        assertClose("500", e.tvm.pmt)
    }

    @Test fun pressingAFinancialKeyAfterAFinancialKeyComputes() {
        val e = run("100 CHS PV 10 n 5 i 0 FV PMT FV")
        // after PMT was computed, FV recomputed from the registers is 0 again
        assertClose("0", e.x, "1E-25")
    }

    @Test fun monthlyConversionKeysUsePeriodsPerYear() {
        val e = run("30 N_TIMES_PER_YEAR 12 I_DIV_PER_YEAR")
        assertClose("360", e.tvm.n)
        assertClose("1", e.tvm.i)
    }

    @Test fun computingWithMissingRegisterIsAnError() {
        val e = run("100 PV 12 n PMT")
        assertEquals(FinancialError.MISSING_REGISTER, e.error)
    }

    @Test fun inconsistentSignsAreReportedNotCorrected() {
        val e = run("1000 PV 100 PMT 0 FV 5 i n")
        assertEquals(FinancialError.INCONSISTENT_SIGNS, e.error)
        assertClose("1000", e.tvm.pv) // signs untouched
    }

    @Test fun beginAndEndModes() {
        val begin = run("BEGIN 1000 CHS PV 12 n 1 i 0 FV PMT")
        val end = run("END 1000 CHS PV 12 n 1 i 0 FV PMT")
        assertTrue(begin.tvm.beginMode)
        assertClose("88.848788678341707339987831227886", end.x, "1E-25")
        assertClose("87.969097701328423108898842799887", begin.x, "1E-25") // end payment / 1.01
    }

    @Test fun clearFinancialResetsRegistersButKeepsTheStack() {
        val e = run("1000 CHS PV 12 n CLEAR_FINANCIAL")
        assertNull(e.tvm.pv)
        assertNull(e.tvm.n)
        assertClose("12", e.x)
    }

    @Test fun recallFinancialRegisters() {
        val e = run("1000 CHS PV CLEAR_STACK RCL_PV")
        assertX("-1000", e)
    }

    // ---- cash flows through the keyboard --------------------------------------------------------------
    @Test fun npvFromKeys() {
        val e = run("1000 CHS CF0 300 CFJ 400 CFJ 500 CFJ 10 i NPV")
        assertClose("-21.03681442524417731029", e.x, "1E-12")
    }

    @Test fun irrFromKeys() {
        val e = run("1000 CHS CF0 300 CFJ 400 CFJ 500 CFJ IRR")
        assertClose("8.896339469335", e.x, "1E-9")
    }

    @Test fun frequencyKeyRepeatsTheLastFlow() {
        val e = run("5000 CHS CF0 1500 CFJ 4 NJ 8 i NPV")
        assertClose("-31.80973993350155520556", e.x, "1E-12")
    }

    @Test fun njBeforeAnyCashFlowIsAnError() = assertEquals(FinancialError.MISSING_REGISTER, run("3 NJ").error)

    @Test fun irrWithoutFlowsIsAnError() = assertEquals(FinancialError.EMPTY_CASH_FLOWS, run("IRR").error)

    @Test fun npvWithoutRateIsAnError() = assertEquals(FinancialError.MISSING_REGISTER, run("100 CHS CF0 50 CFJ NPV").error)

    // ---- amortization through the keyboard -----------------------------------------------------------------
    @Test fun amortFromKeys() {
        // loan 10000 received, 888.49 paid per month, 1% per month; first 6 periods
        val e = run("10000 PV 888.49 CHS PMT 1 i 6 AMORT")
        assertClose("-480.14", e.x)      // interest, sign of the payment
        assertClose("-4850.80", e.y)     // principal, sign of the payment
        assertClose("5149.20", e.z)      // balance, sign of PV
    }

    @Test fun amortContinuesFromTheNextPeriod() {
        val e = run("10000 PV 888.49 CHS PMT 1 i 3 AMORT 3 AMORT")
        assertClose("5149.20", e.z) // balance after 6 periods, same as one call of 6
    }

    // ---- snapshot / restore ------------------------------------------------------------------------------------
    @Test fun snapshotRestoreRoundTrip() {
        val e = run("1000 CHS PV 12 n 1 i 5 ENTER 6 STO2")
        val copy = RpnEngine().also { it.restore(e.snapshot()) }
        assertEquals(stack(e), stack(copy))
        assertEquals(e.tvm, copy.tvm)
        assertClose("6", copy.memory[2])
    }

    @Test fun displayValueShowsTheTypedText() {
        val e = run("12.50")
        assertEquals(0, BigDecimal("12.5").compareTo(e.displayValue))
    }

    // ---- the user never sees developer (English) messages --------------------------------------------------
    @Test fun severalIrrsAreReportedAsNumbersNotText() {
        val e = run("100 CHS CF0 230 CFJ 132 CHS CFJ IRR")
        assertEquals(FinancialError.MULTIPLE_SOLUTIONS, e.error)
        assertEquals(2, e.errorCandidates.size)
        assertClose("10", e.errorCandidates[0], "1E-9")
        assertClose("20", e.errorCandidates[1], "1E-9")
    }

    @Test fun candidatesAreClearedByTheNextKey() {
        val e = run("100 CHS CF0 230 CFJ 132 CHS CFJ IRR CLX")
        assertEquals(null, e.error)
        assertTrue(e.errorCandidates.isEmpty())
    }
}
