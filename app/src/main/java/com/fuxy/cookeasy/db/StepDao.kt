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
}