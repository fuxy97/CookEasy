package com.fuxy.cookeasy.entity

import android.os.Parcel
import android.os.Parcelable

data class ParcelableIngredientFilter(
    val ingredientId: Int,
    val ingredientCount: Int,
    val unitId: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(ingredientId)
        parcel.writeInt(ingredientCount)
        parcel.writeInt(unitId)
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