package com.fuxy.cookeasy.s3

import android.content.Context
import com.fuxy.cookeasy.SingletonHolder
import retrofit2.Retrofit

class AmazonS3Service private constructor(context: Context) {
    private val retrofit = Retrofit.Builder()
        .baseUrl(AmazonS3Credentials.getInstance(context).endPointUrl)
        .build()
    val api: AmazonS3Api? get() = retrofit.create(AmazonS3Api::class.java)

    companion object : SingletonHolder<AmazonS3Service, Context>(::AmazonS3Service)
}