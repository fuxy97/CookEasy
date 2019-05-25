package com.fuxy.cookeasy.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dish_type")
data class DishType (
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "dish_type") val dishType: String
)