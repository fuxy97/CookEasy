package com.fuxy.cookeasy.preference

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.view.View
import android.widget.EditText
import androidx.preference.EditTextPreferenceDialogFragmentCompat

class PositiveNumberPreferenceDialog : EditTextPreferenceDialogFragmentCompat() {

    companion object {
        fun newInstance(key: String): PositiveNumberPreferenceDialog {
            val dialog = PositiveNumberPreferenceDialog()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            dialog.arguments = bundle
            return dialog
        }
    }

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)
        val editText: EditText? = view?.findViewById(android.R.id.edit)
        editText?.inputType = InputType.TYPE_CLASS_NUMBER
        editText?.filters = arrayOf(InputFilter { source: CharSequence, start :Int, end: Int,
                                                  dest: Spanned, dstart: Int, dend: Int ->
            val input = source.subSequence(start, end)
            if (input.contains('-'))
                return@InputFilter ""
            return@InputFilter null
        })
        editText?.setSelection(editText.text.length)
    }

}