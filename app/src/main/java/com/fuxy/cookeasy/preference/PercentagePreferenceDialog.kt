package com.fuxy.cookeasy.preference

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.view.View
import android.widget.EditText
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import java.lang.NumberFormatException

const val MIN_VALUE = 0
const val MAX_VALUE = 100

class PercentagePreferenceDialog : EditTextPreferenceDialogFragmentCompat() {

    companion object {
        fun newInstance(key: String): PercentagePreferenceDialog {
            val dialog = PercentagePreferenceDialog()
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
            try {
                val input = (dest.subSequence(0, dstart).toString() + source.subSequence(start, end)
                        + dest.subSequence(dend, dest.length))
                    .toInt()
                if (input in MIN_VALUE..MAX_VALUE)
                    return@InputFilter null
            } catch (nfe: NumberFormatException) {}
            return@InputFilter ""
        })
    }

}