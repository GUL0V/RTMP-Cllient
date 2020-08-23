package com.esona.webcamcloud.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.util.Log
import java.nio.ByteOrder

class WifiMonitor (val listener: StateChanged){
    private val TAG= WifiMonitor::class.java.simpleName

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager

    private var networkRequest: NetworkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()

    private var callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            var ip= wifiManager.connectionInfo.ipAddress
            if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                ip = Integer.reverseBytes(ip)
            }
            listener.onChanged(true, ip)
            Log.i(TAG, "onAvailable")
        }

        override fun onUnavailable() {
            Log.i(TAG, "onUnavailable")
            listener.onChanged(false, 0)
        }

        override fun onLost(network: Network) {
            Log.i(TAG, "onLost")
            listener.onChanged(false, 0)
        }
    }

    fun enable(context: Context) {
        wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerNetworkCallback(networkRequest, callback)
    }

    fun disable(){
        connectivityManager.unregisterNetworkCallback(callback)
    }

    interface StateChanged{
        fun onChanged(available: Boolean, ip: Int)
    }
}
