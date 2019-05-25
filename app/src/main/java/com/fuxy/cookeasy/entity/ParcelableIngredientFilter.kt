package com.fuxy.cookeasy.entity

import android.os.Parcel
import android.os.Parcelable

data class ParcelableIngredientFilter(
    val ingredientId: Int,
    val fromIngredientCount: Int,
    val toIngredientCount: Int,
    val unitId: Int,
    val ingredientCountOption: IngredientCountOption
) : Parcelable {
    constructor(parcel: Parcel) : this(
        ingredientId = parcel.readInt(),
        fromIngredientCount = parcel.readInt(),
        toIngredientCount = parcel.readInt(),
        unitId = parcel.readInt(),
        ingredientCountOption = IngredientCountOption.valueOf(parcel.readString()
            ?: IngredientCountOption.EXACTLY.name)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(ingredientId)
        parcel.writeInt(fromIngredientCount)
        parcel.writeInt(toIngredientCount)
        parcel.writeInt(unitId)
        parcel.writeString(ingredientCountOption.name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParcelableIngredientFilter> {
        override fun createFromParcel(parcel: Parcel): ParcelableIngredientFilter {
            return ParcelableIngredientFilter(parcel)
        }

        override fun newArray(size: Int): Array<ParcelableIngredientFilter?> {
            return arrayOfNulls(size)
        }
    }
}