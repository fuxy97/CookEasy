package com.fuxy.cookeasy.db


import androidx.room.TypeConverter
import com.fuxy.cookeasy.s3.BucketImageObject
import com.fuxy.cookeasy.s3.getImageObjectFromBucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object BucketImageObjectConverter {

    @TypeConverter
    @JvmStatic
    fun fromBucketImageObject(value: BucketImageObject): String {
        return value.absolutePath
    }

    @TypeConverter
    @JvmStatic
    fun toBucketImageObject(value: String): BucketImageObject = runBlocking {
        return@runBlocking withContext(Dispatchers.Default) { getImageObjectFromBucket(AppDatabase.context, value) }
    }
}