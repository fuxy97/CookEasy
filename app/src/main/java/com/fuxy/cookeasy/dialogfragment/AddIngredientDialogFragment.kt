package com.fuxy.cookeasy.dialogfragment

import android.app.Activity
import android.app.Dialog
import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.entity.Ingredient
import com.fuxy.cookeasy.entity.IngredientCountOption
import com.fuxy.cookeasy.entity.IngredientFilter
import com.fuxy.cookeasy.entity.Unit
import com.google.android.material.textfield.TextInputLayout
import com.toptoche.searchablespinnerlibrary.SearchableSpinner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

enum class AddIngredientDialogMode { FILTER, DEFAULT }

object AddIngredientDialogFragmentConstants {
    const val ARGUMENT_MODE = "mode"
}

class AddIngredientDialogFragment : DialogFragment() {
    private var ingredientSearchableSpinner: SearchableSpinner? = null
    private var unitSearchableSpinner: SearchableSpinner? = null
    private var errorMessageTextView: TextView? = null
    private var listener: AddIngredientListener? = null
    private var fromTextView: TextView? = null
    private var fromIngredientCountEditText: EditText? = null
    private var fromIngredientCountTextInputLayout: TextInputLayout? = null
    private var toTextView: TextView? = null
    private var toIngredientCountEditText: EditText? = null
    private var toIngredientCountTextInputLayout: TextInputLayout? = null
    private var ingredientCountOptionSpinner: Spinner? = null
    private var mode: AddIngredientDialogMode =
        AddIngredientDialogMode.DEFAULT
    private var ingredientCountOption: IngredientCountOption = IngredientCountOption.EXACTLY

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = activity?.layoutInflater?.inflate(R.layout.dialog_add_ingredient, null)

        mode = AddIngredientDialogMode.valueOf(
            arguments?.getString(AddIngredientDialogFragmentConstants.ARGUMENT_MODE) ?:
            AddIngredientDialogMode.DEFAULT.name)

        ingredientSearchableSpinner = view?.findViewById(R.id.sp_ingredient)
        toTextView = view?.findViewById(R.id.tv_to)
        toIngredientCountEditText = view?.findViewById(R.id.et_to_ingredient_count)
        toIngredientCountTextInputLayout = view?.findViewById(R.id.til_to_ingredient_count)
        unitSearchableSpinner = view?.findViewById(R.id.sp_unit)
        errorMessageTextView = view?.findViewById(R.id.tv_error_message)
        fromTextView = view?.findViewById(R.id.tv_from)
        fromIngredientCountEditText = view?.findViewById(R.id.et_from_ingredient_count)
        fromIngredientCountTextInputLayout = view?.findViewById(R.id.til_from_ingredient_count)
        ingredientCountOptionSpinner = view?.findViewById(R.id.sp_ingredient_count_option)

        unitSearchableSpinner?.setTitle(context?.resources?.getString(R.string.select_unit))
        unitSearchableSpinner?.setPositiveButton(context?.resources?.getString(R.string.ok))

        ingredientSearchableSpinner?.setTitle(context?.resources?.getString(R.string.select_ingredient))
        ingredientSearchableSpinner?.setPositiveButton(context?.resources?.getString(R.string.ok))

        ingredientCountOptionSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        fromTextView?.visibility = View.GONE
                        fromIngredientCountTextInputLayout?.visibility = View.GONE
                        fromIngredientCountTextInputLayout?.isErrorEnabled = false
                        fromIngredientCountEditText?.text?.clear()
                        toTextView?.visibility = View.GONE
                        ingredientCountOption = IngredientCountOption.EXACTLY
                    }
                    1 -> {
                        fromTextView?.visibility = View.GONE
                        fromIngredientCountTextInputLayout?.visibility = View.GONE
                        fromIngredientCountTextInputLayout?.isErrorEnabled = false
                        fromIngredientCountEditText?.text?.clear()
                        toTextView?.visibility = View.GONE
                        ingredientCountOption = IngredientCountOption.APPROXIMATELY
                    }
                    2 -> {
                        fromTextView?.visibility = View.VISIBLE
                        fromIngredientCountTextInputLayout?.visibility = View.VISIBLE
                        toTextView?.visibility = View.VISIBLE
                        ingredientCountOption = IngredientCountOption.RANGE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                return
            }
        }

        if (mode == AddIngredientDialogMode.DEFAULT) {
            ingredientCountOptionSpinner?.visibility = View.GONE
        }

        GlobalScope.launch(Dispatchers.IO) {
            val ingredients = AppDatabase.getInstance(context!!)?.ingredientDao()?.getAll()
            val units = AppDatabase.getInstance(context!!)?.unitDao()?.getAll()

            launch(Dispatchers.Main) {
                ingredientSearchableSpinner?.adapter =
                    ArrayAdapter(context!!, android.R.layout.simple_list_item_1, ingredients!!)
                unitSearchableSpinner?.adapter = ArrayAdapter(context!!, android.R.layout.simple_list_item_1, units!!)
            }
        }

        return AlertDialog.Builder(context!!)
            .setTitle(R.string.add_ingredient)
            .setView(view)
            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> }
            .setNegativeButton(R.string.cancel) { _: DialogInterface, _: Int -> }
            .create()
    }

    override fun onResume() {
        super.onResume()

        if (dialog != null) {
            val button = (dialog as AlertDialog).getButton(Dialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                when (ingredientCountOption) {
                    IngredientCountOption.EXACTLY, IngredientCountOption.APPROXIMATELY -> {
                        val text  = toIngredientCountEditText?.text.toString()

                        if (text.isEmpty()) {
                            toIngredientCountTextInputLayout?.error = resources
                                .getString(R.string.enter_ingredient_count_error)
                            errorMessageTextView?.visibility = View.VISIBLE
                            return@setOnClickListener
                        }
                    }
                    IngredientCountOption.RANGE -> {
                        val toText = toIngredientCountEditText?.text.toString()
                        val fromText = fromIngredientCountEditText?.text.toString()

                        if (toText.isEmpty()) {
                            toIngredientCountTextInputLayout?.error = resources
                                .getString(R.string.enter_ingredient_count_error)
                            errorMessageTextView?.visibility = View.VISIBLE
                            return@setOnClickListener
                        }

                        if (fromText.isEmpty()) {
                            fromIngredientCountTextInputLayout?.error = resources
                                .getString(R.string.enter_ingredient_count_error)
                            errorMessageTextView?.visibility = View.VISIBLE
                            return@setOnClickListener
                        }
                    }
                }

                val ingredient: Ingredient = ingredientSearchableSpinner?.selectedItem as Ingredient
                val unit: Unit = unitSearchableSpinner?.selectedItem as Unit

                val fromText = fromIngredientCountEditText?.text?.toString()

                listener?.addIngredient(this,
                    IngredientFilter(
                        ingredient = ingredient,
                        fromIngredientCount = if (fromText.isNullOrEmpty()) null else fromText.toInt(),
                        toIngredientCount = toIngredientCountEditText?.text?.toString()?.toInt(),
                        unit = unit,
                        ingredientCountOption = ingredientCountOption
                    )
                )
                dialog?.dismiss()
            }
        }
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (activity is AddIngredientListener) {
            listener = activity
        } else {
            throw ClassCastException("$activity must implement AddIngredientListener")
        }
    }

    interface AddIngredientListener {
        fun addIngredient(dialog: DialogFragment, ingredient: IngredientFilter)
    }
}