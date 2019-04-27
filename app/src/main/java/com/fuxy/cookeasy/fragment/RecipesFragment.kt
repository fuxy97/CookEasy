package com.fuxy.cookeasy.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.EndlessRecyclerViewScrollListener
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.adapter.RecipeAdapter
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.entity.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipesFragment : Fragment() {

    companion object {
        @JvmField
        val FRAGMENT_NAME = "fragment_recipe"
    }

    private var recipeRecyclerView: RecyclerView? = null
    private var progressBar: ProgressBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_recipes, container, false)

        recipeRecyclerView = view.findViewById(R.id.rv_recipe)
        progressBar = view.findViewById(R.id.pb_recipe_is_loaded)
        val layoutManager = GridLayoutManager(view.context, 2)
        recipeRecyclerView?.layoutManager = layoutManager

        var adapter: RecipeAdapter? = null
        var recipes: MutableList<Recipe>? = null

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val recipeDao = AppDatabase.getInstance(view.context)!!.recipeDao()
                recipes = recipeDao.getPage(0, 10).toMutableList()

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
                        val recipeDao = AppDatabase.getInstance(view.context)!!.recipeDao()
                        recipes?.addAll(recipeDao.getPage(nextItem, 10))

                        launch(Dispatchers.Main) {
                            adapter?.notifyItemInserted(nextItem)
                            progressBar?.visibility = View.GONE
                        }
                    }
                }
            }

        })

        return view
    }


}