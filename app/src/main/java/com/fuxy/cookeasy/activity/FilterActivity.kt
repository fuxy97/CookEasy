package com.fuxy.cookeasy.activity

import android.app.Activity
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.*
import com.fuxy.cookeasy.adapter.IngredientFilterAdapter
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.dialogfragment.AddIngredientDialogFragment
import com.fuxy.cookeasy.dialogfragment.AddIngredientDialogFragmentConstants
import com.fuxy.cookeasy.dialogfragment.AddIngredientDialogMode
import com.fuxy.cookeasy.entity.IngredientFilter
import com.fuxy.cookeasy.entity.ParcelableIngredientFilter
import com.fuxy.cookeasy.fragment.RecipesFragment
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter

object FilterActivityConstants {
    const val ADD_INGREDIENT_DIALOG_TAG = "add_ingredient_dialog"
}

class FilterActivity : AppCompatActivity(), AddIngredientDialogFragment.AddIngredientListener {
    private var ingredientsRecyclerView: RecyclerView? = null
    private var addIngredientButton: Button? = null
    private var applyButton: Button? = null
    private var fromCookingLocalTime: LocalTime? = null
    private var toCookingLocalTime: LocalTime? = null
    private val ingredients: MutableList<IngredientFilter> = mutableListOf()
    private val hourMinuteTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H ч. m мин.")
    private val minuteTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("m мин.")
    private var ingredientsAdapter: IngredientFilterAdapter? = null
    private var addIngredientDialog: AddIngredientDialogFragment? = null
    private var fromTextView: TextView? = null
    private var fromCookingTimeTextView: TextView? = null
    private var toTextView: TextView? = null
    private var toCookingTimeTextView: TextView? = null
    private var timeOptionSpinner: Spinner? = null
    private var errorMessageTextView: TextView? = null
    private var isTimeOptionSwitched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)
        setTitle(R.string.filters)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val fromTimeHour = intent.getIntExtra(RecipesFragment.EXTRA_FILTER_RESULT_FROM_TIME_HOUR, -1)
        val fromTimeMinute = intent.getIntExtra(RecipesFragment.EXTRA_FILTER_RESULT_FROM_TIME_MINUTE, -1)
        val toTimeHour = intent.getIntExtra(RecipesFragment.EXTRA_FILTER_RESULT_TO_TIME_HOUR, -1)
        val toTimeMinute = intent.getIntExtra(RecipesFragment.EXTRA_FILTER_RESULT_TO_TIME_MINUTE, -1)
        val ingredientList =
            intent.getParcelableArrayListExtra<ParcelableIngredientFilter>(
                RecipesFragment.EXTRA_FILTER_RESULT_INGREDIENTS)

        addIngredientDialog = AddIngredientDialogFragment()
        val args = Bundle()
        args.putString(AddIngredientDialogFragmentConstants.ARGUMENT_MODE, AddIngredientDialogMode.FILTER.name)
        addIngredientDialog?.arguments = args

        ingredientsRecyclerView = findViewById(R.id.rv_ingredients)
        addIngredientButton = findViewById(R.id.btn_add_ingredient)
        applyButton = findViewById(R.id.btn_apply)
        fromTextView = findViewById(R.id.tv_from)
        fromCookingTimeTextView = findViewById(R.id.tv_from_cooking_time)
        toTextView = findViewById(R.id.tv_to)
        toCookingTimeTextView = findViewById(R.id.tv_to_cooking_time)
        timeOptionSpinner = findViewById(R.id.sp_time_option)
        errorMessageTextView = findViewById(R.id.tv_error_message)

        timeOptionSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        toTextView?.visibility = View.GONE
                        fromTextView?.visibility = View.GONE
                        if (isTimeOptionSwitched) {
                            fromCookingLocalTime = null
                            fromCookingTimeTextView?.text = resources.getString(R.string.choose_time)
                            toCookingLocalTime = null
                            toCookingTimeTextView?.text = resources.getString(R.string.choose_time)
                        }
                        fromCookingTimeTextView?.setTextColor(resources.getColor(R.color.materialGrey700))
                        fromCookingTimeTextView?.visibility = View.GONE
                        errorMessageTextView?.visibility = View.GONE
                    }
                    1 -> {
                        toTextView?.visibility = View.VISIBLE
                        fromTextView?.visibility = View.VISIBLE
                        fromCookingTimeTextView?.visibility = View.VISIBLE
                        if (isTimeOptionSwitched) {
                            fromCookingLocalTime = null
                            toCookingLocalTime = null
                            toCookingTimeTextView?.text = resources.getString(R.string.choose_time)
                        }
                    }
                }
                isTimeOptionSwitched = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                return
            }
        }

        if (fromTimeHour >= 0 && fromTimeMinute >= 0) {
            timeOptionSpinner?.setSelection(1)
            fromCookingLocalTime = LocalTime.of(fromTimeHour, fromTimeMinute)

            if (fromTimeHour > 0)
                fromCookingTimeTextView?.text = hourMinuteTimeFormatter.format(fromCookingLocalTime)
            else
                fromCookingTimeTextView?.text = minuteTimeFormatter.format(fromCookingLocalTime)
        }

        if (toTimeHour >= 0 && toTimeMinute >= 0) {
            toCookingLocalTime = LocalTime.of(toTimeHour, toTimeMinute)

            if (toTimeHour > 0)
                toCookingTimeTextView?.text = hourMinuteTimeFormatter.format(toCookingLocalTime)
            else
                toCookingTimeTextView?.text = minuteTimeFormatter.format(toCookingLocalTime)
        }

        GlobalScope.launch(IO) {
            val ingredientDao = AppDatabase.getInstance(this@FilterActivity)?.ingredientDao()
            val unitDao = AppDatabase.getInstance(this@FilterActivity)?.unitDao()

            if (ingredientList != null) {
                for (i in ingredientList) {
                    ingredients.add(
                        IngredientFilter(
                            ingredient = ingredientDao?.getById(i.ingredientId),
                            fromIngredientCount = if (i.fromIngredientCount >= 0) i.fromIngredientCount else null,
                            toIngredientCount = i.toIngredientCount,
                            unit = unitDao?.getById(i.unitId)
                        )
                    )
                }
            }

            GlobalScope.launch(Main) {
                ingredientsAdapter = IngredientFilterAdapter(/*this, */ingredients)
                ingredientsRecyclerView?.adapter = ingredientsAdapter
            }
        }
        ingredientsRecyclerView?.layoutManager = LinearLayoutManager(this)
        ingredientsRecyclerView?.addItemDecoration(VerticalSpaceItemDecoration(40))

        val timeErrorDialog = AlertDialog.Builder(this)
            .setTitle(R.string.error)
            .setMessage(R.string.time_error_message)
            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> }
            .create()

        val toTimePickerDialog = TimePickerDialog(this, { _: TimePicker, hourOfDay: Int, minute: Int ->
            val cookingTime = LocalTime.of(hourOfDay, minute)

            if (fromCookingLocalTime == null || cookingTime >= fromCookingLocalTime) {
                toCookingLocalTime = cookingTime

                if (hourOfDay > 0)
                    toCookingTimeTextView?.text = hourMinuteTimeFormatter.format(toCookingLocalTime)
                else
                    toCookingTimeTextView?.text = minuteTimeFormatter.format(toCookingLocalTime)
            } else {
                timeErrorDialog.show()
            }
        }, toTimeHour, toTimeMinute, true)
        toTimePickerDialog.setTitle(R.string.choose_time)
        toCookingTimeTextView?.setOnClickListener {
            toTimePickerDialog.show()
        }

        val fromTimePickerDialog = TimePickerDialog(this, { _: TimePicker, hourOfDay: Int, minute: Int ->
            val cookingTime = LocalTime.of(hourOfDay, minute)

            if (toCookingLocalTime == null || cookingTime <= toCookingLocalTime) {
                fromCookingLocalTime = cookingTime

                if (hourOfDay > 0)
                    fromCookingTimeTextView?.text = hourMinuteTimeFormatter.format(fromCookingLocalTime)
                else
                    fromCookingTimeTextView?.text = minuteTimeFormatter.format(fromCookingLocalTime)
            } else {
                timeErrorDialog.show()
            }
        }, fromTimeHour, fromTimeMinute, true)
        fromTimePickerDialog.setTitle(R.string.choose_time)
        fromCookingTimeTextView?.setOnClickListener {
            fromTimePickerDialog.show()
        }

        addIngredientButton?.setOnClickListener {
            addIngredientDialog?.show(supportFragmentManager,
                FilterActivityConstants.ADD_INGREDIENT_DIALOG_TAG
            )
        }

        applyButton?.setOnClickListener {
/*            for (i in ingredients) {
                if (i.ingredientCount == null) {
                     AlertDialog.Builder(this)
                        .setTitle(R.string.error)
                        .setMessage(R.string.filter_error)
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            dialog.cancel()
                        }
                         .create()
                         .show()
                    return@setOnClickListener
                }
            }*/
            if (timeOptionSpinner?.selectedItemPosition == 1 && fromCookingLocalTime == null
                && toCookingLocalTime != null) {
                errorMessageTextView?.visibility = View.VISIBLE
                fromCookingTimeTextView?.setTextColor(resources.getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            val returnIntent = Intent()
            returnIntent.putExtra(RecipesFragment.EXTRA_FILTER_RESULT_FROM_TIME_HOUR, fromCookingLocalTime?.hour)
            returnIntent.putExtra(RecipesFragment.EXTRA_FILTER_RESULT_FROM_TIME_MINUTE, fromCookingLocalTime?.minute)
            returnIntent.putExtra(RecipesFragment.EXTRA_FILTER_RESULT_TO_TIME_HOUR, toCookingLocalTime?.hour)
            returnIntent.putExtra(RecipesFragment.EXTRA_FILTER_RESULT_TO_TIME_MINUTE, toCookingLocalTime?.minute)
            returnIntent.putParcelableArrayListExtra(RecipesFragment.EXTRA_FILTER_RESULT_INGREDIENTS,
                ArrayList(ingredients.map { it.toParcelable() }) )
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_filters, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item?.itemId) {
            R.id.filters_clear -> {
                fromCookingLocalTime = null
                toCookingLocalTime = null
                fromCookingTimeTextView?.text = resources.getString(R.string.choose_time)
                toCookingTimeTextView?.text = resources.getString(R.string.choose_time)
                ingredients.clear()
                ingredientsAdapter?.notifyDataSetChanged()
                return true
            }
            android.R.id.home -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun addIngredient(dialog: DialogFragment, ingredient: IngredientFilter) {
        ingredients.add(ingredient)
        ingredientsAdapter?.notifyDataSetChanged()
        //ingredientsAdapter?.notifyItemInserted(ingredients.size - 1)
    }



}
