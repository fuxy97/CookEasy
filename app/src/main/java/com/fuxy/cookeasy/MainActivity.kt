package com.fuxy.cookeasy

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.adapter.RecipeAdapter
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.entity.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private var recipeRecyclerView: RecyclerView? = null
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recipeRecyclerView = findViewById(R.id.rv_recipe)
        progressBar = findViewById(R.id.pb_recipe_is_loaded)
        val layoutManager = GridLayoutManager(this, 2)
        recipeRecyclerView?.layoutManager = layoutManager

        var adapter: RecipeAdapter? = null
        var recipes: MutableList<Recipe>? = null

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val recipeDao = AppDatabase.getInstance(applicationContext)!!.recipeDao()
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
                        val recipeDao = AppDatabase.getInstance(applicationContext)!!.recipeDao()
                        recipes?.addAll(recipeDao.getPage(nextItem, 10))

                        launch(Dispatchers.Main) {
                            adapter?.notifyItemInserted(nextItem)
                            progressBar?.visibility = View.GONE
                        }
                    }
                }
            }

        })


    }
}
