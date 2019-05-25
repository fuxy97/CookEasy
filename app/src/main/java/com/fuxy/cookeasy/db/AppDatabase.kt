package com.fuxy.cookeasy.db

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fuxy.cookeasy.entity.*
import com.fuxy.cookeasy.s3.getImageObjectFromBucket
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalTime

@TypeConverters(BucketImageObjectConverter::class, LocalTimeConverter::class)
@Database(entities = [Recipe::class, Ingredient::class, RecipeIngredient::class, Step::class,
    com.fuxy.cookeasy.entity.Unit::class, DishType::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun stepDao(): StepDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun unitDao(): UnitDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao
    abstract fun dishTypeDao(): DishTypeDao

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        @SuppressLint("StaticFieldLeak")
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase? {
            if (instance == null) {
                synchronized(AppDatabase::class) {
                    instance = buildDatabase(context)
                }
            }
            return instance
        }

        private fun buildDatabase(context: Context): AppDatabase {
            this.context = context

            return Room.databaseBuilder(context.applicationContext,
                AppDatabase::class.java,
                "cookeasy.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)

                        GlobalScope.launch {
                            withContext(IO) {
                                val dishTypeIds = getInstance(context)!!.dishTypeDao()
                                    .insert(
                                        DishType(dishType = "Закуска"),
                                        DishType(dishType = "Салат"),
                                        DishType(dishType = "Первое блюдо"),
                                        DishType(dishType = "Второе блюдо"),
                                        DishType(dishType = "Десерт"),
                                        DishType(dishType = "Напиток")
                                    )

                                val recipeIds = getInstance(context)!!.recipeDao()
                                    .insert(
                                        Recipe(
                                            dish = "Салат \"На скорую руку\"",
                                            cookingTime = LocalTime.of(0, 10),
                                            bucketImage = getImageObjectFromBucket(context, "salat-na-skoruyu-ruku.jpg"),
                                            description = "Название говорит само за себя. Очень вкусный салат и готовится очень быстро. Можно приготивать к любому столу.",
                                            dishTypeId = dishTypeIds[1].toInt(),
                                            servings = 4,
                                            calories = 500
                                        ), Recipe(
                                            dish = "Салат \"Маскарад\"",
                                            cookingTime = LocalTime.of(0,25),
                                            bucketImage = getImageObjectFromBucket(context, "salat-maskarad.jpg"),
                                            description = "Самый праздничный салат для праздничного стола. Салат \"Маскарад\" не останется без внимания ваших гостей и станет ярким гостем на вашем праздничном столе.",
                                            dishTypeId = dishTypeIds[1].toInt(),
                                            servings = 4,
                                            calories = 650
                                        ))

                                val unitIds = getInstance(context)!!.unitDao()
                                    .insert(Unit(unit = "г."),
                                        Unit(unit = "ст. л."))

                                val ingredientIds = getInstance(context)!!.ingredientDao()
                                    .insert(Ingredient(ingredient = "ветчина"),
                                        Ingredient(ingredient = "крабовые палочки"),
                                        Ingredient(ingredient = "помидоры")
                                    )

                                getInstance(context)!!.recipeIngredientDao()
                                    .insert(RecipeIngredient(
                                        recipeId = recipeIds[0].toInt(),
                                        ingredientId = ingredientIds[0].toInt(),
                                        ingredientCount = 200,
                                        unitId = unitIds[0].toInt()
                                    ), RecipeIngredient(
                                        recipeId = recipeIds[0].toInt(),
                                        ingredientId = ingredientIds[1].toInt(),
                                        ingredientCount = 100,
                                        unitId = unitIds[0].toInt()
                                    ), RecipeIngredient(
                                        recipeId = recipeIds[0].toInt(),
                                        ingredientId = ingredientIds[2].toInt(),
                                        ingredientCount = 2,
                                        unitId = unitIds[1].toInt()
                                    ))

                                getInstance(context)!!.stepDao()
                                    .insert(Step(
                                        recipeId = recipeIds[0].toInt(),
                                        stepNumber = 1,
                                        description = "Ветчину нарезать соломкой и сложить в миску.",
                                        stepBucketImage = getImageObjectFromBucket(context, "20151130-salat-maskarad-01.jpg")
                                    ), Step(
                                        recipeId = recipeIds[0].toInt(),
                                        stepNumber = 2,
                                        description = "Добавить зеленый горошек и нарезанные помидоры.",
                                        stepBucketImage = getImageObjectFromBucket(context, "20151130-salat-maskarad-02.jpg")
                                    ))
                            }
                        }
                    }
                })
                .build()
        }
    }
}