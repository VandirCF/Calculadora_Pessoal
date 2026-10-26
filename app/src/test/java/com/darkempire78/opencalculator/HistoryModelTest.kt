package com.darkempire78.opencalculator

import com.darkempire78.opencalculator.history.History
import com.darkempire78.opencalculator.history.HistoryType
import com.darkempire78.opencalculator.util.CalculatorMode
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** History is stored as JSON in SharedPreferences, so schema changes must keep old data readable. */
class HistoryModelTest {
    private val gson = Gson()

    // JSON written by version 3.2.1 (no mode, type or details)
    private val legacyJson = """[{"calculation":"1+1","result":"2","time":"1700000000000","id":"legacy-1"},
        {"calculation":"sin(30)","result":"0.5","time":"","id":"legacy-2"}]"""

    @Test
    fun legacyEntriesAreStillReadable() {
        val list = gson.fromJson(legacyJson, Array<History>::class.java).toList()
        assertEquals(2, list.size)
        assertEquals("1+1", list[0].calculation)
        assertEquals("legacy-1", list[0].id)
        assertNull(list[0].mode)
        assertNull(list[0].type)
        assertNull(list[0].details)
    }

    @Test
    fun legacyEntriesAreTreatedAsBasic() {
        val entry = gson.fromJson(legacyJson, Array<History>::class.java)[0]
        assertEquals(CalculatorMode.BASIC, entry.origin)
        assertEquals(HistoryType.BASIC, entry.entryType)
    }

    @Test
    fun newEntriesRoundTrip() {
        val original = History("PMT: n=360", "-599.55", "1", "id-1", CalculatorMode.FINANCIAL_RPN.name, HistoryType.TVM.name, "end of period")
        val copy = gson.fromJson(gson.toJson(original), History::class.java)
        assertEquals(original, copy)
        assertEquals(CalculatorMode.FINANCIAL_RPN, copy.origin)
        assertEquals(HistoryType.TVM, copy.entryType)
    }

    @Test
    fun unknownTypeFallsBackToTheTypeOfTheMode() {
        val scientific = History("x", "y", "1", "a", CalculatorMode.SCIENTIFIC.name, "FUTURE_TYPE")
        assertEquals(HistoryType.SCIENTIFIC, scientific.entryType)
        val financial = History("x", "y", "1", "b", CalculatorMode.FINANCIAL_RPN.name, null)
        assertEquals(HistoryType.RPN_OPERATION, financial.entryType)
    }

    @Test
    fun unknownModeFallsBackToBasic() {
        val entry = History("x", "y", "1", "c", "SOMETHING_NEW")
        assertEquals(CalculatorMode.BASIC, entry.origin)
    }

    @Test
    fun everyHistoryTypeBelongsToAMode() {
        HistoryType.entries.forEach { assertNotNull(it.mode) }
        assertEquals(CalculatorMode.FINANCIAL_RPN, HistoryType.AMORTIZATION.mode)
        assertEquals(CalculatorMode.SCIENTIFIC, HistoryType.SCIENTIFIC.mode)
    }

    @Test
    fun corruptedJsonIsRejectedByGsonSoPreferencesFallBackToEmpty() {
        // MyPreferences.getHistory() wraps parsing in try/catch and returns an empty list
        val result = try {
            gson.fromJson("{not json", Array<History>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
        assertEquals(0, result.size)
    }
}
