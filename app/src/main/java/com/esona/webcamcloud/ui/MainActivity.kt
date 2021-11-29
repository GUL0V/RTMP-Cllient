package com.esona.webcamcloud.ui

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.esona.webcamcloud.R
import com.esona.webcamcloud.databinding.ActivityMainBinding
import com.esona.webcamcloud.service.CamService
import com.esona.webcamcloud.util.ContextWrapper
import com.esona.webcamcloud.util.Utils
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.EasyPermissions
import java.util.*

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, FragmentSettings.OnFragmentCallbacks  {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val PERMS = 111
    private val TAG= MainActivity::class.java.simpleName
    var timer:CountDownTimer? = null;


    override fun changeImage(resourceId: Int) {
        with(binding){
            if(switchTranslation.isChecked){
                Utils.sendStream(false)
                switchTranslation.isChecked = false;
                timer?.cancel()
                Thread.sleep(1000)
                }
            else{
                switchTranslation.isChecked = false;
                timer?.cancel()
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart: ")


    }

    override fun onDestroy() {
        super.onDestroy()
    }


    override fun attachBaseContext(newBase: Context?) {
        val settings= Utils.loadSettings(newBase!!)
        val lang= if(settings.lang== 0) "en" else "ru"
        Log.i(TAG, "activity locale= $lang")
        val context: Context = ContextWrapper.wrap(newBase, Locale(lang))
        super.attachBaseContext(context)
//        super.attachBaseContext(Utils.applyLang(newBase, lang))
    }

    var isible:Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(
                fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?
            ) {
                if (f.javaClass.simpleName.contains("FragmentMain")) {
                    binding.home.performClick()
                } else if (f.javaClass.simpleName.contains("FragmentSettings")) {
                    binding.layoutTop.visibility = View.GONE
                }
            }
        }, true)
//        Utils.applyLang2(this, "en")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController= Navigation.findNavController(this, R.id.nav_host_fragment)
        with(binding){

            val link= getString(R.string.url)
            val text= link.replaceBefore(".", "").substring(1)
            textViewLink.text= Html.fromHtml(String.format("<a href=%s>%s</a>", link, text))
            textViewLink.movementMethod = LinkMovementMethod.getInstance()
            textViewLink.setLinkTextColor(Color.WHITE)
            home.setOnClickListener{
                home.isSelected= true
                settings.isSelected= false
                if(navController.currentDestination?.id== R.id.fragmentSettings)
                    navController.popBackStack()
                layoutTop.visibility= View.VISIBLE
                Utils.storeBoolean(this@MainActivity, "settingsStarted", false)
            }
            settings.setOnClickListener{
                home.isSelected= false
                settings.isSelected= true
                if(navController.currentDestination?.id== R.id.fragmentMain)
                    navController.navigate(R.id.action_fragmentMain_to_fragmentSettings)
                layoutTop.visibility= View.GONE
                Utils.storeBoolean(this@MainActivity, "settingsStarted", true)
            }
            switchTranslation.setOnCheckedChangeListener { _, isChecked ->
                if (!Utils.loadBoolean(this@MainActivity, "streamStarted") && isChecked) {

                    //textViewRtspStatus.text = "Stream started"
                    //textViewRtspStatus.setTextColor(Color.parseColor("#4CAF50"))
                    Utils.sendStream(true)
                    //switchTranslation.isChecked = true
                    connect()
                    Utils.storeBoolean(this@MainActivity, "settingsStarted", true)
                    Log.d("TAGsend", "onCreate: "+Utils.loadBoolean(this@MainActivity, "streamStarted"))

                } else {
                    //textViewRtspStatus.text = "Stream not working"
                    //textViewRtspStatus.setTextColor(Color.parseColor("#F44336"))
                    Utils.sendStream(false)
                    disconnect()
                    //switchTranslation.isChecked = false
                    Utils.storeBoolean(this@MainActivity, "settingsStarted", false)
                    Log.d("TAGsend", "onCreate: "+Utils.loadBoolean(this@MainActivity, "streamStarted"))
                }
            }


            Utils.storeBoolean(this@MainActivity, "settingsStarted", true)


            Log.d("TAGcheckstart", "onCreate:${Utils.keyExists("streamStarted", this@MainActivity)} ${Utils.loadBoolean(this@MainActivity, "streamStarted")}")


        }
        requestPermissions()
        if(Utils.loadBoolean(this, "settingsStarted")) {
            binding.settings.isSelected = true
            binding.home.isSelected= false
            if(navController.currentDestination?.id== R.id.fragmentMain) {
                //switchTranslation.visibility= View.GONE
                navController.navigate(R.id.action_fragmentMain_to_fragmentSettings)
            }
        }
        else binding.home.isSelected= true

    }

    fun showMessage(title: String, text: String, listener: View.OnClickListener? = null){
        val dialog = MessageDialog()
        val bundle = Bundle()
        bundle.putString("title", title)
        bundle.putString("text", text)
        dialog.arguments = bundle
        listener?.let {
            dialog.setOkListener(listener)

        }
        dialog.show(supportFragmentManager, "dialog")
    }

    private fun requestPermissions() {
        val perms = arrayOf(
            Manifest.permission.CAMERA, Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )

        if (EasyPermissions.hasPermissions(this, *perms)) connect()
        else {
            EasyPermissions.requestPermissions(
                this,
                "This app needs permissions for its work",
                PERMS,
                *perms
            )
        }

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        showMessage(getString(R.string.error), getString(R.string.perm_error_text),
            View.OnClickListener {
                finish()
            })
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Log.i(TAG, "---------before connect")
        connect()
    }

    private fun connect(){
        CamService.init(this)
        val serviceIntent= Intent(this, CamService::class.java)
        serviceIntent.putExtra(
            "endpoint",
            "rtmp://media.videosurveillance.cloud:1935/live/10_d67d8ab4f4c10bf22aa353e27879133c?psk=secretPassword"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Log.i(TAG, "---------after connect")
    }
    private fun disconnect(){
        stopService(Intent(applicationContext, CamService::class.java))
        Log.i(TAG, "---------after connect")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @Suppress("DEPRECATION")
    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
