package com.fuxy.cookeasy

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.adapter.RecipeIngredientAdapter
import com.fuxy.cookeasy.adapter.StepAdapter
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.db.LocalTimeConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object RecipeActivityExtras {
    const val EXTRA_RECIPE_ID = "recipe_id"
}

class RecipeActivity : AppCompatActivity() {
    private var dishTextView: TextView? = null
    private var dishImageView: ImageView? = null
    private var descriptionTextView: TextView? = null
    private var cookingTimeTextView: TextView? = null
    private var ingredientsRecyclerView: RecyclerView? = null
    private var stepsRecyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe)

        dishTextView = findViewById(R.id.tv_dish)
        dishImageView = findViewById(R.id.iv_dish_image)
        descriptionTextView = findViewById(R.id.tv_description)
        cookingTimeTextView = findViewById(R.id.tv_cooking_time)
        ingredientsRecyclerView = findViewById(R.id.rv_ingredients)
        stepsRecyclerView = findViewById(R.id.rv_steps)

        ingredientsRecyclerView?.layoutManager = LinearLayoutManager(this)
        stepsRecyclerView?.layoutManager = LinearLayoutManager(this)

        val recipeId = intent.getIntExtra(RecipeActivityExtras.EXTRA_RECIPE_ID, -1)

        if (recipeId != -1) {
            GlobalScope.launch(Dispatchers.IO) {
                val recipeDao = AppDatabase.getInstance(applicationContext)!!.recipeDao()
                val recipeIngredientDao = AppDatabase.getInstance(applicationContext)!!.recipeIngredientDao()
                val stepDao = AppDatabase.getInstance(applicationContext)!!.stepDao()

                val recipe = recipeDao.getById(recipeId)
                val ingredients = recipeIngredientDao.getByRecipeIdWithIngredientAndUnit(recipeId)
                val steps = stepDao.getByRecipeId(recipeId)

                GlobalScope.launch(Dispatchers.Main) {
                    dishTextView?.text = recipe.dish
                    dishImageView?.setImageBitmap(recipe.bucketImage.bitmap)
                    descriptionTextView?.text = recipe.description
                    cookingTimeTextView?.text = LocalTimeConverter.fromLocalTime(recipe.cookingTime)
                    ingredientsRecyclerView?.adapter = RecipeIngredientAdapter(ingredients)
                    stepsRecyclerView?.adapter = StepAdapter(steps)
                }
            }
        }
    }
}
