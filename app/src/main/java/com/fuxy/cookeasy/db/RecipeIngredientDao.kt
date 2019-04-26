package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fuxy.cookeasy.entity.RecipeIngredient
import com.fuxy.cookeasy.entity.RecipeIngredientUnitIngredient

@Dao
interface RecipeIngredientDao {
    @Query("SELECT * FROM recipe_ingredient WHERE recipe_id = :id")
    fun getByRecipeId(id: Int): List<RecipeIngredient>

    @Query("SELECT ingredient.ingredient AS ingredient, ingredient_count, unit.unit AS unit " +
            "FROM recipe_ingredient " +
            "INNER JOIN unit ON unit.id = unit_id " +
            "INNER JOIN ingredient ON ingredient.id = ingredient_id " +
            "WHERE recipe_id = :id")
    fun getByRecipeIdWithIngredientAndUnit(id: Int): List<RecipeIngredientUnitIngredient>

    @Insert
    fun insert(vararg recipeIngredient: RecipeIngredient): List<Long>
}