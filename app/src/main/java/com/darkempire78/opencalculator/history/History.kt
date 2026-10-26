package com.darkempire78.opencalculator.history

import com.google.gson.annotations.SerializedName
import com.darkempire78.opencalculator.util.CalculatorMode
import java.util.UUID

data class History(
    @SerializedName("calculation") var calculation: String,
    @SerializedName("result") var result: String,
    @SerializedName("time") var time: String,
    @SerializedName("id") var id: String = UUID.randomUUID().toString(),
    // Mode that produced the entry. Null for entries saved before multi-mode support.
    @SerializedName("mode") var mode: String? = null
) {
    val origin: CalculatorMode get() = CalculatorMode.fromName(mode)
}
