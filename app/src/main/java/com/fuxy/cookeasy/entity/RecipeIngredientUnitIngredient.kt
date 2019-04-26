package com.fuxy.cookeasy.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
data class RecipeIngredientUnitIngredient(
    val ingredient: String,
    @ColumnInfo(name = "ingredient_count") val ingredientCount: Int,
    val unit: String
)