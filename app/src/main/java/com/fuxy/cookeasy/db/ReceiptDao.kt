package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Query
import com.fuxy.cookeasy.entity.Receipt

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipt")
    fun getAll(): List<Receipt>
}