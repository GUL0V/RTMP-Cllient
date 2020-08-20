package com.esona.webcamcloud.ui

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import com.esona.webcamcloud.R
import com.esona.webcamcloud.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController= Navigation.findNavController(this, R.id.nav_host_fragment)
        with(binding){
            btnCam.isSelected= true
            textViewLink.text= Html.fromHtml("<a href=http://webcameracloud.com>webcameracloud.com</a>")
            textViewLink.movementMethod = LinkMovementMethod.getInstance()
            btnCam.setOnClickListener{
                btnCam.isSelected= true
                btnSettings.isSelected= false
                switchTranslation.visibility= View.VISIBLE
                if(navController.currentDestination?.id== R.id.fragmentSettings)
                    navController.popBackStack()
            }
            btnSettings.setOnClickListener{
                btnCam.isSelected= false
                btnSettings.isSelected= true
                switchTranslation.visibility= View.GONE
                if(navController.currentDestination?.id== R.id.fragmentMain)
                    navController.navigate(R.id.action_fragmentMain_to_fragmentSettings)
            }
            switchTranslation.setOnCheckedChangeListener { _, b ->
                TODO("Not yet implemented")
            }
        }
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

}
