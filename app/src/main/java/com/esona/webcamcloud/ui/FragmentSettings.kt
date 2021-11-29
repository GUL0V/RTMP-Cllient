package com.esona.webcamcloud.ui

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.esona.webcamcloud.R
import com.esona.webcamcloud.data.BaseEvent
import com.esona.webcamcloud.data.EventEnum
import com.esona.webcamcloud.data.Settings
import com.esona.webcamcloud.databinding.FragmentSettingsBinding
import com.esona.webcamcloud.util.Utils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_settings.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class FragmentSettings : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var settings: Settings
    private var fps: Int= 15
    private var auto: Int= 60

    private val TAG= FragmentMain::class.java.simpleName

    override fun onCreateView(
        inflater: LayoutInflater,

        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        settings= Utils.loadSettings(requireContext())

        with(binding){
            textViewLang.text= if(settings.lang== 0) getString(R.string.eng) else getString(R.string.rus)
            textViewRate.text= "${settings.rate}"
            textViewRefresh.text = "${settings.auto}"
            editTextLogin.setText(settings.login)
            editTextPassword.setText(settings.password)
            editTextPort.setText("${settings.port}")
            fps= settings.rate
            auto = settings.auto
            btnInc.setOnClickListener{
                fps= (++fps).coerceAtMost(60)
                textViewRate.text= "$fps"
            }
            btnDec.setOnClickListener{
                fps= (--fps).coerceAtLeast(10)
                textViewRate.text= "$fps"
            }
            btnIncRefresh.setOnClickListener{
                auto= (++auto).coerceAtMost(360)
                textViewRefresh.text= "$auto"
            }
            btnDecRefresh.setOnClickListener{
                auto= (--auto).coerceAtLeast(2)
                textViewRefresh.text= "$auto"
            }
            textViewLang.setOnClickListener{
                val dialog= AlertDialog.Builder(requireContext())
                    .setSingleChoiceItems(arrayOf(getString(R.string.eng), getString(R.string.rus)), settings.lang,
                        DialogInterface.OnClickListener {
                                di, i ->
                            settings.lang= i
                            textViewLang.text= if(i== 0) getString(R.string.eng) else getString(R.string.rus)
                            Utils.storeSettings(settings, requireContext())
                            di.dismiss()

//                            val lang= if(settings.lang== 0) "en" else "ru"
//                            Utils.applyLang2(requireActivity(), lang)

                            activity?.recreate()
                        })
                dialog.create().show()

            }
            textViewSave.setOnClickListener{
                save()
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

    var i = 0;
    var j = 0;
    var saaa = 0;
    var strResolutions4:List<String> = listOf<String>()
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onStickyEvent(event: BaseEvent){
        if(event.type== EventEnum.RESOLUTION) {
            var strResolutions= event.bundle.getStringArray("resolutions")
            var checked= 0;
            if(settings.resolutionW< 0){
                for(i in strResolutions!!.indices){
                    if(strResolutions[i].contains("640 x 480") ||
                        strResolutions[i].contains("480 x 640")) {
                        checked = i
                        Log.d("plamama", "onStickyEvent: "+checked)
                        settings.resolutionW = strResolutions[i].substring(0, strResolutions[i].indexOf(" ")).toInt();
                        settings.resolutionH = strResolutions[i].substring(strResolutions[i].lastIndexOf(" ")+1, strResolutions[i].length).toInt()


                    }
                }
            }
            else{
                for(i in strResolutions!!.indices){
                    if(strResolutions[i].contains("${settings.resolutionW} x ${settings.resolutionH}") || strResolutions[i].contains("${settings.resolutionH} x ${settings.resolutionW}")) {
                        checked = i
                    }
                }
            }


            textViewResolution.text= strResolutions!![checked]

            Log.d("plamama", "onStickyEvent: "+checked)
            with(binding) {
                textViewResolution.setOnClickListener{
                    val dialog= AlertDialog.Builder(requireContext())
                        .setSingleChoiceItems(strResolutions, checked,
                            DialogInterface.OnClickListener {
                                    di, i ->
                                settings.resolutionW = strResolutions[i].substring(0, strResolutions[i].indexOf(" ")).toInt();
                                settings.resolutionH = strResolutions[i].substring(strResolutions[i].lastIndexOf(" ")+1, strResolutions[i].length).toInt()
                                textViewResolution.text= strResolutions[i]
                                di.dismiss()
                            })
                    dialog.create().show()
                }
                settings.resolutionW = strResolutions[checked].substring(0, strResolutions[checked].indexOf(" ")).toInt();
                settings.resolutionH = strResolutions[checked].substring(strResolutions[checked].lastIndexOf(" ")+1, strResolutions[checked].length).toInt()

                Log.d("plamama", "onStickyEvent: $checked" +strResolutions[checked].substring(0, strResolutions[checked].indexOf(" ")).toInt()+"  "+ strResolutions[checked].substring(strResolutions[checked].lastIndexOf(" ")+1, strResolutions[checked].length).toInt() )
            }
        }
    }

    private fun save(){
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
                settings.resolutionW,
                settings.resolutionH,
                fps,
                auto,
                login,
                password,
                port,
                settings.lang
            )
            callbacks?.changeImage(0)
            Utils.storeSettings(settingsNew, requireContext())
            Utils.sendSettings(settingsNew)
            Utils.storeBoolean(requireContext(), "settingsStarted", false)

        }
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).popBackStack()
    }

    var callbacks: OnFragmentCallbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = activity as OnFragmentCallbacks
    }

    interface OnFragmentCallbacks{
        fun changeImage(resourceId: Int)
    }
}

