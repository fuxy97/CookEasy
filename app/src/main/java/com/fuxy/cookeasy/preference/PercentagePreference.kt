package com.fuxy.cookeasy.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.EditTextPreference

class PercentagePreference(context: Context, attrs: AttributeSet) : EditTextPreference(context, attrs) {
    init {
        setOnPreferenceChangeListener { _, newValue ->
            if (newValue.toString().isBlank()) {
                return@setOnPreferenceChangeListener false
            }
            return@setOnPreferenceChangeListener true
        }
    }
}