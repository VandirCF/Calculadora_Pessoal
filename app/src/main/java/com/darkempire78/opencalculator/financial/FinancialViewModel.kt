package com.darkempire78.opencalculator.financial

import androidx.lifecycle.ViewModel
import com.darkempire78.opencalculator.finance.rpn.RpnEngine

/** What the next digit key means after STO or RCL. */
enum class PendingMemory { NONE, STORE, RECALL }

/**
 * Holds the financial calculator state so it survives configuration changes. The engine is
 * additionally persisted by [FinancialStateStore] to survive process death and app restarts.
 */
class FinancialViewModel : ViewModel() {
    val engine = RpnEngine()
    var secondActive = false
    var pendingMemory = PendingMemory.NONE
    var restored = false
}
