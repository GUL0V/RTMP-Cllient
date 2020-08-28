package com.esona.webcamcloud.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.esona.webcamcloud.R
import com.esona.webcamcloud.data.BaseEvent
import com.esona.webcamcloud.data.EventEnum
import com.esona.webcamcloud.data.Settings
import com.esona.webcamcloud.databinding.FragmentSettingsBinding
import com.esona.webcamcloud.util.Utils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class FragmentSettings : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var lang= 0
    private var fps: Int= 15

    private val TAG= FragmentMain::class.java.simpleName

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val settings= Utils.loadSettings(requireContext())
        with(binding){
            textViewResolution.text= settings.resolution
            textViewLang.text= if(settings.lang== 0) getString(R.string.eng) else getString(R.string.rus)
            textViewRate.text= "${settings.rate}"
            editTextLogin.setText(settings.login)
            editTextPassword.setText(settings.password)
            editTextPort.setText("${settings.port}")
            fps= settings.rate
            btnInc.setOnClickListener{
                fps= Math.min(fps++, 60)
                textViewRate.text= "$fps"
            }
            btnInc.setOnClickListener{
                fps= Math.max(fps--, 1)
                textViewRate.text= "$fps"
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding= null
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: BaseEvent){
        if(event.type== EventEnum.MAIN) {
            with(binding) {
                val login =
                    if (editTextLogin.text.isEmpty()) "admin" else editTextLogin.text.toString()
                val password =
                    if (editTextPassword.text.isEmpty()) "admin" else editTextPassword.text.toString()
                val port =
                    if (editTextPort.text.isEmpty()) 1935 else Integer.parseInt(editTextPort.text.toString())
                val settings = Settings(
                    textViewResolution.text.toString(),
                    fps,
                    login,
                    password,
                    port,
                    lang
                )
                Utils.storeSettings(settings, requireContext())
                Utils.sendSettings(settings)
            }
        }
    }
}