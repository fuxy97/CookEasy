package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fuxy.cookeasy.entity.DishType

@Dao
interface DishTypeDao {
    @Query("SELECT * FROM dish_type")
    fun getAll(): List<DishType>

    @Insert
    fun insert(vararg dishType: DishType): List<Long>
}