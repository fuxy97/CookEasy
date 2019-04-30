package com.fuxy.cookeasy

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.adapter.IngredientFilterAdapter
import com.fuxy.cookeasy.entity.IngredientFilter
import com.fuxy.cookeasy.fragment.RecipesFragment

class FilterActivity : AppCompatActivity() {

    private var cookingTimeEditText: EditText? = null
    private var ingredientsRecyclerView: RecyclerView? = null
    private var addIngredientButton: Button? = null
    private var applyButton: Button? = null
    private val ingredients: MutableList<IngredientFilter> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)

        cookingTimeEditText = findViewById(R.id.et_cooking_time)
        ingredientsRecyclerView = findViewById(R.id.rv_ingredients)
        addIngredientButton = findViewById(R.id.btn_add_ingredient)
        applyButton = findViewById(R.id.btn_apply)

        val adapter = IngredientFilterAdapter(this, ingredients)
        ingredientsRecyclerView?.adapter = adapter
        ingredientsRecyclerView?.layoutManager = LinearLayoutManager(this)

        addIngredientButton?.setOnClickListener {
            ingredients.add(IngredientFilter())
            adapter.notifyItemInserted(ingredients.size - 1)
        }

        applyButton?.setOnClickListener {
            for (i in ingredients) {
                if (i.ingredientCount == null) {
                     AlertDialog.Builder(this)
                        .setTitle(R.string.error)
                        .setMessage(R.string.filter_error)
                        .setPositiveButton(R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                            dialog.cancel()
                        })
                        .create()
                         .show()
                    return@setOnClickListener
                }
            }
            val returnIntent = Intent()
            returnIntent.putParcelableArrayListExtra(RecipesFragment.EXTRA_FILTER_RESULT,
                ArrayList(ingredients.map { it.toParcelable() }) )
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }
}
