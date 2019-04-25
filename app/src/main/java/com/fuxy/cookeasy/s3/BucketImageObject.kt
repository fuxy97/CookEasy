package com.fuxy.cookeasy.s3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

data class BucketImageObject(val absolutePath: String,
                             val bitmap: Bitmap)

suspend fun getImageObjectFromBucket(context: Context, absolutePath: String): BucketImageObject {
    val bytes = getObjectFromBucket(context, absolutePath).bytes()
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    return BucketImageObject(absolutePath, bitmap)
}

