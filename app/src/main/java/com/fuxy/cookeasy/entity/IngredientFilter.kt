package com.fuxy.cookeasy.entity

data class IngredientFilter(
    var ingredient: Ingredient? = null,
    var ingredientCount: Int? = null,
    var unit: Unit? = null
) {
    fun toParcelable(): ParcelableIngredientFilter {
        return ParcelableIngredientFilter(
            ingredient?.id ?: -1,
            ingredientCount ?: -1,
            unit?.id ?: -1)
    }
}