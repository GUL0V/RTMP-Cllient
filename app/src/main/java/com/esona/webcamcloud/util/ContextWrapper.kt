package com.esona.webcamcloud.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.*

class ContextWrapper(base: Context?) : android.content.ContextWrapper(base) {
    companion object {
        fun wrap(context: Context, newLocale: Locale?): ContextWrapper {
            var contextRes: Context = context
            val res: Resources = context.resources
            val configuration = Configuration(res.configuration)
            if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.N) {
                configuration.setLocale(newLocale)
                val localeList = LocaleList(newLocale)
                LocaleList.setDefault(localeList)
                configuration.setLocales(localeList)
                contextRes = context.createConfigurationContext(configuration)
            } else  {
                Locale.setDefault(newLocale)
                configuration.setLocale(newLocale)
                contextRes = context.createConfigurationContext(configuration)
            }
            return ContextWrapper(contextRes)
        }
    }
}