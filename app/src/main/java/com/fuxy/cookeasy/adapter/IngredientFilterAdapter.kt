package com.fuxy.cookeasy.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.entity.Ingredient
import com.fuxy.cookeasy.entity.IngredientCountOption
import com.fuxy.cookeasy.entity.IngredientFilter
import com.fuxy.cookeasy.entity.Unit
import com.toptoche.searchablespinnerlibrary.SearchableSpinner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class IngredientFilterAdapter(/*private val context: Context,*/ val ingredients: MutableList<IngredientFilter>)
    : RecyclerView.Adapter<IngredientFilterAdapter.ViewHolder>() {

/*    private var ingredientAdapter: ArrayAdapter<Ingredient>? = null
    private var unitAdapter: ArrayAdapter<Unit>? = null

    init {
        var ingredients: List<Ingredient>? = null
        var units: List<Unit>? = null

        GlobalScope.launch(Dispatchers.IO) {
            ingredients = AppDatabase.getInstance(context)?.ingredientDao()?.getAll()
            units = AppDatabase.getInstance(context)?.unitDao()?.getAll()

            launch(Dispatchers.Main) {
                ingredientAdapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, ingredients!!)
                unitAdapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, units!!)
            }
        }
    }*/

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ingredient_filter_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return ingredients.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ingredient = ingredients[position]
        holder.ingredientTextView.text =
            "${ingredient.ingredient?.ingredient} " +
                    "${if (ingredient.ingredientCountOption == IngredientCountOption.RANGE)
                        "${ingredient.fromIngredientCount} - " else "" } " +
                    "${ingredient.toIngredientCount} " +
                    "${if (ingredient.ingredientCountOption == IngredientCountOption.APPROXIMATELY)
                        "(приблизительно)" else ""} " +
                    "${ingredient.unit?.unit}"

/*        if (ingredient.unit != null) {
            for (i in 0 until unitAdapter?.count!!) {
                if (unitAdapter?.getItem(i)?.id == ingredient.unit?.id)
                    holder.unitSearchableSpinner.setSelection(i)
            }
        }

        if (ingredient.ingredient != null) {
            for (i in 0 until ingredientAdapter?.count!!) {
                if (ingredientAdapter?.getItem(i)?.id == ingredient.ingredient?.id)
                    holder.ingredientSearchableSpinner.setSelection(i)
            }
        }

        val text = ingredient.ingredientCount?.toString()
        if (text != null)
            holder.ingredientCountEditText.setText(text)*/
    }

    inner class ViewHolder(itemView: View)
        : RecyclerView.ViewHolder(itemView), View.OnClickListener {
/*        val unitSearchableSpinner: SearchableSpinner = itemView.findViewById(R.id.sp_unit)
        val ingredientSearchableSpinner: SearchableSpinner = itemView.findViewById(R.id.sp_ingredient)
        val ingredientCountEditText: EditText = itemView.findViewById(R.id.et_ingredient_count)*/
        val ingredientTextView: TextView = itemView.findViewById(R.id.tv_ingredient)
        private val deleteButton: ImageView = itemView.findViewById(R.id.btn_delete)

        init {
            /*unitSearchableSpinner.setTitle(context.resources.getString(R.string.select_unit))
            unitSearchableSpinner.setPositiveButton(context.resources.getString(R.string.ok))
            unitSearchableSpinner.adapter = unitAdapter
            unitSearchableSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                var currPos: Int = -1

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    return
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (currPos != position) {
                        ingredients[adapterPosition].unit = unitAdapter?.getItem(position)!!
                    }
                    currPos = position
                }

            }

            ingredientSearchableSpinner.setTitle(context.resources.getString(R.string.select_ingredient))
            ingredientSearchableSpinner.setPositiveButton(context.resources.getString(R.string.ok))
            ingredientSearchableSpinner.adapter = ingredientAdapter
            ingredientSearchableSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                var currPos: Int = -1

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    return
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (currPos != position) {
                        ingredients[adapterPosition].ingredient = ingredientAdapter?.getItem(position)!!
                    }
                    currPos = position
                }

            }

            ingredientCountEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val text = s.toString()

                    if (text.isNotEmpty())
                        ingredients[adapterPosition].ingredientCount = text.toInt()
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    return
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    return
                }

            })*/

            deleteButton.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val pos = adapterPosition
            ingredients.removeAt(pos)
            notifyItemRemoved(pos)
        }
    }
}