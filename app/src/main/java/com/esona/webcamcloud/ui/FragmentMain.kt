package com.esona.webcamcloud.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.esona.webcamcloud.R
import com.esona.webcamcloud.data.BaseEvent
import com.esona.webcamcloud.data.EventEnum
import com.esona.webcamcloud.data.Settings
import com.esona.webcamcloud.databinding.FragmentMainBinding
import com.esona.webcamcloud.util.Utils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.math.BigInteger
import java.net.InetAddress
import java.net.UnknownHostException


class FragmentMain : Fragment(){

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private var ip: Int= 0
    private lateinit var navController: NavController
    private lateinit var mHandler: Handler
    lateinit var settings: Settings

    private val TAG= FragmentMain::class.java.simpleName

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(ev: BaseEvent){
        if(ev.type== EventEnum.CONNECTION){
            ip= ev.bundle.getInt("ip")
            fillConnectionFields()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        navController= Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        mHandler= Handler(Looper.getMainLooper())
        Log.i(TAG, "before load")
        settings= Utils.loadSettings(requireContext())

        with(binding){
            textViewWifiPort.text= String.format(getString(R.string.port), settings.port)
            fillConnectionFields()
            switchCamera.isChecked= settings.camera==1

            switchCamera.setOnCheckedChangeListener { _, b ->
                settings.camera= if(b) 1 else 0
                Utils.storeSettings(settings, requireContext())
                Utils.sendSettings(settings)
            }
        }
        EventBus.getDefault().register(this)
        return binding.root
    }

    override fun onDestroyView() {
        EventBus.getDefault().unregister(this)
        super.onDestroyView()
        _binding= null
    }


    private fun fillConnectionFields(){
        if(ip!= 0){
            val ipByteArray= BigInteger.valueOf(ip.toLong()).toByteArray()
            val ipAddressString = try {
                InetAddress.getByAddress(ipByteArray).hostAddress
            } catch (ex: UnknownHostException) {
                Log.e(TAG, "Unable to get host address.")
                null
            }
            with(binding){
                textViewRtspStatus.text= "rtsp://${settings.login}:${settings.password}@${ipAddressString}:${settings.port}"
                textViewWifiStatus.text= String.format(getString(R.string.ip_title), ipAddressString)
                textViewWifi.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(requireContext(), R.drawable.ic_wifi_24px),
                    null, null, null)
            }
        }
        else{
            with(binding){
                textViewRtspStatus.text= getString(R.string.rtsp_discon)
                textViewWifiStatus.text= String.format(getString(R.string.ip_title), getString(R.string.ip_none))
                textViewWifi.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(requireContext(), R.drawable.ic_wifi_off_24px),
                    null, null, null)
            }
        }
    }

}