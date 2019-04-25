package com.fuxy.cookeasy.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.ForeignKey.SET_NULL
import androidx.room.PrimaryKey

@Entity(tableName = "recipe_ingredient", foreignKeys = [
    ForeignKey(entity = Ingredient::class,
        parentColumns = ["id"],
        childColumns = ["ingredient_id"],
        onDelete = CASCADE),
    ForeignKey(entity = Unit::class,
        parentColumns = ["id"],
        childColumns = ["unit_id"],
        onDelete = SET_NULL)])
data class RecipeIngredient(
    @PrimaryKey val id: Int? = null,
    @ColumnInfo(name = "recipe_id") val recipeId: Int,
    @ColumnInfo(name = "ingredient_id") val ingredientId: Int,
    @ColumnInfo(name = "ingredient_count") val ingredientCount: Int,
    @ColumnInfo(name = "unit_id") val unitId: Int
)