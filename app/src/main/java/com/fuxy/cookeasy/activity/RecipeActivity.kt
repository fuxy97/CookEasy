package com.fuxy.cookeasy.activity

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
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
import com.fuxy.cookeasy.entity.RecipeIngredientUnitIngredient
import com.fuxy.cookeasy.entity.Step
import com.fuxy.cookeasy.isNetworkConnected
import com.fuxy.cookeasy.isOnline
import com.fuxy.cookeasy.preference.PreferenceKeys
import kotlinx.coroutines.*
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.io.FileOutputStream
import java.net.SocketTimeoutException

object RecipeActivityConstants {
    const val EXTRA_RECIPE_ID = "recipe_id"
    const val EXTRA_RECIPE_STATE = "recipe_state"
    const val EDIT_RECIPE_REQUEST = 401
    const val GET_RECIPE_STATE_REQUEST = 402
}

enum class RecipeState { EDITED, DELETED, DEFAULT }

class RecipeActivity : AppCompatActivity() {
    private var dishTypeTextView: TextView? = null
    private var dishTextView: TextView? = null
    private var dishImageView: ImageView? = null
    private var descriptionTextView: TextView? = null
    private var cookingTimeTextView: TextView? = null
    private var caloriesTextView: TextView? = null
    private var servingsTextView: TextView? = null
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
    private var recipeImageBitmap: Bitmap? = null
    private var ingredients: List<RecipeIngredientUnitIngredient>? = null
    private var steps: List<Step>? = null
    private val handleExceptionContext = Dispatchers.Default + CoroutineExceptionHandler { _, e ->
        GlobalScope.launch(Dispatchers.Main) {
            if (e is SocketTimeoutException) {
                recipeNestedScrollView?.visibility = View.GONE
                noConnectionLinearLayout?.visibility = View.VISIBLE
            } else {
                throw e
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        var isTitleShowed = false
        supportActionBar?.setDisplayShowTitleEnabled(isTitleShowed)

        timeout = PreferenceManager.getDefaultSharedPreferences(this)
            ?.getString(PreferenceKeys.KEY_PREF_TIMEOUT, "0")?.toInt()

        dishTypeTextView = findViewById(R.id.tv_dish_type)
        dishTextView = findViewById(R.id.tv_dish)
        dishImageView = findViewById(R.id.iv_dish_image)
        descriptionTextView = findViewById(R.id.tv_description)
        ingredientsRecyclerView = findViewById(R.id.rv_ingredients)
        stepsRecyclerView = findViewById(R.id.rv_steps)
        recipeNestedScrollView = findViewById(R.id.nsv_recipe)
        cookingTimeTextView = findViewById(R.id.tv_cooking_time)
        caloriesTextView = findViewById(R.id.tv_calories)
        servingsTextView = findViewById(R.id.tv_servings)
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
                if (scrollY >= dishTextView!!.measuredHeight + dishTypeTextView!!.measuredHeight + 80) {
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
            GlobalScope.launch(handleExceptionContext) {
                if (isNetworkConnected(this@RecipeActivity) && isOnline(timeout!!)) {
                    loadRecipe()
                } else {
                    withContext(Dispatchers.Main) {
                        recipeNestedScrollView?.visibility = View.GONE
                        noConnectionLinearLayout?.visibility = View.VISIBLE
                    }
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
            GlobalScope.launch(handleExceptionContext) {
                withContext(Dispatchers.Main) { retryButton?.isEnabled = false }

                if (isNetworkConnected(this@RecipeActivity) && isOnline(timeout!!)) {
                    withContext(Dispatchers.Main) {
                        recipeNestedScrollView?.visibility = View.VISIBLE
                        noConnectionLinearLayout?.visibility = View.GONE
                    }
                    loadRecipe()
                } else {
                    withContext(Dispatchers.Main) {
                        recipeNestedScrollView?.visibility = View.GONE
                        noConnectionLinearLayout?.visibility = View.VISIBLE
                    }
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
            val dishTypeDao = AppDatabase.getInstance(applicationContext)!!.dishTypeDao()

            val recipe = recipeDao.getById(recipeId)
            ingredients = recipeIngredientDao.getByRecipeIdWithIngredientAndUnit(recipeId)
            steps = stepDao.getByRecipeId(recipeId)
            val dishType = dishTypeDao.getById(recipe.dishTypeId)

            GlobalScope.launch(Dispatchers.Main) {
                supportActionBar?.title = recipe.dish
                dishTypeTextView?.text = dishType.dishType
                dishTextView?.text = recipe.dish
                recipeImageBitmap = recipe.bucketImage.bitmap
                dishImageView?.setImageBitmap(recipeImageBitmap)
                descriptionTextView?.text = recipe.description
                caloriesTextView?.text = "${recipe.calories} " + when {
                    recipe.calories == 1 -> "калория"
                    recipe.calories in 2..4 -> "калории"
                    else -> "калорий"
                }
                servingsTextView?.text = "${recipe.servings} " + when {
                    recipe.servings == 1 -> "персона"
                    recipe.servings in 2..4 -> "персоны"
                    else -> "персон"
                }
                ratingBar?.rating = recipe.rating

                if (recipe.cookingTime.hour > 0)
                    cookingTimeTextView?.text = hourMinuteTimeFormatter.format(recipe.cookingTime)
                else
                    cookingTimeTextView?.text = minuteTimeFormatter.format(recipe.cookingTime)

                ingredientsRecyclerView?.adapter = RecipeIngredientAdapter(ingredients!!)
                stepsRecyclerView?.adapter = StepAdapter(steps!!)
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_recipe, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.recipe_share -> {
                val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" +
                    System.currentTimeMillis() + ".jpg")
                val out = FileOutputStream(file)
                recipeImageBitmap?.compress(Bitmap.CompressFormat.JPEG, 50, out)
                out.close()
                val bitmapUri = Uri.fromFile(file)

                val shareText = "${dishTextView?.text}\n\n" +
                        "${caloriesTextView?.text}\n${servingsTextView?.text}\n" +
                        "Время приготовления: ${cookingTimeTextView?.text}\n\n" +
                        ingredients?.fold("", {s, it ->
                            s + "${it.ingredient} - ${it.ingredientCount} ${it.unit}\n"
                        }) + "\n" +
                        steps?.fold("", {s, it ->
                            s + "${it.stepNumber}. ${it.description}\n"
                        })

                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_SUBJECT, dishTextView?.text.toString())
                intent.putExtra(Intent.EXTRA_TEXT, shareText)
                intent.putExtra(Intent.EXTRA_STREAM, bitmapUri)
                startActivity(Intent.createChooser(intent, resources.getString(R.string.share_using)))
            }
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
