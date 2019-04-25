package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fuxy.cookeasy.entity.Recipe

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipe")
    fun getAll(): List<Recipe>

    @Query("SELECT * FROM recipe LIMIT :pageSize OFFSET :offset")
    fun getPage(offset: Int, pageSize: Int): List<Recipe>

    @Insert
    fun insert(vararg recipe: Recipe)
}