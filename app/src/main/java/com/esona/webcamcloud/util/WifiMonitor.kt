package com.esona.webcamcloud.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

class WifiMonitor {
    private val TAG= WifiMonitor::class.java.simpleName

    private lateinit var connectivityManager: ConnectivityManager

    private var networkRequest: NetworkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()

    private var callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.i(TAG, "onAvailable")
        }

        override fun onUnavailable() {
            Log.i(TAG, "onUnavailable")
        }

        override fun onLost(network: Network) {
            Log.i(TAG, "onLost")
        }
    }

    fun enable(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerNetworkCallback(networkRequest, callback)
    }

    fun disable(){
        connectivityManager.unregisterNetworkCallback(callback)
    }
}
