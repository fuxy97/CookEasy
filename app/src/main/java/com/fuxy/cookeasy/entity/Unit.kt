package com.fuxy.cookeasy.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unit")
data class Unit(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val unit: String
)