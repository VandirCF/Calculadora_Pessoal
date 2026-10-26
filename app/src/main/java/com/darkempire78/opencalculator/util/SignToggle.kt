package com.darkempire78.opencalculator.util

/**
 * Inverts the sign of the number being typed (the number at the end of the expression).
 *
 * - "5"      -> "-5"      and "-5"   -> "5"
 * - "2×5"    -> "2×-5"    and "2×-5" -> "2×5"
 * - "2+5"    -> "2-5"     and "2-5"  -> "2+5"   (binary operator flips, value is preserved)
 * - "(5"     -> "(-5"     and "(-5"  -> "(5"
 * Expressions that do not end with a number are returned unchanged.
 */
object SignToggle {
    private const val OPERATORS = "×÷+-^"

    fun toggle(expression: String, decimalSeparator: String, groupingSeparator: String): String {
        var start = expression.length
        while (start > 0) {
            val c = expression[start - 1]
            if (c.isDigit() || decimalSeparator.contains(c) || (groupingSeparator.isNotEmpty() && groupingSeparator.contains(c))) start-- else break
        }
        if (start == expression.length) return expression // no trailing number

        val before = expression.substring(0, start)
        val number = expression.substring(start)
        val prev = before.lastOrNull()
        val prevPrev = if (before.length >= 2) before[before.length - 2] else null

        return when {
            prev == null -> "-$number"
            prev == '-' && (prevPrev == null || prevPrev in OPERATORS || prevPrev == '(') ->
                before.dropLast(1) + number // unary minus -> remove
            prev == '-' -> before.dropLast(1) + "+" + number // binary minus -> plus
            prev == '+' -> before.dropLast(1) + "-" + number
            prev in "×÷^(" -> "$before-$number"
            else -> expression // unknown context (e.g. letter, ')' ): do nothing
        }
    }
}
