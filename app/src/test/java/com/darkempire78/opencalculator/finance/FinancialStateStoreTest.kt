package com.darkempire78.opencalculator.finance

import com.darkempire78.opencalculator.finance.rpn.RpnEngine
import com.darkempire78.opencalculator.finance.rpn.RpnEventType
import com.darkempire78.opencalculator.financial.FinancialStateStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialStateStoreTest {

    private fun engineWithState() = RpnEngine().also {
        it.keys("1000 CHS PV 12 n 1 i 0 FV PMT 5 ENTER 6 STO3 300 CF0 40 CFJ 3 NJ")
        it.periodsPerYear = bd("4")
    }

    @Test
    fun stateSurvivesEncodeAndDecode() {
        val original = engineWithState()
        val json = FinancialStateStore.encode(original)
        val restored = RpnEngine()
        assertTrue(FinancialStateStore.decodeInto(restored, json))
        assertEquals(original.tvm, restored.tvm)
        assertEquals(0, original.x.compareTo(restored.x))
        assertEquals(0, original.y.compareTo(restored.y))
        assertEquals(0, original.memory[3].compareTo(restored.memory[3]))
        assertEquals(original.cashFlows, restored.cashFlows)
        assertEquals(0, bd("4").compareTo(restored.periodsPerYear))
    }

    @Test
    fun precisionIsNotLostInPersistence() {
        val original = RpnEngine().also { it.keys("1000 CHS PV 12 n 1 i 0 FV PMT") }
        val restored = RpnEngine()
        FinancialStateStore.decodeInto(restored, FinancialStateStore.encode(original))
        assertEquals(original.tvm.pmt, restored.tvm.pmt) // all 34 digits
    }

    @Test
    fun restoredEngineKeepsWorking() {
        val restored = RpnEngine()
        FinancialStateStore.decodeInto(restored, FinancialStateStore.encode(engineWithState()))
        restored.keys("CLEAR_STACK 2 ENTER 3 +")
        assertEquals(0, bd("5").compareTo(restored.x))
    }

    @Test
    fun blankOrMissingDataIsIgnored() {
        val engine = RpnEngine()
        assertFalse(FinancialStateStore.decodeInto(engine, null))
        assertFalse(FinancialStateStore.decodeInto(engine, ""))
        assertFalse(FinancialStateStore.decodeInto(engine, "   "))
    }

    @Test
    fun corruptedJsonIsIgnoredAndLeavesTheEngineUntouched() {
        val engine = RpnEngine().also { it.keys("42") }
        assertFalse(FinancialStateStore.decodeInto(engine, "{not json"))
        assertFalse(FinancialStateStore.decodeInto(engine, "[1,2,3]"))
        assertEquals(0, bd("42").compareTo(engine.x))
    }

    @Test
    fun otherVersionsAreRejected() {
        val json = FinancialStateStore.encode(engineWithState()).replace("\"version\":1", "\"version\":99")
        assertFalse(FinancialStateStore.decodeInto(RpnEngine(), json))
    }

    @Test
    fun snapshotsWithMissingMandatoryFieldsAreRejected() {
        assertFalse(FinancialStateStore.decodeInto(RpnEngine(), "{\"version\":1,\"state\":{\"x\":1}}"))
        assertFalse(FinancialStateStore.decodeInto(RpnEngine(), "{\"version\":1}"))
    }

    // ---- events emitted for the history ---------------------------------------------------------------
    @Test
    fun arithmeticEmitsAnOperationEvent() {
        val e = RpnEngine().also { it.keys("2 ENTER 3 +") }
        val event = e.lastEvent
        assertNotNull(event)
        assertEquals(RpnEventType.OPERATION, event!!.type)
        assertEquals("+", event.label)
        assertEquals(0, bd("5").compareTo(event.result))
    }

    @Test
    fun tvmComputationEmitsATvmEventWithItsInputs() {
        val e = RpnEngine().also { it.keys("1000 CHS PV 12 n 1 i 0 FV PMT") }
        val event = e.lastEvent!!
        assertEquals(RpnEventType.TVM, event.type)
        assertEquals("PMT", event.label)
        assertEquals(listOf("n", "i", "PV", "FV"), event.inputs.map { it.first })
    }

    @Test
    fun eventsAreNotEmittedForPlainEntryOrErrors() {
        assertNull(RpnEngine().also { it.keys("5 ENTER") }.lastEvent)
        assertNull(RpnEngine().also { it.keys("5 ENTER 0 /") }.lastEvent)
        assertNull(RpnEngine().also { it.keys("100 PV") }.lastEvent) // storing a register is not a calculation
    }

    @Test
    fun npvIrrAndAmortizationEmitTheirEvents() {
        val npv = RpnEngine().also { it.keys("1000 CHS CF0 300 CFJ 400 CFJ 500 CFJ 10 i NPV") }.lastEvent!!
        assertEquals(RpnEventType.NPV, npv.type)
        val irr = RpnEngine().also { it.keys("1000 CHS CF0 300 CFJ 400 CFJ 500 CFJ IRR") }.lastEvent!!
        assertEquals(RpnEventType.IRR, irr.type)
        val amort = RpnEngine().also { it.keys("10000 PV 888.49 CHS PMT 1 i 6 AMORT") }.lastEvent!!
        assertEquals(RpnEventType.AMORTIZATION, amort.type)
        assertEquals(2, amort.extra.size)
    }
}
