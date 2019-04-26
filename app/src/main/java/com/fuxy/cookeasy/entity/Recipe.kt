package com.fuxy.cookeasy.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fuxy.cookeasy.s3.BucketImageObject
import org.threeten.bp.LocalTime

@Entity(tableName = "recipe")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val dish: String,
    @ColumnInfo(name = "cooking_time") val cookingTime: LocalTime,
    @ColumnInfo(name = "bucket_image_absolute_path") val bucketImage: BucketImageObject,
    val description: String
)