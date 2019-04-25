package com.fuxy.cookeasy.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import com.fuxy.cookeasy.s3.BucketImageObject

@Entity(tableName = "step", primaryKeys = ["recipe_id", "step_number"])
@ForeignKey(entity = Recipe::class, parentColumns = ["id"], childColumns = ["recipe_id"], onDelete = CASCADE)
data class Step(
    @ColumnInfo(name = "recipe_id") val recipeId: Int,
    @ColumnInfo(name = "step_number") val stepNumber: Int,
    val description: String,
    @ColumnInfo(name = "step_bucket_image_absolute_path") val stepBucketImage: BucketImageObject
)