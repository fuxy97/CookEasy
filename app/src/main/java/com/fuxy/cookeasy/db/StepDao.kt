package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fuxy.cookeasy.entity.Step

@Dao
interface StepDao {
    @Query("SELECT * FROM step WHERE recipe_id = :id")
    fun getByRecipeId(id: Int): List<Step>

    @Insert
    fun insert(vararg step: Step): List<Long>

    @Query("INSERT INTO step (recipe_id, step_number, description, step_bucket_image_absolute_path) " +
            "VALUES(:recipeId, :stepNumber, :description, :bucketImagePath)")
    fun insertEditStep(recipeId: Int, stepNumber: Int, description: String,  bucketImagePath: String?): Long

    @Query("UPDATE step SET description = :description " +
            "WHERE recipe_id = :recipeId AND step_number = :stepNumber")
    fun updateByRecipeIdStepNumber(description: String, recipeId: Int, stepNumber: Int): Int

    @Query("UPDATE step SET description = :description, step_bucket_image_absolute_path = :bucketImagePath " +
            "WHERE recipe_id = :recipeId AND step_number = :stepNumber")
    fun updateByRecipeIdStepNumber(description: String, bucketImagePath: String, recipeId: Int, stepNumber: Int): Int

    @Query("DELETE FROM step WHERE recipe_id = :recipeId AND step_number = :stepNumber")
    fun deleteByRecipeIdStepNumber(recipeId: Int, stepNumber: Int): Int
}