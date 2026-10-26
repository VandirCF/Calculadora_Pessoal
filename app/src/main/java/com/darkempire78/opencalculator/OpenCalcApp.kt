package com.darkempire78.opencalculator

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

class OpenCalcApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // The interface exists only in Brazilian Portuguese, whatever the device language is.
        // Numbers, dates and library texts follow it too.
        Locale.setDefault(BRAZIL)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("pt-BR"))
    }

    companion object {
        val BRAZIL: Locale = Locale("pt", "BR")
    }
}
