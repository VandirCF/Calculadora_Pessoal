package com.darkempire78.opencalculator.finance

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Display formatting for the financial calculator. Presentation only: values stored in the
 * engine keep full precision; rounding here uses HALF_EVEN and never feeds back into results.
 */
object RpnFormatter {
    /** Values at or above this magnitude, or non-zero values that round to zero, use scientific notation. */
    private val SCIENTIFIC_LIMIT = BigDecimal("1E15")

    fun format(
        value: BigDecimal,
        scale: Int,
        decimalSeparator: String,
        groupingSeparator: String,
        grouping: Boolean = true
    ): String {
        val rounded = value.setScale(scale.coerceIn(0, 12), RoundingMode.HALF_EVEN)
        if (value.abs() >= SCIENTIFIC_LIMIT || (value.signum() != 0 && rounded.signum() == 0)) {
            return scientific(value, decimalSeparator)
        }
        val plain = rounded.abs().toPlainString()
        val parts = plain.split('.')
        val integerPart = if (grouping) group(parts[0], groupingSeparator) else parts[0]
        val fraction = if (parts.size > 1) decimalSeparator + parts[1] else ""
        val sign = if (rounded.signum() < 0) "-" else ""
        return sign + integerPart + fraction
    }

    /** Text being typed: keeps trailing decimal point and zeros, groups only the integer digits. */
    fun formatEntry(entry: String, decimalSeparator: String, groupingSeparator: String, grouping: Boolean = true): String {
        val negative = entry.startsWith("-")
        val unsigned = entry.removePrefix("-")
        val parts = unsigned.split('.')
        val integerPart = if (grouping) group(parts[0], groupingSeparator) else parts[0]
        val fraction = if (unsigned.contains('.')) decimalSeparator + parts.getOrElse(1) { "" } else ""
        return (if (negative) "-" else "") + integerPart + fraction
    }

    private fun scientific(value: BigDecimal, decimalSeparator: String): String =
        String.format(Locale.ROOT, "%.6E", value).replace(".", decimalSeparator)

    private fun group(digits: String, separator: String): String {
        if (separator.isEmpty() || digits.length <= 3) return digits
        val sb = StringBuilder()
        val first = digits.length % 3
        if (first > 0) sb.append(digits, 0, first)
        var i = first
        while (i < digits.length) {
            if (sb.isNotEmpty()) sb.append(separator)
            sb.append(digits, i, i + 3)
            i += 3
        }
        return sb.toString()
    }
}
