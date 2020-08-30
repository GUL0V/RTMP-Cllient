package com.esona.webcamcloud.ui

import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.esona.webcamcloud.R
import com.esona.webcamcloud.data.BaseEvent
import com.esona.webcamcloud.data.EventEnum
import com.esona.webcamcloud.data.Settings
import com.esona.webcamcloud.databinding.FragmentSettingsBinding
import com.esona.webcamcloud.util.Utils
import com.pedro.rtplibrary.rtsp.RtspCamera1
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class FragmentSettings : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var settings: Settings
    private var fps: Int= 15

    private val TAG= FragmentMain::class.java.simpleName

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        settings= Utils.loadSettings(requireContext())
        val cam= RtspCamera1(requireContext(), null)
        val resolutions= if(settings.camera== 0) cam.resolutionsBack else cam.resolutionsFront
        val strResolutions= resolutions.map {
            "${it.width}x${it.height}"
        }.toTypedArray()

        with(binding){
            textViewResolution.text= strResolutions[settings.resolution]
            textViewLang.text= if(settings.lang== 0) getString(R.string.eng) else getString(R.string.rus)
            textViewRate.text= "${settings.rate}"
            editTextLogin.setText(settings.login)
            editTextPassword.setText(settings.password)
            editTextPort.setText("${settings.port}")
            fps= settings.rate
            btnInc.setOnClickListener{
                fps= (++fps).coerceAtMost(60)
                textViewRate.text= "$fps"
            }
            btnDec.setOnClickListener{
                fps= (--fps).coerceAtLeast(1)
                textViewRate.text= "$fps"
            }
            textViewLang.setOnClickListener{
                val dialog= AlertDialog.Builder(requireContext())
                    .setSingleChoiceItems(arrayOf(getString(R.string.eng), getString(R.string.rus)), settings.lang,
                        DialogInterface.OnClickListener {
                                di, i ->
                            settings.lang= i
                            textViewLang.text= if(i== 0) getString(R.string.eng) else getString(R.string.rus)
                            di.dismiss()
                        })
                dialog.create().show()

            }

            textViewResolution.setOnClickListener{
                val dialog= AlertDialog.Builder(requireContext())
                    .setSingleChoiceItems(strResolutions, 0,
                        DialogInterface.OnClickListener {
                                di, i ->
                            settings.resolution= i
                            textViewResolution.text= strResolutions[i]
                            di.dismiss()
                        })
                dialog.create().show()
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
                var port =
                    if (editTextPort.text.isEmpty()) 1935 else Integer.parseInt(editTextPort.text.toString())
                if(port< 1024){
                    port= 1935
                    (activity as MainActivity).showMessage(
                        getString(R.string.error),
                        getString(R.string.err_port)
                    )
                }
                val settingsNew = Settings(
                    settings.resolution,
                    fps,
                    login,
                    password,
                    port,
                    settings.lang
                )
                Utils.storeSettings(settingsNew, requireContext())
                Utils.sendSettings(settingsNew)
            }
        }
    }

    private fun changeLocale(lang: String){
        val locale= Locale(lang)
        //Log.e("Lan",session.getLanguage());
        //Log.e("Lan",session.getLanguage());
        val config = Configuration(requireContext().resources.configuration)
        Locale.setDefault(locale)
        config.setLocale(locale)

        activity?.baseContext?.resources?.updateConfiguration(
            config,
            requireContext().resources.displayMetrics
        )    }
}

