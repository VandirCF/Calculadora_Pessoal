package com.darkempire78.opencalculator.util

/**
 * Active calculator mode. The three modes live in the same app and share settings,
 * history and themes. Kept free of Android dependencies so it can be unit tested.
 */
enum class CalculatorMode {
    BASIC,
    SCIENTIFIC,
    FINANCIAL_RPN;

    companion object {
        val DEFAULT = BASIC

        /** Parses a persisted name; unknown or missing values fall back to [DEFAULT]. */
        fun fromName(name: String?): CalculatorMode =
            entries.firstOrNull { it.name == name } ?: DEFAULT

        /**
         * Derives the initial mode from the legacy "scientific mode" preference
         * ([ScientificModeTypes] ordinal), used only until the user picks a mode.
         */
        fun fromLegacyScientificMode(legacyOrdinal: Int): CalculatorMode =
            if (legacyOrdinal == ScientificModeTypes.ACTIVE.ordinal) SCIENTIFIC else BASIC
    }
}
