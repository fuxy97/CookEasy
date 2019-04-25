package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Query
import com.fuxy.cookeasy.entity.Ingredient

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredient WHERE id = :id")
    fun getById(id: Int): Ingredient
}