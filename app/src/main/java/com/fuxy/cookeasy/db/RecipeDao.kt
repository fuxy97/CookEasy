package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.fuxy.cookeasy.entity.IngredientFilter
import com.fuxy.cookeasy.entity.Recipe
import org.threeten.bp.LocalTime

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipe")
    fun getAll(): List<Recipe>

    @Query("SELECT * FROM recipe LIMIT :pageSize OFFSET :offset")
    fun getPage(offset: Int, pageSize: Int): List<Recipe>

    @Query("SELECT * FROM recipe WHERE lower(dish) LIKE '%' || lower(:dish) || '%' ORDER BY :orderBy ASC LIMIT :pageSize OFFSET :offset")
    fun getPageByDish(offset: Int, pageSize: Int, dish: String, orderBy: String = "dish"): List<Recipe>

    @RawQuery
    fun rawQuery(query: SupportSQLiteQuery): List<Recipe>

    @Query("SELECT * FROM recipe WHERE id = :id")
    fun getById(id: Int): Recipe

    @Insert
    fun insert(vararg recipe: Recipe): List<Long>

    @Query("UPDATE recipe " +
            "SET dish = :dish, cooking_time = :cookingTime, description = :description, " +
            "bucket_image_absolute_path = :bucketImagePath, dish_type_id = :dishTypeId, servings = :servings, " +
            "calories = :calories WHERE id = :id")
    fun updateById(id: Int, dish: String, cookingTime: LocalTime, description: String, bucketImagePath: String,
                   dishTypeId: Int, servings: Int, calories: Int): Int

    @Query("UPDATE recipe " +
            "SET dish = :dish, cooking_time = :cookingTime, description = :description, dish_type_id = :dishTypeId, " +
            "servings = :servings, calories = :calories WHERE id = :id")
    fun updateById(id: Int, dish: String, cookingTime: LocalTime, description: String,
                   dishTypeId: Int, servings: Int, calories: Int): Int

    @Query("INSERT INTO recipe (dish, cooking_time, description, bucket_image_absolute_path, dish_type_id, " +
            "servings, calories, rating) VALUES(:dish, :cookingTime, :description, :bucketImagePath, :dishTypeId, " +
            ":servings, :calories, 0)")
    fun insert(dish: String, cookingTime: LocalTime, description: String, bucketImagePath: String,
               dishTypeId: Int, servings: Int, calories: Int): Long

    @Query("UPDATE recipe SET rating = :rating WHERE id = :id")
    fun changeRatingById(rating: Float, id: Int): Int

    @Query("DELETE FROM recipe WHERE id = :id")
    fun deleteById(id: Int): Int
}