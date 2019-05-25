package com.fuxy.cookeasy.dialogfragment

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.fuxy.cookeasy.R
import com.google.android.material.textfield.TextInputLayout

object EditRecordDialogFragmentConstants {
    const val ARGUMENT_TITLE = "title"
    const val ARGUMENT_HINT = "hint"
    const val ARGUMENT_ERROR = "error"
}

abstract class EditRecordDialogFragment : DialogFragment() {
    private var recordValueEditText: EditText? = null
    private var recordValueTextInputLayout: TextInputLayout? = null
    private var errorMessageTextView: TextView? = null
    private var recordValueError: String? = null
    var recordId: Int? = null
    var recordValue: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = activity?.layoutInflater?.inflate(R.layout.dialog_edit_record, null)
        val title = arguments?.getString(EditRecordDialogFragmentConstants.ARGUMENT_TITLE)
        recordValueError = arguments?.getString(EditRecordDialogFragmentConstants.ARGUMENT_ERROR)

        recordValueEditText = view?.findViewById(R.id.et_record_value)
        recordValueTextInputLayout = view?.findViewById(R.id.til_record_value)
        errorMessageTextView = view?.findViewById(R.id.tv_error_message)

        //recordValueEditText?.hint =
        recordValueTextInputLayout?.hint = arguments?.getString(EditRecordDialogFragmentConstants.ARGUMENT_HINT)

        return AlertDialog.Builder(context!!)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> }
            .setNegativeButton(R.string.cancel) { _: DialogInterface, _: Int -> }
            .create()
    }

    override fun onResume() {
        super.onResume()

        if (dialog != null) {
            if (recordValue != null) {
                recordValueEditText?.append(recordValue)
            }

            val button = (dialog as androidx.appcompat.app.AlertDialog).getButton(Dialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                if (recordValueEditText?.text.isNullOrBlank()) {
                    recordValueTextInputLayout?.error = recordValueError
                    errorMessageTextView?.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                recordAction(recordId ?: -1, recordValueEditText?.text.toString())
                dialog?.dismiss()
            }
        }
    }

    abstract fun recordAction(recordId: Int, value: String)
}