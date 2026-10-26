package com.darkempire78.opencalculator.util

object ScientificMode {
    fun getScientificModeType(type:Int):ScientificModeTypes{
        return when (type) {
            ScientificModeTypes.NOT_ACTIVE.ordinal -> ScientificModeTypes.NOT_ACTIVE
            ScientificModeTypes.ACTIVE.ordinal -> ScientificModeTypes.ACTIVE
            else -> ScientificModeTypes.OFF
        }
    }
}
enum class ScientificModeTypes{
    NOT_ACTIVE, //0
    ACTIVE,//1
    OFF //2
}