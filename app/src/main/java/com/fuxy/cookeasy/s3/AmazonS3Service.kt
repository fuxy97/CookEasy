package com.fuxy.cookeasy.s3

import android.content.Context
import com.fuxy.cookeasy.BuildConfig
import com.fuxy.cookeasy.SingletonHolder
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class AmazonS3Service private constructor(context: Context) {
    private val retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            })
            .build())
        .baseUrl(HttpUrl.Builder()
                .scheme(AmazonS3Credentials.getInstance(context).protocol)
                .host(AmazonS3Credentials.getInstance(context).host)
                .build())
        .build()
    val api: AmazonS3Api get() = retrofit.create(AmazonS3Api::class.java)

    companion object : SingletonHolder<AmazonS3Service, Context>(::AmazonS3Service)
}