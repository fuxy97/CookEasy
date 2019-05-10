package com.fuxy.cookeasy.fragment

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.db.SimpleSQLiteQuery
import com.fuxy.cookeasy.*
import com.fuxy.cookeasy.adapter.RecipeAdapter
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.db.LocalTimeConverter
import com.fuxy.cookeasy.entity.IngredientCountOption
import com.fuxy.cookeasy.entity.ParcelableIngredientFilter
import com.fuxy.cookeasy.entity.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalTime
import java.lang.StringBuilder
import kotlin.math.ceil

class RecipesFragment : Fragment() {

    companion object {
        @JvmField
        val FRAGMENT_NAME = "fragment_recipe"
        @JvmField
        val FILTER_RECIPES_REQUEST = 101
        @JvmField
        val EXTRA_FILTER_RESULT_INGREDIENTS = "filter_result_ingredients"
        @JvmField
        val EXTRA_FILTER_RESULT_FROM_TIME_HOUR = "filter_result_from_time_hour"
        @JvmField
        val EXTRA_FILTER_RESULT_FROM_TIME_MINUTE = "filter_result_from_time_minute"
        @JvmField
        val EXTRA_FILTER_RESULT_TO_TIME_HOUR = "filter_result_to_time_hour"
        @JvmField
        val EXTRA_FILTER_RESULT_TO_TIME_MINUTE = "filter_result_to_time_minute"
    }

    private var recipeRecyclerView: RecyclerView? = null
    private var progressBar: ProgressBar? = null
    private var searchEditText: EditText? = null
    private var filterButton: Button? = null
    private var sortButton: Button? = null
    private var sortOptionsDialog: AlertDialog? = null
    private var orderColumn: String = "dish"
    private var order: String = "ASC"
    private var query: String? = null
    var adapter: RecipeAdapter? = null
    var recipes: MutableList<Recipe>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_recipes, container, false)

        recipeRecyclerView = view.findViewById(R.id.rv_recipe)
        progressBar = view.findViewById(R.id.pb_recipe_is_loaded)
        //searchView = view.findViewById(R.id.sv_search_bar)
        searchEditText = view.findViewById(R.id.et_search_bar)
        filterButton = view.findViewById(R.id.btn_filter)
        sortButton = view.findViewById(R.id.btn_sort)
        val layoutManager = GridLayoutManager(view.context, 2)
        recipeRecyclerView?.layoutManager = layoutManager

        GlobalScope.launch {
            query = "SELECT * FROM recipe"
            withContext(Dispatchers.IO) {
                val recipeDao = AppDatabase.getInstance(view.context)!!.recipeDao()
                recipes = recipeDao.rawQuery(
                    SimpleSQLiteQuery("$query ORDER BY $orderColumn $order LIMIT ? OFFSET ?",
                        arrayOf(10, 0))).toMutableList()

                launch(Dispatchers.Main) {
                    adapter = RecipeAdapter(this@RecipesFragment, recipes!!)
                    recipeRecyclerView?.adapter = adapter
                }
            }
        }

        recipeRecyclerView?.addOnScrollListener(object : EndlessRecyclerViewScrollListener(layoutManager) {
            override fun fetchData(nextItem: Int) {
                progressBar?.visibility = View.VISIBLE

                GlobalScope.launch {
                    withContext(Dispatchers.IO) {
                        val recipe = AppDatabase.getInstance(context!!)?.recipeDao()
                        val newRecipes = recipe!!.rawQuery(SimpleSQLiteQuery(
                            "$query ORDER BY $orderColumn $order LIMIT ? OFFSET ?", arrayOf(10, nextItem)))
                        //recipes?.clear()
                        recipes?.addAll(newRecipes)
                    }

                    withContext(Dispatchers.Main) {
                        adapter?.notifyItemRangeInserted(nextItem, 10)
                        progressBar?.visibility = View.GONE
                    }
                }
            }

        })

        searchEditText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchEditText?.text.toString()
                if (query.isNotEmpty()) {
                    this@RecipesFragment.query = "SELECT * FROM recipe " +
                            "WHERE lower(dish) LIKE '%${query.toLowerCase()}%'"
                    GlobalScope.launch {
                        runQueryAndUpdateAdapter(0, 10)
                    }
                } else {
                    this@RecipesFragment.query = "SELECT * FROM recipe"
                    GlobalScope.launch {
                        runQueryAndUpdateAdapter(0, 10)
                    }
                }

                searchEditText?.clearFocus()
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(0, 0)
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

/*
        searchEditText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchEditText?.clearFocus()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                return
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                return
            }

        })
*/

        //searchView?.isSubmitButtonEnabled = true

/*        val closeButton: View? = searchView?.findViewById(
            resources.getIdentifier("android:id/search_close_btn", null, null))

        closeButton?.setOnClickListener {
            this@RecipesFragment.query = "SELECT * FROM recipe"
            GlobalScope.launch(Dispatchers.IO) {
                runQueryAndUpdateAdapter(0, 10)
            }
        }*/

/*        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    this@RecipesFragment.query = "SELECT * FROM recipe " +
                            "WHERE lower(dish) LIKE '%${query.toLowerCase()}%'"
                    GlobalScope.launch {
                        runQueryAndUpdateAdapter(0, 10)
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })*/

        filterButton?.setOnClickListener {
            val intent = Intent(view.context, FilterActivity::class.java)
            startActivityForResult(intent, FILTER_RECIPES_REQUEST)
        }

        sortOptionsDialog = AlertDialog.Builder(view.context)
            .setTitle(R.string.sort_by)
            .setSingleChoiceItems(R.array.sort_options, 0) { dialog, which ->
                when(which) {
                    0 -> {
                        orderColumn = "dish"
                        order = "ASC"
                    }
                    1 -> {
                        orderColumn = "dish"
                        order = "DESC"
                    }
                    2 -> {
                        orderColumn = "rating"
                        order = "ASC"
                    }
                    3 -> {
                        orderColumn = "rating"
                        order = "DESC"
                    }
                    4 -> {
                        orderColumn = "cooking_time"
                        order = "ASC"
                    }
                    5 -> {
                        orderColumn = "cooking_time"
                        order = "DESC"
                    }
                }
                GlobalScope.launch {
                    runQueryAndUpdateAdapter(0, 10)
                }
                dialog.dismiss()
            }
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        sortButton?.setOnClickListener {
            sortOptionsDialog?.show()
        }

        return view
    }

    private suspend fun runQueryAndUpdateAdapter(offset: Int, pageSize: Int) {
        withContext(Dispatchers.IO) {
            val recipe = AppDatabase.getInstance(context!!)?.recipeDao()
            val newRecipes = recipe!!.rawQuery(
                SimpleSQLiteQuery("$query ORDER BY $orderColumn $order LIMIT ? OFFSET ?",
                arrayOf(pageSize, offset)))
            recipes?.clear()
            recipes?.addAll(newRecipes)
        }

        withContext(Dispatchers.Main) {
            adapter?.notifyDataSetChanged()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            FILTER_RECIPES_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    val timeFromHour = data?.getIntExtra(EXTRA_FILTER_RESULT_FROM_TIME_HOUR, -1)
                    val timeFromMinute = data?.getIntExtra(EXTRA_FILTER_RESULT_FROM_TIME_MINUTE, -1)
                    val timeToHour = data?.getIntExtra(EXTRA_FILTER_RESULT_TO_TIME_HOUR, -1)
                    val timeToMinute = data?.getIntExtra(EXTRA_FILTER_RESULT_TO_TIME_MINUTE, -1)

                    var timeFrom: LocalTime? = null
                    if (timeFromHour != null && timeFromHour >= 0 && timeFromMinute != null && timeFromMinute >= 0)
                        timeFrom = LocalTime.of(timeFromHour, timeFromMinute)

                    var timeTo: LocalTime? = null
                    if (timeToHour != null && timeToHour >= 0 && timeToMinute != null && timeToMinute >= 0)
                        timeTo = LocalTime.of(timeToHour, timeToMinute)

                    val resultIngredients = data?.getParcelableArrayListExtra<ParcelableIngredientFilter>(
                        EXTRA_FILTER_RESULT_INGREDIENTS)

                    if (resultIngredients != null) {
                        val sb = StringBuilder()

                        val it = resultIngredients.iterator()
                        while (it.hasNext()) {
                            val r = it.next()
                            sb.append("ingredient_id = ", r.ingredientId, " AND ")

                            when (r.ingredientCountOption) {
                                IngredientCountOption.EXACTLY -> {
                                    sb.append("ingredient_count = ", r.toIngredientCount, " AND ")
                                }
                                IngredientCountOption.APPROXIMATELY -> {
                                    sb.append("ingredient_count BETWEEN ",
                                        (r.toIngredientCount - r.toIngredientCount * 0.35).toInt(),
                                        " AND ",
                                        ceil(r.toIngredientCount + r.toIngredientCount * 0.35).toInt(),
                                        " AND "
                                    )
                                }
                                IngredientCountOption.RANGE -> {
                                    sb.append(
                                        "ingredient_count BETWEEN ", r.fromIngredientCount, " AND ",
                                        r.toIngredientCount, " AND "
                                    )
                                }
                            }

                            if (it.hasNext())
                                sb.append("unit_id = ", r.unitId, " OR ")
                            else
                                sb.append("unit_id = ", r.unitId)
                        }

                        query = "SELECT recipe.* FROM " +
                                "(SELECT recipe_id, COUNT(*) AS c FROM recipe_ingredient GROUP BY recipe_id) as l " +
                                "INNER JOIN (SELECT recipe_id, COUNT(*) AS c FROM recipe_ingredient " +
                                "WHERE $sb GROUP BY recipe_id) AS r ON l.recipe_id = r.recipe_id " +
                                "INNER JOIN recipe ON l.recipe_id = recipe.id " +
                                "WHERE l.c = r.c" +
                                if (timeFrom != null) " AND time(recipe.cooking_time) BETWEEN " +
                                        "time('${LocalTimeConverter.fromLocalTime(timeFrom)}')" else {""} +
                                if (timeTo != null) {" AND " + if (timeFrom == null) "time(recipe.cooking_time) = "
                                else {""} +
                                        "time('${LocalTimeConverter.fromLocalTime(timeTo)}')"} else ""

                        GlobalScope.launch {
                            runQueryAndUpdateAdapter(0, 10)
                        }

                    }
                }
            }
            RecipeActivityConstants.GET_RECIPE_STATE_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    val recipeState = RecipeState.valueOf(
                        data?.getStringExtra(RecipeActivityConstants.EXTRA_RECIPE_STATE)!!)
                    val recipeId = data.getIntExtra(RecipeActivityConstants.EXTRA_RECIPE_ID, -1)

                    if (recipeId != -1) {
                        if (recipeState == RecipeState.EDITED) {
                            for (i in 0 until recipes!!.size) {
                                if (recipes!![i].id == recipeId) {

                                    GlobalScope.launch(IO) {
                                        val recipeDao = AppDatabase.getInstance(context!!)?.recipeDao()
                                        val recipe = recipeDao?.getById(recipeId)
                                        recipes!![i] = recipe!!

                                        GlobalScope.launch(Main) {
                                            adapter?.notifyItemChanged(i)
                                        }
                                    }

                                    break
                                }
                            }
                        } else {
                            val indexToRemove = recipes!!.indexOfFirst { it.id == recipeId }
                            recipes?.removeAt(indexToRemove)
                            adapter?.notifyItemRemoved(indexToRemove)
                        }
                    }
                }
            }
        }
    }
}