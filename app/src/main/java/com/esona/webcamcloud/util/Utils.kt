package com.esona.webcamcloud.util

import android.content.Context
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
        res.resolution= prefs.getInt("resolution", 0)
        res.lang= prefs.getInt("lang", 0)
        res.port= prefs.getInt("port", 1935)
        res.rate= prefs.getInt("rate", 15)
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
        val config= context.resources.configuration
//        val dispMetics= context.resources.displayMetrics
        Locale.setDefault(locale)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun loadBoolean(context: Context, key: String) : Boolean {
        val prefs= context.getSharedPreferences("data", 0)
        return prefs.getBoolean("key", false)
    }

    fun storeBoolean(context: Context, key: String, data: Boolean) {
        val prefs= context.getSharedPreferences("data", 0).edit()
        prefs.putBoolean(key, data)
    }


}