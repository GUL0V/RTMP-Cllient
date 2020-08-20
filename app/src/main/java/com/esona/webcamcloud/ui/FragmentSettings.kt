package com.esona.webcamcloud.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.esona.webcamcloud.databinding.FragmentMainBinding
import com.esona.webcamcloud.databinding.FragmentSettingsBinding

class FragmentSettings : Fragment() {

    private var binding: FragmentSettingsBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view= binding!!.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding= null
    }
}