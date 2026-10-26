package com.darkempire78.opencalculator.financial

import com.darkempire78.opencalculator.finance.rpn.RpnEngine
import com.darkempire78.opencalculator.finance.rpn.RpnSnapshot
import com.google.gson.Gson

/**
 * Serializes the engine state as versioned JSON.
 *
 * Decision (documented in docs/ARCHITECTURE.md): financial registers persist across app restarts so
 * a calculation is not lost by closing the app; the user clears them explicitly from the menu
 * (or with 2nd + CLx). Unreadable or newer-version data is ignored and the engine starts empty.
 */
object FinancialStateStore {
    const val CURRENT_VERSION = 1
    private val gson = Gson()

    private data class Envelope(val version: Int = CURRENT_VERSION, val state: RpnSnapshot? = null)

    fun encode(engine: RpnEngine): String = gson.toJson(Envelope(CURRENT_VERSION, engine.snapshot()))

    /** Returns true when [json] was valid and the engine was restored. */
    fun decodeInto(engine: RpnEngine, json: String?): Boolean {
        if (json.isNullOrBlank()) return false
        return try {
            val envelope = gson.fromJson(json, Envelope::class.java)
            val state = envelope?.state
            if (envelope == null || envelope.version != CURRENT_VERSION || state == null) return false
            // Gson bypasses constructors: reject snapshots with missing mandatory fields
            if (listOf<Any?>(state.x, state.y, state.z, state.t, state.lastX, state.memory, state.tvm,
                    state.periodsPerYear, state.cashFlowEntries, state.cashFlow0).any { it == null }) return false
            engine.restore(state)
            true
        } catch (e: Exception) {
            false
        }
    }
}
