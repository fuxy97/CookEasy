package com.fuxy.cookeasy.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fuxy.cookeasy.s3.BucketImageObject
import java.time.LocalTime

@Entity(tableName = "receipt")
data class Receipt(
    @PrimaryKey val id: Int?,
    val dish: String,
    @ColumnInfo(name = "cooking_time") val cookingTime: LocalTime,
    @ColumnInfo(name = "bucket_image_absolute_path") val bucketImage: BucketImageObject
)