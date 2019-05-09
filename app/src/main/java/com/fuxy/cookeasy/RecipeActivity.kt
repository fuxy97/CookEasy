package com.fuxy.cookeasy

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.RecipeActivityConstants.EDIT_RECIPE_REQUEST
import com.fuxy.cookeasy.adapter.RecipeIngredientAdapter
import com.fuxy.cookeasy.adapter.StepAdapter
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.db.LocalTimeConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.format.DateTimeFormatter

object RecipeActivityConstants {
    const val EXTRA_RECIPE_ID = "recipe_id"
    const val EXTRA_RECIPE_STATE = "recipe_state"
    const val EDIT_RECIPE_REQUEST = 401
    const val GET_RECIPE_STATE_REQUEST = 402
}

enum class RecipeState { EDITED, DELETED, DEFAULT }

class RecipeActivity : AppCompatActivity() {
    private var dishTextView: TextView? = null
    private var dishImageView: ImageView? = null
    private var descriptionTextView: TextView? = null
    private var cookingTimeTextView: TextView? = null
    private var ingredientsRecyclerView: RecyclerView? = null
    private var stepsRecyclerView: RecyclerView? = null
    private var recipeId: Int = -1
    private var editResultCode: Int = Activity.RESULT_CANCELED
    private var recipeState: RecipeState = RecipeState.DEFAULT
    private val hourMinuteTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H ч. m мин.")
    private val minuteTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("m мин.")
    private var recipeNestedScrollView: NestedScrollView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        var isTitleShowed = false
        supportActionBar?.setDisplayShowTitleEnabled(isTitleShowed)

        dishTextView = findViewById(R.id.tv_dish)
        dishImageView = findViewById(R.id.iv_dish_image)
        descriptionTextView = findViewById(R.id.tv_description)
        ingredientsRecyclerView = findViewById(R.id.rv_ingredients)
        stepsRecyclerView = findViewById(R.id.rv_steps)
        recipeNestedScrollView = findViewById(R.id.nsv_recipe)
        cookingTimeTextView = findViewById(R.id.tv_cooking_time)

        ingredientsRecyclerView?.layoutManager = LinearLayoutManager(this)
        stepsRecyclerView?.layoutManager = LinearLayoutManager(this)

        ingredientsRecyclerView?.isNestedScrollingEnabled = false
        ingredientsRecyclerView?.addItemDecoration(VerticalSpaceItemDecoration(50))

        stepsRecyclerView?.isNestedScrollingEnabled = false

        recipeNestedScrollView?.setOnScrollChangeListener { v: NestedScrollView?, _: Int, scrollY: Int,
                                                            _: Int, _: Int ->
            if (v != null) {
                if (scrollY >= dishTextView!!.measuredHeight + 40) {
                    if (!isTitleShowed) {
                        isTitleShowed = true
                        supportActionBar?.setDisplayShowTitleEnabled(true)
                    }
                } else {
                    if (isTitleShowed) {
                        isTitleShowed = false
                        supportActionBar?.setDisplayShowTitleEnabled(false)
                    }
                }
            }
        }

        recipeId = intent.getIntExtra(RecipeActivityConstants.EXTRA_RECIPE_ID, -1)

        if (recipeId != -1) {
            GlobalScope.launch { loadRecipe() }
        }
    }

    private suspend fun loadRecipe() {
        withContext(Dispatchers.IO) {
            val recipeDao = AppDatabase.getInstance(applicationContext)!!.recipeDao()
            val recipeIngredientDao = AppDatabase.getInstance(applicationContext)!!.recipeIngredientDao()
            val stepDao = AppDatabase.getInstance(applicationContext)!!.stepDao()

            val recipe = recipeDao.getById(recipeId)
            val ingredients = recipeIngredientDao.getByRecipeIdWithIngredientAndUnit(recipeId)
            val steps = stepDao.getByRecipeId(recipeId)

            GlobalScope.launch(Dispatchers.Main) {
                supportActionBar?.title = recipe.dish
                dishTextView?.text = recipe.dish
                dishImageView?.setImageBitmap(recipe.bucketImage.bitmap)
                descriptionTextView?.text = recipe.description

                if (recipe.cookingTime.hour > 0)
                    cookingTimeTextView?.text = hourMinuteTimeFormatter.format(recipe.cookingTime)
                else
                    cookingTimeTextView?.text = minuteTimeFormatter.format(recipe.cookingTime)

                ingredientsRecyclerView?.adapter = RecipeIngredientAdapter(ingredients)
                stepsRecyclerView?.adapter = StepAdapter(steps)
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_recipe, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.recipe_edit -> {
                val intent = Intent(this, EditRecipeActivity::class.java)
                intent.putExtra(EditRecipeActivity.EXTRA_MODE, EditRecipeActivity.Mode.EDITING.name)
                intent.putExtra(EditRecipeActivity.EXTRA_RECIPE_ID, recipeId)
                startActivityForResult(intent, EDIT_RECIPE_REQUEST)
            }
            R.id.recipe_delete -> {
                GlobalScope.launch(Dispatchers.IO) {
                    AppDatabase.getInstance(this@RecipeActivity)?.recipeDao()?.deleteById(recipeId)
                }.invokeOnCompletion {
                    editResultCode = Activity.RESULT_OK
                    recipeState = RecipeState.DELETED
                    val returnIntent = Intent()
                    returnIntent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_ID, recipeId)
                    returnIntent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_STATE, recipeState.name)
                    setResult(editResultCode, returnIntent)
                    finish()
                }
            }
            android.R.id.home -> {
                val returnIntent = Intent()
                returnIntent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_ID, recipeId)
                returnIntent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_STATE, recipeState.name)
                setResult(editResultCode, returnIntent)
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == EDIT_RECIPE_REQUEST && resultCode == Activity.RESULT_OK) {
            recipeState = RecipeState.EDITED
            editResultCode = Activity.RESULT_OK
            GlobalScope.launch { loadRecipe() }
        }
    }

    override fun onBackPressed() {
        val returnIntent = Intent()
        returnIntent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_ID, recipeId)
        returnIntent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_STATE, recipeState.name)
        setResult(editResultCode, returnIntent)
        super.onBackPressed()
    }
}
