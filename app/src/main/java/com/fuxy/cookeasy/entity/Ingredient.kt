package com.fuxy.cookeasy.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredient")
data class Ingredient(
    @PrimaryKey val id: Int? = null,
    val ingredient: String
)