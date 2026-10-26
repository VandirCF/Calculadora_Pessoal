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
            text = text.replace(groupingSeparator, "")
        }
        text = text.replace(decimalSeparator, ".")
        if (!numberPattern.matches(text)) return null

        return text.removePrefix("+").replace(".", decimalSeparator)
    }
}
