package com.fuxy.cookeasy.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.fuxy.cookeasy.s3.BucketImageObject
import org.threeten.bp.LocalTime

@Entity(tableName = "recipe", foreignKeys = [
    ForeignKey(entity = DishType::class,
        parentColumns = ["id"],
        childColumns = ["dish_type_id"],
        onDelete = ForeignKey.SET_NULL
    )]
)
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val dish: String,
    @ColumnInfo(name = "cooking_time") val cookingTime: LocalTime,
    @ColumnInfo(name = "bucket_image_absolute_path") val bucketImage: BucketImageObject,
    val description: String,
    @ColumnInfo(name = "dish_type_id") val dishTypeId: Int? = null,
    val servings: Int,
    val calories: Int,
    val rating: Float = 0.0f
)
