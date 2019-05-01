package com.fuxy.cookeasy.db


import androidx.room.TypeConverter
import com.fuxy.cookeasy.s3.BucketImageObject
import com.fuxy.cookeasy.s3.getImageObjectFromBucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter


object BucketImageObjectConverter {
    @TypeConverter
    @JvmStatic
    fun fromBucketImageObject(value: BucketImageObject?): String? {
        return value?.absolutePath
    }

    @TypeConverter
    @JvmStatic
    fun toBucketImageObject(value: String?): BucketImageObject? = runBlocking {
        if (value != null) {
            return@runBlocking withContext(Dispatchers.Default) { getImageObjectFromBucket(AppDatabase.context, value) }
        } else {
            return@runBlocking null
        }
    }
}

object LocalTimeConverter {
    private val dtf = DateTimeFormatter.ofPattern("HH:mm")

    @TypeConverter
    @JvmStatic
    fun fromLocalTime(value: LocalTime): String {
        return value.format(dtf)
    }

    @TypeConverter
    @JvmStatic
    fun toLocalTime(value: String): LocalTime {
        return LocalTime.parse(value, dtf)
    }
}