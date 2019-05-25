package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fuxy.cookeasy.entity.Ingredient

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredient WHERE id = :id")
    fun getById(id: Int): Ingredient

    @Query("SELECT * FROM ingredient")
    fun getAll(): List<Ingredient>

    @Insert
    fun insert(vararg ingredient: Ingredient): List<Long>

    @Update
    fun update(ingredient: Ingredient): Int

    @Query("DELETE FROM ingredient WHERE id = :id")
    fun deleteById(id: Int): Int
}