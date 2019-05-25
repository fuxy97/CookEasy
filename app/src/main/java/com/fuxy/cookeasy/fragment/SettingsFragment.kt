package com.fuxy.cookeasy.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.preference.*

const val DIALOG_FRAGMENT_TAG = "android.support.v7.preference.PreferenceFragment.DIALOG"

class SettingsFragment : PreferenceFragmentCompat() {
    companion object {
        @JvmField
        val FRAGMENT_NAME = "FRAGMENT_SETTINGS"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        //val deviationPreference: EditTextPreference? = findPreference(PreferenceKeys.KEY_PREF_DEVIATION)
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater?,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        return recyclerView
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (fragmentManager?.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return
        }

        var fragment: DialogFragment? = null

        when (preference) {
            is PercentagePreference -> fragment = PercentagePreferenceDialog.newInstance(preference.key)
            is PositiveNumberPreference -> fragment = PositiveNumberPreferenceDialog.newInstance(preference.key)
            else -> super.onDisplayPreferenceDialog(preference)
        }

        if (fragment != null) {
            fragment.setTargetFragment(this, 0)
            fragment.show(fragmentManager!!, DIALOG_FRAGMENT_TAG)
        }
    }

    /*    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //ViewCompat.setNestedScrollingEnabled(listView, true)
    }*/


    /*    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }*/
}