package com.darkempire78.opencalculator

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darkempire78.opencalculator.activities.MainActivity
import com.darkempire78.opencalculator.activities.currentTheme
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.startsWith
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Essential user flows, run on a device or emulator. They use ids and tags, not translated texts. */
@RunWith(AndroidJUnit4::class)
class EssentialFlowsTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun cleanState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun close() = scenario.close()

    private fun matchesRegex(regex: String): Matcher<String> = object : TypeSafeMatcher<String>() {
        override fun matchesSafely(item: String) = Regex(regex).matches(item)
        override fun describeTo(description: Description) { description.appendText("matches /$regex/") }
    }

    private fun key(name: String) = onView(withTagValue(equalTo("key:$name"))).perform(click())
    private fun keys(vararg names: String) = names.forEach { key(it) }
    private fun typeNumber(digits: String) = digits.forEach { key(it.toString()) }
    private fun stackX() = onView(withTagValue(equalTo(R.string.fin_stack_x)))
    private fun register(name: String) = onView(withTagValue(equalTo(name)))

    // ---- modes ----------------------------------------------------------------------------------------
    @Test
    fun switchingModesShowsTheRightKeyboard() {
        onView(withId(R.id.modeScientificButton)).perform(click())
        onView(withId(R.id.scientistModeRow2)).check(matches(isDisplayed()))

        onView(withId(R.id.modeFinancialButton)).perform(click())
        onView(withTagValue(equalTo("key:ENTER"))).check(matches(isDisplayed()))

        onView(withId(R.id.modeBasicButton)).perform(click())
        onView(withId(R.id.scientistModeRow2)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.sevenButton)).check(matches(isDisplayed()))
    }

    @Test
    fun lastModeIsRestoredWhenTheActivityIsRecreated() {
        onView(withId(R.id.modeFinancialButton)).perform(click())
        scenario.recreate()
        onView(withTagValue(equalTo("key:ENTER"))).check(matches(isDisplayed()))
    }

    // ---- basic ----------------------------------------------------------------------------------------------
    @Test
    fun basicCalculation() {
        onView(withId(R.id.twoButton)).perform(click())
        onView(withId(R.id.addButton)).perform(click())
        onView(withId(R.id.threeButton)).perform(click())
        onView(withId(R.id.resultDisplay)).check(matches(withText("5")))
    }

    @Test
    fun basicDivisionByZeroShowsAnError() {
        onView(withId(R.id.fiveButton)).perform(click())
        onView(withId(R.id.divideButton)).perform(click())
        onView(withId(R.id.zeroButton)).perform(click())
        onView(withId(R.id.equalsButton)).perform(click())
        onView(withId(R.id.resultDisplay)).check(matches(withText(R.string.division_by_0)))
    }

    // ---- scientific ----------------------------------------------------------------------------------------
    @Test
    fun scientificSineOfThirtyDegrees() {
        onView(withId(R.id.modeScientificButton)).perform(click())
        onView(withId(R.id.sineButton)).perform(click())
        onView(withId(R.id.threeButton)).perform(click())
        onView(withId(R.id.zeroButton)).perform(click())
        onView(withId(R.id.resultDisplay)).check(matches(withText(matchesRegex("0[.,]5"))))
    }

    @Test
    fun degreeRadianToggle() {
        onView(withId(R.id.modeScientificButton)).perform(click())
        onView(withId(R.id.degreeTextView)).check(matches(withText(R.string.degree)))
        onView(withId(R.id.degreeButton)).perform(click())
        onView(withId(R.id.degreeTextView)).check(matches(withText(R.string.radian)))
    }

    // ---- financial RPN --------------------------------------------------------------------------------------------
    @Test
    fun rpnSequenceTwoEnterThreePlus() {
        onView(withId(R.id.modeFinancialButton)).perform(click())
        keys("CLx", "2", "ENTER", "3", "+")
        stackX().check(matches(withText(startsWith("5"))))
    }

    @Test
    fun tvmPaymentOfALoan() {
        onView(withId(R.id.modeFinancialButton)).perform(click())
        typeNumber("1000"); keys("CHS", "PV")
        typeNumber("12"); key("n")
        typeNumber("1"); key("i")
        typeNumber("0"); key("FV")
        key("PMT")
        stackX().check(matches(withText(matchesRegex("88[.,]85"))))
        register("PMT").check(matches(withText(containsString("88"))))
    }

    @Test
    fun financialRegistersSurviveRecreation() {
        onView(withId(R.id.modeFinancialButton)).perform(click())
        typeNumber("500"); key("PV")
        scenario.recreate()
        register("PV").check(matches(withText(containsString("500"))))
    }

    @Test
    fun clearingRegistersNeedsConfirmationAndEmptiesThem() {
        onView(withId(R.id.modeFinancialButton)).perform(click())
        typeNumber("500"); key("PV")
        register("PV").check(matches(withText(containsString("500"))))
        keys("2nd", "CLx")
        onView(withText(R.string.fin_confirm_clear_title)).check(matches(isDisplayed()))
        onView(withId(android.R.id.button1)).perform(click())
        register("PV").check(matches(withText("PV")))
    }

    @Test
    fun financialCalculationsAppearInTheHistory() {
        onView(withId(R.id.modeFinancialButton)).perform(click())
        keys("CLx", "2", "ENTER", "3", "+")
        onView(withText(R.string.fin_history)).perform(click())
        onView(withText(containsString("2 + 3"))).check(matches(isDisplayed()))
    }

    // ---- themes -----------------------------------------------------------------------------------------------------------
    private fun applyTheme(option: ThemeOption) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MyPreferences(context).appThemeId = option.id
        scenario.recreate()
    }

    @Test
    fun flamengoIsTheDefaultTheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(ThemeOption.FLAMENGO, Themes(context).getSelected())
        assertEquals(R.style.Theme_Calc_Flamengo, currentTheme)
    }

    @Test
    fun changingTheThemeIsAppliedAfterRecreation() {
        applyTheme(ThemeOption.CAVALIERS)
        assertEquals(R.style.Theme_Calc_Cavaliers, currentTheme)
        applyTheme(ThemeOption.FLAMENGO)
        assertEquals(R.style.Theme_Calc_Flamengo, currentTheme)
    }

    @Test
    fun theSelectedThemeIsRestoredWhenTheAppStartsAgain() {
        applyTheme(ThemeOption.CAVALIERS)
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        assertEquals(R.style.Theme_Calc_Cavaliers, currentTheme)
    }

    @Test
    fun bothThemesShowAllThreeModes() {
        for (option in ThemeOption.entries) {
            applyTheme(option)
            onView(withId(R.id.modeBasicButton)).perform(click())
            onView(withId(R.id.sevenButton)).check(matches(isDisplayed()))
            onView(withId(R.id.modeScientificButton)).perform(click())
            onView(withId(R.id.scientistModeRow2)).check(matches(isDisplayed()))
            onView(withId(R.id.modeFinancialButton)).perform(click())
            onView(withTagValue(equalTo("key:ENTER"))).check(matches(isDisplayed()))
            onView(withId(R.id.themeLogo)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun theMathDoesNotDependOnTheTheme() {
        for (option in ThemeOption.entries) {
            applyTheme(option)
            onView(withId(R.id.modeFinancialButton)).perform(click())
            keys("CLx", "2", "ENTER", "3", "+")
            stackX().check(matches(withText(startsWith("5"))))
        }
    }

    // ---- language and number format ------------------------------------------------------------------------------------------
    @Test
    fun theInterfaceIsInBrazilianPortuguese() {
        scenario.onActivity { activity ->
            val locale = activity.resources.configuration.locales[0]
            assertEquals("pt", locale.language)
            assertEquals("Calculadora financeira RPN", activity.getString(R.string.mode_financial))
        }
        onView(withId(R.id.modeFinancialButton)).check(matches(isDisplayed()))
    }

    @Test
    fun financialValuesUseTheBrazilianFormat() {
        onView(withId(R.id.modeFinancialButton)).perform(click())
        typeNumber("1000"); keys("CHS", "PV")
        register("PV").check(matches(withText(containsString("-1.000,00"))))
    }

    @Test
    fun basicCalculatorUsesTheBrazilianFormat() {
        onView(withId(R.id.oneButton)).perform(click())
        onView(withId(R.id.zeroButton)).perform(click())
        onView(withId(R.id.zeroButton)).perform(click())
        onView(withId(R.id.zeroButton)).perform(click())
        onView(withId(R.id.input)).check(matches(withText("1.000")))
    }
}
