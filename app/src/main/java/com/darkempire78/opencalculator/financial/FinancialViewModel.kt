package com.darkempire78.opencalculator.financial

import androidx.lifecycle.ViewModel
import com.darkempire78.opencalculator.finance.rpn.RpnEngine

/** What the next digit key means after STO or RCL. */
enum class PendingMemory { NONE, STORE, RECALL }

/** Which shifted layer the next key press uses: the gold f functions (above the keys) or the blue g functions (on the keys). */
enum class ShiftLayer { NONE, F, G }

/**
 * Holds the financial calculator state so it survives configuration changes. The engine is
 * additionally persisted by [FinancialStateStore] to survive process death and app restarts.
 */
class FinancialViewModel : ViewModel() {
    val engine = RpnEngine()
    var shift = ShiftLayer.NONE
    var pendingMemory = PendingMemory.NONE
    var restored = false
}
