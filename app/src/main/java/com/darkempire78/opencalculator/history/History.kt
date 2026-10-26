package com.darkempire78.opencalculator.history

import com.darkempire78.opencalculator.util.CalculatorMode
import com.google.gson.annotations.SerializedName
import java.util.UUID

/** What produced a history entry. Entries saved before multi-mode support have no type. */
enum class HistoryType(val mode: CalculatorMode) {
    BASIC(CalculatorMode.BASIC),
    SCIENTIFIC(CalculatorMode.SCIENTIFIC),
    RPN_OPERATION(CalculatorMode.FINANCIAL_RPN),
    TVM(CalculatorMode.FINANCIAL_RPN),
    NPV(CalculatorMode.FINANCIAL_RPN),
    IRR(CalculatorMode.FINANCIAL_RPN),
    AMORTIZATION(CalculatorMode.FINANCIAL_RPN),
    RATE_CONVERSION(CalculatorMode.FINANCIAL_RPN);

    companion object {
        fun fromName(name: String?): HistoryType? = entries.firstOrNull { it.name == name }
    }
}

data class History(
    @SerializedName("calculation") var calculation: String,
    @SerializedName("result") var result: String,
    @SerializedName("time") var time: String,
    @SerializedName("id") var id: String = UUID.randomUUID().toString(),
    // Mode that produced the entry. Null for entries saved before multi-mode support.
    @SerializedName("mode") var mode: String? = null,
    // Kind of calculation. Null for entries saved before multi-mode support.
    @SerializedName("type") var type: String? = null,
    // Relevant settings or units (payment mode, periodicity, ...). Null when not applicable.
    @SerializedName("details") var details: String? = null
) {
    val origin: CalculatorMode get() = CalculatorMode.fromName(mode)

    val entryType: HistoryType
        get() = HistoryType.fromName(type) ?: when (origin) {
            CalculatorMode.SCIENTIFIC -> HistoryType.SCIENTIFIC
            CalculatorMode.FINANCIAL_RPN -> HistoryType.RPN_OPERATION
            CalculatorMode.BASIC -> HistoryType.BASIC
        }
}
