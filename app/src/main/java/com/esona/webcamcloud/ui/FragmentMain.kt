package com.esona.webcamcloud.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.esona.webcamcloud.R
import com.esona.webcamcloud.data.Settings
import com.esona.webcamcloud.databinding.FragmentMainBinding
import com.esona.webcamcloud.util.Utils
import pub.devrel.easypermissions.EasyPermissions
import java.math.BigInteger
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteOrder


class FragmentMain : Fragment(), EasyPermissions.PermissionCallbacks {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var navController: NavController
    private lateinit var wifiManager: WifiManager
    private lateinit var mHandler: Handler
    lateinit var settings: Settings

    private val PERMS = 111
    private val TAG= FragmentMain::class.java.simpleName


/*
    private val wifiScanReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                scanSuccess()
            } else {
                scanFailure()
            }
        }
    }
*/

    private val wifiReceiver= object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val conMan= context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = conMan.getActiveNetworkInfo()
            mHandler.post {
                if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI){
                    var ip= wifiManager.connectionInfo.ipAddress
                    if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                        ip = Integer.reverseBytes(ip)
                    }

                    val ipByteArray= BigInteger.valueOf(ip.toLong()).toByteArray()
                    val ipAddressString = try {
                        InetAddress.getByAddress(ipByteArray).hostAddress
                    } catch (ex: UnknownHostException) {
                        Log.e(TAG, "Unable to get host address.")
                        null
                    }
                    binding.textViewRtspStatus.text= "rtsp://${settings.login}:${settings.password}@${ipAddressString}:${settings.port}"
                    binding.textViewWifiStatus.text= String.format(getString(R.string.ip_title), ipAddressString)
                }
                else {
                    binding.textViewRtspStatus.text= getString(R.string.rtsp_discon)
                    binding.textViewWifiStatus.text= String.format(getString(R.string.ip_title), getString(R.string.ip_none))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterReceiver(wifiReceiver)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        navController= Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        mHandler= Handler(Looper.getMainLooper())
        Log.i(TAG, "before load")
        settings= Utils.loadSettings(requireContext())

        with(binding){
            textViewWifiPort.text= String.format(getString(R.string.port), settings.port)
            textViewWifiStatus.text= String.format(getString(R.string.ip_title), getString(R.string.ip_none))
            textViewRtspStatus.text= getString(R.string.rtsp_discon)
            switchCamera.isChecked= settings.camera==1
            switchCamera.setOnCheckedChangeListener { _, b ->
                settings.camera= if(b) 1 else 0
                Utils.storeSettings(settings, requireContext())
            }
        }

        requestPermissions()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding= null
    }

    private fun requestPermissions() {
        val perms = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO)

        if (EasyPermissions.hasPermissions(requireContext(), *perms)) connect()
        else {
            EasyPermissions.requestPermissions( this, "This app needs permissions for its work", PERMS, *perms)
        }

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        (activity as MainActivity).showMessage(getString(R.string.error), getString(R.string.perm_error_text),
            View.OnClickListener {
                requireActivity().finish()
            })
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        connect()
    }

    private fun fillFromSettings(){

    }

    private fun connect(){
        wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiver(wifiReceiver, intentFilter)

/*
        val success = wifiManager.startScan()
        if (!success) {
            // scan failure handling
            scanFailure()
        }
*/
    }

/*
    private fun scanSuccess() {
        val results = wifiManager.scanResults

    }

    private fun scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        val results = wifiManager.scanResults

    }
*/
}