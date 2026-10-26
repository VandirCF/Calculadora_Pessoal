package com.darkempire78.opencalculator

import android.app.Application
import java.util.Locale

class OpenCalcApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // The interface exists only in Brazilian Portuguese, whatever the device language is.
        // Numbers, dates and library texts follow it too.
        // (activities also receive a pt-BR context, see BaseActivity)
        Locale.setDefault(BRAZIL)
    }

    companion object {
        val BRAZIL: Locale = Locale("pt", "BR")
    }
}
