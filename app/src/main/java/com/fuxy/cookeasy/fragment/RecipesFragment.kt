package com.fuxy.cookeasy.fragment

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.db.SimpleSQLiteQuery
import com.fuxy.cookeasy.*
import com.fuxy.cookeasy.activity.FilterActivity
import com.fuxy.cookeasy.activity.RecipeActivityConstants
import com.fuxy.cookeasy.activity.RecipeState
import com.fuxy.cookeasy.adapter.RecipeAdapter
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.db.LocalTimeConverter
import com.fuxy.cookeasy.entity.IngredientCountOption
import com.fuxy.cookeasy.entity.ParcelableIngredientFilter
import com.fuxy.cookeasy.entity.Recipe
import com.fuxy.cookeasy.preference.PreferenceKeys
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
    private var query: String = "SELECT * FROM recipe"
    private var adapter: RecipeAdapter? = null
    private var recipes: MutableList<Recipe>? = null
    private var recipesNestedScrollView: NestedScrollView? = null
    private var searchOptionsButton: ImageButton? = null
    private var popupMenu: PopupMenu? = null
    private var recipesConstraintLayout: ConstraintLayout? = null
    private var noConnectionLinearLayout: LinearLayout? = null
    private var retryButton: Button? = null
    private var timeout: Int? = null
    private var filterBundle: Bundle? = null
    private var selectedSortOption: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_recipes, container, false)
        timeout = PreferenceManager.getDefaultSharedPreferences(context)
            ?.getString(PreferenceKeys.KEY_PREF_TIMEOUT, "0")?.toInt()

        recipeRecyclerView = view.findViewById(R.id.rv_recipe)
        progressBar = view.findViewById(R.id.pb_recipe_is_loaded)
        //searchView = view.findViewById(R.id.sv_search_bar)
        searchEditText = view.findViewById(R.id.et_search_bar)
        filterButton = view.findViewById(R.id.btn_filter)
        sortButton = view.findViewById(R.id.btn_sort)
        recipesNestedScrollView = view.findViewById(R.id.nsv_recipes)
        searchOptionsButton = view.findViewById(R.id.btn_search_options)
        recipesConstraintLayout = view.findViewById(R.id.cl_recipes)
        noConnectionLinearLayout = view.findViewById(R.id.ll_no_connection)
        retryButton = view.findViewById(R.id.btn_retry)
        val layoutManager = GridLayoutManager(view.context, 2)
        recipeRecyclerView?.layoutManager = layoutManager
        popupMenu = PopupMenu(context, searchOptionsButton)

        popupMenu?.inflate(R.menu.popupmenu_search_options)
        popupMenu?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.search_options_sort -> {
                    sortOptionsDialog?.show()
                    return@setOnMenuItemClickListener true
                }
                R.id.search_options_filters -> {
                    val intent = Intent(view.context, FilterActivity::class.java)
                    startActivityForResult(intent, FILTER_RECIPES_REQUEST)
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener false
            }
        }

        GlobalScope.launch {
            if (isNetworkConnected(context!!) && isOnline(timeout!!)) {
                withContext(Dispatchers.IO) {
                    val recipeDao = AppDatabase.getInstance(view.context)!!.recipeDao()
                    recipes = recipeDao.rawQuery(
                        SimpleSQLiteQuery(
                            "$query ORDER BY $orderColumn $order LIMIT ? OFFSET ?",
                            arrayOf(10, 0)
                        )
                    ).toMutableList()

                    launch(Dispatchers.Main) {
                        adapter = RecipeAdapter(this@RecipesFragment, recipes!!)
                        recipeRecyclerView?.adapter = adapter
                    }
                }
            } else {
                recipesConstraintLayout?.visibility = View.GONE
                noConnectionLinearLayout?.visibility = View.VISIBLE
            }
        }

        retryButton?.setOnClickListener {
            GlobalScope.launch {
                withContext(Main) { retryButton?.isEnabled = false }

                if (isNetworkConnected(context!!) && isOnline(timeout!!)) {
                    withContext(Main) {
                        recipesConstraintLayout?.visibility = View.VISIBLE
                        noConnectionLinearLayout?.visibility = View.GONE
                    }
                    withContext(IO) {
                        val recipeDao = AppDatabase.getInstance(view.context)!!.recipeDao()
                        val newRecipes = recipeDao.rawQuery(
                            SimpleSQLiteQuery(
                                "$query ORDER BY $orderColumn $order LIMIT ? OFFSET ?",
                                arrayOf(10, 0)
                            )
                        )

                        if (recipes != null) {
                            recipes?.clear()
                            recipes?.addAll(newRecipes)
                            withContext(Dispatchers.Main) {
                                adapter?.notifyDataSetChanged()
                            }
                        } else {
                            withContext(Main) {
                                recipes = newRecipes.toMutableList()
                                adapter = RecipeAdapter(this@RecipesFragment, recipes!!)
                                recipeRecyclerView?.adapter = adapter
                            }
                        }
                    }
                } else {
                    recipesConstraintLayout?.visibility = View.GONE
                    noConnectionLinearLayout?.visibility = View.VISIBLE
                }
            }.invokeOnCompletion {
                GlobalScope.launch(Main) { retryButton?.isEnabled = true }
            }
        }

        recipeRecyclerView?.isNestedScrollingEnabled = false
        var isSearchOptionsButtonShowed = false
        recipesNestedScrollView?.setOnScrollChangeListener { v: NestedScrollView?, _: Int, scrollY: Int,
                                                             _: Int, oldScrollY: Int ->
            if (v?.getChildAt(v.childCount - 1) != null) {
                if (!isSearchOptionsButtonShowed && scrollY > oldScrollY && scrollY >= sortButton!!.measuredHeight) {
                    isSearchOptionsButtonShowed = true
                    searchOptionsButton?.visibility = View.VISIBLE
                }

                if (isSearchOptionsButtonShowed && scrollY < oldScrollY && scrollY <= sortButton!!.measuredHeight) {
                    isSearchOptionsButtonShowed = false
                    searchOptionsButton?.visibility = View.GONE
                }

                if (scrollY > oldScrollY &&
                    scrollY >= (v.getChildAt(0).measuredHeight - v.measuredHeight)) {
                    progressBar?.visibility = View.VISIBLE
                    val recipeBackupSize = recipes!!.size

                    GlobalScope.launch(Dispatchers.IO) {
                        val recipe = AppDatabase.getInstance(context!!)?.recipeDao()
                        val newRecipes = recipe!!.rawQuery(SimpleSQLiteQuery(
                            "$query ORDER BY $orderColumn $order LIMIT ? OFFSET ?", arrayOf(10, recipes!!.size)))
                            //recipes?.clear()
                        recipes?.addAll(newRecipes)
                    }.invokeOnCompletion {
                        if (recipeBackupSize < recipes!!.size)
                            adapter?.notifyItemRangeInserted(recipeBackupSize,
                                recipes!!.size - recipeBackupSize)
                        GlobalScope.launch(Dispatchers.Main) { progressBar?.visibility = View.GONE }
                    }
                }
            }
        }
        /*recipeRecyclerView?.addOnScrollListener(object : EndlessRecyclerViewScrollListener(layoutManager) {
            override fun fetchData(nextItem: Int) {
            }

        })*/

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
            if (filterBundle != null) {
                intent.putExtras(filterBundle!!)
            }
            startActivityForResult(intent, FILTER_RECIPES_REQUEST)
        }

        sortOptionsDialog = AlertDialog.Builder(view.context)
            .setTitle(R.string.sort_by)
            .setSingleChoiceItems(R.array.sort_options, selectedSortOption) { dialog, which ->
                selectedSortOption = which
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

        searchOptionsButton?.setOnClickListener {
            popupMenu?.show()
        }

        return view
    }

    private suspend fun runQueryAndUpdateAdapter(offset: Int, pageSize: Int) {
        if (isNetworkConnected(context!!) && isOnline(timeout!!)) {
            withContext(Dispatchers.IO) {
                val recipe = AppDatabase.getInstance(context!!)?.recipeDao()
                val newRecipes = recipe!!.rawQuery(
                    SimpleSQLiteQuery(
                        "$query ORDER BY $orderColumn $order LIMIT ? OFFSET ?",
                        arrayOf(pageSize, offset)
                    )
                )
                recipes?.clear()
                recipes?.addAll(newRecipes)
            }

            withContext(Dispatchers.Main) {
                adapter?.notifyDataSetChanged()
            }
        } else {
            withContext(Dispatchers.Main) {
                recipesConstraintLayout?.visibility = View.GONE
                noConnectionLinearLayout?.visibility = View.VISIBLE
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            FILTER_RECIPES_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    filterBundle = data?.extras
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

                        if (sb.isNotEmpty()) {
                            query = "SELECT recipe.* FROM " +
                                    "(SELECT recipe_id, COUNT(*) AS c FROM recipe_ingredient GROUP BY recipe_id) as l " +
                                    "INNER JOIN (SELECT recipe_id, COUNT(*) AS c FROM recipe_ingredient " +
                                    "WHERE $sb GROUP BY recipe_id) AS r ON l.recipe_id = r.recipe_id " +
                                    "INNER JOIN recipe ON l.recipe_id = recipe.id " +
                                    "WHERE l.c = r.c" +
                                    if (timeFrom != null) " AND time(recipe.cooking_time) BETWEEN " +
                                            "time('${LocalTimeConverter.fromLocalTime(timeFrom)}')" else {
                                        ""
                                    } +
                                    if (timeTo != null) {
                                        " AND " + if (timeFrom == null) "time(recipe.cooking_time) = "
                                        else {
                                            ""
                                        } +
                                                "time('${LocalTimeConverter.fromLocalTime(timeTo)}')"
                                    } else ""
                        } else {
                            query = "SELECT * FROM recipe " +
                                    if (timeFrom != null) " WHERE time(recipe.cooking_time) BETWEEN " +
                                            "time('${LocalTimeConverter.fromLocalTime(timeFrom)}')" else {
                                        ""
                                    } +
                                    if (timeTo != null) {
                                        if (timeFrom == null) {" WHERE time(recipe.cooking_time) = "} else {" AND "} +
                                                "time('${LocalTimeConverter.fromLocalTime(timeTo)}')"
                                    } else ""
                        }

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