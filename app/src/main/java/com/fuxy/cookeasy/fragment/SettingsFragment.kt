package com.fuxy.cookeasy.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fuxy.cookeasy.R

class SettingsFragment : Fragment() {

    companion object {
        @JvmField
        val FRAGMENT_NAME = "FRAGMENT_SETTINGS"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
}