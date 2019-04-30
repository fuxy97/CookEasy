package com.fuxy.cookeasy.fragment

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.db.SimpleSQLiteQuery
import com.fuxy.cookeasy.EndlessRecyclerViewScrollListener
import com.fuxy.cookeasy.FilterActivity
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.adapter.RecipeAdapter
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.entity.ParcelableIngredientFilter
import com.fuxy.cookeasy.entity.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.StringBuilder

class RecipesFragment : Fragment() {

    companion object {
        @JvmField
        val FRAGMENT_NAME = "fragment_recipe"
        @JvmField
        val FILTER_RECIPES_REQUEST = 1
        @JvmField
        val EXTRA_FILTER_RESULT = "filter_result"
    }

    private var recipeRecyclerView: RecyclerView? = null
    private var progressBar: ProgressBar? = null
    private var searchView: SearchView? = null
    private var filterButton: ImageView? = null
    private var sortButton: ImageView? = null
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
        searchView = view.findViewById(R.id.sv_search_bar)
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
                    adapter = RecipeAdapter(recipes!!)
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
                            "$query ORDER BY $orderColumn $order LIMIT ? OFFSET ?", arrayOf(nextItem, 10)))
                        recipes?.clear()
                        recipes?.addAll(newRecipes)
                    }

                    withContext(Dispatchers.Main) {
                        adapter?.notifyItemRangeInserted(nextItem, 10)
                        progressBar?.visibility = View.GONE
                    }
                }
            }

        })

        searchView?.isSubmitButtonEnabled = true

        val closeButton: View? = searchView?.findViewById(
            resources.getIdentifier("android:id/search_close_btn", null, null))

        closeButton?.setOnClickListener {
            this@RecipesFragment.query = "SELECT * FROM recipe"
            GlobalScope.launch(Dispatchers.IO) {
                runQueryAndUpdateAdapter(0, 10)
            }
        }

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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
        })

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
                        orderColumn = "dish"
                        order = "ASC"
                    }
                    3 -> {
                        orderColumn = "dish"
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
        if (requestCode == FILTER_RECIPES_REQUEST && resultCode == RESULT_OK) {
            val result = data?.getParcelableArrayListExtra<ParcelableIngredientFilter>(EXTRA_FILTER_RESULT)

            if (result != null) {
                val sb = StringBuilder()

                val it = result.iterator()
                while (it.hasNext()) {
                    val r = it.next()
                    sb.append("ingredient_id = ", r.ingredientId, " AND ")
                    sb.append("ingredient_count = ", r.ingredientCount, " AND ")

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
                        "WHERE l.c = r.c"

                GlobalScope.launch {
                    runQueryAndUpdateAdapter(0, 10)
                }

            }
        }
    }
}