package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Query
import com.fuxy.cookeasy.entity.RecipeIngredient

@Dao
interface RecipeIngredientDao {
    @Query("SELECT * FROM recipe_ingredient WHERE recipe_id = :id")
    fun getByRecipeId(id: Int): List<RecipeIngredient>
}