package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.fuxy.cookeasy.entity.IngredientFilter
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

    @Query("DELETE FROM recipe_ingredient WHERE id = :id")
    fun deleteById(id: Int): Int

    @Query("DELETE FROM recipe_ingredient WHERE id IN (:ids)")
    fun deleteByIds(ids: List<Int>): Int

    @Delete
    fun deleteAll(ingredients: List<RecipeIngredient>): Int

    @Query("INSERT INTO recipe_ingredient(recipe_id, ingredient_id, ingredient_count, unit_id) " +
            "VALUES(:recipeId, :ingredientId, :ingredientCount, :unitId)")
    fun insertByIngredientFilter(recipeId: Int, ingredientId: Int, ingredientCount: Int, unitId: Int): Long

    @Query("UPDATE recipe_ingredient " +
            "SET recipe_id = :recipeId, ingredient_id = :ingredientId, ingredient_count = :ingredientCount, " +
            "unit_id = :unitId " +
            "WHERE id = :id")
    fun updateById(id: Int, recipeId: Int, ingredientId: Int, ingredientCount: Int, unitId: Int): Int
}