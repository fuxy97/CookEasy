package com.fuxy.cookeasy.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredient")
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val ingredient: String
) {
    override fun toString(): String {
        return ingredient
    }
}