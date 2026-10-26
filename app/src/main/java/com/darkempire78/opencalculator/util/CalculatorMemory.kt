package com.darkempire78.opencalculator.util

import java.math.BigDecimal

/** Single-register calculator memory (M+, M−, MR, MC). Pure Kotlin so it is unit testable. */
class CalculatorMemory(initial: BigDecimal? = null) {
    var value: BigDecimal? = initial
        private set

    val hasValue: Boolean get() = value != null

    fun add(amount: BigDecimal) {
        value = (value ?: BigDecimal.ZERO).add(amount)
    }

    fun subtract(amount: BigDecimal) {
        value = (value ?: BigDecimal.ZERO).subtract(amount)
    }

    fun clear() {
        value = null
    }

    /** Recalled value, or null when the memory is empty. */
    fun recall(): BigDecimal? = value

    companion object {
        /** Restores a persisted value; invalid or blank text yields an empty memory. */
        fun fromStored(text: String?): CalculatorMemory =
            CalculatorMemory(text?.takeIf { it.isNotBlank() }?.toBigDecimalOrNull())
    }
}
