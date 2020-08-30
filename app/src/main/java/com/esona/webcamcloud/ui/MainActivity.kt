package com.esona.webcamcloud.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.esona.webcamcloud.R
import com.esona.webcamcloud.data.BaseEvent
import com.esona.webcamcloud.data.EventEnum
import com.esona.webcamcloud.data.Settings
import com.esona.webcamcloud.databinding.ActivityMainBinding
import com.esona.webcamcloud.service.CamService
import com.esona.webcamcloud.util.Utils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks  {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val PERMS = 111

    override fun attachBaseContext(newBase: Context?) {
        val settings= Utils.loadSettings(newBase!!)
        val lang= if(settings.lang== 0) "en" else "ru"
        super.attachBaseContext(Utils.applyLang(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController= Navigation.findNavController(this, R.id.nav_host_fragment)
        with(binding){
            if(Utils.loadBoolean(this@MainActivity, "settingsStarted")) {
                btnSettings.isSelected = true
                // todo switch to settings fragment
            }
            else btnCam.isSelected= true
            // todo check whether service is started


            textViewLink.text= Html.fromHtml("<a href=http://webcameracloud.com>webcameracloud.com</a>")
            textViewLink.movementMethod = LinkMovementMethod.getInstance()
            btnCam.setOnClickListener{
                EventBus.getDefault().post(BaseEvent(EventEnum.MAIN))
                btnCam.isSelected= true
                btnSettings.isSelected= false
                if(navController.currentDestination?.id== R.id.fragmentSettings)
                    navController.popBackStack()
                switchTranslation.visibility= View.VISIBLE
            }
            btnSettings.setOnClickListener{
                btnCam.isSelected= false
                btnSettings.isSelected= true
                if(navController.currentDestination?.id== R.id.fragmentMain)
                    navController.navigate(R.id.action_fragmentMain_to_fragmentSettings)
                switchTranslation.visibility= View.GONE
            }
            switchTranslation.setOnCheckedChangeListener { _, b ->
                Utils.sendStream(b)
            }
        }
//        EventBus.getDefault().register(this)
        requestPermissions()
    }

    override fun onDestroy() {
//        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

/*
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: BaseEvent) {
        if(event.type== EventEnum.SETTINGS){
            val settings: Settings = event.bundle.getParcelable("settings")!!
            binding.textViewCam.text= if(settings.camera== 0) getString(R.string.main) else  getString(R.string.front)
        }
    }
*/

    fun showMessage(title: String, text: String, listener: View.OnClickListener?= null){
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

    override fun onBackPressed() {
        if(navController.currentDestination?.id== R.id.fragmentMain)
            super.onBackPressed()
    }

    private fun requestPermissions() {
        val perms = arrayOf(
            Manifest.permission.CAMERA, Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO)

        if (EasyPermissions.hasPermissions(this, *perms)) connect()
        else {
            EasyPermissions.requestPermissions( this, "This app needs permissions for its work", PERMS, *perms)
        }

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        showMessage(getString(R.string.error), getString(R.string.perm_error_text),
            View.OnClickListener {
                finish()
            })
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        connect()
    }

    private fun connect(){
        val settings= Utils.loadSettings(this)
        val service= Intent(this, CamService::class.java)
        service.putExtra("settings", settings)
        startService(service)
    }
}
