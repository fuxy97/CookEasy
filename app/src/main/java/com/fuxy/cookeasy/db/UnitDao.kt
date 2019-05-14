package com.fuxy.cookeasy.db

import androidx.room.*
import com.fuxy.cookeasy.entity.Unit

@Dao
interface UnitDao {
    @Query("SELECT * FROM unit WHERE id = :id")
    fun getById(id: Int): com.fuxy.cookeasy.entity.Unit

    @Query("SELECT * FROM unit")
    fun getAll(): List<com.fuxy.cookeasy.entity.Unit>

    @Update
    fun update(unit: Unit): Int

    @Query("DELETE FROM unit WHERE id = :id")
    fun deleteById(id: Int): Int

    @Insert
    fun insert(vararg unit: com.fuxy.cookeasy.entity.Unit): List<Long>
}