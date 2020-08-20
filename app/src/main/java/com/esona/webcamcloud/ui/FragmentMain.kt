package com.esona.webcamcloud.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.esona.webcamcloud.R
import com.esona.webcamcloud.databinding.FragmentMainBinding

class FragmentMain : Fragment() {

    private var binding: FragmentMainBinding? = null

    private lateinit var navController: NavController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        navController= Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)


        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding= null
    }
}