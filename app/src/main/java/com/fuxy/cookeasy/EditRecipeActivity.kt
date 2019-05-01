package com.fuxy.cookeasy

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.adapter.EditStepAdapter
import com.fuxy.cookeasy.adapter.IngredientFilterAdapter
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.db.LocalTimeConverter
import com.fuxy.cookeasy.entity.EditStep
import com.fuxy.cookeasy.entity.IngredientFilter
import com.fuxy.cookeasy.entity.Recipe
import com.fuxy.cookeasy.entity.RecipeIngredient
import com.fuxy.cookeasy.s3.BucketImageObject
import com.fuxy.cookeasy.s3.putObjectToBucket
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.threeten.bp.LocalTime

class EditRecipeActivity : AppCompatActivity()  {

    enum class Mode { ADDING, EDITING }

    companion object {
        //@JvmField
        //val FRAGMENT_NAME = "FRAGMENT_ADD_RECIPE"
        @JvmField
        val GET_SINGLE_PHOTO_REQUEST = 201
        @JvmField
        val GET_SINGLE_STEP_PHOTO_REQUEST = 202
        @JvmField
        val EXTRA_MODE = "mode"
        @JvmField
        val EXTRA_RECIPE_ID = "recipe_id"
    }

    private var applyButton: Button? = null
    private var dishEditText: EditText? = null
    private var descriptionEditText: EditText? = null
    private var uploadImageLinearLayout: LinearLayout? = null
    private var dishImageView: ImageView? = null
    private var photoActionsLinearLayout: LinearLayout? = null
    private var changePhotoButton: Button? = null
    private var deletePhotoButton: Button? = null
    private var cookingTimeTextView: TextView? = null
    private var cookingLocalTime: LocalTime? = null
    private var ingredientsRecyclerView: RecyclerView? = null
    private var addIngredientButton: Button? = null
    private val ingredients: MutableList<IngredientFilter> = mutableListOf()
    private var ingredientsBackup: List<RecipeIngredient>? = null
    private var stepsRecyclerView: RecyclerView? = null
    private var addStepButton: Button? = null
    private var deleteStepButton: Button? = null
    private val steps: MutableList<EditStep> = mutableListOf()
    private var stepCountBackup: Int? = null
    private var currentStepImageView: ImageView? = null
    private var currentStep: EditStep? = null
    private var mode: Mode? = null
    private var recipeId: Int? = null
    private var dishImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_recipe)

        mode = Mode.valueOf(intent.getStringExtra(EXTRA_MODE))

        dishEditText = findViewById(R.id.et_dish)
        uploadImageLinearLayout = findViewById(R.id.ll_upload_image)
        dishImageView = findViewById(R.id.iv_dish_image)
        photoActionsLinearLayout = findViewById(R.id.ll_photo_actions)
        changePhotoButton = findViewById(R.id.btn_change_photo)
        deletePhotoButton = findViewById(R.id.btn_delete_photo)
        cookingTimeTextView = findViewById(R.id.tv_cooking_time)
        ingredientsRecyclerView = findViewById(R.id.rv_ingredients)
        addIngredientButton = findViewById(R.id.btn_add_ingredient)
        stepsRecyclerView = findViewById(R.id.rv_steps)
        addStepButton = findViewById(R.id.btn_add_step)
        deleteStepButton = findViewById(R.id.btn_remove_last_step)
        descriptionEditText = findViewById(R.id.et_description)
        applyButton = findViewById(R.id.btn_apply)

        uploadImageLinearLayout?.setOnClickListener {
            startChoosePhotoActivity()
        }

        deletePhotoButton?.setOnClickListener {
            uploadImageLinearLayout?.visibility = View.VISIBLE
            dishImageView?.visibility = View.GONE
            photoActionsLinearLayout?.visibility = View.GONE
        }

        changePhotoButton?.setOnClickListener {
            startChoosePhotoActivity()
        }


        val timePickerDialog = TimePickerDialog(this, { _: TimePicker, hourOfDay: Int, minute: Int ->
            cookingLocalTime = LocalTime.of(hourOfDay, minute)
            cookingTimeTextView?.text = LocalTimeConverter.fromLocalTime(cookingLocalTime!!)
        }, 0, 0, true)
        timePickerDialog.setTitle(R.string.choose_time)
        cookingTimeTextView?.setOnClickListener {
            timePickerDialog.show()
        }

        val ingredientAdapter = IngredientFilterAdapter(this, ingredients)
        ingredientsRecyclerView?.adapter = ingredientAdapter
        ingredientsRecyclerView?.layoutManager = LinearLayoutManager(this)

        addIngredientButton?.setOnClickListener {
            ingredients.add(IngredientFilter())
            ingredientAdapter.notifyItemInserted(ingredients.size - 1)
        }

        val stepAdapter = object : EditStepAdapter(steps) {
            override fun setStepImage(stepImageView: ImageView, step: EditStep) {
                currentStepImageView = stepImageView
                currentStep = step

                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                startActivityForResult(
                    Intent.createChooser(intent, resources.getString(R.string.choose_photo)),
                    GET_SINGLE_STEP_PHOTO_REQUEST
                )
            }

        }
        stepsRecyclerView?.adapter = stepAdapter
        stepsRecyclerView?.layoutManager = LinearLayoutManager(this)

        addStepButton?.setOnClickListener {
            steps.add(EditStep(stepNumber = steps.size + 1))
            stepAdapter.notifyItemInserted(steps.size - 1)
        }

        deleteStepButton?.setOnClickListener {
            steps.removeAt(steps.size - 1)
            stepAdapter.notifyItemRemoved(steps.size)
        }

        applyButton?.setOnClickListener {
            if (mode == Mode.EDITING) {
                if (recipeId != -1) {
                    GlobalScope.launch(IO) {
                        val ingredientDao = AppDatabase.getInstance(this@EditRecipeActivity)
                            ?.recipeIngredientDao()

                        var ingredientIds = ingredients.map { it.id }
                        val ingredientsToRemove = ingredientsBackup?.filter { it.id !in ingredientIds }
                        ingredientDao?.deleteAll(ingredientsToRemove!!)

                        ingredientIds = ingredientsBackup?.map { it.id }!!
                        val ingredientsToAdd = ingredients.filter { it.id !in ingredientIds }
                        ingredientsToAdd.forEach {
                            ingredientDao?.insertByIngredientFilter(
                                recipeId = recipeId!!,
                                ingredientId = it.ingredient?.id!!,
                                ingredientCount = it.ingredientCount!!,
                                unitId = it.unit?.id!!
                            )
                        }

                        val ingredientsToUpdate = ingredients.filter { it.id in ingredientIds }
                        ingredientsToUpdate.forEach {
                            ingredientDao?.updateById(
                                id = it.id!!,
                                recipeId = recipeId!!,
                                ingredientId = it.ingredient?.id!!,
                                ingredientCount = it.ingredientCount!!,
                                unitId = it.unit?.id!!
                            )
                        }

                        val stepDao = AppDatabase.getInstance(this@EditRecipeActivity)?.stepDao()
                        if (steps.size > stepCountBackup!!) {
                            val stepsToAdd = steps.filter { it.stepNumber!! > stepCountBackup!! }
                            stepsToAdd.forEach {
                                var bucketPath: String? = null
                                if (it.imageUri != null) {
                                    bucketPath = putImageToBucketByUri(it.imageUri!!)
                                }
                                stepDao?.insertEditStep(
                                    recipeId = recipeId!!,
                                    stepNumber = it.stepNumber!!,
                                    description = it.description!!,
                                    bucketImagePath = bucketPath
                                )
                            }

                            val stepsToUpdate = steps.filter { it.stepNumber!! <= stepCountBackup!! }
                            stepsToUpdate.forEach {
                                if (it.imageUri != null) {
                                    val bucketPath = putImageToBucketByUri(it.imageUri!!)
                                    stepDao?.updateByRecipeIdStepNumber(
                                        description = it.description!!,
                                        recipeId = recipeId!!,
                                        bucketImagePath = bucketPath,
                                        stepNumber = it.stepNumber!!
                                    )
                                }
                                stepDao?.updateByRecipeIdStepNumber(
                                    description = it.description!!,
                                    recipeId = recipeId!!,
                                    stepNumber = it.stepNumber!!
                                )
                            }
                        } else {
                            for (i in steps.size + 1..stepCountBackup!!) {
                                stepDao?.deleteByRecipeIdStepNumber(recipeId!!, i)
                            }

                            steps.forEach {
                                if (it.imageUri != null) {
                                    val bucketPath = putImageToBucketByUri(it.imageUri!!)
                                    stepDao?.updateByRecipeIdStepNumber(
                                        description = it.description!!,
                                        recipeId = recipeId!!,
                                        bucketImagePath = bucketPath,
                                        stepNumber = it.stepNumber!!
                                    )
                                }
                                stepDao?.updateByRecipeIdStepNumber(
                                    description = it.description!!,
                                    recipeId = recipeId!!,
                                    stepNumber = it.stepNumber!!
                                )
                            }
                        }

                        val recipeDao = AppDatabase.getInstance(this@EditRecipeActivity)?.recipeDao()
                        if (dishImageUri != null) {
                            val bucketPath = putImageToBucketByUri(dishImageUri!!)
                            recipeDao?.updateById(
                                id = recipeId!!,
                                dish = dishEditText?.text.toString(),
                                description = descriptionEditText?.text.toString(),
                                cookingTime = cookingLocalTime!!,
                                bucketImagePath = bucketPath
                            )
                        } else {
                            recipeDao?.updateById(
                                id = recipeId!!,
                                dish = dishEditText?.text.toString(),
                                description = descriptionEditText?.text.toString(),
                                cookingTime = cookingLocalTime!!
                            )
                        }
                    }.invokeOnCompletion {
                        val returnIntent = Intent()
                        returnIntent.putExtra(EXTRA_RECIPE_ID, recipeId)
                        setResult(Activity.RESULT_OK, returnIntent)
                        finish()
                    }
                }
            } else {
                GlobalScope.launch(IO) {
                    val recipeDao = AppDatabase.getInstance(this@EditRecipeActivity)?.recipeDao()
                    val bucketPath = putImageToBucketByUri(dishImageUri!!)
                    recipeId = recipeDao?.insert(
                        dish = dishEditText?.text.toString(),
                        description = descriptionEditText?.text.toString(),
                        cookingTime = cookingLocalTime!!,
                        bucketImagePath = bucketPath
                    )?.toInt()

                    val ingredientDao = AppDatabase.getInstance(this@EditRecipeActivity)?.recipeIngredientDao()
                    ingredients.forEach {
                        ingredientDao?.insertByIngredientFilter(
                            recipeId = recipeId!!,
                            ingredientId = it.ingredient?.id!!,
                            ingredientCount = it.ingredientCount!!,
                            unitId = it.unit?.id!!
                        )
                    }

                    val stepDao = AppDatabase.getInstance(this@EditRecipeActivity)?.stepDao()
                    steps.forEach {
                        var bucketPath: String? = null
                        if (it.imageUri != null) {
                            bucketPath = putImageToBucketByUri(it.imageUri!!)
                        }
                        stepDao?.insertEditStep(
                            recipeId = recipeId!!,
                            description = it.description!!,
                            bucketImagePath = bucketPath,
                            stepNumber = it.stepNumber!!
                        )
                    }
                }.invokeOnCompletion {
                    val returnIntent = Intent()
                    returnIntent.putExtra(EXTRA_RECIPE_ID, recipeId)
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish()
                }
            }
        }

        if (mode == Mode.EDITING) {
            recipeId = intent.getIntExtra(EXTRA_RECIPE_ID, -1)
            applyButton?.text = resources.getString(R.string.edit)

            if (recipeId!! > -1) {
                GlobalScope.launch(IO) {
                    val recipeDao = AppDatabase.getInstance(this@EditRecipeActivity)?.recipeDao()
                    val recipeIngredientDao =
                        AppDatabase.getInstance(this@EditRecipeActivity)?.recipeIngredientDao()
                    val stepDao = AppDatabase.getInstance(this@EditRecipeActivity)?.stepDao()
                    val ingredientDao = AppDatabase.getInstance(this@EditRecipeActivity)?.ingredientDao()
                    val unitDao = AppDatabase.getInstance(this@EditRecipeActivity)?.unitDao()

                    val recipe = recipeDao?.getById(recipeId!!)
                    ingredientsBackup = recipeIngredientDao?.getByRecipeId(recipeId!!)
                    val steps = stepDao?.getByRecipeId(recipeId!!)
                    stepCountBackup = steps?.size

                    if (ingredientsBackup != null) {
                        for (i in ingredientsBackup!!) {
                            ingredients.add(IngredientFilter(
                                ingredient = ingredientDao?.getById(i.ingredientId),
                                ingredientCount = i.ingredientCount,
                                unit = unitDao?.getById(i.unitId),
                                id = i.id
                            ))
                        }
                    }

                    if (steps != null) {
                        for (s in steps) {
                            this@EditRecipeActivity.steps.add(EditStep(
                                stepNumber = s.stepNumber,
                                description = s.description,
                                imageBitmap = s.stepBucketImage?.bitmap
                            ))
                        }
                    }

                    GlobalScope.launch(Main) {
                        dishEditText?.setText(recipe?.dish)
                        descriptionEditText?.setText(recipe?.description)

                        dishImageView?.setImageBitmap(recipe?.bucketImage?.bitmap)
                        uploadImageLinearLayout?.visibility = View.GONE
                        dishImageView?.visibility = View.VISIBLE
                        photoActionsLinearLayout?.visibility = View.VISIBLE

                        cookingLocalTime = recipe?.cookingTime
                        cookingTimeTextView?.text = LocalTimeConverter.fromLocalTime(cookingLocalTime!!)

                        ingredientAdapter.notifyDataSetChanged()
                        stepAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private suspend fun putImageToBucketByUri(imageUri: Uri): String {
        val bytes = contentResolver.openInputStream(imageUri)?.readBytes()
        val bucketPath = "recipes/$recipeId/${imageUri.pathSegments.last()}"

        putObjectToBucket(
            context = this@EditRecipeActivity,
            absolutePath = bucketPath,
            contentType = "image/*",
            bytes = bytes!!
        )

        return bucketPath
    }

    private fun startChoosePhotoActivity() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(
            Intent.createChooser(intent, resources.getString(R.string.choose_photo)),
            GET_SINGLE_PHOTO_REQUEST
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            GET_SINGLE_PHOTO_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    val selectedImageUri = data?.data
                    dishImageView?.setImageURI(null)
                    dishImageView?.setImageURI(selectedImageUri)
                    dishImageUri = selectedImageUri

                    uploadImageLinearLayout?.visibility = View.GONE
                    dishImageView?.visibility = View.VISIBLE
                    photoActionsLinearLayout?.visibility = View.VISIBLE
                }
            }
            GET_SINGLE_STEP_PHOTO_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    val selectedImageUri = data?.data
                    currentStepImageView?.setImageURI(null)
                    currentStepImageView?.setImageURI(selectedImageUri)
                    currentStep?.imageUri = selectedImageUri
                }
            }
        }
    }
}