package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fuxy.cookeasy.entity.DishType

@Dao
interface DishTypeDao {
    @Query("SELECT * FROM dish_type")
    fun getAll(): List<DishType>

    @Query("SELECT * FROM dish_type WHERE id = :id")
    fun getById(id: Int): DishType

    @Insert
    fun insert(vararg dishType: DishType): List<Long>

    @Query("DELETE FROM dish_type WHERE id = :id")
    fun deleteById(id: Int): Int

    @Update
    fun update(dishType: DishType): Int
}