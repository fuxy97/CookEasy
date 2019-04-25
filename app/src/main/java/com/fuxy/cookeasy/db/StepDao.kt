package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Query

@Dao
interface StepDao {
    @Query("SELECT * FROM step WHERE recipe_id = :id")
    fun getByRecipeId(id: Int): List<StepDao>
}