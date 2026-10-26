package com.darkempire78.opencalculator.util

enum class DecimalSeparatorSetting { AUTO, DOT, COMMA }

enum class GroupingSetting { AUTO, COMMA, DOT, SPACE, NONE }

/**
 * Resolves the decimal and thousands separators from the user settings and the locale.
 * Pure Kotlin so the rules are unit tested.
 */
object SeparatorSettings {
    private const val NO_BREAK_SPACE = " "

    fun decimalFromName(name: String?) = DecimalSeparatorSetting.entries.firstOrNull { it.name == name } ?: DecimalSeparatorSetting.AUTO
    fun groupingFromName(name: String?) = GroupingSetting.entries.firstOrNull { it.name == name } ?: GroupingSetting.AUTO

    /**
     * Returns (decimal separator, grouping separator). The grouping separator may be empty
     * (no grouping). The two never coincide: when they would, the grouping separator is
     * replaced by the other common symbol, because the same character cannot mean both.
     */
    fun resolve(
        decimal: DecimalSeparatorSetting,
        grouping: GroupingSetting,
        localeDecimal: String,
        localeGrouping: String
    ): Pair<String, String> {
        val decimalSymbol = when (decimal) {
            DecimalSeparatorSetting.AUTO -> localeDecimal
            DecimalSeparatorSetting.DOT -> "."
            DecimalSeparatorSetting.COMMA -> ","
        }
        var groupingSymbol = when (grouping) {
            GroupingSetting.AUTO -> localeGrouping
            GroupingSetting.COMMA -> ","
            GroupingSetting.DOT -> "."
            GroupingSetting.SPACE -> NO_BREAK_SPACE
            GroupingSetting.NONE -> ""
        }
        if (groupingSymbol.isNotEmpty() && groupingSymbol == decimalSymbol) {
            groupingSymbol = if (decimalSymbol == ".") "," else "."
        }
        return decimalSymbol to groupingSymbol
    }
}
