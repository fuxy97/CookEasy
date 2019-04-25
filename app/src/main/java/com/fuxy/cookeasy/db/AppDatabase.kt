package com.fuxy.cookeasy.db

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fuxy.cookeasy.entity.Recipe
import com.fuxy.cookeasy.s3.getImageObjectFromBucket
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalTime

@TypeConverters(BucketImageObjectConverter::class, LocalTimeConverter::class)
@Database(entities = [Recipe::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun stepDao(): StepDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun unitDao(): UnitDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao

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
                                getInstance(context)!!.recipeDao()
                                    .insert(
                                        Recipe(
                                            dish = "Салат \"На скорую руку\"",
                                            cookingTime = LocalTime.of(0, 10),
                                            bucketImage = getImageObjectFromBucket(context, "salat-na-skoruyu-ruku.jpg")
                                        ), Recipe(
                                            dish = "Салат \"Маскарад\"",
                                            cookingTime = LocalTime.of(0,25),
                                            bucketImage = getImageObjectFromBucket(context, "salat-maskarad.jpg")
                                        ))
                            }
                        }
                    }
                })
                .build()
        }
    }
}