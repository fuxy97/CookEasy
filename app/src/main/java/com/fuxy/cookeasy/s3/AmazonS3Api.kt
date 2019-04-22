package com.fuxy.cookeasy.s3

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface AmazonS3Api {

    @GET("{objectName}")
    fun getObject(@Path("objectName") objectName: String,
                  @Header("Host") host: String,
                  @Header("Date") date: String,
                  @Header("Authorization") authorizationString: String) : Call<ResponseBody>

}