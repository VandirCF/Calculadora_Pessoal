package com.darkempire78.opencalculator

import com.darkempire78.opencalculator.util.PastedNumber
import com.darkempire78.opencalculator.util.SignToggle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputHelpersTest {

    private fun paste(raw: String?, dec: String = ".", grp: String = ",") = PastedNumber.parse(raw, dec, grp)
    private fun toggle(s: String) = SignToggle.toggle(s, ".", ",")

    // ---- paste ----
    @Test fun pasteInteger() = assertEquals("123", paste("123"))
    @Test fun pasteDecimal() = assertEquals("12.5", paste("12.5"))
    @Test fun pasteNegative() = assertEquals("-7", paste(" -7 "))
    @Test fun pastePositiveSignIsDropped() = assertEquals("7", paste("+7"))
    @Test fun pasteWithGrouping() = assertEquals("1234567.89", paste("1,234,567.89"))
    @Test fun pasteWithSpacesAsGrouping() = assertEquals("1234", paste("1 234"))
    @Test fun pasteLeadingDecimalPoint() = assertEquals(".5", paste(".5"))
    @Test fun pasteBrazilianFormat() = assertEquals("1234,5", paste("1.234,5", ",", "."))
    @Test fun pasteRejectsLetters() = assertNull(paste("12abc"))
    @Test fun pasteRejectsExpression() = assertNull(paste("1+2"))
    @Test fun pasteRejectsScientificNotation() = assertNull(paste("1e5"))
    @Test fun pasteRejectsTwoDecimalPoints() = assertNull(paste("1.2.3"))
    @Test fun pasteRejectsEmptyAndNull() { assertNull(paste("")); assertNull(paste("   ")); assertNull(paste(null)) }
    @Test fun pasteRejectsLoneSign() = assertNull(paste("-"))
    @Test fun pasteRejectsTooLong() = assertNull(paste("1".repeat(101)))

    // ---- sign toggle ----
    @Test fun toggleOnlyNumber() = assertEquals("-5", toggle("5"))
    @Test fun toggleNegativeOnlyNumber() = assertEquals("5", toggle("-5"))
    @Test fun toggleAfterMultiply() = assertEquals("2×-5", toggle("2×5"))
    @Test fun toggleBackAfterMultiply() = assertEquals("2×5", toggle("2×-5"))
    @Test fun toggleAfterPlusBecomesMinus() = assertEquals("2-5", toggle("2+5"))
    @Test fun toggleAfterBinaryMinusBecomesPlus() = assertEquals("2+5", toggle("2-5"))
    @Test fun toggleInsideParenthesis() = assertEquals("(-5", toggle("(5"))
    @Test fun toggleBackInsideParenthesis() = assertEquals("(5", toggle("(-5"))
    @Test fun toggleDecimalWithGrouping() = assertEquals("-1,234.5", toggle("1,234.5"))
    @Test fun toggleUnaryMinusAfterOperator() = assertEquals("2×5", toggle("2×-5"))
    @Test fun toggleWithoutTrailingNumberIsNoOp() { assertEquals("", toggle("")); assertEquals("2+", toggle("2+")); assertEquals("(2+3)", toggle("(2+3)")) }
}
