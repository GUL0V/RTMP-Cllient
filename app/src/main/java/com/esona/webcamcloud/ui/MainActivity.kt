package com.esona.webcamcloud.ui

import android.Manifest
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
import com.esona.webcamcloud.databinding.ActivityMainBinding
import com.esona.webcamcloud.service.CamService
import com.esona.webcamcloud.service.CamServiceJ
import com.esona.webcamcloud.util.Utils
import org.greenrobot.eventbus.EventBus
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks  {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val PERMS = 111

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController= Navigation.findNavController(this, R.id.nav_host_fragment)
        with(binding){
            btnCam.isSelected= true
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
        requestPermissions()
    }

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
