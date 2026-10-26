package com.darkempire78.opencalculator

import com.darkempire78.opencalculator.util.DecimalSeparatorSetting
import com.darkempire78.opencalculator.util.GroupingSetting
import com.darkempire78.opencalculator.util.SeparatorSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SeparatorSettingsTest {
    private fun resolve(d: DecimalSeparatorSetting, g: GroupingSetting, ld: String = ".", lg: String = ",") =
        SeparatorSettings.resolve(d, g, ld, lg)

    @Test fun autoFollowsTheLocale() {
        assertEquals("." to ",", resolve(DecimalSeparatorSetting.AUTO, GroupingSetting.AUTO))
        assertEquals("," to ".", resolve(DecimalSeparatorSetting.AUTO, GroupingSetting.AUTO, ",", "."))
    }

    @Test fun explicitDecimalCommaWithAutoGroupingOfAnEnglishLocaleSwitchesGroupingToDot() =
        assertEquals("," to ".", resolve(DecimalSeparatorSetting.COMMA, GroupingSetting.AUTO))

    @Test fun commaDecimalWithCommaGroupingSwitchesGroupingToDot() =
        assertEquals("," to ".", resolve(DecimalSeparatorSetting.COMMA, GroupingSetting.COMMA))

    @Test fun dotDecimalWithDotGroupingSwitchesGroupingToComma() =
        assertEquals("." to ",", resolve(DecimalSeparatorSetting.DOT, GroupingSetting.DOT))

    @Test fun localeGroupingEqualToChosenDecimalIsSwitched() =
        // Brazilian locale ("," decimal, "." grouping) with the decimal forced to dot
        assertEquals("." to ",", resolve(DecimalSeparatorSetting.DOT, GroupingSetting.AUTO, ",", "."))

    @Test fun noGrouping() = assertEquals("." to "", resolve(DecimalSeparatorSetting.DOT, GroupingSetting.NONE))

    @Test fun spaceGroupingUsesNoBreakSpace() =
        assertEquals("," to " ", resolve(DecimalSeparatorSetting.COMMA, GroupingSetting.SPACE))

    @Test fun unknownNamesFallBackToAuto() {
        assertEquals(DecimalSeparatorSetting.AUTO, SeparatorSettings.decimalFromName("??"))
        assertEquals(GroupingSetting.AUTO, SeparatorSettings.groupingFromName(null))
        assertEquals(GroupingSetting.SPACE, SeparatorSettings.groupingFromName("SPACE"))
    }

    @Test fun resolvedSeparatorsNeverCollide() {
        for (d in DecimalSeparatorSetting.entries) for (g in GroupingSetting.entries) {
            for ((ld, lg) in listOf("." to "," , "," to ".", "," to " ")) {
                val (dec, grp) = resolve(d, g, ld, lg)
                assertEquals("$d/$g/$ld/$lg", true, grp.isEmpty() || grp != dec)
            }
        }
    }
}
