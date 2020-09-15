package com.esona.webcamcloud.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import com.esona.webcamcloud.data.BaseEvent
import com.esona.webcamcloud.data.EventEnum
import com.esona.webcamcloud.data.Settings
import org.greenrobot.eventbus.EventBus
import java.util.*

object Utils {

    fun loadSettings(context: Context) : Settings {
        val prefs= context.getSharedPreferences("settings", 0)
        val res= Settings()
        res.login= prefs.getString("login", "admin")!!
        res.password= prefs.getString("password", "admin")!!
        res.resolution= prefs.getInt("resolution", -1)
        res.lang= prefs.getInt("lang", if(Locale.getDefault().language == "ru") 1 else 0)
        res.port= prefs.getInt("port", 1935)
        res.rate= prefs.getInt("rate", 30)
        res.camera= prefs.getInt("camera", 0)
        return res
    }

    fun storeSettings(data: Settings, context: Context) {
        val prefs= context.getSharedPreferences("settings", 0).edit()
        prefs.putString("login", data.login)
        prefs.putString("password", data.password)
        prefs.putInt("resolution", data.resolution)
        prefs.putInt("rate", data.rate)
        prefs.putInt("lang", data.lang)
        prefs.putInt("port", data.port)
        prefs.putInt("camera", data.camera)
        prefs.commit()
    }

    fun keyExists(key: String, context: Context): Boolean{
        val prefs= context.getSharedPreferences("data", 0)
        return prefs.contains(key)
    }

    fun storeSet(data: Set<String>, key: String, context: Context){
        val prefs= context.getSharedPreferences("settings", 0).edit()
        prefs.putStringSet(key, data)
        prefs.commit()
    }

    fun loadSet(key:String, context: Context) : Set<String>? {
        val prefs= context.getSharedPreferences("settings", 0)
        return prefs.getStringSet(key, null)
    }

    fun sendSettings(settings: Settings){
        val bundle= Bundle()
        bundle.putParcelable("settings", settings)
        val ev= BaseEvent(EventEnum.SETTINGS, bundle)
        EventBus.getDefault().post(ev)
    }

    fun sendStream(stream: Boolean){
        val b= Bundle()
        b.putBoolean("stream", stream)
        EventBus.getDefault().post(BaseEvent(EventEnum.STREAM, b))
    }

    fun sendConnString(ip: Int){
        val b= Bundle()
        b.putInt("ip", ip)
        EventBus.getDefault().post(BaseEvent(EventEnum.CONNECTION, b))
    }

    fun applyLang(context: Context, lang: String) : Context{
        val locale= Locale(lang)
        val config= Configuration(context.resources.configuration)
        Locale.setDefault(locale)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun applyLang2(context: Context, lang: String) {
        val locale= Locale(lang)
        val config= Configuration(context.resources.configuration)
        Locale.setDefault(locale)
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun loadBoolean(context: Context, key: String) : Boolean {
        val prefs= context.getSharedPreferences("data", 0)
        return prefs.getBoolean(key, false)
    }

    fun storeBoolean(context: Context, key: String, data: Boolean) {
        val prefs= context.getSharedPreferences("data", 0).edit()
        prefs.putBoolean(key, data)
        prefs.commit()
    }

    fun loadInt(context: Context, key: String) : Int {
        val prefs= context.getSharedPreferences("data", 0)
        return prefs.getInt(key, 0)
    }

    fun storeInt(context: Context, key: String, data: Int) {
        val prefs= context.getSharedPreferences("data", 0).edit()
        prefs.putInt(key, data)
        prefs.commit()
    }

}