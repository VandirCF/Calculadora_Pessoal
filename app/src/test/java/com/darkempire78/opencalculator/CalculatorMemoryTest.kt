package com.darkempire78.opencalculator

import com.darkempire78.opencalculator.util.CalculatorMemory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CalculatorMemoryTest {
    @Test fun startsEmpty() { val m = CalculatorMemory(); assertFalse(m.hasValue); assertNull(m.recall()) }

    @Test fun addToEmptyMemory() {
        val m = CalculatorMemory(); m.add(BigDecimal("5"))
        assertEquals(0, BigDecimal("5").compareTo(m.recall()))
    }

    @Test fun addAndSubtractAreExactDecimals() {
        val m = CalculatorMemory(); m.add(BigDecimal("0.1")); m.add(BigDecimal("0.2")); m.subtract(BigDecimal("0.05"))
        assertEquals(0, BigDecimal("0.25").compareTo(m.recall()))
    }

    @Test fun subtractFromEmptyGoesNegative() {
        val m = CalculatorMemory(); m.subtract(BigDecimal("3"))
        assertEquals(0, BigDecimal("-3").compareTo(m.recall()))
    }

    @Test fun clearEmptiesMemory() {
        val m = CalculatorMemory(BigDecimal.TEN); m.clear()
        assertFalse(m.hasValue)
    }

    @Test fun restoreFromStoredText() {
        assertEquals(0, BigDecimal("12.5").compareTo(CalculatorMemory.fromStored("12.5").recall()))
        assertTrue(!CalculatorMemory.fromStored(null).hasValue)
        assertTrue(!CalculatorMemory.fromStored("").hasValue)
        assertTrue(!CalculatorMemory.fromStored("abc").hasValue)
    }
}
