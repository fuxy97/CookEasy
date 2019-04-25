package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Query

@Dao
interface UnitDao {
    @Query("SELECT * FROM unit WHERE id = :id")
    fun getById(id: Int): Unit
}