package com.esona.webcamcloud.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.esona.webcamcloud.R
import com.esona.webcamcloud.data.Settings
import com.esona.webcamcloud.databinding.FragmentMainBinding
import com.esona.webcamcloud.util.Utils
import pub.devrel.easypermissions.EasyPermissions


class FragmentMain : Fragment(), EasyPermissions.PermissionCallbacks {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var navController: NavController
    private lateinit var wifiManager: WifiManager
    lateinit var settings: Settings

    private val PERMS = 111


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
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI){
                val wifiManager = requireContext().applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                binding.textViewRtspStatus.text= "aaaaaaaaaaaaa"
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

        settings= Utils.loadSettings(requireContext())

        with(binding){
            textViewWifiPort.text= String.format(getString(R.string.port), settings.port)
            textViewWifiStatus.text= String.format(getString(R.string.ip_title), getString(R.string.ip_none))
            textViewRtspStatus.text= getString(R.string.rtsp_discon)

            switchCamera.setOnCheckedChangeListener { _, b ->
                settings.camera= if(b) 1 else 0
            }
        }

        wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
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

    private fun connect(){
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