package com.fuxy.cookeasy.db

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.*
import com.fuxy.cookeasy.SingletonHolder
import com.fuxy.cookeasy.db.AppDatabase.Companion.buildDatabase
import com.fuxy.cookeasy.entity.Receipt

@TypeConverters(BucketImageObjectConverter::class)
@Database(entities = [Receipt::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao

    companion object : SingletonHolder<AppDatabase, Context>({ buildDatabase(it) }) {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        private fun buildDatabase(context: Context): AppDatabase {
            this.context = context

            return Room.databaseBuilder(context.applicationContext,
                AppDatabase::class.java,
                "cookeasy.db")
                .build()
        }
    }
}