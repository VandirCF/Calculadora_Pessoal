package com.darkempire78.opencalculator

import com.darkempire78.opencalculator.util.CalculatorMode
import com.darkempire78.opencalculator.util.ScientificModeTypes
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorModeTest {

    @Test
    fun fromName_roundTripsEveryMode() {
        CalculatorMode.entries.forEach {
            assertEquals(it, CalculatorMode.fromName(it.name))
        }
    }

    @Test
    fun fromName_unknownOrMissingFallsBackToDefault() {
        assertEquals(CalculatorMode.BASIC, CalculatorMode.fromName(null))
        assertEquals(CalculatorMode.BASIC, CalculatorMode.fromName(""))
        assertEquals(CalculatorMode.BASIC, CalculatorMode.fromName("HP12C"))
    }

    @Test
    fun legacyScientificMode_activeMapsToScientific() {
        assertEquals(CalculatorMode.SCIENTIFIC, CalculatorMode.fromLegacyScientificMode(ScientificModeTypes.ACTIVE.ordinal))
    }

    @Test
    fun legacyScientificMode_otherValuesMapToBasic() {
        assertEquals(CalculatorMode.BASIC, CalculatorMode.fromLegacyScientificMode(ScientificModeTypes.NOT_ACTIVE.ordinal))
        assertEquals(CalculatorMode.BASIC, CalculatorMode.fromLegacyScientificMode(ScientificModeTypes.OFF.ordinal))
        assertEquals(CalculatorMode.BASIC, CalculatorMode.fromLegacyScientificMode(99))
    }
}
