package com.darkempire78.opencalculator.activities

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import com.darkempire78.opencalculator.OpenCalcApp

/**
 * Every screen of the app is Brazilian Portuguese, whatever the language of the device is: texts,
 * library texts (dialog buttons, accessibility), dates ("Hoje", "Ontem") and number symbols.
 * The context handed to the activity is given the pt-BR locale before any resource is read.
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(brazilian(newBase))
    }

    /** AppCompat re-applies its own configuration over the wrapper: keep the locale on that path too. */
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        overrideConfiguration?.setLocale(OpenCalcApp.BRAZIL)
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    companion object {
        fun brazilian(base: Context): Context {
            val configuration = Configuration(base.resources.configuration)
            configuration.setLocale(OpenCalcApp.BRAZIL)
            configuration.setLayoutDirection(OpenCalcApp.BRAZIL)
            return base.createConfigurationContext(configuration)
        }
    }
}
