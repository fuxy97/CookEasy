package com.fuxy.cookeasy.activity

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.VerticalSpaceItemDecoration
import com.fuxy.cookeasy.activity.RecipeActivityConstants.EDIT_RECIPE_REQUEST
import com.fuxy.cookeasy.adapter.RecipeIngredientAdapter
import com.fuxy.cookeasy.adapter.StepAdapter
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.isNetworkConnected
import com.fuxy.cookeasy.isOnline
import com.fuxy.cookeasy.preference.PreferenceKeys
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
    private var ratingBar: RatingBar? = null
    private var isRatingChanged: Boolean = false
    private var removeConfirmationDialog: AlertDialog? = null
    private var noConnectionLinearLayout: LinearLayout? = null
    private var retryButton: Button? = null
    private var timeout: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        var isTitleShowed = false
        supportActionBar?.setDisplayShowTitleEnabled(isTitleShowed)

        timeout = PreferenceManager.getDefaultSharedPreferences(this)
            ?.getString(PreferenceKeys.KEY_PREF_TIMEOUT, "0")?.toInt()

        dishTextView = findViewById(R.id.tv_dish)
        dishImageView = findViewById(R.id.iv_dish_image)
        descriptionTextView = findViewById(R.id.tv_description)
        ingredientsRecyclerView = findViewById(R.id.rv_ingredients)
        stepsRecyclerView = findViewById(R.id.rv_steps)
        recipeNestedScrollView = findViewById(R.id.nsv_recipe)
        cookingTimeTextView = findViewById(R.id.tv_cooking_time)
        ratingBar = findViewById(R.id.rb_rating)
        noConnectionLinearLayout = findViewById(R.id.ll_no_connection)
        retryButton = findViewById(R.id.btn_retry)

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

        ratingBar?.onRatingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { _, _, fromUser ->
                if (fromUser) {
                    isRatingChanged = true
                }
            }

        recipeId = intent.getIntExtra(RecipeActivityConstants.EXTRA_RECIPE_ID, -1)

        if (recipeId != -1) {
            GlobalScope.launch {
                if (isNetworkConnected(this@RecipeActivity) && isOnline(timeout!!)) {
                    loadRecipe()
                } else {
                    recipeNestedScrollView?.visibility = View.GONE
                    noConnectionLinearLayout?.visibility = View.VISIBLE
                }
            }
        }

        removeConfirmationDialog = AlertDialog.Builder(this)
            .setTitle(R.string.remove_dialog_title)
            .setMessage(R.string.remove_dialog_message)
            .setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
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
            .setNegativeButton(R.string.no) { _: DialogInterface, _: Int -> }
            .create()

        retryButton?.setOnClickListener {
            GlobalScope.launch {
                withContext(Dispatchers.Main) { retryButton?.isEnabled = false }

                if (isNetworkConnected(this@RecipeActivity) && isOnline(timeout!!)) {
                    withContext(Dispatchers.Main) {
                        recipeNestedScrollView?.visibility = View.VISIBLE
                        noConnectionLinearLayout?.visibility = View.GONE
                    }
                    loadRecipe()
                } else {
                    recipeNestedScrollView?.visibility = View.GONE
                    noConnectionLinearLayout?.visibility = View.VISIBLE
                }
            }.invokeOnCompletion {
                GlobalScope.launch(Dispatchers.Main) { retryButton?.isEnabled = true }
            }
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
                ratingBar?.rating = recipe.rating

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
                removeConfirmationDialog?.show()
            }
            android.R.id.home -> {
                if (isRatingChanged) {
                    GlobalScope.launch(Dispatchers.IO) {
                        AppDatabase.getInstance(this@RecipeActivity)?.recipeDao()?.changeRatingById(
                            ratingBar!!.rating, recipeId)
                    }.invokeOnCompletion {
                        editResultCode = Activity.RESULT_OK
                        recipeState = RecipeState.EDITED
                        val returnIntent = Intent()
                        returnIntent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_ID, recipeId)
                        returnIntent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_STATE, recipeState.name)
                        setResult(editResultCode, returnIntent)
                        finish()
                    }
                } else {
                    val returnIntent = Intent()
                    returnIntent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_ID, recipeId)
                    returnIntent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_STATE, recipeState.name)
                    setResult(editResultCode, returnIntent)
                    finish()
                }
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
        if (!isRatingChanged) {
            val returnIntent = Intent()
            returnIntent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_ID, recipeId)
            returnIntent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_STATE, recipeState.name)
            setResult(editResultCode, returnIntent)
            super.onBackPressed()
        } else {
            GlobalScope.launch(Dispatchers.IO) {
                AppDatabase.getInstance(this@RecipeActivity)?.recipeDao()?.changeRatingById(
                    ratingBar!!.rating, recipeId)
            }.invokeOnCompletion {
                editResultCode = Activity.RESULT_OK
                recipeState = RecipeState.EDITED
                val returnIntent = Intent()
                returnIntent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_ID, recipeId)
                returnIntent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_STATE, recipeState.name)
                setResult(editResultCode, returnIntent)
                GlobalScope.launch(Dispatchers.Main) { super.onBackPressed() }
            }
        }
    }
}
