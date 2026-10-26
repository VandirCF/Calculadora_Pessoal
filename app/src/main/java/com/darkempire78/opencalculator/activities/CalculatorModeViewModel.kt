package com.darkempire78.opencalculator.activities

import androidx.lifecycle.ViewModel
import com.darkempire78.opencalculator.util.CalculatorMode

/** Holds the active mode so it survives configuration changes (rotation, theme, locale). */
class CalculatorModeViewModel : ViewModel() {
    var mode: CalculatorMode? = null
}
