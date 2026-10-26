package com.darkempire78.opencalculator.util

/** Validates text pasted into the display so only well-formed numbers reach the expression. */
object PastedNumber {
    private const val MAX_LENGTH = 100
    private val numberPattern = Regex("^[+-]?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)$")

    /**
     * Returns the number in display format (app decimal separator, no grouping separators),
     * or null when [raw] is not a plain decimal number. Scientific notation, letters and
     * expressions are rejected on purpose: paste is for entering a single value.
     */
    fun parse(raw: CharSequence?, decimalSeparator: String, groupingSeparator: String): String? {
        if (raw == null) return null
        var text = raw.toString().trim()
            .replace("\u00A0", "").replace("\u202F", "").replace(" ", "")
        if (text.isEmpty() || text.length > MAX_LENGTH) return null

        if (groupingSeparator.isNotEmpty() && groupingSeparator != decimalSeparator) {
            val decimalIndex = text.indexOf(decimalSeparator)
            val integerPart = if (decimalIndex >= 0) text.substring(0, decimalIndex) else text
            val fractionPart = if (decimalIndex >= 0) text.substring(decimalIndex) else ""
            // A grouping mark inside the decimals, or in the wrong place, means the number is written in
            // another convention (e.g. "1,234.5" read with "," as decimal): refuse instead of misreading it.
            if (fractionPart.contains(groupingSeparator)) return null
            if (integerPart.contains(groupingSeparator)) {
                val properlyGrouped = Regex("^[+-]?[0-9]{1,3}(" + Regex.escape(groupingSeparator) + "[0-9]{3})+$")
                if (!properlyGrouped.matches(integerPart)) return null
            }
            text = integerPart.replace(groupingSeparator, "") + fractionPart
        }
        text = text.replace(decimalSeparator, ".")
        if (!numberPattern.matches(text)) return null

        return text.removePrefix("+").replace(".", decimalSeparator)
    }
}
