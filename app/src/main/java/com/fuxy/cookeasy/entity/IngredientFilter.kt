package com.fuxy.cookeasy.entity

enum class IngredientCountOption { EXACTLY, APPROXIMATELY, RANGE }

data class IngredientFilter(
    var ingredient: Ingredient? = null,
    var fromIngredientCount: Int? = null,
    var toIngredientCount: Int? = null,
    var unit: Unit? = null,
    var id: Int? = null,
    var ingredientCountOption: IngredientCountOption? = null
) {
    fun toParcelable(): ParcelableIngredientFilter {
        return ParcelableIngredientFilter(
            ingredient?.id ?: -1,
            fromIngredientCount ?: -1,
            toIngredientCount ?: -1,
            unit?.id ?: -1,
            ingredientCountOption ?: IngredientCountOption.EXACTLY)
    }
}