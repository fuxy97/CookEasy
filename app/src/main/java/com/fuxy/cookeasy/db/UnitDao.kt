package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UnitDao {
    @Query("SELECT * FROM unit WHERE id = :id")
    fun getById(id: Int): com.fuxy.cookeasy.entity.Unit

    @Query("SELECT * FROM unit")
    fun getAll(): List<com.fuxy.cookeasy.entity.Unit>

    @Insert
    fun insert(vararg unit: com.fuxy.cookeasy.entity.Unit): List<Long>
}